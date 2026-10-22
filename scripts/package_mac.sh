#!/usr/bin/env bash
# Builds English Saathi.app for Intel Macs and its .dmg (docs/packaging.md):
#
#   desktop/build/packaging/English-Saathi-<version>.dmg
#   desktop/build/packaging/corretto-17-<version>.tar.gz   (the Java runtime's source)
#
# Both go in the Google Drive folder. Runs on any Mac; an Apple silicon Mac
# builds the Intel app under Rosetta. Needs internet the first time: Gradle,
# the libraries and models (scripts/fetch_deps.sh) and Amazon Corretto 17 for
# Intel, whose runtime goes inside the app. The Macs that run the app need
# none of this.
#
# The app is signed "ad hoc" (no Apple Developer ID, not notarized): every
# program and library inside is re-signed without the hardened runtime.
# Compose leaves an Intel build unsigned, which macOS calls "damaged", and on
# Apple silicon it would sign with the hardened runtime and no microphone
# entitlement, which records only silence (docs/packaging.md, "Signing").
set -euo pipefail
cd "$(dirname "$0")/.."

CORRETTO_VERSION=17.0.20.12.1
CORRETTO_URL=https://corretto.aws/downloads/resources/$CORRETTO_VERSION/amazon-corretto-$CORRETTO_VERSION-macosx-x64.tar.gz
# As published in the release notes of github.com/corretto/corretto-17, tag 17.0.20.12.1.
CORRETTO_SHA=cf269b31d6b987b16cf8acf3ce20aaee561858d4d424c482e686d783c63ef2d4
# Its source (GPLv2 asks for it beside the runtime): the tag's archive, whose commit is checked.
CORRETTO_SRC_URL=https://github.com/corretto/corretto-17/archive/refs/tags/$CORRETTO_VERSION.tar.gz
CORRETTO_SRC_COMMIT=c2187e213d0a8c22e22aecb1ac9f581da094787d

[ "$(uname)" = Darwin ] || { echo "Build the Mac app on a Mac." >&2; exit 1; }
if [ "$(uname -m)" = arm64 ] && ! arch -x86_64 /usr/bin/true 2>/dev/null; then
  echo "This Mac needs Rosetta to build the Intel app: softwareupdate --install-rosetta --agree-to-license" >&2
  exit 1
fi

out=desktop/build/packaging
mkdir -p "$out"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# 1. Amazon Corretto 17 for Intel, checked against Amazon's checksum.
jdk_dir=$out/corretto-$CORRETTO_VERSION-x64
JDK=$PWD/$jdk_dir/amazon-corretto-17.jdk/Contents/Home
if [ ! -x "$JDK/bin/java" ]; then
  echo "downloading Amazon Corretto $CORRETTO_VERSION for Intel"
  curl -fsSL --retry 3 -o "$work/corretto.tar.gz" "$CORRETTO_URL"
  echo "$CORRETTO_SHA  $work/corretto.tar.gz" | shasum -a 256 -c - >/dev/null || { echo "checksum mismatch: $CORRETTO_URL" >&2; exit 1; }
  # Unpacked beside it and then renamed, so an interrupted run never leaves a half-unpacked JDK to reuse.
  rm -rf "$jdk_dir" "$jdk_dir.partial"
  mkdir -p "$jdk_dir.partial"
  tar -xzf "$work/corretto.tar.gz" -C "$jdk_dir.partial"
  mv "$jdk_dir.partial" "$jdk_dir"
fi

# 2. The speech library and models, checked against scripts/deps.sha256.
scripts/fetch_deps.sh

# 3. The app. Gradle itself runs on the Intel JDK (under Rosetta on Apple silicon),
#    so Compose takes the Intel graphics library and the bundle gets the Intel speech library.
echo "building English Saathi.app (Intel)"
JAVA_HOME=$JDK ./gradlew --no-daemon --quiet -Pdistribution :desktop:createDistributable
app="desktop/build/compose/binaries/main/app/English Saathi.app"
[ -d "$app" ] || { echo "no $app" >&2; exit 1; }

# 4. Every program and library inside must be for Intel. Skiko's jar for Intel Macs also
#    carries its Apple silicon library (21 MB), which the app never loads: it loads the
#    Intel one Compose unpacked next to the jars (skiko.library.path). It is taken out.
for jar in "$app"/Contents/app/skiko-awt-runtime-macos-x64-*.jar; do
  unzip -Z1 "$jar" > "$work/jar-entries"
  if grep -q '^libskiko-macos-arm64' "$work/jar-entries"; then
    zip -q -d "$jar" 'libskiko-macos-arm64.dylib' 'libskiko-macos-arm64.dylib.sha256'
  fi
done
wrong=0
while IFS= read -r -d '' f; do
  kind=$(file -b "$f")
  case $kind in
    Mach-O*)
      archs=$(lipo -archs "$f" 2>/dev/null || echo "?")
      case " $archs " in *" x86_64 "*) ;; *) echo "not for Intel ($archs): $f" >&2; wrong=1 ;; esac ;;
  esac
  case $f in
    *.jar)
      unzip -Z1 "$f" > "$work/jar-entries"
      if grep -E '\.(dylib|jnilib)$' "$work/jar-entries" | grep -vE 'x64|x86_64|amd64' > "$work/jar-native"; then
        echo "a library not for Intel inside $f: $(tr '\n' ' ' < "$work/jar-native")" >&2; wrong=1
      fi ;;
  esac
