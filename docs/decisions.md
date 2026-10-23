# v0.1 decisions

Agreed with the product owner on 2026-10-19: before milestone a, and again
after milestone b ("Content decisions for milestone c"). These settle the
gaps between the spec, the design mockup (`design/`) and the web pilot
(`docs/pilot/english-practice-pilot.html`).

**Status (2026-10-21):** the macOS desktop app comes first and Android is
paused (see "Direction change" below). The desktop plan is
`docs/desktop-plan.md` (milestones D1–D9). D1–D8 are built. Students will
use the desktop app for the next three months, as a normal Mac app with
lessons that can be added later (see "Distribution, defaults and lessons:
owner answers").

**Assessment (2026-10-23):** the monthly assessment, cohort dashboard and
Ubuntu build are designed in `docs/assessment-design.md`, not built. The
owner's first answers, and the change to a monthly check plus a full test
every three months, are under "Assessment design: owner answers".

## Direction change: macOS desktop app first (2026-10-19)

- **Build a macOS desktop app first**, with Compose Multiplatform (desktop,
  JVM). Students practise in sessions on one shared MacBook with a
  facilitator present; the web pilot (`docs/pilot`) shows the target flow.
- **Target machine:** a 2019 Intel 16-inch MacBook Pro (x86_64) with 16 GB
  RAM, not Apple silicon.
- **Android is paused.** It stays in the repo unchanged. If the Mac version
  works well with students, Android resumes.
- **Milestone c is the desktop UI**, and it does not start until the owner
  says go.
- The pure Kotlin domain module is reused unchanged and all its tests must
  still pass.
- Before any app code, the owner asked for a dependency check and speech and
  LLM benchmarks on Intel macOS (`docs/intel-macbook-benchmarks.md`), and
  will give an OK before anything is added to the app.
- **No LLM in the app** (owner, 2026-10-20, after the benchmark). LLMs are
  used only offline, to draft lesson content (Task A in the benchmark), and
  a teacher reviews every draft before it reaches students. qwen2.5:3b is
  out because its licence forbids commercial use.

## Desktop app: owner answers (2026-10-20)

Answers to the desktop plan's questions (`docs/desktop-plan.md`).

- **Domain changes (Q1):** yes. D4 builds the decided "Homophones" and
  "Unscored words" rules and moves the word table to the lesson, as
  described below. The test suite keeps passing; only the cases this file
  says will change, change.
- **Shared speech code (Q2):** a new plain Kotlin module `:speech` holds
  the speech code that doesn't depend on Android, with its tests. `:app`
  depends on it and behaves the same.
- **Speech model default (Q3):** both tiny.en and small.en (int8) stay,
  behind a hidden settings switch, and every attempt logs its model. The
  switch starts on small.en if a 3-second clip takes 1.5 s or less on the
  MacBook (measured in D1), otherwise tiny.en. The final default also
  depends on the student recordings.
- **Backups (Q4):** the desktop data folder is excluded from Time Machine,
  as Android app data is excluded from backups.
- **Slow speech (Q5):** `say -r` barely slows the voices (rate 105 instead
  of 175 makes speech only about 1.2× longer). So `say` writes to a
  temporary file and the app time-stretches it to 0.6× with the pitch
  kept: slow single words matter most, in fixing step 1. The time-stretch
  code must be our own or a permissively licensed library, named to the
  owner before it is added.
  - **Speech is prepared per lesson** (owner, 2026-10-20): when a lesson
    opens, every sentence and chunk (at 0.9 and 0.6) and every word (at 0.6)
    is rendered and stretched in memory, so Listen is instant even where
    `say` is slow. Memory only; nothing is written to disk but `say`'s
    temporary file.
  - **Sonic** (owner's choice, 2026-10-20): `desktop/src/main/java/sonic/Sonic.java`,
    copied unchanged with its licence header from
    https://github.com/waywardgeek/sonic at commit
    `b93885dcb70aae50c6f76b0fe4e0868f029a077e` (the file last changed in
    `1d7051354f32392a3b3cdb8a8bcfe56e6f8b744f`; SHA-256
    `c23fb313fafa9cdab4c9aa291bfdc2426f33a42d0e19e3e8d1cfd3f85258d7df`).
    Apache License 2.0: the licence text is next to it, and `NOTICE` names
    it.
- **Run JDK:** Amazon Corretto 17 on the MacBooks, not Temurin: Temurin's
  `java` lacks the microphone entitlement its hardened runtime needs, so
  it records silence (`docs/desktop-plan.md`, "Version set").
- **Profiles (P1):** on the desktop, a colour-and-symbol card the learner
  recognises, plus a name the facilitator types, used in Results and the
  CSV. The recorded audio name (see "Owner answers") comes later.
- **Locked places (P2):** Market, Bank and Hospital, as decided below (the
  pilot shows only Bank and Hospital).
- **Labels (P3):** English above Hindi everywhere, at least 16 sp, as
  decided below. The pilot's one-line labels ("Skip for now · अभी आगे
  बढ़ें") and its 14–15 px text are not copied.
- **Streak wording (P4):** a paused streak shows "paused" / "रुका हुआ"
  under the count, marked for teacher review.
- **Teacher-review note (P5):** "To be checked by an English teacher"
  appears on the Results and Settings screens only, never on student
  screens (the desktop app run from Gradle has no separate test build).
- **CSV (P6):** the pilot's nine columns in the same order (time, student,
  sentence, mode, target, heard, correct, total, stars), then `model`,
  `result` (scored / passed / failed / not heard) and `wait_ms`. "Didn't
  hear you" attempts are logged with empty counts.
- **Start screen (P7):** the app always starts at the student picker (the
  pilot reopens the last student's home screen).
- **Deleting a student:** a facilitator action with a confirmation step
  removes the student's progress, attempt-log rows and recordings (D5 or
  D8).
- **Recordings on the desktop:** the latest recording per sentence per
  learner, for sentence attempts only (not fixing steps).
- **Show words as written** in "Practise again tomorrow" too, not only in
  the tip, the Fix button and the big word.
- **Packaging:** a .app with a bundled Corretto runtime comes after D9
  (`docs/later.md`).
- **One MacBook trip** covers the D1 speech check and the D3 microphone test
  (owner, 2026-10-20): `docs/macbook-trip.md`.

### Choices made while building D3

Within the plan; each is a setting or a small change.

- **The model switch starts on tiny.en** at the first start until the
  MacBook trip measures small.en (the 1.5 s rule above); Whisper runs with
  4 threads (the trip's D1 check also times 2 and 6). *Changed 2026-10-21:
  small.en is the default (owner).*
- **The microphone check runs at every start**, not only the first: one
  silent second, so the macOS prompt appears while the facilitator is there
  and a microphone that stopped working is caught before a student speaks.
  It is also in Settings.
- **The check log** is `logs/speech-check.log` in the data folder:
  machine, Java and its microphone entitlement, the permission, inputs,
  voices, graphics, and every result with its model. It holds what the
  recognizer heard, never audio.
- **Settings** opens with ⌘, (the standard macOS Settings item): model,
  microphone input, the microphone check, test clips.
- **The chosen model and microphone are kept** in `settings.json` in the
  data folder and restored at start (the retest after the end-of-speech
  fix, 2026-10-21, found both back on tiny.en and the Mac's default).
  When the saved microphone isn't connected, the app uses the Mac's
  built-in microphone (found with `system_profiler`; the Mac's default if
  there is none or the lid is closed) and says so at the top of the
  window; the saved choice stays, for when that microphone is back. The
  main window shows the model in use at the top. *Changed 2026-10-21: with
  nothing chosen, the app uses the built-in microphone, not the Mac's
  default (owner). `settings.json` now says `"microphone": "built-in"`,
  `"mac-default"` or `"named"` (with `"microphone_name"`); files with the
  old `"input"` keep a named choice, and their `null` (written even when
  nothing was chosen) now means the built-in microphone. Settings lists the
  built-in microphone first, marked "(built-in)".*
- **Buttons never take keyboard focus**, so Space and Enter always reach
  the shortcuts instead of clicking the last button used.
- **Leaving the window** (another app comes to the front) stops listening
  and speech, as leaving the Android app does.
- **Mukta is bundled** (2026-10-21): Regular, SemiBold and Bold, the
  weights the pilot loads, version 2.538 from google/fonts at
  `4ed247183585550f5ee6d09665680c282227df41` (SHA-256: Regular
  `2958e4af…9688`, SemiBold `f437fb09…510a`, Bold `7d22891c…0a12`), with
  `OFL.txt` next to them and an entry in `NOTICE`. Text uses the pilot's
  line heights (1.35, big headings 1.15); glyphs Mukta lacks (⌘, ✓) come
  from the Mac's fonts.
- **Icons are drawn**, from the pilot's own SVG paths (`ui/Icons.kt`): the
  mic and stop buttons, stars instead of "3 stars", and the chips' check,
  retry and dash marks, which were text characters.

## Distribution, defaults and lessons: owner answers (2026-10-21)

- **Speech model:** small.en is the default; tiny.en stays as the fallback
  in Settings. Revisit only if the MacBook check shows small.en too slow.
  This replaces Q3's "tiny.en until the MacBook trip measures small.en".
- **Microphone:** the Mac's built-in microphone by default, not "Mac's
  default", so the input can't switch to an iPhone or headphones on its
  own. If the built-in microphone isn't available (none, or the lid is
  closed), the app uses the Mac's default and says so. Settings can still
  choose another, and the choice is saved.
- **Distribution:** students get English Saathi as a normal Mac app from a
  download link. No Terminal, Gradle, Java install or internet after the
  download. Shared through Google Drive for now; GitHub Releases once the
  repo goes public with the final product. Packaging moves from "after D9"
  (`docs/later.md`) to now.
- **Student profiles:** created once; several per Mac. All data stays on
  that Mac. No tracking or sharing for now.
- **Target:** Hikmat Foundation's 2019 Intel MacBook Pros, 16 GB RAM.
- **Lessons are kept separate from the app.** The app ships with the first
  lesson pack inside and also reads extra lesson packs from the data
  folder. Settings › "Add lessons" picks a pack file, checks it with the
  lesson loader, shows what it adds (in English and Hindi) and installs it.
  A bad pack is rejected with a clear message and never breaks existing
  lessons or progress. A pack has a version number, so adding an older one
  never replaces a newer one. App updates replace English Saathi.app only;
  progress and added lessons in the data folder survive (with a test).
- **Lessons unlock in order:** lesson 2 opens once every sentence of
  lesson 1 has been said at least once (any stars). Later lessons show with
  a lock and their names. A Settings switch, "Open all lessons", is for
  testing and for students who already know the basics; it is per student
  (Settings › Students), so one student can skip ahead without opening
  everything for the class.
- **Daily goal with several lessons:** the goal (5 different sentences a
  day) counts sentences from any lesson. Start practice continues the
  learner's current lesson and moves to the next open lesson once every
  sentence in it is done today. (How lessons move on was changed on
  2026-10-22: "Lesson review: owner answers".)
- **Pack format:** text only for now: one `.json` file. The format carries
  its own version number, so a later format can add pictures in a `.zip`
  without breaking installed packs.
- **Commits:** one per part, pushed once its tests pass. The `.dmg` is
  never committed.

### Choices made while building lesson packs

The pack format and its checks are in `docs/lesson-packs.md`.

- **The first pack is `content/packs/starter.json`** (id `starter`,
  version 1): the Bus stand lesson and the pilot's five tips, exactly as in
  the former `content/bus_stand.json` and `content/hints.json`, which it
  replaces.
- **Added packs live in the data folder** as `lessons/<pack id>.json`, a
  copy of the file the facilitator picked, written safely. For each pack id
  the highest version is used (on a tie, the app's own copy), so an app
  update can also bring a newer version of a pack, and an older pack never
  replaces a newer one. A shadowed copy is never deleted.
- **The app's packs always come first** in the unlock order (Bus stand
  stays lesson 1), then added packs by their `order`, then id.
- **Ids are unique within a pack**, ignoring case, and safe as file names:
  progress, the attempt log and the recordings are kept by sentence id. The
  app puts the pack's id in front, so packs can't clash ("Lesson review:
  owner answers"; at first a pack reusing another pack's id was refused). An
  added file that can't be read at start is left out and reported on the
  picker, and the app's own lessons always load.
- **Each learner's open lessons are saved** (`learners/<id>/lessons.json`:
  the current lesson, the lessons opened so far, "Open all lessons"). An
  opened lesson stays open, so a pack update that adds a sentence to the
  lesson before it never locks it again. Learners from before packs get
  their lessons worked out from the sentences they have said.
- **When a lesson opens,** the feedback screen says "New lesson open: …".
  Home lists every lesson after the current lesson's sentences: open ones
  with "n of m said" (tap to make it the current lesson), locked ones with a
  lock and their names. The hard-coded Market, Bank and Hospital rows show
  only while there is a single lesson.
- **Next and Skip also moved on** to the next open lesson once every
  sentence of the current one was done today, as Start practice did.
  Replaced by "Lesson review: owner answers".
- **Adding a pack** goes through Settings › Add lessons: the macOS Open
  dialog (Downloads, `.json` files), the check (nothing changes yet), a
  preview with each lesson's titles in English and Hindi, its sentence count
  and whether it is new or updated, lessons a new version drops, and
  "Students' progress is kept"; then Add these lessons. A learner in the
  middle of a sentence goes back home, as the lesson may have changed.
  There is no way to remove a pack yet.
- **Tips** come from the sentence's own pack (its templates over the app's).
- **Results** count "Sentences tried" against every installed sentence. The
  CSV's sentence column now carries the pack's id (`starter.bus_01`).
- **Speech is prepared** for the learner's current lesson when it changes
  (one lesson at a time, the newest request wins), and for the first lesson
  at start, for the speech check.
- **Size:** a 100-lesson pack of lessons like Bus stand is about 313 KB as
  written, about 78 KB compressed, and loads in about 50 ms (`PackSizeTest`).

### Choices made while packaging

How the app is built is in `docs/packaging.md`; the staff guide is
`docs/setup-guide.pdf`.

- **Signed ad hoc, not notarized** (the owner: no notarization). No Apple
  Developer ID is needed, and the microphone works. Because the app isn't
  notarized, a staff member allows it once per Mac and version (System
  Settings › Privacy & Security › Open Anyway), and macOS asks for the
  microphone again after each update. Everything inside is signed without
  the hardened runtime, so no entitlements are needed; Compose's own
  signing would leave an Intel build unsigned.
- **macOS 11 or later:** Corretto 17.0.20's Intel programs are built for it.
- **Bundle ID `net.bodhya.speak`**, never to change: macOS keeps the
  microphone permission under it.
- **Everything inside, nothing downloaded at first launch:** the Java
  runtime, both speech models (small.en is 375 MB of the app's 599 MB), the
  first lesson pack and the LibriSpeech clip for the speech check. The
  bus-stand clips stay out: they are made with the macOS voices, whose
  licence doesn't allow sharing recordings of them.
- **The `.dmg`** has the app, a link to Applications and the setup guide,
  LZMA-compressed (the smallest format). There is no licence click-through;
  `THIRD-PARTY-NOTICES.txt` is inside the app.
- **A self-test** (`English Saathi --self-test`) checks a copy of the app on
  any Mac without a window: signature, microphone text, notices, lessons,
  both models on the LibriSpeech clip, drawing, voices. The build runs it,
  and whoever looks after the Macs can run it in Terminal; staff never need
  to.
- **The version is 1.0.0** (`appVersion` in `desktop/build.gradle.kts`),
  raised for every copy sent out.
- **The microphone prompt:** the app checks the microphone for a second at
  every start, which is what makes macOS ask. On macOS 26.5.1 the check
  simply waits for the answer and then hears sound (checked with the
  packaged app on 2026-10-22). In case another macOS version gives silence
  while the prompt is open, the check then runs again once it is answered,
  and the notice it showed meanwhile goes away.
- **A microphone turned on in System Settings** only works after the app is
  opened again, so the app's message and the guide both say: turn on
  English Saathi, then Quit & Reopen if macOS offers it, or quit and open
  it again.
- **The staff guide is English only, simple and short** (owner,
  2026-10-22), replacing the two-column English and Hindi version: one
  page, six steps. Its steps are for macOS 13 to 26, worded so they fit
  every version (Done or Cancel, Open Anyway or Open). The app runs on 11
  and 12 too, where System Preferences looks different; the app's own
  voice hint gives their path.

## Lesson review: owner answers (2026-10-22)

A design review of lesson packs raised four questions about the rules.
The owner's answers:

- **Moving on:** Start practice and Next offer, after sentences with words
  due for revision, the current lesson's sentences never said yet. Once
  every sentence of the current lesson has been said (which opens the next
  lesson), the next lesson becomes current, even if the student stopped at
  the goal screen. This replaces "moves to the next open lesson once every
  sentence in it is done today". Bus stand has 5 sentences and the goal is
  5, so a student who stopped at the goal would have started Bus stand again
  every day, and sentences 6 and later of a longer lesson would never have
  been offered.
- **Revision from other lessons:** Start practice and Next can show a
  sentence from an earlier open lesson when it has a word due for revision,
  then go back to the current lesson. Before, a word only in Bus stand never
  came back after the student moved on, although the goal screen said it
  would.
- **Ids carry the pack's id:** the app keeps every lesson and sentence id as
  `<pack>.<id>` (`starter.bus_01`), so packs can never clash. The CSV's
  sentence column changes from the pilot's `bus_01` to `starter.bus_01`
  (and the attempt log's lesson from `bus_stand` to `starter.bus_stand`).
- **Typos are refused:** a field the app doesn't know in a word, sentence or
  lesson refuses the pack, naming the field and the one probably meant
  (`word "patna": unknown field "scored" (did you mean "score"?)`). Before,
  it was ignored with a warning, so Patna would quietly have been scored.
  Unknown fields at the top of a pack stay warnings, so a later format can
  add one.

### Choices made while building the review's answers

- **The sentence order** for Start practice, Next and Skip:
  1. A sentence not done today with a word due for revision (scored in that
     sentence): the current lesson's first, then the earlier open lessons'
     in the unlock order. Never a later lesson, even one "Open all lessons"
     opened: the owner's answer says "an earlier open lesson".
  2. The current lesson's first sentence never said.
  3. Otherwise the pilot's order in the current lesson. It continues after
     the current lesson's sentence last shown, so a revision sentence
     doesn't lose the place.
- **Moving on** happens when the attempt that says the current lesson's
  last unsaid sentence is scored. The next open lesson in the unlock order
  with a sentence never said becomes current; if there is none, the lesson
  stays current. The feedback screen still shows the sentence just said,
  with "New lesson open: …", and Next continues in the new lesson. It also
  happens at start and when the lessons change, for a lesson that is all
  said with nothing after it yet: once a new pack or a pack update brings
  the next lesson, that becomes current. A lesson the student chooses on
  Home stays current, even once all of it has been said
  (`lessons.json` remembers that it was chosen).
- **When the saved current lesson can't be used** (none yet, dropped by a
  pack update, or closed when "Open all lessons" is turned off), the current
  lesson is the first open lesson with a sentence never said, or else the
  furthest open one. It used to be the first open lesson, which sent a
  student back to Bus stand.
- **A revision sentence says so** on the practice screen: "Revision: this
  sentence from Bus stand has a word to practise again." (drafted; needs the
  teacher review). Its speech isn't prepared with the current lesson's, so
  its first Listen takes a moment longer.
- **The goal screen's "Practise again tomorrow"** lists only words that can
  come back: words scored in a sentence of the current lesson or an earlier
  open one.
- **Data saved before this change** has bare ids, which can only be the
  first pack's (Bus stand was the only lesson in use). Progress, open lessons
  and the attempt log read them as `starter.<id>`, so old rows also export as
  `starter.bus_01`, and the next save writes them in full. Recordings saved
  under the old names stay as they are; a sentence's next recording uses the
  new name.
- **Tips are checked like words and sentences:** an unknown field in a tip
  (`text_hi`, `reviewed`) refuses the pack too.
- **More checks from the review:**
  - A pack file must be UTF-8. A byte-order mark is allowed and dropped;
    UTF-16 or another encoding is refused, as its Hindi would turn to
    nonsense.
  - Lists and objects may nest at most 32 deep. A lesson needs 6, and a
    deeper file could overflow the JSON reader and stop the app at start.
  - The 20 MB limit also applies to files found in the data folder at start.
  - The preview lists what the checks flagged: up to 8 items, and all of
    them in the log.
  - The per-lesson warning "the daily goal is more than the sentences" is
    gone, as the goal counts any lesson. The app warns only if all lessons
    together can't reach the goal.
- **Installing never overwrites another file.** A new version replaces the
  added file of the version in use; otherwise it gets a free name
  (`<id>.json`, `<id>-2.json`, …). So a file this version can't read (a
  later format, say) is kept.
