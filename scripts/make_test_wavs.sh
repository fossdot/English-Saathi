#!/usr/bin/env bash
# Records the five bus-stand sentences with the macOS Indian English voice
# "Rishi", as test clips for the debug build's speech check screen and the
# on-device test. macOS only. The voices' licence doesn't allow sharing these
# recordings, so they stay out of git.
set -euo pipefail
cd "$(dirname "$0")/.."
out=app/src/debug/assets/asr-test
mkdir -p "$out"
n=0
while IFS= read -r sentence; do
  n=$((n + 1))
  say -v Rishi -o "$out/bus_0$n.wav" --data-format=LEI16@16000 "$sentence"
done <<'SENTENCES'
Where is the bus stop?
When will the bus come?
One ticket to Patna, please.
How much is the ticket?
Does this bus go to Bettiah?
SENTENCES
echo "made $n clips in $out"