done < <(find "$app/Contents" -type f -print0)
[ "$wrong" = 0 ] || exit 1
# Everyone must be able to read every file: macOS calls an app "damaged" for an
# account that can't read part of it (its signature can't be checked).
chmod -R a+rX "$app"
unreadable=$(find "$app" ! -perm -004 | head -5)
[ -z "$unreadable" ] || { echo "not readable by everyone: $unreadable" >&2; exit 1; }

# 5. Sign ad hoc, without the hardened runtime: the libraries and programs first, then the
#    runtime bundle, then the app, which seals everything else. Mixed or broken signatures
#    would make macOS call the app "damaged", with no way to open it.
echo "signing (ad hoc)"
while IFS= read -r -d '' f; do
  if file -b "$f" | grep -q '^Mach-O'; then codesign --force --sign - --timestamp=none "$f"; fi
done < <(find "$app/Contents" -type f ! -path "*/Contents/MacOS/*" -print0)
codesign --force --sign - --timestamp=none "$app/Contents/runtime"
codesign --force --sign - --timestamp=none "$app"
codesign --verify --deep --strict "$app"
# No program or library may keep the hardened runtime: without the audio-input entitlement it
# would record only silence. (Read from a variable: under pipefail, grep -q could hide a match.)
while IFS= read -r -d '' f; do
  case $(file -b "$f") in
    Mach-O*)
      flags=$(codesign -dv "$f" 2>&1 || true)
      case $flags in *flags=*runtime*) echo "still has the hardened runtime: $f" >&2; exit 1 ;; esac ;;
  esac
done < <(find "$app/Contents" -type f -print0)

# 6. The app checks itself: signature, microphone text, lessons, both speech models on the
#    LibriSpeech clip, the window's drawing and font. Then the signature must still be intact
#    (the app writes nothing inside itself).
echo "self-test"
"$app/Contents/MacOS/English Saathi" --self-test
codesign --verify --deep --strict "$app"

# 7. The .dmg: the app, a link to Applications and the setup guide.
version=$(sed -n 's/^val appVersion = "\(.*\)"/\1/p' desktop/build.gradle.kts)
dmg=$out/English-Saathi-$version.dmg
stage=$work/stage
mkdir -p "$stage"
ditto "$app" "$stage/English Saathi.app"
ln -s /Applications "$stage/Applications"
if [ -f docs/setup-guide.pdf ]; then
  if [ docs/setup-guide.html -nt docs/setup-guide.pdf ]; then
    echo "warning: docs/setup-guide.html is newer than its PDF; run scripts/make_setup_guide.sh if the guide changed" >&2
  fi
  cp docs/setup-guide.pdf "$stage/Setup guide.pdf"
else
  echo "note: docs/setup-guide.pdf is missing, so the .dmg has no setup guide" >&2
fi
rm -f "$dmg"
# ULMO (LZMA) is the smallest .dmg format; macOS 10.15 and later open it.
hdiutil create -quiet -volname "English Saathi" -srcfolder "$stage" -fs HFS+ -format ULMO "$dmg"
hdiutil verify -quiet "$dmg"

# 8. The app as it is on the .dmg must be intact too.
mnt=$work/mnt
mkdir -p "$mnt"
hdiutil attach -quiet -nobrowse -readonly -mountpoint "$mnt" "$dmg"
if ! codesign --verify --deep --strict "$mnt/English Saathi.app"; then
  hdiutil detach -quiet "$mnt"
  echo "the app on the .dmg doesn't verify: macOS would call it damaged" >&2
  exit 1
fi
hdiutil detach -quiet "$mnt"
echo "the app on the .dmg is intact"

# 9. Corretto's source, for the same folder as the .dmg.
src=$out/corretto-17-$CORRETTO_VERSION.tar.gz
if [ ! -f "$src" ] || [ "$(gunzip -c "$src" | git get-tar-commit-id)" != "$CORRETTO_SRC_COMMIT" ]; then
  echo "downloading Corretto $CORRETTO_VERSION's source"
  curl -fsSL --retry 3 -o "$work/src.tar.gz" "$CORRETTO_SRC_URL"
  [ "$(gunzip -c "$work/src.tar.gz" | git get-tar-commit-id)" = "$CORRETTO_SRC_COMMIT" ] || { echo "not Corretto $CORRETTO_VERSION's source: $CORRETTO_SRC_URL" >&2; exit 1; }
  mv "$work/src.tar.gz" "$src"
fi

echo
echo "English Saathi $version for Intel Macs; put both files in the Google Drive folder:"
echo "  $dmg ($(du -h "$dmg" | cut -f1); the app inside is $(du -sh "$app" | cut -f1))"
echo "  $src ($(du -h "$src" | cut -f1), the Java runtime's source)"
shasum -a 256 "$dmg" "$src"
