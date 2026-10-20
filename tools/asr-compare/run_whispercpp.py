"""Runs whisper.cpp's whisper-cli over the test clips, in the same result format as run_asr.py.

    python run_whispercpp.py --cli build/bin/whisper-cli --model ggml-tiny.en.bin \
        --audio WORK/audio --threads 6 --config whispercpp-tiny.en --out WORK/results

Each clip is one whisper-cli process with greedy decoding (-bs 1 -bo 1), so
"decode_s" is whisper-cli's own "total time" minus its model load time, and
"load_s" is the model load time. Peak memory is the largest whisper-cli
process. Run it under the CPU architecture you want to measure; a cross-built
x86_64 whisper-cli runs under Rosetta on Apple Silicon.

--audio-ctx N shrinks the encoder window (whisper.cpp always encodes 30 s
otherwise); 650 frames is about 13 s, which is what sherpa-onnx encodes for a
3 s clip with its default tail padding.
"""

import argparse
import json
import re
import resource
import statistics
import subprocess
import time
from pathlib import Path

TIMING = re.compile(r"whisper_print_timings:\s+(\w+) time\s*=\s*([0-9.]+) ms")


def transcribe(cli, model, wav, threads, audio_ctx=0):
    start = time.perf_counter()
    cmd = [cli, "-m", model, "-f", str(wav), "-t", str(threads), "-bs", "1", "-bo", "1", "-nt", "-l", "en"]
    if audio_ctx:
        cmd += ["-ac", str(audio_ctx)]
    proc = subprocess.run(cmd, capture_output=True, text=True, check=True)
    wall = time.perf_counter() - start
    timings = {k: float(v) / 1000 for k, v in TIMING.findall(proc.stderr)}
    text = " ".join(line.strip() for line in proc.stdout.splitlines() if line.strip())
    return text, timings, wall


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cli", required=True)
    ap.add_argument("--model", required=True)
    ap.add_argument("--audio", required=True)
    ap.add_argument("--threads", type=int, default=4)
    ap.add_argument("--config", required=True, help="run name, e.g. whispercpp-base.en-x64")
    ap.add_argument("--audio-ctx", type=int, default=0)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    audio_dir = Path(args.audio)
    manifest = [json.loads(line) for line in open(audio_dir / "manifest.jsonl")]
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    loads = []
    with open(out_dir / f"{args.config}-t{args.threads}.jsonl", "w") as f:
        for row in manifest:
            text, timings, wall = transcribe(args.cli, args.model, audio_dir / row["clip"], args.threads, args.audio_ctx)
            loads.append(timings.get("load", 0.0))
            decode = timings["total"] - timings.get("load", 0.0) if "total" in timings else wall
            f.write(json.dumps({"clip": row["clip"], "text": text, "decode_s": round(decode, 4), "wall_s": round(wall, 4)}) + "\n")

    peak_child_mb = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss / 1e6
    summary = {
        "config": args.config,
        "threads": args.threads,
        "files": {args.model: Path(args.model).stat().st_size},
        "load_s": round(statistics.median(loads), 3),
        "first_decode_s": None,
        "rss_before_mb": 0.0,
        "rss_after_load_mb": None,
        "rss_peak_mb": round(peak_child_mb, 1),
    }
    (out_dir / f"{args.config}-t{args.threads}.summary.json").write_text(json.dumps(summary, indent=1))
    print(json.dumps(summary))


if __name__ == "__main__":
    main()