- **Released packs keep loading.** Every released pack is kept in
  `desktop/src/test/resources/released-packs/`. `ReleasedPacksTest` loads
  each one with the current app, so a stricter check can never make an app
  update drop lessons students have opened. The test also fails if a pack
  inside the app changes without a new version.
- **Changing lessons while a student practises** (Settings opens over the
  window): adding a pack, or closing the lesson on screen by turning "Open
  all lessons" off, takes the student home. Turning it on leaves them where
  they are.

## Licences: owner answers (2026-10-22)

- **Speech library:** sherpa-onnx's prebuilt Java native library (1.13.8,
  `libsherpa-onnx-jni.dylib`) compiles in eSpeak NG (GPL-3.0) for
  text-to-speech, which the app doesn't use. Shipping it would bring GPLv3
  obligations for the whole app. The owner chose to rebuild the same 1.13.8
  library from its source with text-to-speech off, as sherpa-onnx's
  maintainer advises, for Intel and Apple silicon.
- **Java runtime source:** the bundled runtime (Amazon Corretto, GPLv2 with
  the Classpath Exception) needs its source available. Corretto's source
  archive goes in the same Google Drive folder as the `.dmg`, and the
  notices say so.

### Choices made while rebuilding the speech library

- `scripts/build_sherpa_onnx.sh` builds sherpa-onnx v1.13.8 (commit
  11afbd0) as sherpa-onnx's own macOS JNI build does, with text-to-speech,
  speaker diarization, PortAudio, the websocket server, the C API and the
  command-line programs off. It checks that no eSpeak, piper or ucd symbol
  is left, that each library is for its processor, and that it loads
  `@rpath/libonnxruntime.dylib`.
