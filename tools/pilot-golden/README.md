# Pilot golden cases

The web pilot (`docs/pilot/english-practice-pilot.html`) is the reference for
the domain rules, with the owner-agreed changes listed in
`docs/decisions.md` ("Domain rules: where the Kotlin port differs from the
pilot"). These scripts run the pilot's own JavaScript in Node, patched with
those changes, and record what it does. `PilotGoldenTest` checks that the
Kotlin port gives the same results.

```sh
TZ=Asia/Kolkata node tools/pilot-golden/generate.mjs
./gradlew :domain:test
```

- `pilot-harness.mjs` loads the pilot's `<script>` into a Node `vm` with
  stubbed DOM and storage, and a fake clock for multi-day histories.
- `agreed-changes.mjs` is the overlay. It runs inside the pilot's `vm` and
  replaces only the pilot functions that the agreed changes touch:
  - the content decisions: Patna and Bettiah get `score: false` and lose
    their accept lists
  - the one normalizer (`agreedNormalize`), and recognizer output with
    bracketed tags removed (`agreedHeard`)
  - normalized accept entries, and homophones (`agreedHomophones`)
  - unscored words (`agreedUnscored`): aligned, but left out of the count,
    the stars, the word to fix and the Leitner box
  - "nothing heard"
  - the word to fix and the step-4 chunk, chosen by position
  - passing steps 3–5 on the fixed word only
  - the streak (`openApp`, `streakView`)
  - the Leitner pull-back
  - `celebratedOn`
  - `nextIndex` and `startIndex`

  It is written from `docs/decisions.md` alone, independently of the Kotlin
  code. Everything else is the pilot's own code.
- `generate.mjs` writes `domain/src/test/resources/pilot-golden/*.json`:
  - `content.json`: the pilot's lesson, words and hints, with the agreed
    content changes
  - `normalize.json`: `pilot` (the pilot's `normText`), `agreed` (the one
    normalizer) and `heard` (recognizer output: tags removed, then
    normalized; empty means "Didn't hear you")
  - `align.json`: the pilot's alignment on word lists
  - `attempts.json`: attempts on the bus-stand sentences. `scored` says, per
    target word, whether it counts (false for unscored words).
  - `probes.json`: edge-case sentences with their `chunks`, `accept` lists
    and `unscored` words. The `pilot*` fields come from the unpatched pilot. `fixChunk`
    and `fixWordIndex` are present only when there is a word to fix.
  - `picker.json`: Next (`next`) and Start practice (`start`). `dueWords` are
    due today and `notDueWords` are due tomorrow, all in box 1.
  - `traces.json`: learner traces over many days, with fixing loops, skips
    and the lesson-done screen

Unprefixed fields are the agreed behaviour. Before writing, the generator
throws if the patched pilot disagrees with the unpatched one where no agreed
change applies. Output is deterministic (seeded random, fixed start date,
Indian time zone).

`features` tags mark inputs that touch an agreed change:

- `apostrophe`
- `bigNumber`
- `hyphenInTarget`
- `emptyTargetToken`
- `homophone`
- `unscored`
- `bracketTag`
- `nothingHeard`
- `nonAscii` (for information only)

## Trace semantics

- An `openApp` event comes first whenever the phone's day has changed.
- Learners with `followFlow` start each day with `startIndex` (Start
  practice), then go on with the sentence that Next or a fix chose.
- After the lesson-done screen, they repeat the current sentence.
- Pulled-back due dates are stored only when the Leitner box is updated.
  Due words (`nextIndex`, `startIndex`) and the "Practise again tomorrow"
  list (`after.tomorrow`) read through the pull-back without storing it.
- A broken streak keeps its stored count: `openApp` only clears
  `pausedSince`, and `streakView` shows 0.
- A fix step said to "nothing heard" has `passed: false` and
  `nothingHeard: true`, and its step and fails are unchanged.

Re-run the generator after any change to the pilot or to the agreed rules,
then run the domain tests. A failure means the Kotlin port and the recorded
behaviour now disagree.
