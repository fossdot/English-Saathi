#!/bin/bash
SP=/private/tmp/claude-501/-Users-fossdot-Developer-English-Bolo/172e7bb2-0c51-4bde-bc6f-ebcbedd324ef/scratchpad
while ! grep -q SPEECH-BATCH-DONE $SP/intel/speech-batch.log; do sleep 20; done
while ! grep -q PULLS2-DONE $SP/intel/pulls2.log 2>/dev/null; do sleep 20; done
while ! grep -q SSE42-DONE $SP/intel/speech-sse42.log 2>/dev/null; do sleep 20; done
export OLLAMA_HOST=127.0.0.1:11435
cd "/Users/fossdot/Developer/English Saathi/tools/llm-bench"
MODELS=""
for m in llama3.2:1b-instruct-q4_K_M llama3.2:3b gemma3:1b gemma3:4b gemma3n:e4b qwen3:1.7b qwen3:4b qwen2.5:3b phi4-mini nemotron-mini hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M; do
  if ollama list 2>/dev/null | grep -qi "^$m" ; then MODELS="$MODELS $m"; else echo "not available: $m"; fi
done
echo "models:$MODELS"
PID=$(pgrep -f '/usr/local/bin/ollama serve')
arch -x86_64 $SP/intel/venv-x64/bin/python bench.py --host 127.0.0.1:11435 --server-pid $PID --models $MODELS \
  --speech-model "/Users/fossdot/Developer/English Saathi/app/src/main/assets/asr/whisper-tiny.en" \
  --clip "/Users/fossdot/Developer/English Saathi/app/src/androidTest/assets/asr-test/librispeech-0.wav" \
  --out $SP/intel/results-llm
echo "### llama-bench (llama.cpp x86_64 AVX2 build) on the same 4-bit files, 6 threads"
LB=$SP/intel/llama.cpp/build-x64/bin/llama-bench
for m in $MODELS; do
  blob=$(ollama show "$m" --modelfile 2>/dev/null | grep -m1 '^FROM ' | awk '{print $2}')
  echo "== $m -> $blob"
  [ -f "$blob" ] && arch -x86_64 $LB -m "$blob" -t 6 -p 256 -n 64 -r 2 -o json 2>/dev/null | python3 -c "
import json,sys
for r in json.load(sys.stdin): print(json.dumps({'model':'$m','test':r.get('test') or r.get('n_prompt') and 'pp' or 'tg','n_prompt':r.get('n_prompt'),'n_gen':r.get('n_gen'),'tokens_per_s':round(r.get('avg_ts',0),2),'size_mb':round(r.get('model_size',0)/1e6)}))" >> $SP/intel/results-llm/llama-bench-x64avx2.jsonl
done
echo LLM-BATCH-DONE