- **The only change to sherpa-onnx's files:** its list of functions to
  export names 16 text-to-speech and diarization ones, and macOS's linker
  refuses to export a function that isn't built, so they are taken out.
- sherpa-onnx's build breaks on a space in its path ("English Saathi"), so
  it builds in `$TMPDIR`.
- **No paths of the Mac that built it:** the source paths the compiler
  writes into the library read `sherpa-onnx-v1.13.8/…`, and it looks for
  ONNX Runtime only next to itself (`@loader_path`), as sherpa-onnx's own
  installed copy does. The script checks both, then signs it ad hoc again
  (changing it breaks the signature, which Apple silicon won't load).
- **The two libraries are kept in git** (`desktop/native/osx-x64/` and
  `desktop/native/osx-aarch64/libsherpa-onnx-jni.dylib`, listed in
  `scripts/deps.sha256`), as a rebuild elsewhere gives different bytes.
  ONNX Runtime 1.28.2 stays the file from sherpa-onnx's release jar, fetched
  by `scripts/fetch_deps.sh`; it is byte for byte the file the build links
  against.
- **The same results:**
  - Both models recognize the LibriSpeech clip word for word
    (`JvmWhisperRecognizerTest`, the self-test).
  - On the speech check's six clips, both models, the Intel library's
    transcripts are identical to the prebuilt one's (run on Intel Java
    under Rosetta).
  - On Apple silicon, they are identical except small.en on bus_03: "1
    ticket to Patna, please." instead of "One ticket…". Both score 5 of 5.
    The difference is rounding from another compiler, and it is the same on
    every run.
