# v0.1 decisions

Agreed with the product owner on 2026-10-19, before milestone a. These settle
the gaps between the spec, the design mockup (`design/`) and the web pilot
(`docs/pilot/english-practice-pilot.html`).

## Owner answers

- **Profiles:** each learner picks a colour and one of about 8 drawn symbols.
  The name is recorded as audio (no typing); tapping the card plays it.
- **App name and package ID:** not decided yet. Milestone a uses the
  temporary Kotlin package `englishbolo.domain`, to be renamed in milestone b.
- **Place names:** lesson words get an optional `accept` list of alternate
  spellings. Fill it in after testing what Whisper actually hears.
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

## Open questions for milestone c (content)

The pilot and the spec also differ in their seed content. This doesn't affect
the domain rules, so the owner will decide when bus_stand.json and
hints.json are written. The pilot trials this week may change the content
anyway.

- **Hint wording.** The pilot rewrote v_w, th and s_cluster in the polite
  form, with bus-stand examples. It has no z_j. The spec's seed uses
  "water", "think" and "school" examples.
- **Accept lists.** The pilot ships guessed spellings for Patna and Bettiah,
  taken from Chrome's recognizer. The owner said to fill them in after testing
  Whisper. The domain tests use the pilot's lists as fixtures only.
- **Word data scope.** The pilot has one word table for the whole lesson; the
  spec puts word data inside each sentence. Decide whether entries apply
  lesson-wide.
- **Missing word entries.** The pilot has no data for is, one, to, how and go,
  so their tip is generic and has no Hindi sound hint. The spec says to fill
  words for each sentence.
- **Place names the model can't hear** (`docs/asr-model-comparison.md`).
  - Whisper got "Patna" right in only 7–9 of 54 test clips, and "Bettiah" in
    none. It mostly heard "Patna" as two words ("but now"), which a one-word
    `accept` list cannot match.
  - Options:
    - single-word accept entries (partner, padna, betia, …);
    - multi-word alternates (`docs/later.md`);
    - not scoring place names;
    - sentences without them.
- **"to" heard as "two".** Whisper writes "to" as the digit 2 in "1 ticket 2
  Patna", so "two" and "too" may belong in the `accept` list for "to".

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

## Later milestones: carried over from the pilot review

- **Tie a recording to its screen and step.** In the pilot, a transcript
  arriving after the learner pressed Skip or Back was scored against
  whatever was on screen by then.
- **Show the fix word as written.** The tip, the Fix button and the big word
  should show "Patna", not the normalized "patna".
- **Check content when it is loaded.** The domain already rejects:
  - a lesson without sentences, or with repeated sentence ids;
  - a sentence without English words;
  - word keys that aren't normalized words.

  The loader should also flag:
  - accept entries that aren't one word (they are ignored);
  - word keys not found in their sentence;
  - a daily goal larger than the number of unlocked sentences, which makes
    the goal unreachable.
- **Keep the Leitner box order in storage.** "Practise again tomorrow" lists
  words in the order they entered the box, as the pilot does. A word table
  read back by key loses that order, so it needs an order column (or the
  owner accepts another order).

## Domain rules: where the Kotlin port differs from the pilot

Decided by the owner on 2026-10-19. Everything not listed here behaves exactly
like the pilot. That is checked by `PilotGoldenTest`, using cases recorded from
the pilot's own code (see `tools/pilot-golden/`).

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
