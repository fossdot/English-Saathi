# Speech on the desktop (milestones D1 and D2)

Speech in: Whisper through sherpa-onnx 1.13.8's Java library, behind the
same `Transcriber` interface as the Android app (`docs/desktop-plan.md`,
D1). Speech out: the macOS voices (D2, "Speech out" below).
Measured on 2026-10-20 on a MacBook Pro with an Apple M4, both natively and
with Intel Java under Rosetta. **The real 2019 MacBook Pro hasn't run it
yet** (see the end of this page).

## What runs

- **Library:** `sherpa-onnx-jvm-1.13.8.jar` and the native libraries from
  the same GitHub release (onnxruntime 1.28.2), downloaded and checksummed
  by `scripts/fetch_deps.sh`. The app loads the native libraries for its
  processor from `desktop/native/osx-x64/` or `desktop/native/osx-aarch64/`
  instead of letting sherpa-onnx unpack them into `$TMPDIR` on every start.
- **Models:** tiny.en is the Android app's 44.3 MB build (same files);
  small.en is the official int8 build (375.5 MB, Hugging Face
  `csukuangfj/sherpa-onnx-whisper-small.en` at a fixed commit).
- **Settings:** as on Android (greedy decoding, no prompt, tail padding
  1000), plus `debug` off: the Java library's default prints the whole
  configuration on every load.
- **Errors:** a missing model file is an ordinary error on the JVM (on
  Android it ends the app). A damaged model file, a second waveform on one
  stream or a decoding method other than greedy still end the whole
  process, so the build checks every file against `scripts/deps.sha256`.
- **The check:** `./gradlew :desktop:speechCheck` runs the LibriSpeech clip
  and the five bus-stand clips (`scripts/make_test_wavs.sh`, the "Rishi"
  voice) through the app's own `Recorder`, `SpeechEngine` and domain
  scoring, one model per process. Each clip is recognized 4 times: the
  first run, then the median of the other three.

## Results on the Apple M4

**Same transcripts as Python.** Python sherpa-onnx 1.13.8 with the same
files and the same samples gave exactly the same text as the Java library
for all 12 model-and-clip pairs, on each processor type (24 of 24).

The Intel and Apple silicon libraries don't always agree with each other,
because their arithmetic differs slightly: "One ticket to Patna, please"
came out as "One ticker two Patna" (tiny.en, Apple silicon) and "One
Tikatu Patna" (tiny.en, Intel); small.en wrote "One ticket" on Apple
silicon and "1 ticket" on Intel (which counts as right).

Time per clip after the end of the recording (median); recordings are
2.4–3.2 s long, the LibriSpeech one 7.3 s:

| | tiny.en, bus clips | tiny.en, LibriSpeech | small.en, bus clips | small.en, LibriSpeech | Load | Highest memory |
|---|---|---|---|---|---|---|
| M4 native, 4 threads | 0.07–0.08 s | 0.18 s | 0.49–0.64 s | 1.61 s | 0.3 s / 1.2–1.5 s | 395 / 1,238–1,351 MB |
| M4, Intel Java under Rosetta, 2 threads | 0.33–0.38 s | 0.65 s | 3.11–3.41 s | 5.77 s | 0.6 s / 2.3 s | 347 / 1,205 MB |
| same, 4 threads | 0.20–0.23 s | 0.44 s | 1.66–2.03 s | 3.24 s | 0.6 s / 2.0 s | 353 / 1,036 MB |
| same, 6 threads | 0.17–0.20 s | 0.41 s | 1.47–1.75 s | 3.34 s | 0.6 s / 2.3 s | 372 / 965 MB |

- Memory is the whole Java process (`ps`), sampled during the run.
- The very first load under Rosetta took 8.2 s while Rosetta translated
  the library; later loads took 0.6 s.