- **Smaller:** the Intel library went from 4.5 to 3.2 MB, the Apple
  silicon one from 4.2 to 3.0 MB.
- The Android app's archive (`sherpa-onnx-static-link-onnxruntime-1.13.8.aar`)
  has the same text-to-speech code inside. Android is paused; before an
  Android build is shared, it needs the same rebuild, or sherpa-onnx 2.0,
  which drops eSpeak NG.

## Assessment design: owner answers (2026-10-23)

Answers to the open questions in `docs/assessment-design.md` (§13), and one
change to the design. Nothing is built yet: the owner asked for the
remaining questions first.

- **Data leaving the student computers (Q1):** results may leave the
  student computers for the facilitator dashboard. Answer text with names
  removed may go to Anthropic, but only with recorded consent, and only
  after an Indian data-protection lawyer reviews the consent design. Until
  then, open answers (writing and speaking) are teacher-scored only.
  - This changes "All data stays on that Mac. No tracking or sharing for
    now." (Distribution, defaults and lessons) for assessment results.
  - It also allows a new use of an LLM beside "No LLM in the app": scoring
    consented, name-removed answer text with Claude on a staff computer,
    after the lawyer's review. The app itself stays LLM-free.
  - The design reads "results" as including answer text and transcripts,
    which the teacher scores on the dashboard, but not audio; and "teacher-
    scored only" as also ruling out the local model. Both readings wait for
    the owner's confirmation (design Q43).
- **Exports through the dashboard (Q14):** accepted as proposed. Student
  computers export to the facilitator's dashboard, which checks consent and
  removes names before anything reaches the staff computer. Whether the
  facilitator and staff computers are one machine is still open (Q31).
- **Speaking measures (Q15):** accepted. Speed and pause measures for open
  speaking answers are calculated on the student computer, with no score
  shown there.
- **Saving and resuming limits (Q24):** accepted. After two interruptions
  in one section a facilitator confirms before it continues; once a
  sitting's window closes, an unfinished section is kept but gets no level.
- **Roll numbers (Q33):** every student gets a Hikmat roll number before
  their first test.
- **Change: a monthly check and a quarterly full test.** The monthly
  two-hour test is replaced by a short monthly check (30–40 minutes, two
  skills, rotating) plus the full four-skill test every three months. The
  design, the form rotation and the costs are updated (§4.2, §4.6, §7).
  The design proposes, for the owner to confirm (Q18, Q20, Q39–Q41):
  - the full test in months 1, 4, 7 and 10, still in two sittings of about
    an hour; Listening and Speaking checks in months 2, 5, 8 and 11;
    Reading and Writing checks in months 3, 6, 9 and 12;
  - checks at about half length, showing a score and its error band but no
    level; Speaking and Writing checks reuse full-test tasks;
  - three full forms and three check forms per skill, given to three random
    groups, so no student repeats a form within nine months, and a second
    set of forms from month 10.
- **Still open (Q25, Q26, Q28, Q29):** the answers arrived with blanks
  where the figures and names go: the EF SET levels, the judges and their
  days, the number of Macs and Ubuntu PCs, and who holds the administrator
  passwords.

### Choices made while revising the design

- **What the change costs and saves.** Computer time drops from about 300
  hours a month to about 160 on average. AI scoring, once allowed, would be
  about $2.30 a month on Sonnet 5.5 instead of $4. While teachers score
  every open answer, they need about 30–45 hours in a full-test month,
  15–23 in a Listening and Speaking month and 8–10 in a Reading and Writing
  month (design §7.1).
- **Item writing:** about 160 items per receptive skill for nine months,
  where the monthly design needed about 120 for three months and 210 for
  six. Each check form shares 6 items with one full form, paired so that no
  student meets a shared item again sooner than four months later.
- **Calibration** moves from after month 3 to after month 4, when every
  item has enough answers. The full forms share no items, so they are
  joined by the random groups alone, with an error stated beside any
  individual change. The two-form agreement study moves out of the cohort,
  to the trial students before month 1 (in the cohort it would break the
  rotation).
- **Consent:** the design proposes that the first consent forms carry the
  local scopes plus a new `benchmark_local` (answers used as benchmarks by
  the teacher panel), and that the AI scopes come later, depending on when
  the lawyer reviews (Q44, Q50).
