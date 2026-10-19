"""Downloads the compared models, checks them, and builds the quantized Whisper decoders.

    python fetch_models.py --models WORK/models
"""

import argparse
import hashlib
import shutil
import subprocess
import sys
import tarfile
import urllib.request
from pathlib import Path

RELEASE = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models"

# Archive name -> sha256 of every file the comparison uses.
ARCHIVES = {
    "sherpa-onnx-whisper-tiny.en": {
        "tiny.en-encoder.int8.onnx": "0ce578b827c94a961aacb8fa14b02f096504b337e5c94be37c36238cbe3e8bc6",
        "tiny.en-decoder.int8.onnx": "06c0e6ff6348d427e51839219d1c886c18cfdf411e629e33f5e1679bff9c1527",
        "tiny.en-encoder.onnx": "dc7de696432d97a3d64387800b230ac69d18e5e4efea0eec0613209dd8b7b0c9",
        "tiny.en-decoder.onnx": "54d5a5fd2a757175aeb70a837a8404fc9c31b659610724694f5a0e2c51715f94",
        "tiny.en-tokens.txt": "306cd27f03c1a714eca7108e03d66b7dc042abe8c258b44c199a7ed9838dd930",
    },
    "sherpa-onnx-moonshine-tiny-en-int8": {
        "preprocess.onnx": "f33addce61a143460fe753b5ee5b7db255e5140b5b779c065b94f6c83ff0bf4e",
        "encode.int8.onnx": "8774dfba578de027ec6595c2c654a0836434489bc963a0db124a7f181f571acb",
        "uncached_decode.int8.onnx": "216737000dd5881a17aa043f6bbd286add33e4c3b0ae257153e2ec15438bdc41",
        "cached_decode.int8.onnx": "2aff28bba6a03d8dcf5c9feac45462629bae37317442299f28115ad09da773f6",
        "tokens.txt": "1165c2aeb9f72f457a83be2d459a09054f27490acd9b41bd43794dfd25e296ea",
    },
    "sherpa-onnx-moonshine-tiny-en-quantized-2026-02-27": {
        "encoder_model.ort": "94e90a4654fc45cdfedb77c4c08e1739f48862998e58fada384b25118134f221",
        "decoder_model_merged.ort": "cf524c4862d36e9e5ab032eddc73637efd822d70e868ac575cf1a46e1e4708a0",
        "tokens.txt": "2870d843e14c1e187bf1913a521562a63b53933814bd7f2145120468f494a049",
    },
}

# Built here from the official int8 decoder; the same onnx/numpy versions give the same bytes.
QUANTIZED = {
    "storage": "3bfe3623ccbd7a6121898803c31c3322d04cca8adeaf392ecf3ef2029b38e18b",
    "compute": "01f50ff8ddfcbd3659dcae4bd638c453ed95e7f479593d7ea9c65f839052db7d",
}


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            h.update(block)
    return h.hexdigest()


def check(path, expected):
    got = sha256(path)
    if got != expected:
        sys.exit(f"checksum mismatch for {path}: {got}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--models", required=True)
    models = Path(ap.parse_args().models)
    models.mkdir(parents=True, exist_ok=True)

    for name, files in ARCHIVES.items():
        target = models / name
        if not all((target / f).exists() for f in files):
            archive = models / f"{name}.tar.bz2"
            print("downloading", name)
            urllib.request.urlretrieve(f"{RELEASE}/{name}.tar.bz2", archive)
            with tarfile.open(archive) as tar:
                tar.extractall(models, filter="data")
            archive.unlink()
        for f, digest in files.items():
            check(target / f, digest)

    whisper = models / "sherpa-onnx-whisper-tiny.en"
    script = Path(__file__).with_name("quantize_whisper_decoder.py")
    for mode, digest in QUANTIZED.items():
        out = models / f"whisper-fq-{mode}"
        out.mkdir(exist_ok=True)
        for f in ["tiny.en-encoder.int8.onnx", "tiny.en-tokens.txt"]:
            shutil.copy(whisper / f, out / f)
        decoder = out / "tiny.en-decoder.int8.onnx"
        subprocess.run([sys.executable, str(script), "--input", str(whisper / "tiny.en-decoder.int8.onnx"),
                        "--output", str(decoder), "--mode", mode], check=True)
        check(decoder, digest)
    print("models ready in", models)


if __name__ == "__main__":
    main()
