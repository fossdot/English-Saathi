#!/bin/bash
SP=/private/tmp/claude-501/-Users-fossdot-Developer-English-Bolo/172e7bb2-0c51-4bde-bc6f-ebcbedd324ef/scratchpad
while ! grep -q SPEECH-BATCH-DONE $SP/intel/speech-batch.log; do sleep 20; done
T="/Users/fossdot/Developer/English Saathi/tools/asr-compare"; W=$SP/intel/whisper.cpp; G=$SP/intel/models/ggml; A=$SP/asr/work/audio; R=$SP/intel/results-speech
X64="arch -x86_64 $SP/intel/venv-x64/bin/python"
cd "$T"
echo "### whisper.cpp extra full-clip runs"
for m in tiny.en small.en; do
  $X64 run_whispercpp.py --cli $W/build-x64-sse42/bin/whisper-cli --model $G/ggml-$m.bin --audio $A --threads 6 --config whispercpp-$m-x64sse42 --out $R || echo "FAILED sse42 $m"
done
$X64 run_whispercpp.py --cli $W/build-x64/bin/whisper-cli --model $G/ggml-base.en.bin --audio $A --threads 6 --audio-ctx 650 --config whispercpp-base.en-ac650-x64 --out $R || echo "FAILED ac650 base"
echo "### whisper.cpp 3-second clip timing: AVX2 and SSE4.2 builds, with and without -ac 650, 6 threads"
for build in build-x64 build-x64-sse42; do for m in tiny.en base.en small.en; do for q in "" "-q5_1"; do for ac in 0 650; do
  for i in 1 2 3 4 5; do
    extra=""; [ "$ac" != "0" ] && extra="-ac $ac"
    arch -x86_64 $W/$build/bin/whisper-cli -m $G/ggml-$m$q.bin -f $SP/intel/clip3s.wav -t 6 -bs 1 -bo 1 -nt -l en $extra 2>&1 >/dev/null | grep -E 'load time|encode time|total time' | sed 's/ \+/ /g' | tr '\n' ';' | sed "s/^/$build $m$q ac=$ac run$i: /"; echo
  done
done; done; done; done >> $R/speed-whispercpp-x64.txt
echo SSE42-DONE