- **The first AI study** runs on the first full test after families give
  the AI consent. The design proposes that teachers keep scoring every
  answer that month, so the AI is compared with them on all consented
  answers (Q16, reworded).
- **New questions** raised by these answers are Q39–Q50 in the design:
  the check pairing and length, starting with a full test, where the
  teacher listens to speaking answers, the two readings of Q1, the scope
  and timing of the lawyer's review, AI consent and earlier answers, double
  scoring, roll-number logistics, make-up tests, blind scoring and
  benchmark consent. The design's §13.1 groups every open question by the
  milestone it blocks.

## Desktop screens: owner answers (2026-10-21)

- **Start D5–D8 now** (owner): students use the desktop app for the next
  three months. Data layer and student picker first; the desktop screen
  designs come separately, so the screens follow the pilot's laptop layout
  until then. D8 (results and CSV) is in scope from the start: it is the
  evidence at the end of the three months.
- **Results summary:** "Sentence attempts" counts heard attempts only, as
  in the pilot, and a separate "Not heard" column counts the "Didn't hear
  you" attempts (owner's choice of the recommended option).
- **CSV time:** the Mac's local time, `2026-10-21 13:45:30`, which Excel
  reads as a date and time (the pilot wrote UTC,
  `2026-10-21T08:15:30.000Z`).
- **Commits:** one per milestone, pushed once its tests pass.

### Choices made while building D5

Within the plan; each is a setting or a small change.

- **One folder per learner** in the data folder:
  `learners/<id>/learner.json` (name, card, when added),
  `progress.json`, `attempts/<day>.jsonl` (the attempt log, one row per
  line, rewritten whole for each row) and `recordings/<sentence>.wav`.
  Deleting a student renames the folder first and then removes it, so the
  student disappears at once even if the delete is cut short; the next
  start finishes it.
- **No half-written files:** every file is written to a temporary file,
  flushed to the disk and moved over the old one. A file that can't be
  read is reported on the picker and in the log, never silently replaced:
  a damaged progress file is kept as `progress.damaged-<time>.json` before
  the learner starts afresh. A log row or learner file with a time that
  isn't a time is reported and left out, and so is a learner folder not
  named by its id (a copy made in Finder, "s1 copy"), so nobody is
  counted twice.
- **A second copy refuses to start**: it shows a small window saying
  English Saathi is already open. The lock is released by macOS when the
  app ends, even after a crash.
- **Names and cards are unique** (names compared without case or extra
  spaces), because Results and the CSV show the name. Names are up to 40
  letters, as in the pilot. Rename and delete are in Settings (⌘,), not on
  student screens; delete asks for a confirmation.
- **Cards:** 8 colours × 8 drawn symbols (sun, moon, star, flower, leaf,
  fish, kite, house), so up to 64 learners. Provisional until the desktop
  designs arrive. The learner chooses the card on the new-student screen;
  the facilitator types the name there.
- **The picker shows what the facilitator should know** before a student
  starts: the speech model, problems found at start (voices, Java, a
  microphone that isn't connected, the start-up microphone check) and saved
  data that couldn't be read.
- **The speech check** moved behind Settings ("Open the speech check");
  `scripts/run_desktop.sh --check` opens it directly for the MacBook trip.
- **Hindi help on the picker and the new-student screen** uses drafted
  lines (the pilot's picker line describes typing a name), marked for
  teacher review with the other new messages.

### Choices made while building D6

- **Every recording of a sentence goes into the attempt log**, "Didn't
  hear you" included: when nothing was loud enough (the recognizer isn't
  run; `heard` is empty), when the transcript had no words, and when the
  microphone sent only zeros. A microphone that couldn't be opened, or a
  recognizer that failed, is shown but not logged: there was no attempt.
- **The latest recording is kept only when the recognizer ran**, so a
  silent try doesn't replace the learner's last real attempt.
- **Keys as in the pilot:** Space, L and S on the practice screen (Enter
  does nothing there, as in the pilot); Enter (Fix, or Next sentence) and L
  ("Hear the correct sentence") on feedback.
- **The mic can't be pressed until the speech model has loaded**, and says
  so; if the model fails to load, it says to tell the facilitator.
- **The mic's ring grows with the learner's voice** while listening,
  instead of the pilot's pulsing animation, so a learner can see the
  microphone hears them.
- **The feedback screen has no close button**, as in the pilot: Fix, Skip
  or Next lead on.
- The session log (`logs/speech-check.log`) also gets a line per attempt
  with what the endpointer saw, as the speech check's results do.

### Choices made while building D7

- **Fixing steps follow the pilot:** steps 1–2 need Next (Enter), steps
  3–5 the mic (Space); L and S play the word slowly on steps 1–3, and the
  phrase or sentence at normal speed (L) or slowly (S) on steps 4–5. The
  back arrow returns to the feedback; Skip goes on to the next sentence.
  Passing step 5 goes straight on, as in the pilot.
- **Each fixing try goes into the attempt log** as `fix step 3` to
  `fix step 5`, with `passed`, `failed` or `not heard`. The target is
  what the screen asked for: the word as written ("Where"; the pilot
  logged the normalized "where"), the lesson's phrase, or the sentence.
  `correct`/`total` count the words of that target, leaving out unscored
  words such as Patna; there are no stars. Fixing tries keep no recording.
- **"Practise again tomorrow"** shows each word as the lesson writes it
  inside a sentence ("the", "ticket"); a word that only starts sentences
  keeps its capital ("Where"). Plain chips, as in the pilot.
- **The goal screen's streak** is the count home shows; Enter is One more
  sentence, as in the pilot.

### Choices made while building D8

- **Results opens from the picker**, as in the pilot (a Results button next
  to Hindi help). It and Settings show the "To be checked by an English
  teacher" note; no student screen does.
- **The summary** has the pilot's columns plus "Not heard" (owner's
  answer), from sentence attempts only, in the order students first appear
  in the log. **Every attempt** shows the newest 200, as in the pilot, and
  says how many there are in all; the CSV has every row.
- **Save as CSV…** opens the macOS Save dialog in Documents with the name
  `english-saathi-results-<date>.csv` (the pilot's was
  `english-pilot-results-<date>.csv`) and writes the file safely; the
  screen says when it is saved or why it couldn't be. Rows are oldest
  first, all students together, with each student's current name; fixing
  tries are included, as in the pilot.
- **Settings** gains the voices found (and where to download clearer
  ones), the data folder and log with "Open the data folder" (Finder), and
  the teacher note, alongside the students, model, microphone and speech
  check.
- The attempt log is read off the main thread when Results opens (108,000
  rows took 0.6 s on the M4), and again when a student is deleted or
  renamed in Settings meanwhile; Save as CSV… reads it afresh, so a
  deleted student's rows never reach the CSV.

## Owner answers

- **Profiles:** each learner picks a colour and one of about 8 drawn symbols.
  The name is recorded as audio (no typing); tapping the card plays it.
