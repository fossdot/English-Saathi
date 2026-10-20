# LLM benchmark (Ollama)

Measures small open-weight models on the two things an LLM might do for
English Saathi, separately from the app (the app has no LLM):

- **Task A, content drafting:** given an English sentence, draft the Hindi
  translation, the chunks, the syllable splits and a sound hint per hard
  word, in the lesson JSON format.
- **Task B, live explanation:** given the sentence, what the recognizer heard
  and the first mistake (from the app's own aligner), write 1–2 sentences of
  simple Hindi explaining it.

For every answer it records time to first token, tokens per second, prompt
speed, the stop reason, and the peak memory of the Ollama server and its
runner, sampled every 0.25 s. The app's speech model is loaded in the same
process for the whole run, so the memory figures are for both together.

**Nothing here judges the Hindi.** The outputs go to `review.md` and
`scoring-sheet.csv` for a fluent Hindi speaker to score. The only automatic
checks are mechanical: valid JSON, the right keys, chunks that cover the
sentence, Devanagari present, at most two sentences.

```sh
python3 make_cases.py                      # cases.json: 10 sentences, 8 mistake cases
OLLAMA_HOST=127.0.0.1:11435 ollama serve &  # a server of your own, CPU-only options are sent per request
python bench.py --host 127.0.0.1:11435 --server-pid <pid of ollama serve> \
  --models llama3.2:1b gemma3:4b qwen3:4b \
  --speech-model ../../app/src/main/assets/asr/whisper-tiny.en \
  --clip ../../app/src/androidTest/assets/asr-test/librispeech-0.wav --out results
```

`bench.py` needs the `sherpa-onnx` and `soundfile` packages when
`--speech-model` is given (the same environment as `tools/asr-compare`).
Models that have a thinking mode (`qwen3*`) are run with it off.

Some community GGUF files come with a wrong chat template. `model_overrides.json`
fixes them per model: `stop` adds stop markers, and `prompt` replaces the
template with a raw prompt in which `{system}` and `{user}` are filled in.
Each entry says why it is needed. Text a model writes inside `<think>` tags is
counted as `thinking_chars`, so an empty answer can be explained.

`report.py` merges runs (a later directory wins, so a rerun goes last) and can
write a shorter review for chosen models:

```sh
python report.py --out results results/results-llm results/results-llm2 results/rerun
python report.py --out results results/results-llm results/results-llm2 results/rerun \
  --models gemma3:4b qwen3:4b-instruct --prefix short-
```

On an Intel Mac, `tools/bench-intel/run_on_intel_mac.sh` runs this together
with the speech benchmarks.
