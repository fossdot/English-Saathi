# macOS desktop app: plan

Agreed with the owner on 2026-10-20. The answers behind it are in
`docs/decisions.md` ("Desktop app: owner answers"). Target: a 2019 16-inch
Intel MacBook Pro (16 GB, macOS up to Tahoe 26), shared by students taking
turns with a facilitator present. Fully offline, no LLM, run from Gradle
for now (no signing or notarization).

**Go-ahead:** D1–D4 now. D5–D9 wait until this week's student pilots are
done, because the pilots may change the practice and feedback screens.
**Changed 2026-10-21:** students use the desktop app for the next three
months, so D5–D8 start now, in the pilot's layout until the desktop screen
designs arrive; D8 (results and CSV) is in scope from the start, as the
evidence at the end of the three months.

Milestones are called D1–D9 so they don't clash with the Android
milestones a–f. "MacBook run" means one command on a real 2019 MacBook
Pro, with the log sent back.

## Milestones

**D1. sherpa-onnx on the JVM (MacBook run)**

Status: done on the Apple M4, natively and under Rosetta
(`docs/desktop-speech.md`); waiting for the MacBook run.

- Move the speech code that doesn't depend on Android (`Transcriber`,
  `SpeechEngine`, `Recorder`, `Endpointer`, `WavReader`, `AsrSettings`) and
  its tests from `:app` into a new plain Kotlin module, `:speech`. Android
  behaves the same and still builds.
- New `:desktop` module, plain JVM for now: a Whisper recognizer behind
  `Transcriber`, for tiny.en and small.en int8.
- `scripts/fetch_deps.sh` and `scripts/deps.sha256` also get the
  sherpa-onnx 1.13.8 JVM jars and small.en. The native libraries load from
  the checksummed folder, not unpacked into `$TMPDIR` on every start.
- A check task runs the LibriSpeech clip and the five bus-stand clips
  through `Recorder`, `SpeechEngine` and the domain scoring with each model,
  and logs transcript, model, load time, time per clip and memory.
- Done when, here under Rosetta (Intel Corretto) and then on the MacBook:
  both models get the LibriSpeech clip word for word, the transcripts match
  Python sherpa-onnx's for the same clips, and small.en's wait on the
  MacBook is known.

**D2. Voices**

Status: done on the Apple M4 (`docs/desktop-speech.md`, "Speech out"). The
voices on the MacBook are checked in D3's MacBook run.

- Read `say -v '?'` once; pick an Indian English voice for sentences and
  words and a Hindi voice for instructions, preferring Enhanced or Premium
  ones. Check a voice exists before using it: `say` silently uses another
  voice when the named one is missing.
- `say` writes speech to a temporary file; the app plays it with Java
  Sound. "Slowly" is a time-stretch to 0.6× with the pitch kept, so single
  words (fixing step 1) are really slower. Normal speech is 0.9×, Hindi
  help 0.95×, as in the pilot. The time-stretch code is our own or a
  permissively licensed library, named to the owner before it is added.
- Text goes to `say` as UTF-8 (JDK 17 would otherwise encode Hindi
  arguments by the system locale). Speech stops before the microphone
  opens.
- A missing voice gets a message in English and Hindi with the download
  steps for the Mac's macOS version (Tahoe: System Settings ›
  Accessibility › Read & Speak › System voice › ⓘ; Sequoia and Sonoma say
  "Spoken Content").
- Done when the voice-list parser and the time-stretch are tested and a
  check command plays a sentence, its slow version and a Hindi line.

**D3. Microphone and the first window (MacBook run)**

Status: built and tested on the Apple M4 without opening the microphone
(that would prompt on this Mac); waiting for the MacBook trip,
`docs/macbook-trip.md`, which covers D1 too. Start it with
`scripts/run_desktop.sh`.

- 16 kHz mono 16-bit capture with Java Sound into the same `Recorder` and
  `Endpointer`: milestone b's silence rules unchanged. The line is reopened
  for every answer, so it follows input changes.
- Permission: run from Terminal, macOS asks whether Terminal may use the
  microphone. A one-second microphone check on first start makes the prompt
  appear while the facilitator is there. A denied or unanswered prompt
  gives silence, not an error; the "a second of exact zeros" rule catches
  it and the app explains in English and Hindi how to allow Terminal
  (System Settings › Privacy & Security › Microphone). At start the app
  also checks that its Java may use the microphone.
