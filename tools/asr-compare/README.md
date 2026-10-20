# Speech model comparison

Compares on-device English speech models in sherpa-onnx on the bus-stand
sentences, scoring the transcripts with the app's own word matching. The
results and the decision are in `docs/asr-model-comparison.md`.

Requires Python 3.12. Making the test audio requires macOS, because it uses
the system voices.

```sh
cd tools/asr-compare
python3.12 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/python appmatch.py                     # checks the matcher against the golden cases
.venv/bin/python fetch_models.py --models work/models
.venv/bin/python make_test_audio.py --out work/audio
for c in whisper-int8 whisper-fq-compute moonshine-tiny-v2-quantized; do
  .venv/bin/python run_asr.py --models work/models --audio work/audio --config $c --out work/results
done
.venv/bin/python score.py --models work/models --audio work/audio --results work/results > work/report.md
```

`run_asr.py --help` lists every configuration (Whisper tiny.en, base.en and
small.en, Moonshine). Run each one in its own process: the memory figure is
the process's peak. `--tag` adds a label to the run name, e.g. the CPU
architecture.

Other measurements:

```sh
.venv/bin/python score.py ... --app-rules            # score as the app will: place names unscored, to/two/too etc. equal
.venv/bin/python bench_speed.py --models work/models --config whisper-base.en-int8 \
    --clip ../../app/src/androidTest/assets/asr-test/librispeech-0.wav --threads 6   # seconds per 3-second clip
.venv/bin/python run_whispercpp.py --cli whisper.cpp/build/bin/whisper-cli --model ggml-base.en.bin \
    --audio work/audio --threads 6 --config whispercpp-base.en --out work/results   # whisper.cpp, same scoring
```

On Apple Silicon, `arch -x86_64 python ...` with an x86_64 Python and the
x86_64 wheels runs the Intel code under Rosetta (see
`docs/intel-macbook-benchmarks.md` for what that does and doesn't tell you).

## Files

- `appmatch.py`: the app's normalizer and aligner, ported from
  `domain/` and checked against the same golden cases as the Kotlin tests.
- `fetch_models.py`: downloads the models, checks their SHA-256 and builds
  the two quantized Whisper decoders.
- `quantize_whisper_decoder.py`: stores the Whisper decoder's token embedding
  as int8 (see the script's docstring for the two modes).
- `make_test_audio.py`: the test clips and `manifest.jsonl`.
- `run_asr.py`: transcripts, decode time, load time and peak memory for one
  configuration.
- `run_whispercpp.py`: the same for whisper.cpp's `whisper-cli`.
- `bench_speed.py`: seconds per 3-second clip for one configuration.
- `score.py`: the tables.

`work/` is ignored by git. The macOS voices' licence doesn't allow sharing
their recordings, so the test audio is never committed.

## Testing with real recordings

Put 16 kHz mono WAVs in `<dir>/wav/` and write `<dir>/manifest.jsonl`, one
line per clip:

```json
{"clip": "wav/s01-bus_03.wav", "seconds": 3.2, "kind": "correct", "sentence": "bus_03", "target": "One ticket to Patna, please.", "said": "One ticket to Patna, please.", "voice": "s01", "group": "learners", "style": "normal", "condition": "village"}
```

- `kind`:
  - `correct` when the sentence was said right;
  - `mistake` when a word was wrong, missing or added;
  - `nonspeech` for clips with no speech.
- `said`: what the person actually said, written by a listener, not by a
  model. Mistakes are scored against it.
- `voice`, `group`, `style` and `condition`: free labels used to split the
  tables.

Then run `run_asr.py` and `score.py` with `--audio <dir>`.
