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
case $src in /*) ;; *) src=$PWD/$src ;; esac
case $src in *" "*) echo "sherpa-onnx can't be built in a path with a space: $src (set SHERPA_SRC)" >&2; exit 1 ;; esac
# The source and build paths, which the compiler would write into the library
# (__FILE__), are written as sherpa-onnx-<tag>/… instead: this Mac's paths
# must not ship inside the app.
prefix_map="-ffile-prefix-map=$src/=sherpa-onnx-$TAG/"

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
    -D CMAKE_C_FLAGS="$prefix_map" \
    -D CMAKE_CXX_FLAGS="$prefix_map" \
    > "$build.configure.log"
  cmake --build "$build" --target sherpa-onnx-jni -j "$(sysctl -n hw.ncpu)" > "$build.build.log"
  built=$(find "$build" -name libsherpa-onnx-jni.dylib -type f | head -1)
  [ -n "$built" ] || { echo "no libsherpa-onnx-jni.dylib in $build" >&2; exit 1; }
  mkdir -p "desktop/native/$dir"
  lib=desktop/native/$dir/libsherpa-onnx-jni.dylib
  cp "$built" "$lib"

  # The build tree's copy also looks for ONNX Runtime in this Mac's build folder: keep only
  # @loader_path (the app's folder), as sherpa-onnx's installed copy has. Changing the library
  # breaks its signature, which Apple silicon refuses to load, so it is signed again (ad hoc).
  otool -l "$lib" > "$build.load-commands"
  awk '$1 == "cmd" { rpath = ($2 == "LC_RPATH") } rpath && $1 == "path" { print $2 }' "$build.load-commands" | while read -r rpath; do
    [ "$rpath" = "@loader_path" ] || install_name_tool -delete_rpath "$rpath" "$lib"
  done
  codesign --force --sign - "$lib"

  # The checks read files, not pipes: with pipefail, grep -q stopping early would hide a match.
  nm -a "$lib" > "$build.symbols"
  otool -l "$lib" > "$build.load-commands"
  otool -L "$lib" > "$build.libraries"
  strings -a "$lib" > "$build.strings"
  # The point of this build: no eSpeak NG, piper-phonemize or other text-to-speech code inside.
  if grep -qiE 'espeak|piper|ucd_' "$build.symbols"; then echo "text-to-speech code is still in $lib" >&2; exit 1; fi
  case " $(lipo -archs "$lib") " in *" $arch "*) ;; *) echo "$lib is not $arch" >&2; exit 1 ;; esac
  grep -q '@rpath/libonnxruntime.dylib' "$build.libraries" || { echo "$lib doesn't load @rpath/libonnxruntime.dylib" >&2; exit 1; }
  rpaths=$(awk '$1 == "cmd" { rpath = ($2 == "LC_RPATH") } rpath && $1 == "path" { print $2 }' "$build.load-commands")
  [ "$rpaths" = "@loader_path" ] || { echo "$lib looks for libraries in: $rpaths" >&2; exit 1; }
  if grep -qF -e "$src" -e "$HOME/" "$build.strings"; then echo "$lib has this Mac's paths inside" >&2; exit 1; fi
  codesign --verify "$lib"

  echo "$lib: $(shasum -a 256 "$lib" | cut -d' ' -f1)"
done
echo "Now put the new checksums in scripts/deps.sha256."