- `scripts/run_desktop.sh` starts the app with Corretto 17 and a fresh
  Gradle daemon (so the permission is Terminal's), offline once everything
  is downloaded.
- A Speech check window like Android's: record, level meter, transcript,
  wait time, Listen buttons, the microphone's name, and the model switch in
  Settings (⌘,, no visible button). Every result is logged with its model.
  Compose asks for the integrated GPU.
- Done when, on the MacBook in the session room: the window draws, Space,
  L, S and Enter work, recording ends about 1.2 s after speech, and "Didn't
  hear you" and the blocked-microphone message appear when they should.

**D4. Domain rules and the lesson file**

Status: done. `:domain` has the two rules and the lesson-level word table;
`content/bus_stand.json` and `content/hints.json` are the lesson files;
`LessonLoader` (desktop) reads them with the checks.

- Build homophones and unscored words as decided, with the word table moved
  to the lesson. Update the golden overlay and re-record the cases, as
  `docs/decisions.md` describes. `:app` doesn't use these types.
- A loader for the decided lesson JSON, with the load checks
  `docs/decisions.md` lists. `bus_stand.json` from the pilot: Patna and
  Bettiah unscored with no accept lists, the five drafted words, the
  pilot's five hints.
- Done when every domain test passes, and the only tests that changed are
  the ones `docs/decisions.md` says will change.

**D5. Saved data, student picker, home**

Status: built and tested on the Apple M4 (headless and off-screen tests);
the student screens are in the MacBook trip's part D.
- `~/Library/Application Support/English Saathi`: learners, progress
  (Leitner order kept), attempt log, latest recording per sentence per
  learner (sentence attempts only), settings. Excluded from Time Machine.
  A write never leaves a half-written file; a second copy of the app
  refuses to start.
- Student picker: a colour-and-symbol card the learner recognises, plus a
  name the facilitator types for Results and the CSV. The app always starts
  here.
- Home: today's goal, sentences with best stars, locked places (Market,
  Bank, Hospital), streak with "paused" and 0, XP, Hindi help button.
- A facilitator action to delete a student, with a confirmation step: it
  removes their progress, attempt-log rows and recordings.

**D6. Practice and feedback**

Status: built and tested on the Apple M4 (headless and off-screen tests);
in the MacBook trip's part D.
- Two-column layout, Listen and Slowly, the mic button, Space/L/S/Enter as
  in the pilot.
- Chips (neutral for unscored words), stars, praise, "n of m words
  correct", "We heard", tip, XP, Fix/Next/Skip.
- Model-loading and microphone states. A result arriving after the learner
  moved on is dropped.

**D7. Fixing loop and goal screen** (after the pilots)
- Five steps with the step list, Skip always visible, back one step after
  two failures.
- The goal screen once per learner per day, with "Practise again tomorrow"
  (words shown as written).

**D8. Results, CSV, Settings** (after the pilots)
- The pilot's two results tables; CSV through the macOS Save dialog with
  the pilot's nine columns plus `model`, `result` and `wait_ms`.
- Settings (⌘,): model, voices found, microphone check, open the data
  folder. The "To be checked by an English teacher" note appears on Results
  and Settings only.

**D9. Trial (MacBook run)** (after the pilots)
- A facilitated session, several students in turn, Wi-Fi off. Check speed
  per model, memory, battery and GPU, voices, and that the CSV opens in
  Excel; make the fix list.

## Version set

Checked on 2026-10-20 in a throwaway copy of the repo with a `:desktop`
module in the same build (Gradle 9.7.1, JDK 17, Apple M4): the Compose
1.10.3 plugin works with Kotlin 2.4.21 and its Compose compiler, with no
warnings; a frame rendered off screen with Hindi shaped correctly;
`:domain:test` (144 tests) passed and `:app:compileDebugKotlin` compiled in
the same build; `:desktop:run` worked with no Android SDK configured.
Nothing changes for the domain module.

