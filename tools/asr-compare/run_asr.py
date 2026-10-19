"""Runs one model over the test clips and records transcripts, timing and memory.

Run each model in its own process so peak memory belongs to that model:

    python run_asr.py --models WORK/models --audio WORK/audio --config whisper-int8 --out WORK/results
"""

import argparse
import json
import resource
import time
from pathlib import Path

import numpy as np
import sherpa_onnx
import soundfile as sf

WHISPER = "sherpa-onnx-whisper-tiny.en"
MOONSHINE_V1 = "sherpa-onnx-moonshine-tiny-en-int8"
MOONSHINE_V2 = "sherpa-onnx-moonshine-tiny-en-quantized-2026-02-27"


def whisper(directory, encoder, decoder, tail_paddings=-1):
    files = [f"{directory}/{encoder}", f"{directory}/{decoder}", f"{directory}/tiny.en-tokens.txt"]

    def build(root, threads):
        return sherpa_onnx.OfflineRecognizer.from_whisper(
            encoder=str(root / files[0]), decoder=str(root / files[1]), tokens=str(root / files[2]),
            num_threads=threads, tail_paddings=tail_paddings,
        )

    return files, build


def moonshine_v1():
    d = MOONSHINE_V1
    files = [f"{d}/preprocess.onnx", f"{d}/encode.int8.onnx", f"{d}/uncached_decode.int8.onnx",
             f"{d}/cached_decode.int8.onnx", f"{d}/tokens.txt"]

    def build(root, threads):
        p = [str(root / f) for f in files]
        return sherpa_onnx.OfflineRecognizer.from_moonshine(
            preprocessor=p[0], encoder=p[1], uncached_decoder=p[2], cached_decoder=p[3], tokens=p[4],
            num_threads=threads,
        )

    return files, build


def moonshine_v2():
    d = MOONSHINE_V2
    files = [f"{d}/encoder_model.ort", f"{d}/decoder_model_merged.ort", f"{d}/tokens.txt"]

    def build(root, threads):
        p = [str(root / f) for f in files]
        return sherpa_onnx.OfflineRecognizer.from_moonshine_v2(
            encoder=p[0], decoder=p[1], tokens=p[2], num_threads=threads,
        )

    return files, build


CONFIGS = {
    "whisper-fp32": whisper(WHISPER, "tiny.en-encoder.onnx", "tiny.en-decoder.onnx"),
    "whisper-int8": whisper(WHISPER, "tiny.en-encoder.int8.onnx", "tiny.en-decoder.int8.onnx"),
    "whisper-int8-tp50": whisper(WHISPER, "tiny.en-encoder.int8.onnx", "tiny.en-decoder.int8.onnx", 50),
    "whisper-fq-storage": whisper("whisper-fq-storage", "tiny.en-encoder.int8.onnx", "tiny.en-decoder.int8.onnx"),
    "whisper-fq-storage-tp50": whisper("whisper-fq-storage", "tiny.en-encoder.int8.onnx", "tiny.en-decoder.int8.onnx", 50),
    "whisper-fq-compute": whisper("whisper-fq-compute", "tiny.en-encoder.int8.onnx", "tiny.en-decoder.int8.onnx"),
    "whisper-fq-compute-tp50": whisper("whisper-fq-compute", "tiny.en-encoder.int8.onnx", "tiny.en-decoder.int8.onnx", 50),
    "whisper-fq-compute-tp300": whisper("whisper-fq-compute", "tiny.en-encoder.int8.onnx", "tiny.en-decoder.int8.onnx", 300),
    "whisper-fq-compute-tp3000": whisper("whisper-fq-compute", "tiny.en-encoder.int8.onnx", "tiny.en-decoder.int8.onnx", 3000),
    "moonshine-tiny-v1-int8": moonshine_v1(),
    "moonshine-tiny-v2-quantized": moonshine_v2(),
}


def peak_rss_mb():
    # ru_maxrss is in bytes on macOS (kilobytes on Linux).
    return resource.getrusage(resource.RUSAGE_SELF).ru_maxrss / 1e6


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--models", required=True)
    ap.add_argument("--audio", required=True)
    ap.add_argument("--config", required=True, choices=sorted(CONFIGS))
    ap.add_argument("--threads", type=int, default=2)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    root, audio_dir = Path(args.models), Path(args.audio)
    manifest = [json.loads(line) for line in open(audio_dir / "manifest.jsonl")]
    clips = [sf.read(audio_dir / row["clip"], dtype="float32") for row in manifest]
    warmup = np.zeros(16000, dtype=np.float32)
    files, build = CONFIGS[args.config]

    rss_before = peak_rss_mb()
    t0 = time.perf_counter()
    recognizer = build(root, args.threads)
    load_s = time.perf_counter() - t0
    rss_loaded = peak_rss_mb()

    def decode(samples, sr):
        start = time.perf_counter()
        stream = recognizer.create_stream()
        stream.accept_waveform(sr, samples)
        recognizer.decode_stream(stream)
        return stream.result.text, time.perf_counter() - start

    _, first_s = decode(warmup, 16000)
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    with open(out_dir / f"{args.config}-t{args.threads}.jsonl", "w") as f:
        for row, (samples, sr) in zip(manifest, clips):
            text, seconds = decode(samples, sr)
            f.write(json.dumps({"clip": row["clip"], "text": text, "decode_s": round(seconds, 4)}) + "\n")

    summary = {
        "config": args.config,
        "threads": args.threads,
        "files": {f: (root / f).stat().st_size for f in files},
        "load_s": round(load_s, 3),
        "first_decode_s": round(first_s, 3),
        "rss_before_mb": round(rss_before, 1),
        "rss_after_load_mb": round(rss_loaded, 1),
        "rss_peak_mb": round(peak_rss_mb(), 1),
    }
    (out_dir / f"{args.config}-t{args.threads}.summary.json").write_text(json.dumps(summary, indent=1))
    print(json.dumps(summary))


if __name__ == "__main__":
    main()
