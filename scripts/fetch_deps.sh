#!/usr/bin/env bash
# Downloads the files the app needs that are not kept in git, and checks
# them against scripts/deps.sha256 (the build checks the same list):
#   - the sherpa-onnx speech library (Android archive)
#   - the Whisper tiny.en model, with its decoder re-quantized by
#     tools/asr-compare/quantize_whisper_decoder.py (needs python3; the
#     script installs onnx and numpy into tools/asr-compare/.venv)
#   - a LibriSpeech sample (CC BY 4.0) for the on-device speech test
# Safe to re-run: files that are already present and correct are kept.
# Everything is downloaded into a temporary folder first, so an interrupted
# run never leaves partial files inside the app.
set -euo pipefail
cd "$(dirname "$0")/.."

SHERPA_URL=https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-static-link-onnxruntime-1.13.8.aar
# csukuangfj/sherpa-onnx-whisper-tiny.en at a fixed commit (2024-07-13).
HF=https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny.en/resolve/d026532c022fa99fd789d6b32446a1df7b6bfc43
OFFICIAL_DECODER_SHA=06c0e6ff6348d427e51839219d1c886c18cfdf411e629e33f5e1679bff9c1527

AAR=app/libs/sherpa-onnx-static-link-onnxruntime-1.13.8.aar
MODEL_DIR=app/src/main/assets/asr/whisper-tiny.en
TEST_WAV=app/src/androidTest/assets/asr-test/librispeech-0.wav

if command -v shasum >/dev/null; then
  sha256() { shasum -a 256 "$1" | cut -d' ' -f1; }
else
  sha256() { sha256sum "$1" | cut -d' ' -f1; }
fi

expected() { # path -> its sha256 from deps.sha256
  awk -v p="$1" '$2 == p { print $1 }' scripts/deps.sha256
}

ok() { [ -f "$1" ] && [ "$(sha256 "$1")" = "$2" ]; }

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

fetch() { # url dest [sha256, default from deps.sha256]
  local want=${3:-$(expected "$2")}
  [ -n "$want" ] || { echo "no checksum for $2" >&2; exit 1; }
  if ok "$2" "$want"; then return; fi
  echo "downloading $(basename "$2")"
  local tmp="$work/$(basename "$2")"
  curl -fsSL -o "$tmp" "$1"
  if ! ok "$tmp" "$want"; then
    echo "checksum mismatch: $1" >&2
    exit 1
  fi
  mkdir -p "$(dirname "$2")"
  mv "$tmp" "$2"
}

fetch "$SHERPA_URL" "$AAR"
fetch "$HF/tiny.en-encoder.int8.onnx" "$MODEL_DIR/tiny.en-encoder.int8.onnx"
fetch "$HF/tiny.en-tokens.txt" "$MODEL_DIR/tiny.en-tokens.txt"
fetch "$HF/test_wavs/0.wav" "$TEST_WAV"

DECODER=$MODEL_DIR/tiny.en-decoder.int8-embedding.onnx
if ! ok "$DECODER" "$(expected "$DECODER")"; then
  fetch "$HF/tiny.en-decoder.int8.onnx" "$work/official/tiny.en-decoder.int8.onnx" "$OFFICIAL_DECODER_SHA"
  venv=tools/asr-compare/.venv
  if ! "$venv/bin/python" -m pip --version >/dev/null 2>&1; then
    rm -rf "$venv"
    python3 -m venv "$venv"
  fi
  "$venv/bin/python" -m pip install -q onnx==1.23.2 numpy==2.5.4
  echo "quantizing the decoder's token embedding"
  "$venv/bin/python" tools/asr-compare/quantize_whisper_decoder.py \
    --input "$work/official/tiny.en-decoder.int8.onnx" --output "$work/decoder.onnx" --mode compute
  if ! ok "$work/decoder.onnx" "$(expected "$DECODER")"; then
    echo "the quantized decoder differs from the tested one (onnx or numpy version?)" >&2
    exit 1
  fi
  mkdir -p "$MODEL_DIR"
  mv "$work/decoder.onnx" "$DECODER"
fi

echo "dependencies ready"
