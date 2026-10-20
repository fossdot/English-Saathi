#!/bin/bash
SP=/private/tmp/claude-501/-Users-fossdot-Developer-English-Bolo/172e7bb2-0c51-4bde-bc6f-ebcbedd324ef/scratchpad
T="/Users/fossdot/Developer/English Saathi/tools/asr-compare"
M=$SP/asr/models; A=$SP/asr/work/audio; R=$SP/intel/results-speech; mkdir -p $R
ARM=$SP/asr/venv/bin/python; X64="arch -x86_64 $SP/intel/venv-x64/bin/python"
cd "$T"
echo "### sherpa-onnx accuracy+timing runs, 6 threads"
for cfg in whisper-fq-compute whisper-base.en-int8 whisper-base.en-fp32 whisper-small.en-int8 whisper-small.en-fp32; do
  echo "== $cfg x64"; $X64 run_asr.py --models $M --audio $A --config $cfg --threads 6 --tag x64 --out $R || echo "FAILED $cfg x64"
  echo "== $cfg arm64"; $ARM run_asr.py --models $M --audio $A --config $cfg --threads 6 --tag arm64 --out $R || echo "FAILED $cfg arm64"
done
echo "### whisper.cpp runs, 6 threads"
W=$SP/intel/whisper.cpp; G=$SP/intel/models/ggml
for m in tiny.en base.en small.en; do
  for q in "" "-q5_1"; do
    echo "== whispercpp-$m$q x64"; arch -x86_64 $X64 run_whispercpp.py --cli $W/build-x64/bin/whisper-cli --model $G/ggml-$m$q.bin --audio $A --threads 6 --config whispercpp-$m$q-x64 --out $R || echo "FAILED whispercpp $m$q x64"
  done
done
for m in tiny.en small.en; do
  echo "== whispercpp-$m arm64"; $ARM run_whispercpp.py --cli $W/build-arm64/bin/whisper-cli --model $G/ggml-$m.bin --audio $A --threads 6 --config whispercpp-$m-arm64 --out $R || echo "FAILED whispercpp $m arm64"
done
echo "### 3-second clip timing loops (sherpa), x64 and arm64, threads 2/4/6"
CLIP="/Users/fossdot/Developer/English Saathi/app/src/androidTest/assets/asr-test/librispeech-0.wav"
for cfg in whisper-fq-compute whisper-int8 whisper-base.en-int8 whisper-base.en-fp32 whisper-small.en-int8 whisper-small.en-fp32; do
  for t in 2 4 6; do
    $X64 bench_speed.py --models $M --config $cfg --clip "$CLIP" --threads $t --runs 7 >> $R/speed-x64.jsonl || echo "FAILED speed $cfg x64 t$t"
    $ARM bench_speed.py --models $M --config $cfg --clip "$CLIP" --threads $t --runs 7 >> $R/speed-arm64.jsonl || echo "FAILED speed $cfg arm64 t$t"
  done
done
echo "### whisper.cpp 3-second clip timing, x64, threads 2/4/6"
python3 - "$CLIP" $SP/intel/clip3s.wav <<'PY'
import sys, wave
src, dst = sys.argv[1], sys.argv[2]
with wave.open(src) as r:
    params = r.getparams(); frames = r.readframes(3 * r.getframerate())
with wave.open(dst, "wb") as w:
    w.setparams(params); w.writeframes(frames)
PY
for m in tiny.en base.en small.en; do for q in "" "-q5_1"; do for t in 2 4 6; do
  for i in 1 2 3 4 5; do arch -x86_64 $W/build-x64/bin/whisper-cli -m $G/ggml-$m$q.bin -f $SP/intel/clip3s.wav -t $t -bs 1 -bo 1 -nt -np -l en 2>&1 >/dev/null | grep -E 'load time|total time' | tr -d '\n' | sed "s/^/whispercpp-$m$q x64 t$t run$i: /"; echo; done
done; done; done >> $R/speed-whispercpp-x64.txt
echo SPEECH-BATCH-DONE
