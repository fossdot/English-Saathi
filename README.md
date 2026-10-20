# English Saathi (इंग्लिश साथी)

Package ID `net.bodhya.speak`.

Offline Android app that helps Hindi speakers in rural Bihar practise spoken
English. A learner says a sentence; the app finds the words that went wrong
and walks them through fixing them.

**Status:** milestones a and b of v0.1 are done.
- a: the domain logic and its tests.
- b: recording and on-device speech recognition, proven on test WAVs on the
  emulator. The app has one temporary "Speech check" screen.

The practice screens come in milestones c–f. Milestone c is on hold until
the student pilots of the web version finish. This README will grow into the
full one at the end of v0.1.

## Layout

| Path | What |
|---|---|
| `domain/` | Pure Kotlin rules, with no Android imports: normalizer, aligner, scoring, tip selection, fixing loop, streak, Leitner box, sentence choice |
| `speech/` | Speech code shared by the Android and desktop apps, with no Android imports: recording, end-of-speech detection, the speech engine |
| `app/` | The Android app. `speech/` holds the microphone and the Whisper recognizer; `ui/` holds the screens |
| `desktop/` | The macOS desktop app (`docs/desktop-plan.md`). So far: Whisper through sherpa-onnx's Java library, the speech check (`docs/desktop-speech.md`) and the lesson loader |
| `content/` | Lesson files in the agreed format (`docs/decisions.md`, "Word data scope") and the tip templates; all marked for teacher review |
| `scripts/` | `fetch_deps.sh` downloads the speech library and model; `make_test_wavs.sh` makes test clips (macOS) |
| `docs/decisions.md` | Everything agreed with the product owner, including where the Kotlin rules deliberately differ from the pilot |
| `docs/asr-model-comparison.md` | Which speech model the app uses, and why |
| `docs/speech-on-device.md` | Speed and memory on the emulator, and what to test on a real phone |
| `docs/later.md` | Ideas kept out of v0.1 |
| `docs/pilot/` | The web pilot; the reference implementation for the rules |
| `design/` | Screen designs (one HTML file with all six screens) |
| `tools/pilot-golden/` | Records the pilot's behaviour as golden test cases for the Kotlin port |
| `tools/asr-compare/` | Compares on-device speech models on the lesson sentences; builds the quantized Whisper decoder |

## Setup

You need:
- JDK 17;
- the Android SDK with platform 36 and build tools 36.0.0;
- `python3` and `curl`.

The Gradle wrapper downloads Gradle 9.7.1.

```sh
export JAVA_HOME=/path/to/jdk-17
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # your SDK path
scripts/fetch_deps.sh        # speech library, model, test clip (checksummed)
scripts/make_test_wavs.sh    # optional, macOS only: lesson sentences for the debug build
./gradlew :app:assembleDebug
```

`fetch_deps.sh` gets these files, which are not kept in git:
- the sherpa-onnx 1.13.8 library (the Android archive, and for the desktop
  app the Java library and its macOS native libraries);
- the Whisper tiny.en files, and small.en (int8) for the desktop app;
- a LibriSpeech test clip.

It then builds the quantized decoder with
`tools/asr-compare/quantize_whisper_decoder.py`, and checks that the result
matches the tested file byte for byte.

The build fails early with a hint if these files are missing or differ from
the checksums in `scripts/deps.sha256`. It also fails if any merged manifest
asks for the internet permission: the app must stay offline. That check runs
for every APK, bundle and install.

## Running the tests

```sh
./gradlew :domain:test                  # domain rules, including the pilot golden cases
./gradlew :speech:test                  # recording, end of speech, speech engine
./gradlew :app:testDebugUnitTest        # answer check, memory figures (JVM)
./gradlew :desktop:test                 # desktop speech recognizer (with the real models) and lesson loader
./gradlew :desktop:speechCheck          # times both models on the test clips (docs/desktop-speech.md)
./gradlew :app:connectedDebugAndroidTest  # on a running emulator or phone
./gradlew check                         # unit tests, lint and the offline check
```

- **Domain tests:** hand-written unit tests plus `PilotGoldenTest`. That test
  replays several thousand cases recorded from the pilot's own JavaScript,
  with the agreed changes applied. To re-record them after the pilot changes,
  see `tools/pilot-golden/README.md`.
- **On-device tests:** they recognize the LibriSpeech clip word for word and
  log speed and memory with the tag `EnglishSaathiTest`. For a clean memory
  figure, run the first test on its own:

  ```sh
  ./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=net.bodhya.speak.speech.WhisperOnDeviceTest#recognizesTheLibriSpeechClipAndMeasuresTheEndToEndPipeline
  ```

- **Microphone tests:** these are skipped on devices whose microphone can't be
  opened, such as the Android 8.0 emulator image.