| Piece | Version | Note |
|---|---|---|
| Kotlin, Compose compiler and serialization plugins | 2.4.21 | unchanged |
| Gradle; JDK for building | 9.7.1; 17 | unchanged |
| JDK for running the app | Amazon Corretto 17 x64 on the MacBooks | see below |
| Compose Multiplatform | 1.10.3 (Skiko 0.9.37.4) | last version that officially supports Intel Macs; Skiko's x86_64 library needs macOS 10.15+ |
| UI library | Compose foundation 1.10.3 + our own theme from the pilot's CSS | Material 3 for 1.10.3 is a pre-release (plugin default `1.9.0-beta03`) |
| kotlinx-coroutines and coroutines-swing | 1.11.0 | Compose 1.10.3 doesn't bring the Swing dispatcher |
| kotlinx-serialization-json | 1.11.0 | lesson files and saved data |
| sherpa-onnx | 1.13.8: `sherpa-onnx-jvm`, `sherpa-onnx-native-lib-osx-x64` (+ `-osx-aarch64` for development on Apple silicon) | GitHub release with checksums, not JitPack; onnxruntime 1.28.2, macOS 10.15+, Java 8 class files |
| Speech models | tiny.en (the Android app's 44.3 MB files), small.en int8 (375.5 MB) | fetched and checksummed, not in git |
| Font | Mukta (SIL Open Font License), bundled | the pilot's font, which the pilot loads online |
| Tests | JUnit 4.13.2; Compose `ui-test-junit4` 1.10.3 | |

1.10.3 deprecates the `compose.*` dependency shortcuts, so the version
catalog names each library in full.

**Why Corretto, not Temurin.** Temurin 17's `java` (17.0.20.1, checked) is
signed with Apple's hardened runtime but without the
`com.apple.security.device.audio-input` entitlement, which Apple requires
for microphone access; temurin-build#1720 shows the result, recordings of
pure silence. Corretto 17 and Microsoft's OpenJDK 17 have the entitlement
(checked). Homebrew's `openjdk@17` isn't hardened and works too.

## Risks, most serious first

1. **The microphone is silently blocked by the wrong Java.** Covered by
   Corretto, the start-up check and the zero-sample rule.
2. **Results differ from the pilot's.** The pilot uses Chrome's online
   recognizer; the app uses Whisper offline. On correctly read synthetic
   sentences tiny.en marks a word wrong in 43% of readings, small.en in
   22%. Both models stay switchable and logged until student recordings
   decide the default.
3. **small.en may be too slow on the MacBook.** 1.4 s per 3-second clip
   under Rosetta on the M4; the real i7/i9 is unknown until D1.
4. **`say -r` barely slows speech.** Rate 105 instead of 175 makes a
   sentence and a single word only about 1.2× longer, so "Slowly" is a
   time-stretch instead.
5. **Some sherpa-onnx errors end the whole process** instead of raising an
   error: a corrupt model, a second waveform on one stream, a decoding
   method other than greedy. A missing model file is an ordinary error on
   the JVM. Covered by checksums before loading and one recognition thread.
6. **The microphone can fail in other ways.** The permission belongs to
   Terminal, or to whichever app first started the Gradle daemon; a closed
   lid disconnects the built-in microphone in hardware; macOS may switch
   input to AirPods or a nearby iPhone; Bluetooth headsets drop to low
   quality while recording. The Speech check shows which microphone is in
   use.
7. **The silence rules may not suit a classroom.** The levels were tuned
   on synthetic and emulator audio; students waiting nearby may keep a
   recording running to 8 s. D3 measures in the room; the levels are
   settings.
8. **Compose 1.10.3 gets no more fixes.** Metal drawing on the AMD Radeon
   is unconfirmed (fallback: software rendering). On this GPU pair, Skiko's
   automatic choice picks the AMD GPU, which costs battery, so the app asks
   for the integrated GPU.
9. **Voices have to be set up on each MacBook.** Only basic Rishi and Lekha
   come with macOS; clearer voices are a one-time download. The macOS
   licence allows the system voices "while running the Apple Software" but
   not recording or sharing them (our reading, not legal advice). The app
   keeps rendered speech only in a temporary file for playback and never
   shares it.
10. **Running from Gradle needs setup.** The first run on each MacBook
    needs internet (Gradle, libraries, models: about 1 GB) and Terminal
    stays open while the app runs. Packaging as a .app comes after D9
    (`docs/later.md`).
11. **Shared laptop.** All progress and recordings are in one macOS
    account, visible to anyone at the Mac. Every new Hindi message needs
    teacher review.

## Where the pilot and decisions.md disagree

The desktop app follows `docs/decisions.md` wherever the two differ. The
owner's answers on the points decisions.md didn't settle (profiles, locked
places, labels, streak wording, the teacher-review note, CSV columns, start
screen) are in `docs/decisions.md`, "Desktop app: owner answers".
