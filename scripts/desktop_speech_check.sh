#!/usr/bin/env bash
# Milestone D1's speech check on a real MacBook (docs/desktop-plan.md):
# loads tiny.en and small.en through sherpa-onnx's Java library and times
# them on the test clips with 2, 4 and 6 threads. Writes
# desktop/build/speech-check/<processor>.log; send that file back.
#
# Needs: Amazon Corretto 17 (or another JDK 17), python3 3.11 or later for
# scripts/fetch_deps.sh, and internet the first time (Gradle, libraries,
# models: about 1 GB). The MacBook trip checklist is docs/macbook-trip.md.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "machine: $(sysctl -n machdep.cpu.brand_string), $(uname -m), macOS $(sw_vers -productVersion)"
if [ "$(uname -m)" != x86_64 ]; then
  echo "note: this is not an Intel Mac; the results are for $(uname -m)"
fi

if [ -z "${JAVA_HOME:-}" ]; then
  JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null) || {
    echo "No JDK 17 found. Install Amazon Corretto 17 (macOS x64 .pkg) from https://aws.amazon.com/corretto/" >&2
    exit 1
  }
  export JAVA_HOME
fi
echo "java: $JAVA_HOME"

scripts/fetch_deps.sh
if ! ls app/src/debug/assets/asr-test/bus_0?.wav >/dev/null 2>&1; then
  scripts/make_test_wavs.sh
fi

rm -rf desktop/build/speech-check
# --no-daemon: a fresh Gradle process, so nothing is left running afterwards.
./gradlew --no-daemon --console=plain :desktop:speechCheck -Pthreads=2,4,6
echo
echo "Done. Send back: $(ls desktop/build/speech-check/*.log)"
