# v0.1 decisions

Agreed with the product owner on 2026-10-19: before milestone a, and again
after milestone b ("Content decisions for milestone c"). These settle the
gaps between the spec, the design mockup (`design/`) and the web pilot
(`docs/pilot/english-practice-pilot.html`).

**Status (2026-10-20):** the macOS desktop app comes first and Android is
paused (see "Direction change" below). The desktop plan is
`docs/desktop-plan.md` (milestones D1–D9). D1–D4 are approved; D5–D9 (the
screens) wait until this week's student pilots are done, because they may
change the practice and feedback screens.

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

- **The model switch starts on tiny.en** until the MacBook trip measures
  small.en (the 1.5 s rule above); Whisper runs with 4 threads (the trip's
  D1 check also times 2 and 6).
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
- **Buttons never take keyboard focus**, so Space and Enter always reach
  the shortcuts instead of clicking the last button used.
- **Leaving the window** (another app comes to the front) stops listening
  and speech, as leaving the Android app does.
- **Mukta is not bundled yet:** the check window uses the Mac's own fonts,
  which shape Hindi correctly. The font comes with the screens (D5).

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
