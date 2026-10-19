"""Quantizes the token embedding of sherpa-onnx's int8 Whisper decoder.

sherpa-onnx's export quantizes only MatMul weights, so the decoder keeps its
token embedding (51864 x 384 for tiny.en, 80 MB) in float32. The table is used
twice: to look up input tokens (Gather) and, transposed, to compute the
output logits (MatMul). This script stores it as int8 with one scale per token
row (symmetric, zero point 0). The rest of the graph is unchanged.

--mode storage
    The int8 table is turned back into float32 by a Cast and a Mul on
    constants, which onnxruntime folds when the model loads. Results and
    speed are those of float32 weights; only the file is smaller.
--mode compute
    The table stays int8 in memory. The lookup dequantizes only the rows it
    gathers; the logits use MatMulInteger with the decoder state quantized on
    the fly (DynamicQuantizeLinear), as the other int8 MatMuls already do.
"""

import argparse
from collections import defaultdict

import numpy as np
import onnx
from onnx import TensorProto, helper, numpy_helper

EMBEDDING = "textDecoder.token_embedding.weight"


def per_row_int8(e):
    scale = np.abs(e).max(axis=1, keepdims=True) / 127.0
    scale[scale == 0] = 1.0
    q = np.clip(np.round(e / scale), -127, 127).astype(np.int8)
    return q, scale.astype(np.float32)


def consumers(graph):
    out = defaultdict(list)
    for node in graph.node:
        for name in node.input:
            out[name].append(node)
    return out


def single(nodes, op_type):
    found = [n for n in nodes if n.op_type == op_type]
    assert len(found) == 1, f"expected one {op_type}, found {[n.name for n in nodes]}"
    return found[0]


def quantize(model, mode):
    g = model.graph
    init = {i.name: i for i in g.initializer}
    e = numpy_helper.to_array(init[EMBEDDING])
    q, scale = per_row_int8(e)
    g.initializer.remove(init[EMBEDDING])
    users = consumers(g)

    if mode == "storage":
        g.initializer.extend([
            numpy_helper.from_array(q, EMBEDDING + "_int8"),
            numpy_helper.from_array(scale, EMBEDDING + "_scale"),
        ])
        # Rebuild the float table under its old name, so its users don't change.
        g.node.insert(0, helper.make_node("Cast", [EMBEDDING + "_int8"], [EMBEDDING + "_cast"], to=TensorProto.FLOAT))
        g.node.insert(1, helper.make_node("Mul", [EMBEDDING + "_cast", EMBEDDING + "_scale"], [EMBEDDING]))
        return

    assert mode == "compute", mode
    lookup = single(users[EMBEDDING], "Gather")
    identity = single(users[EMBEDDING], "Identity")
    logits_matmul = single(users[identity.output[0]], "MatMul")
    states_t = logits_matmul.input[1]  # decoder states, transposed to [B, D, N]
    transpose_in = next(n for n in g.node if states_t in n.output)
    assert transpose_in.op_type == "Transpose" and list(transpose_in.attribute[0].ints) == [0, 2, 1]
    assert len(users[states_t]) == 1
    transpose_out = single(users[logits_matmul.output[0]], "Transpose")
    assert list(transpose_out.attribute[0].ints) == [0, 2, 1]
    states = transpose_in.input[0]  # [B, N, D]
    logits_bnv = transpose_out.output[0]

    p = "/quant_embedding"
    g.initializer.extend([
        numpy_helper.from_array(np.ascontiguousarray(q.T), p + "/table_t"),  # [D, V] int8
        numpy_helper.from_array(scale, p + "/row_scale"),  # [V, 1]
        numpy_helper.from_array(scale.reshape(-1), p + "/col_scale"),  # [V]
    ])
    new_nodes = [
        # Lookup: gather columns of the [D, V] table, then back to [B, N, D].
        helper.make_node("Gather", [p + "/table_t", lookup.input[1]], [p + "/rows_t"], axis=1),
        helper.make_node("Transpose", [p + "/rows_t"], [p + "/rows_q"], perm=[1, 2, 0]),
        helper.make_node("Cast", [p + "/rows_q"], [p + "/rows_f"], to=TensorProto.FLOAT),
        helper.make_node("Gather", [p + "/row_scale", lookup.input[1]], [p + "/rows_scale"], axis=0),
        helper.make_node("Mul", [p + "/rows_f", p + "/rows_scale"], [lookup.output[0]]),
        # Logits: states [B, N, D] x table [D, V], in int8.
        helper.make_node("DynamicQuantizeLinear", [states], [p + "/x_q", p + "/x_scale", p + "/x_zero"]),
        helper.make_node("MatMulInteger", [p + "/x_q", p + "/table_t", p + "/x_zero"], [p + "/acc"]),
        helper.make_node("Cast", [p + "/acc"], [p + "/acc_f"], to=TensorProto.FLOAT),
        helper.make_node("Mul", [p + "/acc_f", p + "/x_scale"], [p + "/acc_x"]),
        helper.make_node("Mul", [p + "/acc_x", p + "/col_scale"], [logits_bnv]),
    ]
    for node in [lookup, identity, logits_matmul, transpose_in, transpose_out]:
        g.node.remove(node)
    # Nodes must stay in topological order: the lookup goes first (it reads only
    # graph inputs and initializers), the logits part last, followed by the
    # Cast that turns it into the "logits" output.
    final_cast = single(users[logits_bnv], "Cast")
    g.node.remove(final_cast)
    for i, node in enumerate(new_nodes[:5]):
        g.node.insert(i, node)
    g.node.extend(new_nodes[5:] + [final_cast])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", required=True, help="sherpa-onnx tiny.en-decoder.int8.onnx")
    ap.add_argument("--output", required=True)
    ap.add_argument("--mode", choices=["storage", "compute"], required=True)
    args = ap.parse_args()
    model = onnx.load(args.input)
    quantize(model, args.mode)
    onnx.checker.check_model(model)
    onnx.save(model, args.output)


if __name__ == "__main__":
    main()
