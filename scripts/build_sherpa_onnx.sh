#!/usr/bin/env bash
# Builds sherpa-onnx 1.13.8's Java native library, libsherpa-onnx-jni.dylib,
# for macOS (Intel and Apple silicon) from its source, with text-to-speech and
# speaker diarization turned off (docs/decisions.md, "Licences: owner
# answers"). The prebuilt library compiles in eSpeak NG (GPL-3.0) for
# text-to-speech, which the app doesn't use; sherpa-onnx's maintainer
# recommends this build for that reason.
#
# Writes desktop/native/osx-x64/libsherpa-onnx-jni.dylib and
# desktop/native/osx-aarch64/libsherpa-onnx-jni.dylib, which are kept in git
# and listed in scripts/deps.sha256 (a rebuild elsewhere gives different
# bytes, so the tested files are the ones in the repo). libonnxruntime.dylib
# stays the file from sherpa-onnx's release (ONNX Runtime 1.28.2), fetched by
# scripts/fetch_deps.sh; it is the same file this build links against.
#
# Needs git, cmake and the Xcode command line tools, and internet for
# sherpa-onnx's pinned dependencies. Only needed to change the library.
set -euo pipefail
cd "$(dirname "$0")/.."

TAG=v1.13.8
COMMIT=11afbd009a7f8c08f4bcf2fc1b265d0df4670fbf
# sherpa-onnx's build breaks on a space in its path (such as "English
# Saathi"), so it is built outside the repo.
tmp=${TMPDIR:-/tmp}
src=${SHERPA_SRC:-${tmp%/}/sherpa-onnx-$TAG}
case $src in *" "*) echo "sherpa-onnx can't be built in a path with a space: $src (set SHERPA_SRC)" >&2; exit 1 ;; esac

# The JNI part needs jni.h from a JDK (any processor: the headers are the same).
if [ -z "${JAVA_HOME:-}" ]; then
  JAVA_HOME=$(/usr/libexec/java_home -v 17) || { echo "needs a JDK 17 (JAVA_HOME)" >&2; exit 1; }
  export JAVA_HOME
fi

if [ ! -d "$src/.git" ]; then
  git clone --quiet --depth 1 --branch "$TAG" https://github.com/k2-fsa/sherpa-onnx "$src"
fi
[ "$(git -C "$src" rev-parse HEAD)" = "$COMMIT" ] || { echo "$src is not sherpa-onnx $TAG ($COMMIT)" >&2; exit 1; }

# The list of functions the library exports also names the text-to-speech
# and diarization ones, and macOS's linker refuses to export a function that
# isn't built. Taking those 16 out is the only change to sherpa-onnx's files.
exp=sherpa-onnx/jni/sherpa-onnx-symbols.exp
git -C "$src" checkout --quiet -- "$exp"
grep -vE '^_Java_com_k2fsa_sherpa_onnx_(OfflineTts|GeneratedAudio|OfflineSpeakerDiarization)_' "$src/$exp" > "$src/$exp.trimmed"
mv "$src/$exp.trimmed" "$src/$exp"

for arch in x86_64 arm64; do
  case $arch in x86_64) dir=osx-x64 ;; arm64) dir=osx-aarch64 ;; esac
  build=$src/build-$arch
  echo "== sherpa-onnx $TAG for $arch"
  # As sherpa-onnx's own .github/workflows/macos-jni.yaml at $TAG, plus everything the app doesn't use turned off.
  cmake -S "$src" -B "$build" \
    -D BUILD_SHARED_LIBS=ON \
    -D CMAKE_BUILD_TYPE=Release \
    -D CMAKE_OSX_ARCHITECTURES=$arch \
    -D SHERPA_ONNX_ENABLE_JNI=ON \
    -D SHERPA_ONNX_ENABLE_TTS=OFF \
    -D SHERPA_ONNX_ENABLE_SPEAKER_DIARIZATION=OFF \
    -D SHERPA_ONNX_ENABLE_PORTAUDIO=OFF \
    -D SHERPA_ONNX_ENABLE_WEBSOCKET=OFF \
    -D SHERPA_ONNX_ENABLE_BINARY=OFF \
    -D SHERPA_ONNX_BUILD_C_API_EXAMPLES=OFF \
    -D SHERPA_ONNX_ENABLE_C_API=OFF \
    -D SHERPA_ONNX_USE_PRE_INSTALLED_ONNXRUNTIME_IF_AVAILABLE=OFF \
    > "$build.configure.log"
  cmake --build "$build" --target sherpa-onnx-jni -j "$(sysctl -n hw.ncpu)" > "$build.build.log"
  lib=$(find "$build" -name libsherpa-onnx-jni.dylib -type f | head -1)
  [ -n "$lib" ] || { echo "no libsherpa-onnx-jni.dylib in $build" >&2; exit 1; }

  # The point of this build: no eSpeak NG, piper-phonemize or other text-to-speech code inside.
  if nm -a "$lib" | grep -qiE 'espeak|piper|ucd_'; then echo "text-to-speech code is still in $lib" >&2; exit 1; fi
  case " $(lipo -archs "$lib") " in *" $arch "*) ;; *) echo "$lib is not $arch" >&2; exit 1 ;; esac
  otool -L "$lib" | grep -q '@rpath/libonnxruntime.dylib' || { echo "$lib doesn't load @rpath/libonnxruntime.dylib" >&2; exit 1; }

  mkdir -p "desktop/native/$dir"
  cp "$lib" "desktop/native/$dir/libsherpa-onnx-jni.dylib"
  echo "desktop/native/$dir/libsherpa-onnx-jni.dylib: $(shasum -a 256 "desktop/native/$dir/libsherpa-onnx-jni.dylib" | cut -d' ' -f1)"
done
echo "Now put the new checksums in scripts/deps.sha256."
