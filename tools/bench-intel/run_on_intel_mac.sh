#!/usr/bin/env bash
# Runs the speech and LLM benchmarks on a real Intel Mac (the 2019 16-inch
# MacBook Pro), so the numbers in docs/intel-macbook-benchmarks.md can be
# replaced by measurements from the target machine.
#
# Needs: the macOS voices Aman, Rishi, Tara, Lekha and Samantha (for the test
# clips), Python 3.12 (PYTHON=python3.12 by default), Homebrew's cmake (for
# whisper.cpp), the Xcode command line tools, Ollama installed but quit (this
# script starts its own server on port 11435), about 25 GB of disk and two to
# four hours. docs/intel-macbook-benchmarks.md has step-by-step instructions.
#
#   tools/bench-intel/run_on_intel_mac.sh ~/bench-work
#
# Results land in <work>/report-speech.md, <work>/results-speech and
# <work>/results-llm.
set -euo pipefail
WORK=${1:?usage: run_on_intel_mac.sh <work dir>}
PYTHON=${PYTHON:-python3.12}
REPO=$(cd "$(dirname "$0")/../.." && pwd)
mkdir -p "$WORK"
WORK=$(cd "$WORK" && pwd)
cd "$REPO/tools/asr-compare"

arch=$(uname -m)
echo "== machine: $(sysctl -n machdep.cpu.brand_string) ($arch), $(( $(sysctl -n hw.memsize) / 1073741824 )) GB, $(sw_vers -productVersion)"
if [ "$arch" != "x86_64" ]; then echo "this script is for an Intel Mac; on Apple Silicon see docs/intel-macbook-benchmarks.md" >&2; exit 1; fi

echo "== checks"
command -v "$PYTHON" >/dev/null || { echo "$PYTHON not found (brew install python@3.12)" >&2; exit 1; }
for tool in cmake git curl ollama; do command -v "$tool" >/dev/null || { echo "$tool not found" >&2; exit 1; }; done
voices=$(say -v '?')
for v in Aman Rishi Tara Lekha Samantha; do
  grep -q "^$v " <<< "$voices" || { echo "macOS voice $v is missing: System Settings > Accessibility > Spoken Content > System voice > Manage Voices" >&2; exit 1; }
done
# The app's speech model and test clip are not in git.
"$REPO/scripts/fetch_deps.sh"

echo "== python environment"
[ -x "$WORK/venv/bin/python" ] || "$PYTHON" -m venv "$WORK/venv"
"$WORK/venv/bin/pip" install -q -r requirements.txt

echo "== models (sherpa-onnx tiny/base/small, whisper.cpp ggml files)"
"$WORK/venv/bin/python" fetch_models.py --models "$WORK/models"
for m in base.en small.en; do
  [ -d "$WORK/models/sherpa-onnx-whisper-$m" ] || {
    curl -fsSL -o "$WORK/models/$m.tar.bz2" "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-$m.tar.bz2"
    tar xjf "$WORK/models/$m.tar.bz2" -C "$WORK/models" && rm "$WORK/models/$m.tar.bz2"; }
done
mkdir -p "$WORK/ggml"
for f in ggml-tiny.en.bin ggml-base.en.bin ggml-small.en.bin ggml-tiny.en-q5_1.bin ggml-base.en-q5_1.bin ggml-small.en-q5_1.bin; do
  [ -f "$WORK/ggml/$f" ] || curl -fsSL -o "$WORK/ggml/$f" "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$f"
done

echo "== test clips (macOS voices)"
[ -f "$WORK/audio/manifest.jsonl" ] || "$WORK/venv/bin/python" make_test_audio.py --out "$WORK/audio"

