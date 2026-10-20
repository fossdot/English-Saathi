#!/usr/bin/env bash
# Downloads the files the app needs that are not kept in git, and checks
# them against scripts/deps.sha256 (the build checks the same list):
#   - the sherpa-onnx speech library (Android archive)
#   - the Whisper tiny.en model, with its decoder re-quantized by
#     tools/asr-compare/quantize_whisper_decoder.py (needs python3; the
#     script installs onnx and numpy into tools/asr-compare/.venv)
#   - a LibriSpeech sample (CC BY 4.0) for the on-device speech test
#   - for the desktop app: the sherpa-onnx Java library and its macOS
#     native libraries (Intel and Apple silicon), and Whisper small.en (int8)
# Safe to re-run: files that are already present and correct are kept.
# Everything is downloaded into a temporary folder first, so an interrupted
# run never leaves partial files inside the app.
set -euo pipefail
cd "$(dirname "$0")/.."

SHERPA_URL=https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-static-link-onnxruntime-1.13.8.aar
# csukuangfj/sherpa-onnx-whisper-tiny.en at a fixed commit (2024-07-13).
HF=https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny.en/resolve/d026532c022fa99fd789d6b32446a1df7b6bfc43
OFFICIAL_DECODER_SHA=06c0e6ff6348d427e51839219d1c886c18cfdf411e629e33f5e1679bff9c1527

# The desktop app's Java library and native libraries, from the same release.
SHERPA_RELEASE=https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8
# csukuangfj/sherpa-onnx-whisper-small.en at a fixed commit (2024-07-13).
HF_SMALL=https://huggingface.co/csukuangfj/sherpa-onnx-whisper-small.en/resolve/d9533f69affd85061aee349af7fea5cb2996dbbe
# The native jars are only unpacked, not kept, so their checksums live here.
NATIVE_JAR_SHA_osx_x64=9190c28951d85efdbd376bae6b6dff12993311ad605945289e8459bf9c886a96
NATIVE_JAR_SHA_osx_aarch64=42e272180c8836127f024f3335d7afcdfb30fe0164b78330d5d449034e34ce34

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
  curl -fsSL --retry 3 -o "$tmp" "$1"
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

# Desktop app (docs/desktop-plan.md). The native libraries are unpacked from
# their release jars into desktop/native/<os-arch>/, where the app loads them
# directly instead of letting sherpa-onnx unpack them into $TMPDIR on every start.
fetch "$SHERPA_RELEASE/sherpa-onnx-jvm-1.13.8.jar" desktop/libs/sherpa-onnx-jvm-1.13.8.jar
for arch in osx-x64 osx-aarch64; do
  dir=desktop/native/$arch
  if ok "$dir/libonnxruntime.dylib" "$(expected "$dir/libonnxruntime.dylib")" &&
     ok "$dir/libsherpa-onnx-jni.dylib" "$(expected "$dir/libsherpa-onnx-jni.dylib")"; then
    continue
  fi
  jar=sherpa-onnx-native-lib-$arch-1.13.8.jar
  sha_var=NATIVE_JAR_SHA_${arch//-/_}
  fetch "$SHERPA_RELEASE/$jar" "$work/$jar" "${!sha_var}"
  unzip -q -o -j "$work/$jar" "sherpa-onnx/native/$arch/*.dylib" -d "$work/$arch"
  for lib in libonnxruntime.dylib libsherpa-onnx-jni.dylib; do
    if ! ok "$work/$arch/$lib" "$(expected "$dir/$lib")"; then
      echo "$jar holds an unexpected $lib" >&2
      exit 1
    fi
  done
  mkdir -p "$dir"
  mv "$work/$arch"/*.dylib "$dir/"
done
SMALL_DIR=desktop/models/whisper-small.en
fetch "$HF_SMALL/small.en-encoder.int8.onnx" "$SMALL_DIR/small.en-encoder.int8.onnx"
fetch "$HF_SMALL/small.en-decoder.int8.onnx" "$SMALL_DIR/small.en-decoder.int8.onnx"
fetch "$HF_SMALL/small.en-tokens.txt" "$SMALL_DIR/small.en-tokens.txt"

echo "dependencies ready"
