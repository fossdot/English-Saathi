#!/usr/bin/env bash
# Starts the desktop app from Terminal (docs/desktop-plan.md). For now this is
# milestone D3's speech check window. macOS asks whether *Terminal* may use
# the microphone: the app runs inside Terminal's permission.
#
# It runs on Amazon Corretto 17 when installed: Temurin's Java can't use the
# microphone (docs/desktop-plan.md, "Why Corretto, not Temurin").
set -euo pipefail
cd "$(dirname "$0")/.."

corretto=""
for home in /Library/Java/JavaVirtualMachines/*/Contents/Home; do
  release="$home/release"
  [ -f "$release" ] || continue
  if grep -q '^JAVA_VERSION="17' "$release" && grep -qi '^IMPLEMENTOR=.*Amazon' "$release"; then
    corretto=$home
  fi
done
if [ -n "$corretto" ]; then
  export JAVA_HOME=$corretto
elif [ -z "${JAVA_HOME:-}" ]; then
  JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null) || {
    echo "No JDK 17 found. Install Amazon Corretto 17 (macOS x64 .pkg on an Intel Mac) from https://aws.amazon.com/corretto/" >&2
    exit 1
  }
  export JAVA_HOME
  echo "note: Amazon Corretto 17 is not installed; using $JAVA_HOME (the app warns if it can't use the microphone)"
fi
echo "java: $JAVA_HOME"

scripts/fetch_deps.sh >/dev/null
# --no-daemon: a fresh Gradle process started from this Terminal, so the
# microphone permission is Terminal's (a reused daemon keeps the permission
# of whichever app first started it). Offline once everything is downloaded.
./gradlew --no-daemon --quiet --offline :desktop:run 2>/dev/null || ./gradlew --no-daemon --quiet :desktop:run
