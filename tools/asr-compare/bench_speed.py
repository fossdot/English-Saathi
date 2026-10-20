"""Seconds per 3-second clip for one sherpa-onnx configuration.

Loads the model, warms up, then recognizes the first N seconds of a clip
several times and prints one JSON line: load time, median and minimum decode
time, and the process's peak memory. Run it under the CPU architecture you
want to measure (e.g. `arch -x86_64 python bench_speed.py ...`).
"""

import argparse
import json
import platform
import resource
import statistics
import time
from pathlib import Path

import soundfile as sf

from run_asr import CONFIGS


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--models", required=True)
    ap.add_argument("--config", required=True, choices=sorted(CONFIGS))
    ap.add_argument("--clip", required=True, help="16 kHz mono WAV")
    ap.add_argument("--seconds", type=float, default=3.0)
    ap.add_argument("--threads", type=int, default=4)
    ap.add_argument("--runs", type=int, default=7)
    args = ap.parse_args()

    samples, rate = sf.read(args.clip, dtype="float32")
    samples = samples[: int(args.seconds * rate)]
    files, build = CONFIGS[args.config]
    t0 = time.perf_counter()
    recognizer = build(Path(args.models), args.threads)
    load_s = time.perf_counter() - t0

    def decode():
        start = time.perf_counter()
        stream = recognizer.create_stream()
        stream.accept_waveform(rate, samples)
        recognizer.decode_stream(stream)
        return time.perf_counter() - start, stream.result.text

    decode()  # warm-up
    times = []
    text = ""
    for _ in range(args.runs):
        seconds, text = decode()
        times.append(seconds)
    print(json.dumps({
        "config": args.config,
        "arch": platform.machine(),
        "threads": args.threads,
        "clip_s": round(len(samples) / rate, 2),
        "load_s": round(load_s, 3),
        "median_s": round(statistics.median(times), 3),
        "min_s": round(min(times), 3),
        "runs": [round(t, 3) for t in times],
        "peak_rss_mb": round(resource.getrusage(resource.RUSAGE_SELF).ru_maxrss / 1e6),
        "text": text,
    }))


if __name__ == "__main__":
    main()