- **App name and package ID** (decided 2026-10-19, after milestone b): the
  app is **English Saathi** (Hindi: इंग्लिश साथी) and the package ID is
  **`net.bodhya.speak`**. The display name can change later; the package ID
  cannot, so check it everywhere before any Play upload:
  - `app/build.gradle.kts`: `applicationId` and `namespace`;
  - the built APK (`aapt dump badging`) and the bundle must show
    `package: name='net.bodhya.speak'`;
  - Kotlin packages `net.bodhya.speak.domain`, `.speech` and `.ui`, and the
    Application class `net.bodhya.speak.EnglishSaathiApp`;
  - Gradle root project `english-saathi`; log tags `EnglishSaathi` and
    `EnglishSaathiTest`.
  - Release builds are not signed yet: a Play upload also needs a signing key.
  - There is no launcher icon yet (Play rejects an upload without one); it
    comes with the artwork in milestone f.
- **Place names:** lesson words get an optional `accept` list of alternate
  spellings. Fill it in after testing what Whisper actually hears.
  - Changed after milestone b: place names are not scored at all, and have
    no `accept` lists (see "Content decisions for milestone c"). The
    `accept` mechanism stays for other words; nothing uses it yet.
- **"To be checked by an English teacher" note:** shown in test builds only.
- **Recordings:** keep only the latest recording per sentence per learner;
  delete older ones.
- **Reference implementation:** the web pilot. The Kotlin domain logic must
  behave the same way. Where the pilot and the spec disagree, ask the owner
  instead of picking.
- **Before milestone b:** compare smaller English models in sherpa-onnx (e.g.
  Moonshine tiny) and a fully quantized Whisper decoder against Whisper
  tiny.en on the 5 bus-stand sentences. Report size, latency and accuracy side
  by side. Keep Whisper tiny.en unless something beats it.
  - Done: see `docs/asr-model-comparison.md`. Whisper tiny.en with a fully
    quantized decoder won under this rule. It is 44.3 MB instead of 103.6 MB,
    with the same accuracy, so it is bundled in the app's assets (under the
    spec's ~80 MB limit) instead of a Play Asset Delivery pack.
- **Testing:** minSdk 26 must be tested (Android 26 emulator image approved).

## Build stack

- Stability over newest: compileSdk 36 with the version set that works on it.
  - Compose BOM 2026.06.01 (UI 1.11.4)
  - activity-compose 1.13.0
  - navigation-compose 2.9.8
  - lifecycle 2.10.0
  - core-ktx 1.18.0
- Gradle 9.7.1, Kotlin 2.4.21, JDK 17, Android Gradle Plugin 9.3.3 (chosen
  in milestone b). AGP 9.3 is the newest line Kotlin 2.4.21 lists as fully
  supported. It needs Gradle 9.5 or later and supports up to API 37.
- `:domain` is a plain Kotlin/JVM module, so it cannot import Android classes.
  Tests use JUnit 4.13.2.
- No per-token confidence from sherpa-onnx Whisper, so the UNCLEAR label is
  skipped (see `docs/later.md`).

## Accepted defaults from the plan

- **Fixing loop:** the spec's 5 steps. The design's "Step 2: Say the word" is
  spec step 3, and the syllable split with the Hindi sound hint is its own
  step 2. A "Skip" button is always visible.
- **Labels:** English above, Hindi below, everywhere; text at least 16 sp. The
  feedback screen gets the English "4 of 5 words correct" line.
- **Places:** only Bus stand is unlocked; Market, Bank and Hospital show as
  locked.
- **XP:** XP shown always comes from the config numbers.
- **Tips:** the `hint_tag` wording, shown next to the word and its Hindi sound
  hint.
- **Screens the design doesn't show:**
  - 3 stars shows "Next sentence" instead of "Fix it".
  - Missing words get a dashed chip with an icon.
  - "Didn't hear you" gives no XP.
  - There are model-loading and mic-denied states.
- **Completed sentence:** counts once its feedback screen appears for
  recognised speech. It feeds the daily goal, the streak and "XP today".
- **Streak:** 1–2 missed days pause it; 3 or more missed days reset it to 1 on
  the next practice.
- **Leitner box:** wrong words go into box 1 (due in 1 day). A correct answer
  when due moves the word up a box (3 days, then 7 days); after box 3 it is
  done. A wrong answer sends it back to box 1.
- **"Slowly" button:** shows "0.6×" and uses speech rate 0.6.
- **Layout:** screens scroll on 360×640 phones with the main button pinned;
  portrait only.
- **Content review:** lesson word data and Hindi instruction lines are drafted
  by us and marked for teacher review.

## Content decisions for milestone c

Answered by the owner on 2026-10-19, after milestone b. Built in desktop
milestone D4 (2026-10-20): the domain rules are under "Domain rules" below,
the lesson file is `content/bus_stand.json` (with `content/hints.json`), and
the desktop app's loader (`LessonLoader`) reads it with the load checks.

- **Place names are not scored.** A word entry gets an optional
  `"score": false` flag in the lesson JSON, set for Patna and Bettiah. Such a
  word:
  - shows as a neutral chip: no check, no retry icon;
  - is left out of the "n of m words correct" count and the stars;
  - has no `accept` list. The guessed spellings the pilot shipped for Patna
    and Bettiah are removed.

  Background: Whisper got "Patna" right in only 7–9 of 54 test clips and
  "Bettiah" in none (`docs/asr-model-comparison.md`), mostly hearing "Patna"
  as two words ("but now"), which a one-word `accept` list cannot match.
- **Homophones.** Whisper writes "to" as the digit 2 ("1 ticket 2 Patna").
  Rather than an `accept` entry on "to", a general rule applied after number
  normalization treats these as the same word: to/two/too, for/four, one/won.
  The list lives in one place so it can grow after testing.
- **Hint wording:** the pilot's templates, not the spec's. They work for any
  word with that sound. All stay `"reviewed": false`. The pilot's five, as
  in `docs/pilot`:
  - `v_w`: “w” बोलते समय होंठ गोल कीजिए, दाँत होंठ पर न लगाइए।
  - `th`: “th” में जीभ हल्की-सी दाँतों के बीच रखिए।
  - `s_cluster`: “स्ट” सीधे बोलिए, आगे “इ” मत जोड़िए (“इस्टॉप” नहीं)।
  - `short_vowel`: इस शब्द को छोटा और जल्दी बोलिए।
  - `generic`: फिर से सुनिए और धीरे-धीरे बोलिए।

  The pilot has no `z_j`, so the spec's `z_j` seed is not used. Default for
  the drafted entries below: they use only these five tags. A z/j tip (for
  "is", "does", "please") would be added only if a teacher review asks for
  one.
- **Missing word entries** (is, one, to, how, go): drafted by us, with
  syllables, a Hindi sound hint and a tag, all marked `"reviewed": false`.
- **Real learner recordings.** The web pilot doesn't save audio, so it can't
  supply Whisper test clips. The owner will record a few students separately,
  with notes on what they said, and send them for `tools/asr-compare` (its
  README gives the manifest format).