echo "== whisper.cpp (native build: AVX2 is detected on this machine)"
if [ ! -x "$WORK/whisper.cpp/build/bin/whisper-cli" ]; then
  [ -d "$WORK/whisper.cpp" ] || git clone -q --depth 1 --branch v1.9.5 https://github.com/ggml-org/whisper.cpp "$WORK/whisper.cpp"
  cmake -S "$WORK/whisper.cpp" -B "$WORK/whisper.cpp/build" -DCMAKE_BUILD_TYPE=Release -DGGML_METAL=OFF -DWHISPER_COREML=OFF -DWHISPER_BUILD_TESTS=OFF -DWHISPER_BUILD_SERVER=OFF > "$WORK/whisper-build.log" 2>&1
  cmake --build "$WORK/whisper.cpp/build" -j "$(sysctl -n hw.physicalcpu)" --config Release --target whisper-cli >> "$WORK/whisper-build.log" 2>&1
fi
WCLI="$WORK/whisper.cpp/build/bin/whisper-cli"
THREADS=$(sysctl -n hw.physicalcpu)

echo "== speech accuracy and timing over all clips ($THREADS threads)"
R="$WORK/results-speech"; mkdir -p "$R"
for cfg in whisper-fq-compute whisper-base.en-int8 whisper-small.en-int8; do
  "$WORK/venv/bin/python" run_asr.py --models "$WORK/models" --audio "$WORK/audio" --config "$cfg" --threads "$THREADS" --tag intel --out "$R"
done
for m in tiny.en base.en small.en; do
  "$WORK/venv/bin/python" run_whispercpp.py --cli "$WCLI" --model "$WORK/ggml/ggml-$m.bin" --audio "$WORK/audio" --threads "$THREADS" --config "whispercpp-$m-intel" --out "$R"
  "$WORK/venv/bin/python" run_whispercpp.py --cli "$WCLI" --model "$WORK/ggml/ggml-$m-q5_1.bin" --audio "$WORK/audio" --threads "$THREADS" --audio-ctx 650 --config "whispercpp-$m-q5_1-ac650-intel" --out "$R"
done
"$WORK/venv/bin/python" score.py --models "$WORK/models" --audio "$WORK/audio" --results "$R" --threads "$THREADS" --app-rules > "$WORK/report-speech.md"

echo "== seconds per 3-second clip"
CLIP="$REPO/app/src/androidTest/assets/asr-test/librispeech-0.wav"
: > "$R/speed-intel.jsonl"
for cfg in whisper-fq-compute whisper-base.en-int8 whisper-small.en-int8; do for t in 2 4 "$THREADS"; do
  "$WORK/venv/bin/python" bench_speed.py --models "$WORK/models" --config "$cfg" --clip "$CLIP" --threads "$t" >> "$R/speed-intel.jsonl"
done; done

echo "== LLMs through Ollama (CPU), with the app's speech model loaded"
export OLLAMA_HOST=127.0.0.1:11435
OLLAMA_HOST=127.0.0.1:11435 OLLAMA_NUM_PARALLEL=1 OLLAMA_MAX_LOADED_MODELS=1 ollama serve > "$WORK/ollama.log" 2>&1 &
SERVER=$!
trap 'kill $SERVER 2>/dev/null || true' EXIT
sleep 3
# qwen3:4b-instruct, not qwen3:4b: the plain tag is the thinking model, which can't be turned off.
MODELS="llama3.2:1b-instruct-q4_K_M llama3.2:3b gemma3:1b gemma3:4b qwen3:1.7b qwen3:4b-instruct qwen2.5:3b phi4-mini nemotron-mini
  hf.co/itlwas/Nemotron-4-Mini-Hindi-4B-Instruct-Q4_K_M-GGUF hf.co/arunmcops/Param-1-2.9B-Instruct-GGUF:Q4_K_M
  hf.co/QuantFactory/EuroLLM-1.7B-Instruct-GGUF:Q4_K_M hf.co/tiiuae/Falcon-H1-3B-Instruct-GGUF:Q4_K_M"
for m in $MODELS; do ollama pull "$m"; done
cd "$REPO/tools/llm-bench"
"$WORK/venv/bin/python" bench.py --host 127.0.0.1:11435 --server-pid "$SERVER" --models $MODELS \
  --speech-model "$REPO/app/src/main/assets/asr/whisper-tiny.en" --clip "$CLIP" --out "$WORK/results-llm"
echo "== done: $WORK/report-speech.md, $R/speed-intel.jsonl, $WORK/results-llm/summary.json, review.md, scoring-sheet.csv"
