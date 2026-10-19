# Speech model comparison (before milestone b)

Run on 2026-10-19 with sherpa-onnx 1.13.8. Asked by the owner: does a
smaller English model in sherpa-onnx (such as Moonshine tiny) or a fully
quantized Whisper decoder get well under 100 MB with similar accuracy on
the five bus-stand sentences? Keep Whisper tiny.en unless something beats it.

## Answer

**Whisper tiny.en with a fully quantized decoder beats the official int8
files.** It is 44.3 MB instead of 103.6 MB, and its accuracy is the same:
over all 1,908 scored words it got 11 right that the official model got
wrong, and 12 the other way round (p = 1). It is also faster and uses less
memory. It is the same model; only the storage of one table changed.

**Moonshine tiny does not beat Whisper.**
- The 2026 version (44.2 MB) is 3–5× faster, but less accurate (p < 0.001):
  it catches fewer learner mistakes, flags more correct words, and writes
  "You" when nobody speaks.
- The 2024 version is larger than Whisper (124 MB) and also less accurate.

**Decision, following the owner's rule:** the app uses Whisper tiny.en with
the fully quantized decoder. At 44.3 MB it is under the spec's ~80 MB limit,
so it is bundled in the app's assets and Play Asset Delivery is not needed.

## Side by side

MacBook Pro M4, 2 threads unless noted. Accuracy is scored with the app's own
normalizer and aligner (a Python port checked against the Kotlin golden
cases).

| | Whisper int8 (official) | **Whisper, fully quantized decoder** | Moonshine tiny 2026 | Moonshine tiny 2024 | Whisper fp32 (reference) |
|---|---|---|---|---|---|
| Model files | 103.6 MB | **44.3 MB** | 44.2 MB | 124.0 MB | 153.0 MB |
| Zipped (≈ download) | 62.4 MB | **26.8 MB** | 29.0 MB | 104.8 MB | 90.1 MB |
| Words right, correct readings | 75.4% | **75.1%** | 72.6% | 70.8% | 76.4% |
| Sentences fully right | 37.0% | **36.3%** | 37.4% | 39.6% | 37.4% |
| Learner mistakes caught | 90.6% | **90.6%** | 83.3% | 82.3% | 90.6% |
| Correct words flagged in mistake clips | 15.2% | **14.4%** | 21.2% | 19.0% | 14.1% |
| Word by word vs official int8: better / worse | – | **11 / 12 (p = 1)** | 111 / 179 (p < 0.001) | 105 / 191 (p < 0.001) | 41 / 22 (p = 0.02) |
| Silence or noise → "nothing heard" | 6 of 6 | **6 of 6** | 0 of 6 ("You") | 6 of 6 | 6 of 6 |
| Median time per clip, 2 threads | 101 ms | **60 ms** | 20 ms | 17 ms | 98 ms |
| Median time per clip, 1 thread | 115 ms | **79 ms** | 23 ms | – | – |
| Load time | 0.25 s | **0.18 s** | 0.18 s | 0.30 s | 0.19 s |
| Peak memory added | 389 MB | **370 MB** | 212 MB | 276 MB | 453 MB |

Speech clips average 3.6 s. The Mac is far faster than a cheap phone: these times
only rank the models. Memory is the peak of a Python process on macOS, so
it also only ranks them. Milestone b measures both on the emulator.

## The fully quantized decoder

sherpa-onnx's export quantizes only the decoder's matrix weights. The token
embedding (51,864 × 384 numbers, 80 MB) stays in float32. It is used twice:
to look up each input token, and to turn the decoder's output into scores
for every token.

`tools/asr-compare/quantize_whisper_decoder.py` stores that table as int8,
with one scale per token. The rest of the official int8 decoder is
unchanged, and the build is reproducible (checked by SHA-256 in
`fetch_models.py`). Two variants were tested:

- **storage:** int8 only in the file. ONNX Runtime turns it back into
  float32 when the model loads. Accuracy is the same (11 / 6 vs official,
  p = 0.33), but it saves no memory and no time.
- **compute:** the table stays int8 in memory, and the output scores are
  computed in int8, like the decoder's other matrices. This is the chosen
  one: same accuracy, 30–40% less decode time.