- **Word data scope:** one shared word list per lesson file, keyed by the
  normalized word, like the pilot's `WORDS` table. Sentences reference words
  by their text; there are no per-sentence word entries. A sentence may
  override a word only to set `"score": false`.
  - The lesson file (this replaces the spec's per-sentence `words`):

    ```json
    {
      "id": "bus_stand",
      "title_en": "Bus stand", "title_hi": "बस स्टैंड",
      "words": {
        "bus":   { "syllables": ["bus"], "hint_hi": "बस", "hint_tag": "short_vowel" },
        "patna": { "syllables": ["Pat", "na"], "hint_hi": "पट-ना", "hint_tag": "generic", "score": false }
      },
      "sentences": [
        {
          "id": "bus_03",
          "text": "One ticket to Patna, please.", "hi": "पटना का एक टिकट दीजिए।",
          "chunks": ["One ticket", "to Patna", "please"], "picture": null
        }
      ]
    }
    ```

    A sentence that must not score a word which is scored elsewhere lists it:
    `"unscored": ["word"]` (proposed name). The override can only turn scoring
    off. Field names stay the spec's: `syllables`, `hint_hi`, `hint_tag`,
    `accept`, `score`.
  - Domain (built in D4): the word table moved from `Sentence` to
    `Lesson`, and a sentence keeps only its `unscored` list. A word is unscored
    in a sentence when its entry says so or the sentence overrides it. A word
    with no entry behaves as in the pilot: generic tip, no sound hint.

## Open questions: wrong phone dates

The reviewers found two corners of the agreed wrong-date rules. Both need a
phone whose date is wrong by several days. They are left as decided for now.

- **Practising while the date is far ahead.** If a learner practises once
  while the phone is, say, a year ahead, the last practice day is stored a
  year ahead. Once the date is fixed, every real day is "earlier than the last
  practice day", so the streak ignores practice until that date comes round.
  - Possible fix: treat a stored last practice day after today as wrong, and
    pull it back like Leitner dates.
- **Practising while the date is far behind.** The pull-back also moves
  correctly scheduled words earlier, so after the date is fixed they come
  due, and can be learned, days early.
  - Possible fix: skip the pull-back on a date before the learner's last
    practice day.

## Milestone b: recording and speech recognition

Choices made while building, within the spec. Each is a setting or a small
change.

- **Model:** Whisper tiny.en with the fully quantized decoder
  (`docs/asr-model-comparison.md`). It is bundled in the app's assets, and
  `scripts/fetch_deps.sh` downloads and builds it; it is not kept in git.
- **Recognizer settings:**
  - 2 threads. On both emulators 2 threads were about 1.5× faster than 1,
    and 4 were no faster.
  - Tail padding 1000, greedy decoding, no prompt.
- **Microphone:** the `VOICE_RECOGNITION` source at 16 kHz mono. Android's
  compatibility rules require it to support that format, with noise
  suppression and automatic gain off.
- **End of speech:** detected from loudness, with no extra model.
  - Audio counts as speech when it lasts 60 ms and is 12 dB above the
    background level and above −55 dBFS.
  - Recording stops after 1.2 s without speech once the learner has spoken,
    or at 8 s.
  - The 1.2 s of silence stays in the clip, as in the model comparison.
  - The levels need checking on real phones (`docs/speech-on-device.md`).
  - **Changed 2026-10-20 after the first live desktop test** (an M4's
    built-in microphone: recording never stopped on silence, 9 of 9 takes
    ran until stopped by hand or to 8 s):
    - only speech (60 ms of loud audio, as above) restarts the 1.2 s of
      quiet; before, a single 20 ms click, tap or breath did;
    - the first 300 ms are not used for the background level: a
      microphone that starts with a moment far quieter than the room made
      the room itself count as speech for many seconds. The first fix
      left out 100 ms; the M4's own recording then showed its microphone
      takes 180 ms to settle (fading out, 80 ms of exact zeros, then
      -71 dBFS in a -47 dBFS room).
    Both are reproduced in `EndpointerTest`. Replaying the M4 take with a
    1.5 s pause inserted after the first sentence: the old rules never
    stopped; the new ones stop 1.2 s after the sentence. Android uses the
    same code.
  - **Desktop check window:** each result logs what the endpointer saw
    (quietest and loudest frame, start-up quietest, background, when speech
    started, short sounds ignored), and the latest microphone recording of
    each sentence is kept in the data folder
    (`recordings/speech-check/<sentence>.wav`), so the levels can be tuned
    on real audio.
- **"Didn't hear you"**:
  - When nothing counted as speech, the recognizer is not run at all.
  - Otherwise, as decided in milestone a: the transcript has no words once
    the non-speech tags are removed.
- **Blocked microphone:** a recording of a second or more that is exactly
  zero gets its own message: the microphone sends no sound, and may be
  switched off in the phone's settings or taken by a call.
- **The speech model is loaded once per app process** on its own thread, and
  every recognition runs on that thread. Screens don't own it.
- **Leaving the app stops listening.** A turned phone doesn't.
- **Failures are shown on screen, never crash the app**, including a speech
  library that can't load.
- **Build checks:** the speech library and model must match
  `scripts/deps.sha256`, the same list `fetch_deps.sh` uses. A wrong model
  file would make sherpa-onnx end the app.
- **Speech library:** only the ARM builds (64-bit and 32-bit) are packaged.
- **Offline:** the manifest removes the internet permission if a library
  adds it, and the build fails if any merged manifest contains it.
- **No backups or phone-to-phone transfers** of app data. `allowBackup` is
  false, and data-extraction rules exclude everything. Without those rules,
  Android 12 and later would still copy app data to a new phone.
- **Names:** application ID `net.bodhya.speak`, Kotlin packages under
  `net.bodhya.speak`, app label "English Saathi" (see "Owner answers").
- **Test clips from the macOS voices are not committed**, because the voices'
  licence doesn't allow sharing recordings made with them.
  `scripts/make_test_wavs.sh` makes them locally.

## Later milestones: carried over from the pilot review

- **Tie a recording to its screen and step.** In the pilot, a transcript
  arriving after the learner pressed Skip or Back was scored against
  whatever was on screen by then.
- **Show the fix word as written.** The tip, the Fix button and the big word
  should show "Patna", not the normalized "patna".
- **Check content when it is loaded.** Built in D4 (`LessonLoader`, in the
  desktop app): the domain's rejections and a sentence with no scored word
  are errors; the rest are warnings. The domain already rejects:
  - a lesson without sentences, or with repeated sentence ids;
  - a sentence without English words;
  - word keys that aren't normalized words.

  The loader should also flag:
  - accept entries that aren't one word (they are ignored);
  - a word entry that no sentence uses, and a sentence word with no entry
    (allowed, but then the tip is generic with no sound hint);
  - an `unscored` override naming a word not in its sentence;
  - a daily goal larger than the number of unlocked sentences, which makes
    the goal unreachable;
  - a sentence whose words are all unscored (nothing could be scored);
  - an `accept` list on an unscored word (it would have no effect).
- **Keep the Leitner box order in storage.** "Practise again tomorrow" lists
  words in the order they entered the box, as the pilot does. A word table
  read back by key loses that order, so it needs an order column (or the
  owner accepts another order).

## Domain rules: where the Kotlin port differs from the pilot

Decided by the owner on 2026-10-19. Everything not listed here behaves exactly
like the pilot. That is checked by `PilotGoldenTest`, using cases recorded from
the pilot's own code (see `tools/pilot-golden/`).

Two rules were decided after milestone b and built in desktop milestone D4
(2026-10-20): "Homophones" and "Unscored words". The overlay in
`tools/pilot-golden/agreed-changes.mjs` has the same rules, with the content
change (Patna and Bettiah unscored, their accept lists removed), and the
golden cases were re-recorded.

### Text matching

- **One normalizer** for lesson text, chunks, fix targets, accept entries and
  recognizer output:
  1. Lowercase.
  2. Hyphens separate words on both sides ("bus-stop" = "bus stop"). The pilot
     did this only for what was heard.
  3. Split on whitespace (JavaScript's `\s`, so Unicode spaces count).
  4. In each token keep only a–z and 0–9. Apostrophes and all other
     punctuation are removed, so "don't" = "dont" = "don’t". The pilot kept `'`
     and dropped `’`.
  5. Tokens with nothing left are ignored: stray spaces, a lone "?" or "—".
     The pilot kept them as words nobody could say.
  6. A token of digits only is spelled out in words, without "and", up to
     999,999,999 ("21" → "twenty one", "120" → "one hundred twenty", "1,000" →
     "one thousand"). Longer digit strings stay as they are. The pilot did 0–10
     only.
  - Accented letters and non-Latin script are removed, as in the pilot.
- **Nothing heard.** Bracketed non-speech tags in recognizer output (e.g.
  `[BLANK_AUDIO]`, `(music)`) are removed. Each tag is replaced by a space,
  so it never joins the words around it. Only innermost bracket pairs count,
  so an unclosed bracket can't swallow speech; removal repeats until no tag
  is left, so nested tags go too. If no word is left:
  - The result is "Didn't hear you": no score, no XP, nothing marked done, no
    Leitner change, no streak change.
  - In the fixing loop it is not counted as a failed try.
  - The pilot scored such text as a 1-star attempt.
- **Accept lists** are normalized the same way. An entry must normalize to a
  single word; other entries are ignored, because multi-word alternates are
  not supported (see `docs/later.md`).
- **Digit limit:** "up to 999,999,999" is by value, so leading zeros don't
  count ("0000000001" → "one").
- **Hint tag without a template:** gets the generic tip. The pilot showed
  "undefined".
- **Homophones** (built in D4: `Homophones`). Two normalized words match when they
  are equal, when they are in the same homophone group, or when the heard
  word is in the target word's `accept` list. The groups: to/two/too,
  for/four, one/won. One table in the domain module holds them.
  - "After number normalization": the digits are spelled out first, so "2"
    becomes "two" and then matches "to"; "4" matches "for".
  - The rule is symmetric: a lesson's "two tickets" is also matched by "to
    tickets". That is accepted; the sounds are the same.
  - It lives in the comparison, not the normalizer, so words keep their
    written form everywhere else: chips, the word to fix, and the Leitner
    box keys.
  - It applies wherever words are compared: sentence attempts and fixing-loop
    steps 3–5.

### Unscored words

Decided after milestone b; built in D4. A word is unscored when its
lesson entry says `"score": false` (Patna, Bettiah) or when the sentence lists
it as `unscored`: a word the recognizer can't be expected to hear.

- It stays in the alignment, so the words around it line up as before, but
  its status is not used: it is left out of the correct/total count and the
  stars. "All words correct" (3 stars) means all scored words.
- It shows as a neutral chip: no check, no retry icon.
- Consequences the owner has not been asked about yet; the defaults below
  apply unless told otherwise:
  - it is never the word to fix, so it gets no tip and no fixing loop;
  - it never enters the Leitner box, and a step-5 pass skips it;
  - a sentence must keep at least one scored word (a loader check).
- The milestone-a test that passes "1 ticket to patana please" through
  Patna's `accept` list became a test of the unscored flag.
- A consequence seen in the re-recorded cases: a place name said right no
  longer counts either, so stars can drop. "one tickets to patna" was 3 of 5
  words (2 stars) and is now 2 of 4 (1 star, below the 0.6 needed for 2).
- **Star thresholds: to be reviewed after the student pilots** (owner,
  2026-10-20). With place names unscored, each remaining mistake weighs
  more in a short sentence, so the 0.6 ratio for 2 stars (`twoStarRatio`)
  may need changing.

### Fixing loop

- **Word to fix:** the first word marked WRONG. If no word is WRONG, the first
  word marked MISSING. The feedback tip is for this word too. The pilot took
  the first word that was not correct, WRONG or MISSING.
- **Step 4 chunk:** the chunk that contains the position of the word being
  fixed. Chunks are located in order within the sentence's words; a chunk
  with no words takes no room.
  - If the chunks can't be matched to positions, or the word sits between
    located chunks, use the first chunk that contains the word.
  - If no chunk contains it, use the full sentence.
  - The pilot always used the first chunk containing the word.
- **Passing steps 3–5:** a step passes when the word being fixed is said
  correctly at its position. Other words of the chunk or sentence, and extra
  words, are ignored. The pilot required every word of the step's target.
- **Unchanged from the pilot:**
  - Failing a step twice drops back one step.
  - Each passed step 3–5 earns +5 XP (config `fixStepXp`), also when it is
    passed again.
- **Passing step 5** updates the Leitner box with the step-5 result for every
  word of the sentence, as the pilot does. With the new pass rule, other words
  said wrong at step 5 now go back to box 1.

### Streak

- **Practising** gives the pilot's numbers:
  - the first practice day counts 1;
  - each later practice day adds 1 when fewer than 3 days were missed;
  - otherwise it starts again at 1.
- **`pausedSince`** is the first missed day.
  - It is stored when the app sees 1–2 missed days.
  - Any practice clears it, even on the same day or an earlier phone date.
  - After 3 or more missed days the streak is broken, not paused, so
    `pausedSince` is empty.
- **What learners see:**
  - Practised today or yesterday: the count.
  - 1–2 missed days: the count, marked "paused".
  - 3 or more missed days: 0 until they practise again (then 1).
  - The 0 is only shown. The stored count is kept, so a phone date that was
    briefly wrong costs nothing.
- **Phone date earlier than the last practice day:** the streak ignores it.
  The pilot added 1.

### Leitner box

- Rules as in the pilot. In addition, due dates further ahead than their box's
  interval (written while the phone's date was wrong) are pulled back to today
  plus that interval.
  - The pulled-back dates are stored when the box is updated.
  - Due words and the "Practise again tomorrow" list read through the
    pull-back without storing it.
- **"Practise again tomorrow"** lists words due tomorrow or earlier. The pilot
  listed every word in the box.

### Daily goal and sentence choice

- **Daily goal:** different sentences per day, as in the pilot.
- **Lesson-done screen:** shown once per learner per day, remembered across
  app restarts. The pilot kept this only in memory.
- **Next, Skip, and after a fix:**
  1. The first sentence not done today that contains a word due for revision,
     in lesson order.
  2. Otherwise the pilot's order: the next sentence after the current one
     (wrapping round) that is not done today; if all are done, simply the next
     one.
- **Start practice / One more sentence:**
  1. A sentence with a due word first, as above.
  2. Otherwise the first sentence not done today, in lesson order. The pilot
     started after the current sentence and so skipped a sentence opened but
     not said.
  3. If all are done today, the one after the current sentence.
