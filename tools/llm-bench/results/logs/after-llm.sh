#!/bin/bash
SP=/private/tmp/claude-501/-Users-fossdot-Developer-English-Bolo/172e7bb2-0c51-4bde-bc6f-ebcbedd324ef/scratchpad/intel
while ! grep -q LLM-BATCH-DONE $SP/llm-batch.log; do sleep 20; done
while ! grep -q PULLS3-DONE $SP/pulls3.log; do sleep 20; done
export OLLAMA_HOST=127.0.0.1:11435
cd "/Users/fossdot/Developer/English Saathi/tools/llm-bench"
MODELS=""
for m in qwen3:4b-instruct hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M; do
  if ollama list 2>/dev/null | grep -qi "^$m" ; then MODELS="$MODELS $m"; else echo "not available: $m"; fi
done
echo "models (pass 2):$MODELS"
PID=$(pgrep -f '/usr/local/bin/ollama serve')
[ -n "$MODELS" ] && arch -x86_64 $SP/venv-x64/bin/python bench.py --host 127.0.0.1:11435 --server-pid $PID --models $MODELS \
  --speech-model "/Users/fossdot/Developer/English Saathi/app/src/main/assets/asr/whisper-tiny.en" \
  --clip "/Users/fossdot/Developer/English Saathi/app/src/androidTest/assets/asr-test/librispeech-0.wav" \
  --out $SP/results-llm2
echo "### llama-bench for pass 2 models"
LB=$SP/llama.cpp/build-x64/bin/llama-bench
for m in $MODELS; do
  blob=$(ollama show "$m" --modelfile 2>/dev/null | grep -m1 '^FROM ' | awk '{print $2}')
  [ -f "$blob" ] && arch -x86_64 $LB -m "$blob" -t 6 -p 256 -n 64 -r 2 -o json 2>/dev/null | python3 -c "
import json,sys
for r in json.load(sys.stdin): print(json.dumps({'model':'$m','n_prompt':r.get('n_prompt'),'n_gen':r.get('n_gen'),'tokens_per_s':round(r.get('avg_ts',0),2),'size_mb':round(r.get('model_size',0)/1e6)}))" >> $SP/results-llm/llama-bench-x64avx2.jsonl
done
echo "### whisper.cpp AVX2 full-clip accuracy reruns"
T="/Users/fossdot/Developer/English Saathi/tools/asr-compare"; W=$SP/whisper.cpp; G=$SP/models/ggml; A=/private/tmp/claude-501/-Users-fossdot-Developer-English-Bolo/172e7bb2-0c51-4bde-bc6f-ebcbedd324ef/scratchpad/asr/work/audio; R=$SP/results-speech
cd "$T"
for m in tiny.en base.en small.en; do
  arch -x86_64 $SP/venv-x64/bin/python run_whispercpp.py --cli $W/build-x64/bin/whisper-cli --model $G/ggml-$m.bin --audio $A --threads 6 --config whispercpp-$m-x64 --out $R || echo "FAILED whispercpp $m x64"
done
echo AFTER-LLM-DONE