## Tail padding (Whisper only)

sherpa-onnx appends silent frames after the audio so Whisper can find the
end of the sentence. Its Kotlin default is 1000 frames (10 s); its C++ header
suggests 50 for English. Measured with the chosen model:

| Tail padding | Words right | Mistakes caught | vs official int8: better / worse | Median, 2 threads | Peak memory added |
|---|---|---|---|---|---|
| 50 | 74.5% | 85.4% | 45 / 70 (p = 0.02, worse) | 38 ms | 230 MB |
| 300 | 74.7% | 90.6% | 28 / 39 (p = 0.22) | 39 ms | 256 MB |
| **1000 (default)** | 75.1% | 90.6% | 11 / 12 (p = 1) | 60 ms | 370 MB |
| 3000 (Whisper's 30 s window) | 76.9% | 91.7% | 55 / 35 (p = 0.04, better) | 122 ms | 524 MB |

The app starts with 1000. 300 is the fallback if a cheap phone is too slow,
and 3000 is worth trying if it's fast enough. 50 loses accuracy: it misses
"When the bus will come?" in 7 of 8 clips.

## How it was tested

375 clips made with the macOS system voices (`make_test_audio.py`).
- **270 correct readings:** the 5 sentences × 6 voices × 3 speaking styles
  × 3 recording conditions.
  - Voices:
    - 3 Indian English voices (Aman, Rishi, Tara);
    - the Hindi voice Lekha, reading the English text and also reading it
      written in Devanagari (वेयर इज़ द बस स्टॉप?) for a strong Hindi accent;
    - one US voice as a control.
  - Styles: normal, slow, and halting (a pause between every word).
  - Conditions:
    - clean;
    - noisy: background talk at 10 dB below the speech;
    - phone-like: narrow band, quiet and with hiss.
- **96 mistake clips:** 12 typical learner mistakes, 4 voices each, clean and
  noisy. Examples: "Where is bus stop?", "Van ticket to Patna", "When the bus
  will come?", "Does this bus goes to Bettiah?".
- **9 clips without speech:** room tone, noise, and background talk.

Every speech clip has 0.4 s of silence before the speech and 1.2 s after it,
as the recorder will capture it. "Mistakes caught" means a word that was really said
wrong was not marked correct. The paired test compares two models on the same
words (exact sign test).

## Limits of this test

- **These are synthetic voices, not learners.** Children and adults from
  Bihar, cheap microphones and village noise will differ. The tools can
  re-run on real recordings: see "Testing with real recordings" in
  `tools/asr-compare/README.md`. A few pilot recordings, each with a note of
  what was actually said, would show whether the ranking holds.
- **Speed and memory were measured on a Mac.** The phone numbers come from
  the emulator in milestone b, and then from a real 2–3 GB phone.
- **Results vary slightly with the thread count.** With 1 thread the chosen
  model scored 75.6% words right instead of 75.1%. Differences that small
  are noise.

## Findings that affect the lesson content (milestone c)

- **Place names.** Every model mostly failed on them. These will need the
  owner's decision before `bus_stand.json` is written.
  - **Patna** was right in only 7–9 of 54 clips with Whisper (3–4 with
    Moonshine). Whisper usually heard "but now", which is two words, so a
    one-word `accept` list cannot fix it. Other versions heard: partner,
    padna, pugna, not.
  - **Bettiah** was right in none of the 54 clips, for every model. It was
    heard as betia, bethea, betchia, better, here, beta, and others.
- **"to" before Patna** came out as "but" or "two" ("1 ticket 2, but now
  please"). "two" and "too" could go in the `accept` list for "to".
- **"How match is the ticket?"** was heard as "much" by every model, in 0 of 8
  clips (1 of 8 with tail padding 50). Some vowel mistakes will go unnoticed
  whatever model is used.
- **Hardest audio:**
  - The chosen model got 56% of words right for the Hindi voice reading
    Devanagari, against 77–86% for the three Indian English voices.
  - Phone-like audio cost 14–19 points of words right.
  - "bus" was the word most often missed in noisy and phone-like audio
    (heard as first, must, boss, but).