- **Words right:** both models got LibriSpeech word for word everywhere.
  On the bus clips small.en missed only "Bettiah" (heard "Bethea"); tiny.en
  also missed "ticket" and "to" in sentence 3. Both place names are to be
  unscored and "two" to match "to" (D4).
- **Rosetta is not the real machine.** Under Rosetta, onnxruntime runs
  without AVX2; a real i7/i9 runs AVX2 code on slower cores. Only the
  MacBook run decides small.en's wait. The rule agreed for the model
  switch's starting position: small.en if a 3-second clip takes 1.5 s or
  less there, otherwise tiny.en.

## Running it on a MacBook

The MacBook trip checklist, `docs/macbook-trip.md`, covers this check and
D3's microphone test in one visit. The D1 part on its own, on the 2019
MacBook Pro, once:

1. Install **Amazon Corretto 17** for macOS x64 (the `.pkg`) from
   https://aws.amazon.com/corretto/. Not Temurin: its Java can't use the
   microphone, which D3 needs (`docs/desktop-plan.md`, "Version set").
2. If the speech benchmark ran on this MacBook, the rest is already
   installed. Otherwise, in Terminal: `xcode-select --install`, then
   Homebrew (https://brew.sh) and `brew install python@3.12` (used once, to
   build the tiny.en decoder).
3. Get the repo: `gh repo clone fossdot/English-Saathi ~/English-Saathi`,
   or `git pull` in an existing copy. Download the large files on the
   MacBook itself (step 4 does it): files copied over AirDrop are marked as
   downloaded, and macOS may then refuse to load the speech library.

Then, with the MacBook plugged in and other apps closed:

```sh
cd ~/English-Saathi
caffeinate -i scripts/desktop_speech_check.sh
```

It needs internet the first time (Gradle, libraries and models: about
1 GB) and takes about 10 minutes. Send back
`desktop/build/speech-check/x86_64.log`.

## Speech out (D2)

- **Voices:** the app reads `say -v '?'` once and picks an Indian English
  voice (en_IN) for sentences and words and a Hindi voice (hi_IN) for
  instructions: the best quality installed (Premium, then Enhanced), then
  Rishi and Lekha first, since every Mac has a basic version of both. `say`
  never reports a missing voice (it quietly uses another), so the app checks
  the list first.
- **Missing voices:** a message in English and Hindi gives the download
  steps in Apple's wording for the Mac's macOS version (Tahoe: System
  Settings › Accessibility › Read & Speak › System voice › ⓘ). Without an
  Indian English voice, sentences use another English voice, as in the
  pilot. The Hindi lines are drafted and need the teacher review.
- **Speeds:** `say` renders each text once at its normal rate, into a
  temporary file that is deleted as soon as it is read. The app then
  stretches it with Sonic (`NOTICE`) to the pilot's speeds, keeping the
  pitch, and plays it with Java Sound: 0.9 for sentences, 0.6 for "Slowly",
  0.95 for Hindi help. `say -r` can't do this: rate 105 instead of 175 made
  speech only 1.2× longer.
- **Text** goes to `say` as UTF-8 on standard input: Java 17 would encode a
  Hindi command-line argument by the system locale.

Measured on the M4 (`./gradlew :desktop:voiceCheck -Psilent`):

| Speech | 1× | Played at | Length | Ready after |
|---|---|---|---|---|
| Rishi, "Where is the bus stop?" | 1,320 ms | 0.9× | 1,474 ms | 880 ms (`say`) |
| same | | 0.6× | 2,204 ms | 9 ms (stretched from the first) |
| Rishi, "ticket" | 433 ms | 0.6× | 723 ms | 851 ms (`say`) |
| Lekha, a Hindi instruction | 4,226 ms | 0.95× | 4,448 ms | 845 ms (`say`) |

- 0.6× makes speech exactly 1.67× as long, single words included.
- `say` takes about 0.85 s per new text here, longer on an Intel MacBook.
  Screens should prepare their speech when they appear, so Listen plays at
  once; everything stays in memory for the session.
