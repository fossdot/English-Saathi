# Monthly assessment, cohort dashboard and Linux: design

**Status: research and design only, for the owner's approval. Nothing here is
built yet, and nothing will be built until the owner says go.**

**Revised 2026-10-23** after the owner's first answers (recorded in
`docs/decisions.md`, "Assessment design: owner answers"): the monthly
two-hour test became a short monthly check plus the full four-skill test
every three months (§4.2, §4.6, §7); open answers are teacher-scored only
until an Indian data-protection lawyer has reviewed the consent design
(§5.3, §6); every student has a roll number before their first test
(§11.2). §13 marks what is answered.

Written 2026-10-22 for the Hikmat Foundation pilot (about 150 students). The
research behind it was done by seven research passes and one fact-check pass
against primary sources; claims taken from outside sources cite them in §16.
Where the sources or the research disagreed, or where this design differs
from the brief, this document picks one recommendation and lists the choice
under "Open questions" (§13). Nothing here is legal advice: the
consent and data-protection parts need review by an Indian data-protection
lawyer before the pilot.

Contents

1. [Summary](#1-summary) (and [words used in this document](#words-used-in-this-document))
2. [Why: what EF SET got wrong, and what we need](#2-why-what-ef-set-got-wrong-and-what-we-need)
3. [What changes with the device mix and the current EF SET levels](#3-what-changes-with-the-device-mix-and-the-current-ef-set-levels)
4. [Assessment design](#4-assessment-design)
5. [Scoring](#5-scoring)
6. [Consent and data protection](#6-consent-and-data-protection)
7. [Monthly cost](#7-monthly-cost)
8. [Listening audio: recording guide](#8-listening-audio-recording-guide)
9. [Platforms: Mac and Ubuntu](#9-platforms-mac-and-ubuntu)
10. [Test-form packs](#10-test-form-packs)
11. [Cohort dashboard](#11-cohort-dashboard)
12. [Milestones (smallest first)](#12-milestones-smallest-first)
13. [Open questions for the owner](#13-open-questions-for-the-owner)
14. [Risks](#14-risks)
15. [Later (not in scope now)](#15-later-not-in-scope-now)
16. [Sources](#16-sources)

Appendix A: [How it fits the existing code](#appendix-a-how-it-fits-the-existing-code)

---

## 1. Summary

- **What students take.** Every three months, the **full test**: two
  sittings of about an hour on a shared computer, Listening and Reading in
  one, Writing and Speaking in the other. In the two months between, a
  **monthly check** of 30–40 minutes in one sitting: two skills at half
  length, rotating, Listening and Speaking one month, Reading and Writing
  the next, so every skill is checked once between full tests (§4.2). The
  test runs offline on Mac and Ubuntu. Every answer is saved the moment it
  is given, and the test picks up where it stopped after the app closes or
  the computer restarts.
- **What they get.** A score out of 100 for each skill they took. From the
  full test, also an overall score and an *estimated level* per skill (below
  A1, A1, A2, B1, B2 or above) when the evidence is strong enough. When a
  score is close to a level boundary, the level is shown as a range
  ("between A2 and B1"). A monthly check is too short to place a level
  reliably, so it shows the score with its error band, not a level; once
  items are calibrated (after month 4) the facilitator also sees how it
  compares with the last full test (§4.3). Results never say "CEFR level",
  "certified" or "equivalent to EF SET".
- **How the levels are set.** By the Council of Europe's method for linking a
  test to the CEFR: specify the test against the CEFR, train judges, set the
  cut scores with a teacher panel, then check them [2][5]. The design asks
  Hikmat for a panel of 6–8 judges (teachers plus one or two outside
  people) for about 5–6 working days each over the first two months (Q26,
  still open). That is below the Council
  of Europe's 12–15 [2], so we document it as a low-stakes adaptation, treat
  the cuts as provisional, and check the precision of each cut [2][3][5].
- **Retakes** use parallel forms built from one blueprint and linked
  statistically: three full forms and three check forms per skill, given to
  three random groups at a time. Each student sees a given form at most once
  in nine months, and no scored item sooner than four months after meeting
  it (§4.6).
- **Closed items** (multiple choice, matching, ordering, gap fill, sentence
  building, dictation) are scored by rules in the app, offline.
- **Open answers** (writing, speaking) are saved on the student computer;
  speaking is transcribed offline by Whisper. The facilitator exports
  results to the dashboard on a facilitator computer (owner, 2026-10-23).
  **For now a teacher scores every open answer there**, with a written
  rubric per level. AI scoring waits until an Indian data-protection lawyer
  has reviewed the consent design and students or families have given the
  recorded consent; then one staff computer scores consented answers, names
  removed, with Claude through the Message Batches API, and a teacher
  checks the AI against her own scores (§5.3, §5.5).
- **Cost:** while teachers score everything, about **30–45 teacher-hours in
  a full-test month, 15–23 in a Listening and Speaking month and 8–10 in a
  Reading and Writing month** (§7). Once AI scoring is allowed: about
  **US $2.30 a month** on average for 150 students on Claude Sonnet 5.5 at
  batch prices, $0.11 on Haiku 5.5, $4.50–9.30 on Opus 5.5, and a teacher's
  spot checks.
- **Consent:** no answer text goes to Anthropic until the lawyer has
  reviewed the consent design and the recorded consent exists (owner). The
  proposal: the first consent forms cover taking the test, keeping results
  at the centre, recording speech and benchmark use, and the AI scopes are
  added after the review (§6.2, §13, Q44). No answer is ever sent for AI scoring unless
  the student has agreed and, for under-18s, a parent or guardian has also
  agreed; the scoring file leaves everyone else out, and withdrawals are
  honoured everywhere.
- **Listening audio** is recorded by real people (8–12 adult voices, mostly
  Indian English), to a fixed technical spec (§8).
- **Linux:** the assessment runs on Ubuntu 22.04, 24.04 and 26.04 as a
  `.deb`. Practice stays Mac-only. Ubuntu needs its own build of the speech
  library without text-to-speech, as the Mac did, and must be built on Linux.
- **Dashboard:** a facilitator computer imports one results file from each
  computer, offline. Imports never duplicate a student and never lose data.
  It shows each student's history, the cohort, and exports one CSV.
- **Smallest first:** the first code milestone is one built-in Reading form
  on the Mac that saves after every answer and resumes after the app is
  killed (§12). Before the first milestones, the owner decides the §13
  questions that block them (§13.1) and Hikmat starts the content work,
  which is the longest part.

### Words used in this document

- **Full test:** all four skills at full length, every three months, in two
  sittings of about an hour.
- **Monthly check:** two skills at about half length, 30–40 minutes in one
  sitting, in the two months between full tests.
- **Form:** one version of a test. Forms are different but built to the
  same plan, so they are equally hard. A *full form* is one skill's part of
  the full test; a *check form* is one skill's part of a monthly check.
- **Item:** one question.
- **Cut score:** the score at which one level ends and the next begins.
- **Standard setting:** a structured meeting where trained teachers decide
  the cut scores.
- **Reliability:** how consistent the scores are; 1.0 would mean a student
  always gets the same score.
- **SEM (standard error of measurement):** how far a score could move by
  chance if the student took the test again.
- **Equating:** adjusting scores so that different forms count the same.
- **Calibration (Rasch):** measuring every item's difficulty from real
  answers, so all forms can be put on one shared scale. A *logit* is that
  scale's unit.
- **QWK:** the standard number for how closely two scorers (for example the
  AI and a teacher) agree on ordered scores; 1.0 is perfect agreement.
- **Rubric:** the written description of what an answer at each score looks
  like.
- **Batch:** a set of scoring requests sent to Claude together, at half
  price.
- **Tombstone:** a note that says a student's data was erased, so an old
  file can never bring it back.
- **Pack:** a downloadable file with test forms and recordings, like the
  lesson packs.

Sections 9.2 and Appendix A contain build details for the developer; the
owner can skip them.

---

## 2. Why: what EF SET got wrong, and what we need

| EF SET problem | Evidence | What this design does |
|---|---|---|
| One test every 90 days | "1 test session every 90 days" [10] | A full test every three months and a short check of two skills in each month between, on forms of equal difficulty, rotated so no one repeats a form within nine months (§4.6) |
| Disconnections, lost or incomplete tests | "if you get a fatal error and your test session is cancelled, your results will not be saved" [10] | Offline; every answer saved to disk at once; resume after a crash or restart (§4.7) |
| No skill-level history | EF SET reports a section score per sitting [11] | Every assessment kept per student, per skill, with dates, on the dashboard (§11) |
| Hard to export | n/a | One combined CSV for the cohort (§11.5) |

Other tests looked at: British Council Aptis is the closest model (computer
based, not adaptive, tasks from A1 to B2, reliability 0.83 for Listening and
0.86 for Reading) [12]. Adaptive tests such as EF SET, Linguaskill and the
Oxford tests need large calibrated item banks that a 150-student pilot cannot
build [19]. Linguaskill and the Oxford Test of English do not report below B1
or A2 at all [15], which rules them out for this cohort: in rural
Muzaffarpur only 50.8% of 14–16-year-olds can read an English sentence [23].

---

## 3. What changes with the device mix and the current EF SET levels

The owner will confirm both. This is what each answer changes.

### 3.1 Device mix

| If… | Then… |
|---|---|
| **All Macs** (the 2019 MacBook Pros) | The Linux work (§9.2) can wait until later. Assessment ships inside the existing app. |
| **Macs and some Ubuntu PCs** | The Linux build must be ready before the first test on those PCs. We need one Ubuntu PC of the same kind to test on (microphone, speed); a cloud build machine is enough for building, not for audio. |
| **Mostly Ubuntu PCs** | Linux (M8) moves up to straight after the first Reading slice (M2a). The PCs' speed decides the speech model: small.en may be too slow on low-end PCs without AVX2 [79], which is why transcription runs in the background and never holds up a student (§5.2). |
| **Old or low-memory PCs** (under 8 GB) | small.en peaks at about 1 GB of memory (repo benchmark, `docs/intel-macbook-benchmarks.md`); on such PCs use tiny.en for transcription and say so in the results. |
| **Headphones or not** | Listening in a shared room needs headphones. Without them, noise lowers listening scores and some students hear the next student's audio. Budget one headset (with microphone) per computer. |
| **Shared login or one per student** | A shared login is simpler (one data folder per computer). Several logins means several data folders per computer, each exporting separately. |
| **Number of computers** | 150 students × 2 sittings of about an hour = about 300 computer-hours in a full-test month; 150 × 30–40 minutes = about 90 in a check month; about 160 a month on average, half the earlier monthly two-hour test. With 10 computers that is about 30 hours of testing in a full-test month, spread over a testing week, and about 9 in a check month. Fewer computers means a longer test window. |
| **Who has the admin password** | On Ubuntu, installing or updating a `.deb` needs it. Without it, we ship a `.tar.gz` that runs from the home folder (no menu entry). On Macs, Open Anyway after each update needs an administrator (§9.1). |

### 3.2 Current EF SET levels

| If most students are… | Then… |
|---|---|
| **Below A1 or A1** | Make the floor block larger and more visual (pictures, single words, numbers), give all instructions in Hindi audio, and start with forms covering A1–B1 only, reporting "B1 or above" at the top. This removes the B2 block (a quarter of the scored items) from the first full and check forms, so there is less to write and record. |
| **A2–B1** | The blueprint as written (§4.2). |
| **Spread from A1 to B2** | A short routing block followed by a level-targeted block (two-stage testing) becomes worth building earlier, to keep the test short for everyone (§4.3). |
| **Any** | EF SET results from the last few months can serve as an outside check on the month-1 levels (they will not match exactly: EF SET uses native-speaker accents and is adaptive, and our test uses Indian English voices, §8.1). Never convert one into the other. |

---

## 4. Assessment design

### 4.1 Framework: the CEFR scales we test against

The CEFR Companion Volume (2020) gives the level descriptions; it is explicit
that its scales "are not intended to be used as assessment instruments" [1,
p.41], so every claim that a score means "A2" must be built and checked
locally. The Council of Europe does not validate such links [4].

| Skill | Overall scale | Activity scales sampled | How-well scales (for rubrics) |
|---|---|---|---|
| Listening | Overall oral comprehension | Understanding announcements and instructions; audio media and recordings; conversation between other people | Vocabulary range |
| Reading | Overall reading comprehension | Reading correspondence; for orientation; for information and argument; instructions | Vocabulary range |
| Speaking | Overall oral production | Sustained monologue (describing experience, giving information); addressing audiences | Phonological control; fluency; grammatical accuracy; general linguistic range; vocabulary range; coherence |
| Writing | Overall written production; overall written interaction | Creative writing; reports and essays; correspondence; notes, messages and forms | Grammatical accuracy; vocabulary range and control; coherence; orthographic control |

Three points from the Companion Volume shape the design [1]:

- **Pre-A1 exists** (p.24). We report "below A1" rather than calling a low
  score A1, and "B2 or above" at the top [2, p.58].
- **Phonology is judged by intelligibility, not accent** (p.133). That suits
  Hindi-speaking learners: we never score towards a native accent.
- **Speaking here is production only.** Students answer recorded prompts
  with no live partner, so we report "Speaking (spoken production)" and make
  no claim about spoken interaction (p.72).

### 4.2 Test structure per skill

**The schedule.** The full test comes first, in month 1: it gives the
baseline and the answers the standard-setting panel needs (§4.4). Then
every three months (months 4, 7, 10…). The two months between have a
monthly check of two skills:

| Month | 1 | 2 | 3 | 4 | 5 | 6 | 7 | … |
|---|---|---|---|---|---|---|---|---|
| Sitting | Full test | Check | Check | Full test | Check | Check | Full test | |
| Skills | L, R, S, W | **L + S** | **R + W** | L, R, S, W | L + S | R + W | L, R, S, W | |

So every skill is measured twice a quarter: once at full length, once at
half length. Each check pairs one receptive skill with one productive one,
so every check month has open answers of only one kind to score, and the two
checks take about the same time. Listening and Speaking share the sitting
that needs headsets and a quiet room; a Reading and Writing check needs
neither (apart from the Hindi instruction clips). The pairing is a
recommendation (§13, Q39).

Each sitting has its forms, with blocks in level order and a gentle
stopping rule for beginners. Instructions are in Hindi (text and recorded
audio) with one practice item per task type. Targets are large and
mouse-friendly; typing is needed only in Writing and a few gap fills.

The tables below are the **full forms**; the **check forms** after them
use the same item types at about half length.

**Listening: about 25–30 minutes.** 4 floor items plus 32 scored items (8 per
level). Every clip plays twice, automatically (§8.2).

| Block | Items | Item types | Scored by |
|---|---|---|---|
| Below A1 (floor) | 4 | Hear a word or number, tap one of 4 pictures | Rule (key) |
| A1 | 8 | Picture choice (4 options); names, numbers, times, prices in short messages | Rule |
| A2 | 8 | Short dialogues, 4 options; **hear a sentence, arrange word tiles** (dictation without typing) | Rule; tiles scored pairwise (each correct neighbouring pair earns credit) |
| B1 | 8 | Match 4 speakers to 6 statements; facts from a conversation | Rule |
| B2 | 8 | Man / woman / both (opinions); a talk: attitude or purpose | Rule |

**Reading: about 30 minutes.** 4 floor items plus 32 scored items.

| Block | Items | Item types | Scored by |
|---|---|---|---|
| Below A1 | 4 | Word to picture | Rule |
| A1 | 8 | Sentence gap fill (4 options); picture to sentence | Rule |
| A2 | 8 | Two tasks ordering 5 sentences (pairwise credit); notices and messages, 4 options | Rule |
| B1 | 8 | Match opinions to 4 people; short texts | Rule |
| B2 | 8 | Headings to paragraphs (bank of 8); questions on a longer text | Rule |

These follow the Aptis task design at each level [12]. Formats that resist
guessing (tiles, ordering, typed gap fill, 4-option pictures) matter: in a
simulation, 32 three-option multiple-choice items placed students at the
right level 59% of the time, against 72% for the same number of items that
cannot be guessed [S1].

**Speaking: about 13–15 minutes, 6 parts.** Reliability depends more on the
number of tasks than on the number of raters [20], so: many short responses
rather than a few long ones. Prompts use characters and pictures, never "talk
about yourself", to keep personal details out of what is exported (§6.4).

| Part | Level | Task | Scored by |
|---|---|---|---|
| 1 | A1–A2 | Read aloud 3 short sentences | Rule: share of the passage's words recognised, aggregated over the whole part (never per word) (§5.2) |
| 2 | All | Repeat 8–10 sentences of rising length, as in the Versant test [14] (elicited imitation, r ≈ .66 with proficiency [21]) | Rule, aggregated over the part; shown alongside the level until it agrees with the teacher (§5.2) |
| 3 | A1–A2 | Three short questions about a picture story, 30 s each | Rubric |
| 4 | A2–B1 | Describe a picture (45 s) and answer 2 follow-up questions | Rubric |
| 5 | B1 | Compare two pictures or give an opinion (60 s) | Rubric |
| 6 | B2 | Long turn: 1 minute to prepare, 2 minutes to speak | Rubric |

**Writing: about 35 minutes, 4 parts.**

| Part | Level | Task | Scored by |
|---|---|---|---|
| 0 | none | One-minute typing warm-up | Not scored; flags very slow typists |
| 1 | A1 | 5 short answers on a form about a character (1–5 words each) | Rule (list of accepted answers); near misses are listed for the teacher, and the ones she accepts join the answer list in the next pack version |
| 2 | A2 | Build 6 sentences from word tiles; then a message of 20–30 words | Tiles by rule; message by rubric |
| 3 | B1 | 3 chat replies of 30–40 words | Rubric |
| 4 | B2 | An email of 80–120 words | Rubric |

**Check forms (monthly checks).** Listening and Reading use the same
blueprint at half length, so their checks can be put on the full test's
scale once items are calibrated (§4.6). Speaking and Writing checks use a
subset of the full test's own rubric task types, unchanged but with their
own prompts, so a task type can be compared between a check and a full
test; they leave out the B2 tasks,
so their ceiling is "B1 or above". The typing warm-up is left out: the
full test already flags slow typists. There is no stopping rule in checks
(recommended, §13, Q19): with 4 items per level a stopping decision would
rest on too few answers, and a check is short anyway.

| Skill | Length | Contents | Scored by |
|---|---|---|---|
| Listening | about 13–15 min | 2 floor items plus 16 scored (4 per level), the same item types as the full form; every clip plays twice | Rule |
| Reading | about 15 min | 2 floor items plus 16 scored (4 per level) | Rule |
| Speaking | about 9–10 min, 5 parts | Read aloud 2 sentences; repeat 5 sentences; then full Parts 3, 4 and 5 as they are: three questions about a picture story (30 s each); describe a picture (45 s) and 2 follow-up questions; compare two pictures or give an opinion (60 s) | Rule for the first two; rubric for Parts 3–5 |
| Writing | about 20 min, 3 parts | 3 short answers on a form (A1); full Part 2 with 3 tile sentences instead of 6, and the message of 20–30 words (A2); full Part 3, 3 chat replies of 30–40 words (B1) | Rule for the form and tiles; rubric for the message and the replies |

With Hindi instructions, practice items and the sound check, a Listening
and Speaking check takes about 30–35 minutes and a Reading and Writing
check about 40.

**Timing.** No limit per item in Listening and Reading, only a generous
section limit the facilitator can extend (as EF SET and Aptis do [10][12]).
Speaking has fixed answer windows with a visible countdown. Writing has a
section limit and saves as the student types. These times, for full and
check forms, are estimates from comparable tests and must be measured with
real students (§12, M1). Rubric-scored answers are scored by a teacher
until AI scoring is allowed, then by the AI with teacher checks (§5.3).

### 4.3 Reliability, and when a level is "justified"

How many levels a test can separate depends on its reliability: at least
0.80 for 3 levels, above 0.88 for 4, above 0.92 for 5 [3, Table 3]. A
32-item section reaches about 0.86 (estimated from Aptis's figures [12]);
that is enough to separate 3 levels reliably, not 4 or 5. Even at 0.90, and
with only 3 levels, about 1 student in 5 is placed one level off and about 1
in 4 would land at a different level on a parallel form [2, pp.106–108].
With 5 levels it is worse: our rough simulation of the 5-level form gives
70% at the right level and 60% at the same level on a second form, almost
never two levels off [S1].

A Listening or Reading **check form** has half as many items, so its
reliability is about 0.75 (the Spearman-Brown formula applied to 0.86),
below the 0.80 of rule 2 below. A Speaking or Writing check rests on only 3
or 2 rubric tasks, and reliability depends mostly on the number of tasks
[20], so it is probably lower still; it is estimated from the teacher
scores, not assumed. **Checks never show a level.** They have no cut scores
of their own, and what decides a level near a boundary is the precision at
that cut, which 4 items per level cannot give. Checks show a score with its
band; the only route to levels from checks is to lengthen them (§13,
Q41).

So the app reports levels with honesty built in. A skill's estimated level
is shown only when **all** of these hold; otherwise the report shows the
score and "Level: not enough evidence yet":

1. **The skill and form are linked:** the cut scores came from the
   standard-setting study (§4.4), or the form is linked to one that was by
   documented equating (§4.6). Cut scores live in the test pack with the ID of
   the study that set them. The one exception: month 1's provisional
   levels, if the owner agrees (§4.4, Q12).
2. **The form's reliability, measured on real answers, is at least 0.80.**
   That supports about 3 categories [3]; with rule 3 below, a student is
   shown one level or a range across one boundary, never a precise place
   among all five.
3. **The score is clear of the boundary.** If a score is so close to a
   boundary that a retake could easily land on the other side, the level is
   shown as a range ("between A2 and B1"). Technically: within one SEM of a
   cut [6, Standard 2.14; 12, the Aptis "grey area"].
4. **The cut is precise enough.** The teacher panel must agree closely
   enough on where each boundary sits; if their views are too spread out, we
   hold another round or show ranges. Technically: the standard error of the
   cut score (from the spread of the judges) is at most half the SEM [2,
   p.104].
5. **The session is valid:** at least 80% of the items the student should
   have seen were answered, the audio was played, there were no more than two
   interruptions in the section, and fewer than 20% of answers were
   implausibly fast.
6. **For Speaking and Writing:** at least 2 tasks were scored, by a
   teacher with the rubric or, once AI scoring is allowed, by AI scoring
   that passed the teacher-agreement check for that skill (§5.5).

**Over time:** a one-level change for one student between full tests is
within what parallel-form noise explains, so the dashboard never headlines
it. It shows each student's trend on the score scale with an error band
(wider for check points than for full-test points). The **band for a
change** is the standard error of the difference between the two scores,
√(SEM₁² + SEM₂²), at 90% (±1.645 of it); once items are calibrated, each
student's own SEM from the Rasch model is used. With a full form (SEM about
0.37 SD) and a check (about 0.50 SD) that is about ±1 SD, so a "higher" or
"lower" for one student after one check will be rare, and a change is
flagged only when it exceeds the band at a full test, or holds across two
sittings; never on one check alone.

Until items are calibrated (after month 4, §4.6), check scores are
comparable only with other checks of the same skill, and no change since
the full test is shown. After calibration, Listening and Reading checks sit
on the full test's scale; Speaking and Writing checks are compared with the
full test only task by task, on the same task types and rubric (§4.2, §4.6).

Growth is claimed for the cohort, where random error is tiny [S1], but one
bias does not shrink with the cohort: the first full test is the students'
first test of this kind, and alternate-form practice gains (about 0.24 SD
[18]) do not fade with time. So growth from month 1 to month 4 includes a
practice effect that cannot be separated out. To absorb it before the
baseline, a short untimed practice sitting with one item of every type is
recommended in the week before month 1 (§13, Q40); growth claims start
from month 4 otherwise.

**Later improvement:** after month 4, when every item has 50–150 answers, a
two-stage design (a short routing block, then a block aimed at the student's
level) gives the same accuracy in fewer items and lets each block decide
among only 3 levels, as the Council of Europe's own advice suggests [3,
pp.22–23; 13]. It needs calibrated items, so it is a later milestone (M10).

### 4.4 Setting level boundaries (standard setting)

The recognised method has five stages: familiarisation, specification,
standardisation and benchmarking, standard setting, validation [2, pp.9–11].
A small provider may scale each down but should not skip any [2, p.13]:
"some degree of empirical alignment data is preferable to not attempting any
standard setting" [5, p.59].

**Method per skill**

| Skill | Method | How it works |
|---|---|---|
| Listening, Reading | **Basket** warm-up, then **Modified Angoff**, two rounds [2, pp.61–64, 75–76; 5, pp.52–54] | Judges first take the items themselves, with audio and transcripts. Basket: each judge puts every item in the lowest level at which a learner can already answer it. Angoff: for each item, each judge estimates how many out of 100 borderline learners (just at, say, B1) would answer it correctly; the cut is the sum. Between rounds judges see how real students near the cut did on each item and how many students each cut would put at each level (from month 1). Basket alone gives lenient cuts [2, p.76], so it is used only for warm-up and item targeting. |
| Speaking, Writing | **Benchmarking** against the CEFR rating grids, then **Body of Work** to turn judgments into cut scores [2, pp.70–73] | The panel agrees a set of local benchmark performances per level and writes down why. Then judges sort about 30 scored performances per skill (scores hidden) into levels, and a logistic regression puts each cut where a performance is equally likely to be judged either side. Once AI scoring is allowed, benchmarks with the matching consent become the examples in the AI's prompts (§5.4, §5.5, Q45). |
| All | **Contrasting groups** as an outside check [2, pp.67–70] | Before seeing month-1 scores, each teacher places her own students at a level per skill. Agreement with the test is checked, not used to set the cuts (groups of 150 are too small). |

Bookmark and other item-response-theory methods wait until items are
calibrated (about 200 answers per item by the Manual's rule of thumb [2,
p.93], 64–144 by Linacre's [7]).

**The panel**

- **6–8 judges per skill group**, ideally the same panel for all four skills
  [5, p.55]: 4–6 Hikmat English teachers who know the students, and 1–2
  outside people with CEFR or testing experience (a university ELT lecturer
  or a CEFR-exam examiner) [2, p.38]. The facilitator runs it but does not
  judge.
- The Council of Europe asks for 12–15 as a minimum [2, p.38; 5, p.50] and
  Kaftandjieva for at least 10 [3, p.29]. The 2022 handbook notes that even
  "five or six colleagues" can identify problems in teachers' own classroom
  tests, but that results from small panels "may need to be treated
  cautiously" [5, p.59]; our use is wider than a classroom test, so we treat
  the panel's cuts as provisional until their precision is checked. We
  document the smaller panel openly and accept a cut only if its standard
  error is at most half the SEM (an extra round, more judges, or a range if
  not) [2, p.104].
- Judges' own English should be about B2/C1 or better [2, p.37].

**What Hikmat provides**

1. Per judge, about 5–6 working days over two months: 3–5 hours of
   self-study, a 3-hour familiarisation seminar (training on the Council of
   Europe's illustrative samples first [9], then local ones), about 1.5–2
   days for Listening and Reading, about 2–3 days for Speaking and Writing,
   and a half-day review [2, pp.19–23; 5, pp.53–55]. Sessions can be split
   over weeks or held online. Whether the Council of Europe's English samples
   may be used for this, and under what licence, is not yet checked.
2. Month-1 data: full forms A–C, each taken by one random group of
   consenting students, and each
   teacher's independent level judgment of her students before she sees any
   results.
3. At least 30 speaking and 30 writing performances spanning below A1 to B2,
   with the `benchmark_local` consent (§6.2, §13, Q50).
4. A coordinator who prepares the materials and writes up the study.
5. Optionally a few hours of advice from a language-testing specialist on the
   analysis.

**Timeline**

| When | What |
|---|---|
| Before month 1 | Familiarisation; specification forms; Basket review of the item pool for targeting; rubrics drafted from the CEFR grids; first estimate of the cuts |
| Month 1 | Full test taken; teachers' independent judgments collected; students and facilitators see scores, and, if the owner agrees (Q12), levels from the first estimate of the cuts marked **provisional**: the one exception to rule 1 of §4.3 |
| Month 1–2 | Benchmarking and Body of Work (Speaking and Writing first, as the Manual advises [2, p.9]); Angoff for Listening and Reading; cuts computed on the full forms; month 1 re-scored and estimated levels released |
| Before month 1 | The trial students outside the cohort (§4.6, point 2) take two full forms of Listening and Reading in the same week; once the cuts exist, their answers show directly how often the level agrees [6, Standard 2.16]. In the cohort this would break the rotation: any second full form is one a student has just taken or is about to take |
| After calibration (month 5) | A light check that the cuts, carried onto the shared scale, still hold on all three full forms [2, p.85] |
| New full forms D–F (by month 10, §4.6, Q20) | A light standard-setting check on each [2, p.85] |

Cut scores are set on the full forms only. Check forms report no level,
so they need no cuts of their own. Once calibrated, the facilitator's trend
chart for Listening and Reading draws the full test's cuts as lines behind
both kinds of point, but a check point is never labelled with a level.

### 4.5 Reporting

**Student report** (English and Hindi), per skill:

- the score out of 100;
- "Estimated level: A2", or "between A2 and B1", or "not enough evidence
  yet", with one short can-do line in plain words: for example, "Students
  with results like yours can usually understand phrases about everyday
  things when people speak clearly and slowly" [1, p.48];
- "This is an estimate from one short test, not a certificate."

After a **monthly check**, per skill taken: the score out of 100 with its
error band, and "Level: shown after the full test in <month>". The change
since the last full test (§4.3) is shown on the facilitator's trend chart,
not in the student's report, and only once items are calibrated.

**Overall score:** the average of the four skill scores, shown only after a
full test, when all four were measured in the same month. An **overall level** is not shown
unless the panel agrees a rule for combining skills (the Manual warns that
"lowest skill wins" can give unacceptable results [2, p.84]); see §13.

**Facilitator view** adds the raw score, the SEM, the form, the cut-score
study, and the footnote: "Levels are estimated using cut scores set by a
Hikmat teacher panel following the Council of Europe Manual (2009), adapted
for a low-stakes, small-scale context. The Council of Europe does not
validate links to the CEFR." [4]

**Never:** "CEFR certified", "official CEFR level", "equivalent to IELTS or
EF SET".

### 4.6 Parallel forms for retakes

Reusing one form measures memory: the practice effect is 0.46 SD for
identical forms and 0.24 SD for alternate forms [18]. Parallel forms cut it
roughly in half, but only if they are equal in difficulty. That takes four
things:

1. **One blueprint** for every form of a kind (full or check; a check
   blueprint is the full one at half length for Listening and Reading, and
   a subset of the full test's tasks for Speaking and Writing, §4.2): the
   same level blocks, item types,
   counts, text and audio lengths, topics, and target spread of difficulty [6,
   Standard 5.12; 8, p.99]. Listening forms also keep the same mix of voices,
   genders and accents and the same speech rates per level (§8), because an
   uneven accent mix makes versions unequal [57, p.118].
2. **Writing to the blueprint and trialling.** Write about 25–30% more items
   than needed. Before month 1, a think-aloud check with 10–20 students and a
   trial with 30 or more students outside the pilot cohort, so live items are
   not exposed [7][8, p.61].
3. **Random groups at every sitting.** Students are assigned at random
   within each class to three groups; at every full test and every check,
   the three groups take three different forms at the same time. Because the
   groups are chosen at random, they are equally able on average, so any
   difference in their scores shows how much harder one form is, and we
   adjust for it. With about 50 students per form we are at the smallest
   size studied (50–400 per form, where circle-arc equating did best [17]),
   so this link is rough on its own; points 4 and 5 firm it up. Groups are
   assigned once, on the dashboard, from the class lists, and travel to the
   student computers in the roster file with the roll numbers (§6.3), so a
   student has the same group on every computer (§13, Q47).
4. **Links between check and full forms (Listening and Reading).** Each
   check form shares 6 of its 16 scored items with one full form, chosen so
   that no student meets a shared item sooner than four months later (the
   pairing below). The 6 are a small copy of the form: 1 at A1, 2 at A2, 2
   at B1, 1 at B2, in the same place within their level block on both
   forms, with the full form's recordings. Six is far fewer than the 20
   that ETS prefers [16, p.36], so this link is rough.
5. **Calibration after month 4.** Every item's difficulty is measured from
   real answers, and each form gets a table that turns its raw score into a
   score on one shared scale, using about 50–150 answers per item (enough
   for ±0.5–0.7 logits in a low-stakes setting [7]). By then full-form
   items have about 100 answers, shared items about 150 and check-only items
   about 50. What joins the forms needs saying plainly, because the full
   forms share no items:
   - **Full forms A, B and C** are joined only by the random groups: in the
     Rasch run the three groups' average abilities are set equal at months 1
     and 4 (they are random thirds of the same classes). With about 50 per
     form, each link has an error of about 0.14–0.2 SD. That matters for one
     student moving from A to B, so it is shown beside any claim of change;
     it does not touch the cohort's growth, because every full test uses all
     three forms equally.
   - **Each check form** hangs on its paired full form through its 6
     shared items, and the random groups at each check give a cross-check.
   - **Speaking and Writing** have no shared items: a check is compared with
     a full test only task by task, on the same task types and rubric (§4.2).
   - Reusing a full form carries its conversion forward, so a form keeps
     the same scale from one quarter to the next.
   The shared items' difficulties are fixed from this first calibration,
   when every taker was meeting them for the first time. From month 5 on,
   some takers have met them before, so later sittings test them for a
   retest gain (those who met them before against those who did not)
   rather than re-estimating the link. The tables ship as a scoring-only
   pack update, and months 1–4 are re-scored from the stored answers so the
   whole history sits on one scale.

**Rotation (per skill).** Three full forms, A–C, and three check forms,
a–c. The same three groups throughout. Listening and Speaking checks fall in
months 2, 5, 8…; Reading and Writing checks in months 3, 6, 9….

| Group | Full test, months 1 / 4 / 7 / 10 | Check, Listening and Speaking, months 2 / 5 / 8 / 11 | Check, Reading and Writing, months 3 / 6 / 9 / 12 |
|---|---|---|---|
| Group 1 | A / B / C / A | a / b / c / a | a / b / c / a |
| Group 2 | B / C / A / B | b / c / a / b | b / c / a / b |
| Group 3 | C / A / B / C | c / a / b / c | c / a / b / c |

**Pairing for the shared items:** check form a shares items with full form
C, b with A, and c with B. Checked against every group's schedule in both
check slots, the shortest gap between meeting an item and meeting it again
is four months (pairing a with A would put them one month apart).

So each student sees a full form at most once in nine months and a check
form at most once in nine months, and meets a shared item again no sooner
than four months later (after 4 or 5 months). **Month 10 needs new forms.**
Each group would meet its own month-1 form again, and the identical-form
retest gain fades slowly: in Hausknecht et al.'s data it takes about two
years to fall to the alternate-form level, and they suggest at least a year
between identical forms [18]. Because every group would repeat at once,
random-groups equating could not see the gain, and it would look like
extra growth. So a second set (full forms D–F, check forms d–f) is
recommended, ready by month 10 if the pilot runs that long (§13, Q20).
Neighbours on shared computers usually have different forms, which also
discourages copying.

**How much to write, per receptive skill:**

| | Scored items | With the 25% margin |
|---|---|---|
| Full forms A–C (share nothing) | 3 × 32 = 96 | about 120 |
| Check forms a–c: 10 new items each (the other 6 come from a full form) | 3 × 10 = 30 | about 38 |
| **Total** | **126** | **about 160** |

Plus floor items (4 per full form, 2 per check form, each form its own)
and 4 unscored trial slots per full form to refresh the bank and seed the
second set [8, p.60]. This covers nine months without a repeat. The
earlier monthly design needed about 120 items for three months and about
210 for six; this one needs about 160 for nine, and about the same again
for a second set from month 10.

**Speaking and Writing prompts.** Full forms and check forms have their own
prompts, never shared, though the check tasks are full-test tasks (§4.2):
one prompt per rubric-scored task in each form. That is 21 speaking prompts
(4 tasks × 3 full forms + 3 tasks × 3 check forms) and 15 writing prompts
(3 × 3 + 2 × 3), rotated with the groups as above, plus the read-aloud,
repeat, form and tile sentences for the rule-scored parts. Any prompt whose average score
sits more than about 0.3 SD from the others of its task is flagged. (The
earlier design asked for at least 6 prompts per task type, about 42.)

### 4.7 Saving and resuming

In short: answers are never lost and a test can always be continued, with
two limits (points 7 and 8) that the owner accepted on 2026-10-23. The same
rules apply to full tests and monthly checks.

Guidance: "the system should ensure that no information is lost", with
frequent backups and recovery [22, pp.15–16]; and the supervisor must be
shown that the data is stored before the student leaves [8, p.78].

1. **Unit of saving: one answer.** Each answer is appended to the session's
   log and flushed to disk before the next item appears; a snapshot of the
   whole session is rewritten after it (Appendix A.2). Writing is also saved
   every few seconds and when typing pauses. A speaking recording is saved
   when it ends; its transcript is added later.
2. **Time** counts only while the item is on screen. Time while the app was
   closed never counts [22].
3. **Listening clip cut off mid-play:** on resume the item starts again from
   the beginning and the cut-off play does not count towards the two plays.
   The play count is written to disk *before* playback starts, so a restart
   can never give extra plays. Any resume during a listening item is flagged.
4. **Speaking answer cut off:** the partial recording is discarded and the
   prompt (with any planning time) is shown again, once. Flagged.
5. **Reading and Writing** resume at the saved position with the saved
   answers and text.
6. **No going back** once an answer is committed, as in EF SET. It keeps
   resuming simple and stops answer-changing after a restart.
7. **Interruptions:** every resume is counted. After two interruptions in
   one section, a facilitator confirms before it continues (to stop
   deliberate restarts); nothing is lost either way. Every interruption is
   recorded in the results [6, Standard 6.3] and shown on the dashboard.
8. **Sections are independent.** A crash in Speaking never touches the
   Listening score. After the sitting's test window closes (the full test's
   or the check's), an unfinished section is kept but gets no level;
   finished sections stand.
9. **Before the student leaves,** a facilitator screen confirms every
   section's answers are saved.

---

## 5. Scoring

### 5.1 Closed items: rules, offline, in the app

Each item type has a deterministic rule in the test pack: the key for choice
items; pairwise credit for ordering and tiles; a list of accepted answers for
typed gap fill, compared after the app's existing normalizer (which already
ignores case, punctuation and spells out numbers). Scores are computed the
moment the answer is saved. Item-level answers are always stored, not just
totals, so items can be analysed, cuts revised and everything re-scored later.

### 5.2 Speaking: what rules can and cannot do

Whisper transcripts are good evidence for content, vocabulary and
organisation, but not for pronunciation or fluency, and they flatter
grammar:

- Whisper drops fillers and repetitions by design [43], and on learner
  speech it "corrects" grammatical errors: in one Indian study it reproduced
  what was actually said 46% of the time, against 84.7% for a constrained
  recognizer [45]; [44].
- About 1% of Whisper transcriptions contain hallucinated phrases, more
  often for speakers with long silences [46], which describes hesitant
  beginners.
- On this project's own test sentences, read correctly, tiny.en marks at
  least one word wrong 43% of the time and small.en 22% (repo benchmark).
- **sherpa-onnx's Whisper hears only about 29.5 seconds per call** and drops
  the rest with a warning [48]. Long answers must be cut into pieces at
  pauses into pieces of at most 28 seconds and transcribed in order, or most
  of a 2-minute answer is lost.

So each speaking criterion is scored where the evidence is:

| Criterion | Scored by |
|---|---|
| Task fulfilment, vocabulary range, organisation, grammatical range | Teacher now; the AI, from the transcript, once allowed (§5.3) |
| Grammatical accuracy in speech | Teacher now (listening); once allowed, the AI, provisional and low weight (Whisper may correct errors); accuracy evidence comes mainly from Writing |
| Fluency (speed, pauses) | Rules on the audio timing, offline: speech rate, pause count and length, length of runs. Pause and speed measures best predict fluency ratings [47] |
| Read aloud | Rules against the reference text, aggregated over the part, never per word |
| Repeat (elicited imitation) | Rules, aggregated over the part; reported alongside the level, not inside it, until month-1 agreement with the teacher is shown |
| Pronunciation, intelligibility | Teacher: on every answer while teachers score everything; once AI scoring is allowed, on the sample she listens to, otherwise reported as "not assessed" |
| Silent, too short, or transcript empty for long audio | Rules (flags the answer for the teacher instead of sending it for scoring) |

**How this fits "open answers are never scored on student computers".**
Read aloud and Repeat have one correct answer, so we treat them as closed
items, scored by rule in the app as the brief allows. For the open speaking
parts, the student computer only *measures* speed and pauses from the audio
(which never leaves it). It shows no score to the student; the measures
travel with the answer and are combined with the AI and teacher scores on
the dashboard. The owner accepted this on 2026-10-23 (§13, Q15).

Whisper tiny.en and small.en are English-only: an answer in Hindi or mixed
Hindi-English will be mistranscribed. Such answers are flagged (very low
recognised-word rate for the audio length) and go to the teacher.

### 5.3 Open responses: the flow after each sitting

**Now: teachers score every open answer.** The owner decided (2026-10-23)
that results may leave the student computers for the facilitator's
dashboard, and that answer text may go to Anthropic only with recorded
consent and only after an Indian data-protection lawyer has reviewed the
consent design. Until then:

```
Student computer (offline)                    Facilitator computer (offline)
--------------------------                    ------------------------------
answer saved after every item                 imports every computer's file
speaking: WAV kept here (but see §13, Q42)    applies consent and withdrawals
transcribed in background (Whisper)           teacher scores every open answer with the rubric
after each sitting: "Export results" ──USB──► timing measures, flags, levels
```

Nothing goes to the staff computer or to any AI. That includes the local
model of §5.7: our reading of "teacher-scored only", to confirm (§13,
Q43).

**Later: AI scoring after the lawyer's review**, for answers given after
the recorded consent:

```
Student computer (offline)                    Facilitator computer (offline)          Staff computer (internet)
--------------------------                    ------------------------------          -------------------------
answer saved after every item                 imports every computer's file           scorer prepare  (rubric + prompt + answer)
speaking: WAV stays at the centre (Q42)      applies consent and withdrawals         scorer submit   (one Message Batch)
transcribed in background (Whisper)           writes scoring-request file:            scorer collect  (scores file; batch deleted)
after each sitting: "Export results" ──────►  only consented answers, no names  ────► scorer local    (or a local model overnight)
                                              imports the scores file  ◄────────────  scores-<month>.jsonl
                                              teacher enters her sample scores
                                              agreement report, flags, levels
```

**This differs from the brief, and the owner accepted it (2026-10-23,
§13, Q14).** The brief has exports go straight from each student computer
to the staff computer. Here they go through the facilitator's dashboard
first, so that consent is checked in one place, names and student IDs are
removed, and answers without AI consent stay at the centre for the teacher,
all before anything reaches the computer with internet. The facilitator and
staff computers can be the same machine if it has internet when scoring;
which computers they are is still open (§13, Q31).

**Where the teacher listens to speaking answers.** While teachers score
everything, they score every speaking answer, and a transcript alone hides
pronunciation and flatters grammar (§5.2). The audio is on the student
computers. Either the teacher listens there, in a facilitator-only screen
that plays answers and shows no scores, and enters her scores on the
dashboard; or the audio travels with the results file to the dashboard
computer, inside the centre, and is deleted there on the same schedule.
The owner chooses (§13, Q42).

The rest of this section describes the AI path, once it is allowed.

- **What each file holds.** The results file from a student computer goes
  only to the dashboard and carries all of that computer's answers, each
  marked with its consent status; it stays at the centre, which the
  required `assessment_local` consent covers (§6.2). The scoring file built
  by the dashboard leaves out every answer without the matching consent.
  Teacher scoring grows with the number of families who say no.
- **Two different files.** The *export* from a student computer carries a
  pseudonymous student ID, form and item IDs and the answer. The *scoring
  request* made by the dashboard carries only a fresh random ID per answer,
  the task and rubric IDs and the text. The API request built from it carries
  only that ID (as Anthropic's `custom_id`), the rubric, the task prompt and
  the answer. Anthropic never sees a student ID, name, centre or date.
- **Audio never goes to Anthropic or the staff computer.** Transcripts go
  out; WAV files stay at the centre (on the student computer, or also on
  the dashboard computer if the owner chooses that in Q42), and are deleted
  on a schedule (§6.5).
- **Transcription never holds up the student.** The WAV is saved first, the
  student moves on, and a background queue transcribes. Export finishes any
  pending transcripts first ("Preparing results… 3 left").
- **The API key lives only on the staff computer**, in the operating
  system's keychain or a file only that user can read, in a dedicated
  Anthropic workspace with a monthly spend limit (you cannot set one on the
  default workspace [36]). It is never in the app, the repo or an export.

### 5.4 Rubrics

One rubric per task type and level, written from the CEFR grids: the oral
assessment criteria grid (range, accuracy, fluency, coherence; with the
Companion Volume's phonology column) and the written assessment grid [1,
pp.183, 187; 2, pp.184–187]. Each criterion has 6 bands (0–5) with
descriptors in plain English. Rules from the research:

- **Examples beat descriptions.** With one example answer per level, GPT-4
  rating short learner essays on the CEFR scale reached a quadratic weighted
  kappa (QWK, the standard agreement measure for ordered scores) of 0.81,
  close to a commercial scoring engine (0.84) and two humans (0.87); without
  examples it did no better than counting words [40]. So every rubric carries
  benchmark answers as examples: the panel's (§4.4), where their consent
  allows sending them to Anthropic, or new ones (§5.5, Q45).
- **But we cannot assume 0.81 here.** In the same study agreement was
  lowest for an Indian first language (Telugu, 0.66), and the AI scored
  slightly too high on average (about +0.15 of a level) [40]. Across 65
  studies, agreement ranged from 0.00 to 0.97 and was "highly
  context-dependent", higher with a detailed rubric and examples [39]. The
  first AI study (§5.5) decides.
- **Don't punish B1 risk-taking.** The CEFR notes accuracy dips around B1
  as learners try more [1, p.132].
- **Accent is not an error.** Intelligibility is the criterion (§4.1).
- **Never add "was this written by AI?"** Detectors misjudge non-native
  writing as AI-written [82].

### 5.5 Teacher agreement: the first AI study and spot checks

Until the lawyer's review, teachers score every open answer, so there is
no AI to check. Those months are not wasted: every answer gets a teacher's
score with the rubric, and the panel's benchmarks (§4.4) come from them.

**When AI scoring is allowed,** the study below runs on the first **full
test** after families have given the recorded AI consent: a full test has
every rubric task, and the checks reuse full-test tasks (§4.2), so the
study covers them too. Only answers given after that consent can be used
(§6.4). Proposed: in that month the teacher still scores every answer, as
before, so the AI is compared with her on all consented answers, not a
sample: hundreds per skill instead of about 50, enough to confirm a
borderline scorer at once. The alternative is a sample of about 200 or 400
answers, so that the AI takes over sooner (§13, Q16).

**Acceptance standard**, from operational automated scoring at ETS [37][38],
measured on answers the AI was not tuned on:

- QWK of at least 0.70;
- AI-teacher agreement no more than 0.10 below teacher-teacher agreement;
- the AI's average no more than 0.15 standard deviations from the teacher's
  (0.10 for subgroups: centre, gender, age band, under-18 vs adult).

We also check, as our own secondary rule (from our simulation, not an ETS
threshold), that at least 90% of AI scores are within one band of the
teacher's.

**The first AI study**

1. The teacher's scores for that full test, given blind to the AI and with
   the same rubric, are the reference.
2. A second teacher, or the same teacher two weeks later, scores 60–80 of
   them, to give the teacher-teacher baseline. This can be done in any
   earlier teacher-only month too; while teachers score alone, a second
   teacher double-scoring about 10% at every sitting is recommended, so a
   Speaking or Writing level rests on a measured agreement (§13, Q46).
3. Answers from the earlier teacher-only months whose students later gave
   AI consent are **not** used to tune the prompt: they were given before
   that consent. The prompt's examples are benchmark answers; those from
   months 1–2 may be sent to Anthropic only with a separate consent from
   their families that the lawyer confirms, otherwise new benchmarks are
   collected after the AI consent (§13, Q45). The prompt is tuned on about
   half of the study month's students and agreement is measured on the
   other half, split by student, not by answer, so no student is in both.
4. Run Haiku 5.5, Sonnet 5.5 and Opus 5.5 on the same answers (about
   $20–35 for a whole full test, Opus thinking and held-out answers
   scored twice included; prompt-tuning runs come on top)
   and choose the cheapest model that passes. If none passes for a skill,
   the teacher scores that skill until one does. Because choosing the best
   of three on one held-out set flatters the winner, the chosen model's
   agreement is confirmed on the next sitting's teacher scores before it is
   trusted.

**Every sitting after**

- The teacher scores a random 10% of open answers (about 100 in a full-test
  month, 45 in a Listening and Speaking check, 30 in a Reading and Writing
  check) and every
  flagged answer (AI and second-opinion disagree by more than one band;
  off-topic, non-English, too short or empty transcript; refused or expired).
- **Drift alarm** if any of: the average AI-minus-teacher difference on the
  random sample exceeds 0.15 SD; the QWK for a skill over its last three
  sittings (about 100–170 sample answers) drops below 0.70; or a fixed set
  of about 30 answers from the first AI study, re-scored at every sitting,
  moves by more than 0.15 SD on average or any one by two bands. On an
  alarm, that sitting's open-answer scores and levels are marked
  provisional and the teacher scores the skill while we fix it.
- **Revalidate** whenever the model, the prompt or a task type changes.
  Claude model IDs are pinned snapshots [27], but the 5.5 models will retire
  no earlier than late September 2027 (Haiku: October 2027).

Scores are not repeatable to the decimal: the 5.5 models do not accept a
temperature setting [29], so the same answer can score differently twice.
The first AI study measures that too (each held-out answer scored twice).

### 5.6 Claude through the Message Batches API

- **How batches work.** Many requests go in one batch, processed within 24
  hours (usually under an hour) at half price [26][25]. A batch holds up to
  100,000 requests or 256 MB; a full-test month here is about 1,050. Results stay
  available for 29 days; the script downloads them and then **deletes the
  batch** through the API [26].
- **Settings.** Model pinned (`claude-sonnet-5-5` to start, chosen by the
  first AI study); thinking turned off where the model allows it (Opus 5.5
  always thinks) [29];
  structured output with a JSON schema of whole-number bands per criterion,
  a justification of at most 40 words, evidence quotes and flags [28]. (On
  Opus and Sonnet 5.5, forced tool use is rejected; structured outputs are
  the supported way to get JSON.) The rubric and examples come first and are
  identical for every answer to a task, so prompt caching applies [25].
- **Data handling.** Anthropic does not train on API data [32]. Batch data
  is stored up to 29 days unless deleted, and batches are not covered by
  zero data retention [30]. Content flagged by Anthropic's safety systems can
  be kept up to 2 years [35]. Anthropic's guidance for organisations serving
  minors expects safeguards and disclosure from us [56] (§13). Processing
  happens in the US or globally (no
  India option) [31]. Hikmat is the data fiduciary and opens the account in
  its own name; Anthropic is its processor under the Commercial Terms and
  Data Processing Addendum [32][33].
- **Usage Policy.** Grading an assessment that decides "admission,
  graduation, licensure, or certification" is a high-risk use requiring
  human review and disclosure [34] (read from the version effective
  12 November 2026; the version in force now must be checked too). A
  low-stakes monthly estimate is outside that, but we meet both requirements
  anyway: a teacher is accountable for results, and families are told that AI
  helps score.

### 5.7 A local model as the free alternative

- **Candidates** with permissive licences: Qwen3.5-9B (Apache-2.0), Gemma 4
  (Apache-2.0), gpt-oss-20b (Apache-2.0, tight on 16 GB) [50]. Run through
  Ollama, which accepts a JSON schema [50].
- **Evidence:** small open models without fine-tuning averaged QWK 0.36–0.45
  on essay scoring (fine-tuned: 0.76–0.79) [42]; an 8B model with a few
  graded examples reached 0.67 against 0.73 for humans on transcribed learner
  speech [41]. It must pass the same test (§5.5).
- **Speed:** about 5–9 hours for 1,000 answers with a 9B model on an
  Apple-silicon Mac (benchmarks are for 7B models; 9B adds about 25–30%) [49];
  about 24 hours on a 2019 Intel MacBook (estimated from the repo's
  benchmarks, which were run on an M4 under Rosetta). Overnight on the Intel
  MacBooks is not realistic; the staff computer would need Apple silicon or a
  PC with a graphics card.
- **Recommended role:** scoring for students whose families allow AI scoring
  only on our own computers (the `ai_scoring_local` consent, §6.2), and a
  second opinion that flags
  disagreements. It becomes the main scorer only if it passes on its own.

---

## 6. Consent and data protection

### 6.1 The law in brief

- India's Digital Personal Data Protection Act 2023 and Rules 2025 (G.S.R.
  846(E), 13 November 2025) [51][52]. Most obligations (notice, security,
  children, rights, transfers) start on **13 May 2027**; a proposal to bring
  that forward to November 2026 had not been gazetted as of October 2026
  [52, r.1(4); 53]. The pilot may run before then, but its data will still be
  held after, so we comply now.
- A child is anyone under 18 [51, s.2(f)]. Processing a child's data needs
  the **verifiable consent of a parent** [51, s.9(1)], with due diligence that
  the parent is an identifiable adult [52, r.10]. Rule 10's illustrations are
  all online; a staff member seeing the parent and a government ID in person
  is a reasonable reading, but untested.
- An exemption exists for an "educational institution", limited to tracking
  for its educational activities [52, Fourth Schedule]. Whether Hikmat
  qualifies, and whether exporting answers to an AI vendor is covered, is
  unclear. **Recommendation: don't rely on it; always get parental consent.**
- Consent must be free, specific, informed and limited to what is needed
  [51, s.6(1)]; withdrawal as easy as giving it [52, r.3]; on withdrawal,
  stop processing and erase, including at processors [51, s.6(6), s.8(7)].
  Penalties for breaching the children's rules reach ₹200 crore [51].
- Transfers abroad are allowed unless a country is restricted; none is
  [51, s.16].
- Tension to resolve with counsel: Rule 8(3) asks for personal data and logs
  to be kept at least a year, which pulls against "delete after scoring" and
  erasure on withdrawal [52].

### 6.2 Consent scopes

Separate tick boxes, so a "no" to one never blocks the test.

The owner decided (2026-10-23) that no answer text goes to Anthropic until
an Indian data-protection lawyer has reviewed the consent design and
recorded consent exists. How that plays out depends on when the lawyer
reviews (§13, Q44). Proposed: the lawyer reviews the whole design, the AI
part included, before month 1. The first consent forms carry
`assessment_local`, `audio_local`, `benchmark_local` and
`deidentified_reports`; the two AI scopes, and items 6 and 7 of the notice
(§6.6), are added when AI scoring is to start, families are asked again,
and AI scoring covers only answers given after that consent (§13, Q45).
The alternative is to put the AI scopes on the first forms, unused until
scoring starts: one round of consent instead of two, but families are
asked about something that is not happening yet.

| Scope | Allows | If no |
|---|---|---|
| `assessment_local` (required) | Taking the test; answers and scores kept at the centre, including on the facilitator's dashboard, and scored there by a teacher | Only a "practice, nothing saved" mode |
| `audio_local` | Recording speaking answers on the centre computer | Speaking scored live by a teacher, or skipped (owner decides, §13) |
| `benchmark_local` (proposed, §13, Q50) | An answer kept longer and shown to the teacher panel as a benchmark, and used as a scoring example at the centre | Not used as a benchmark |
| `ai_scoring_local` | Answers and transcripts scored by a computer program on Hikmat's own staff computer (nothing leaves Hikmat) | Teacher scoring |
| `ai_scoring_abroad` | Sending written answers and speech *transcripts* to Anthropic (USA) for scoring | Local-model scoring if `ai_scoring_local` is given, otherwise teacher scoring |
| `deidentified_reports` | Numbers without names in reports to funders | Left out of reports |

Under 18: a parent or lawful guardian signs **and** the child agrees (in
writing from 12, spoken under 12, following ICMR's guidance on assent [54]);
both are needed. 18 and over: the student signs. A student turning 18 during
the pilot gives fresh consent of their own; until then AI scoring for them
is paused (recommended; owner decides, §13).

### 6.3 The consent record

**Where it is entered (recommended):** once, on the facilitator's dashboard,
and copied to each student computer in a small roster file (which can also
carry roll numbers). A student computer with no consent record for a
student treats them as not consented, and a withdrawal entered anywhere
travels to every computer (§6.4). The owner chooses (§13).

Kept per student as an append-only list of events (granted, scope changed,
withdrawn, erased), never overwritten:

- the paper form's serial number (the paper is the evidence), the notice
  version and language;
- who consented (self, parent, guardian) and how their adulthood was
  checked (ID type seen, never the number);
- signature or thumb impression with witness; the child's assent;
- month and year of birth (for the turning-18 check);
- the scopes; who recorded it, when, on which computer;
- withdrawals and proof of erasure.

The guardian's name and phone stay on paper at the centre, not on the shared
computer. A second staff member checks 10% of paper forms against the app
each month.

### 6.4 Gating the export, and withdrawal

An answer goes into a scoring file only if the consent record shows active
consent for that path (`ai_scoring_abroad` for Claude, `ai_scoring_local`
for the local model), given before the answer, on an accepted notice
version, and not withdrawn: for students 18 and over, their own; for
under-18s, **both** the parent's or guardian's consent **and** the student's
own agreement. If either is missing or withdrawn, the answer is left out.

The check runs three times. On the student computer at export it removes
nothing but marks each answer with its consent status; the dashboard
applies it when it builds the scoring file; and the staff computer checks
again before submitting. Each step prints how many answers were left out and
why.

**Withdrawal** is accepted in person, by phone or WhatsApp, or on a slip, and
recorded the same day. A withdrawal travels as a permanent *tombstone* with
every export and import, so an old results file imported later can never
bring erased data back. Queued answers are deleted; a submitted batch is
deleted at Anthropic once finished [26]. Withdrawing everything erases
answers, audio, transcripts and scores on every computer; only the consent
events and a processing log (no answer text) remain.

**Keeping personal details out of answers:** prompts use characters and
pictures ("Ravi is 15. Write three sentences about his day"), never "write
about yourself". Before export, a local step replaces roster names,
10-digit and 12-digit numbers and email addresses with placeholders.

**Safeguarding:** a student may disclose abuse in an answer, and POCSO
places a reporting duty on anyone who learns of it [55]. A simple keyword
flag (English and Hindi in Roman letters) holds such an answer back for a
teacher to read before export.

### 6.5 Retention (proposal for the owner and counsel)

| Data | Where | Kept until |
|---|---|---|
| Paper consent forms | Locked cabinet at the centre | End of programme + 3 years |
| Consent events | Centre computers, dashboard | Same |
| Speaking audio | Student computer (and the dashboard computer too, if chosen in Q42) | 60 days after the test (longer only for the benchmark sample, if the notice says so) |
| Answer text and transcripts | Student computer; dashboard; staff computer | Student computer: until imported + 30 days. Dashboard: in the imported files it keeps (§11.3), so with the scores, end of programme + 1 year, unless erased on withdrawal (to confirm with counsel, Q6). Staff computer: deleted once scores are back and checked |
| Anthropic's copy | Anthropic (US) | Batch deleted right after download; at most 29 days; up to 2 years if flagged [35] |
| Scores and estimated levels | Dashboard | End of programme + 1 year, then de-identified or deleted |
| Processing log (no answer text) | Staff computer, dashboard | At least 1 year [52, r.6(1)(e), r.8(3)] |

### 6.6 Notice outline (plain English; Hikmat to translate)

1. Who we are, and a named contact for questions and complaints, with a reply
   time (the legal maximum is 90 days).
2. What this is: a short monthly English check, and every three months a
   fuller test that shows an *estimated level*; not an exam or
   certificate.
3. What we collect, item by item.
4. Why: to show progress, plan teaching and check the programme. No
   advertising, never sold.
5. Voice recordings stay at the centre and are deleted after about two
   months.
6. Optional AI help with scoring: the text of answers goes to Anthropic, a
   US company working for us, with a code number and no name; it does not
   train on it; we delete it at Anthropic as soon as scores come back, and
   Anthropic deletes anything left within 30 days. Anthropic may keep an
   answer for up to 2 years only if its safety system flags it. A teacher
   checks the AI's scores and is responsible. If you say no, a teacher
   scores instead.
7. Optional scoring by a computer program on Hikmat's own computer, where
   nothing leaves Hikmat.
8. Optional voice recording; optional use of an answer as a benchmark by
   the teacher panel; optional anonymous reports.
9. How long we keep things.
10. Your rights, and how to withdraw any part at any time (in person, phone,
    WhatsApp). Withdrawing does not affect classes.
11. Complaints, first to us, then to the Data Protection Board.
12. Signatures: parent or guardian (or thumb impression with witness), the
    staff member and the ID type seen; the student's agreement; adults sign
    for themselves.
13. A half-page student version in simple Hindi.

The decision record said "All data stays on that Mac. No tracking or
sharing for now." The owner changed it on 2026-10-23 (§13, Q1): results may
leave the student computers for the facilitator's dashboard; answer text
with names removed may go to Anthropic, but only with recorded consent and
only after the lawyer's review of the consent design.

---

## 7. Monthly cost

The schedule repeats every three months: a full test, a Listening and
Speaking check, a Reading and Writing check (§4.2). Costs are given per
sitting and as a monthly average over the three.

### 7.1 Now: teacher time

While teachers score every open answer (§5.3), the cost is teacher time,
not money. **Assumptions:** 150 students; a writing answer takes 1.5–2
minutes to score (the earlier estimate of 5–7 hours for 200 answers), a
speaking answer 2–3 minutes, as the teacher also listens to it. Beginners
who stop early give fewer answers, so these are upper estimates.

| Sitting | Open answers | Teacher-hours |
|---|---|---|
| Full test | 450 writing (3 parts) + 600 speaking (4 parts) | 11–15 + 20–30 = **about 30–45** |
| Listening and Speaking check | 450 speaking (3 parts) | **about 15–23** |
| Reading and Writing check | 300 writing (2 parts) | **about 8–10** |
| **Average per month** | | **about 18–26** |

Plus, once, a second teacher scoring 60–80 answers for the teacher-teacher
baseline (§5.5, about 2–3 hours), and the standard-setting panel (§4.4).
With the earlier monthly two-hour test, teacher-only scoring would have
been about 30–45 hours every month.

### 7.2 Later: AI scoring, once allowed

**Assumptions:** each rubric-scored part of §4.2 is one request, with all
its answers scored together.

- Full test: 3 writing parts (the A2 message, the B1 chat replies, the B2
  email; about 230 words in all) and 4 speaking parts (Parts 3–6; about 75,
  100, 100 and 200 words): 7 requests per student, **1,050 a sitting**.
- Listening and Speaking check: full Parts 3–5 (about 75, 100 and 100
  words): **450 requests**.
- Reading and Writing check: the A2 message and the B1 chat replies (about
  25 and 105 words): **300 requests**.

Beginners who stop early send fewer, so these are upper estimates. Each
request: a 2,000-token rubric with examples, 120 tokens of task
instructions, the answer (about 1.8 tokens per word with the current
tokenizer [25]), and 300 output tokens with thinking off. Prices are
Anthropic's published batch prices on 22 October 2026 [25]. It assumes
every family consents; teacher scoring replaces AI scoring for those who
don't.

| US $, batch prices | Haiku 5.5 | Sonnet 5.5 | Opus 5.5 |
|---|---|---|---|
| Price per million tokens (input / output) | $0.05 / $0.25 | $1 / $5 | $2 / $10 |
| Full test (2.42 M input, 0.315 M output tokens) | $0.20 | $3.99 | $7.98 |
| Listening and Speaking check (1.03 M, 0.135 M) | $0.09 | $1.70 | $3.41 |
| Reading and Writing check (0.67 M, 0.09 M) | $0.06 | $1.12 | $2.24 |
| **Average per month, no caching** | **$0.11** | **$2.27** | **$4.54** |
| Average per month, prompt caching with 90% hits | $0.06 | $1.27 | $2.55 |
| Average per month, no caching, plus 800 thinking tokens per answer | $0.23 | $4.67 | $9.34 |

Opus 5.5 always thinks, so its real cost is nearer the last row (about $9
a month); the Haiku and Sonnet rows assume thinking is off, which those
models allow.

Worked example (Sonnet, no caching): a full test is 2.42 M × $1 + 0.315 M
× $5 = $2.42 + $1.58 = $3.99; a Listening and Speaking check 1.03 M × $1 +
0.135 M × $5 = $1.70; a Reading and Writing check $1.12. Over a quarter
about $6.82, so **about $2.27 a month, or 1.5 US cents per student per month**,
and about $27 a year against about $48 for the earlier monthly two-hour
test. Scoring everything twice for a consistency check, or running all
three models in the first AI study, adds a few dollars to that month (about
$20–35 for the study, §5.5). Cache hits in batches are best-effort,
typically 30–98% [26].

Without the Batch API, the same scoring through ordinary requests costs
twice as much (about $4.50 a month on Sonnet). Anthropic's API documentation
says such requests are not kept by default [30], but its privacy page still
says up to 30 days [35]; only Zero Data Retention, which Anthropic must
approve, guarantees no storage. Flagged content can be kept up to 2 years
either way. Given the tiny sums, this is a privacy choice, not a cost choice
(§13).

With AI scoring, the teacher's part shrinks to the 10% sample and the
flagged answers (§5.5): about 3–4.5 hours in a full-test month, 1.5–2.5 in a
Listening and Speaking check and about 1 in a Reading and Writing check,
plus the flagged answers and every answer from families who said no.

The real cost is people (§12.3).

---

## 8. Listening audio: recording guide

### 8.1 Who speaks

- **Real adults only** (18+), volunteers or staff, recorded with a written
  release. No text-to-speech, no macOS voices.
- **Their own natural accent, never an imitation.** Cambridge stopped asking
  actors to simulate accents in 2009 [57, p.118].
- **Mostly clear Indian English.** It is fair between these students, who
  share a Hindi-belt background; listeners who share a speaker's first
  language do slightly better [60], so estimated levels may sit a little
  higher than on a test with native-speaker voices. Optionally one B1/B2 item
  per full form in another variety, from a real speaker, and the same in
  every full form (§13, Q17).
- **Pool: 8–12 voices**, at least half women. **Per form: 4–6 voices**, no
  voice in more than about 30% of items, and the same voice, gender and
  accent mix by level in every parallel form.
- Prefer speakers who do not teach the tested students, and ask all of them
  to keep the scripts confidential (forms are reused).

### 8.2 Plays and speed

- **Every clip plays twice, automatically**, at every level, as Cambridge
  A2 Key, B1 Preliminary and B2 First do [58][59]. The research is mixed:
  one British Council study favours two plays [65], others favour one. So
  the rule is fixed for the whole pilot: changing
  it would change item difficulty and break comparisons.
- **Speech rate targets** (words per minute, measured without long pauses):

| Level | Target | Basis |
|---|---|---|
| A1 | 90–115 | CEFR "slowly and clearly"; "moderately slow" speech is 100–125 [67]; slower than Cambridge's A2 |
| A2 | 115–145 | Cambridge A2 Key averages about 151, its slowest part about 123 [57, p.119] |
| B1 | 145–170 | Cambridge B1 Preliminary averages about 167 [57] |
| B2 | 170–200 | Cambridge B2 First averages about 208; conversation is about 200 [57] |

These are targets derived from Cambridge's measured rates, not a published
standard. Tolerance ±10%; outside ±15%, record again. Never time-stretch test
audio: ask the speaker to slow down and record again.

### 8.3 Script format

One entry per item, kept next to the pack source:

```json
{
  "item_id": "l-a2-dlg-007", "form_id": "lt-2026-11-b", "audio_rev": 1,
  "level": "A2", "task_type": "short-dialogue-mcq-picture",
  "focus": "specific information: time",
  "context_en": "Ravi and Priya are talking about the bus.",
  "context_hi": "रवि और प्रिया बस के बारे में बात कर रहे हैं।",
  "speakers": [ {"role": "Ravi", "voice": "v03", "gender": "m"},
                {"role": "Priya", "voice": "v06", "gender": "f"} ],
  "turns": [ {"who": "Ravi",  "text": "Priya, what time is the bus to Gaya?"},
             {"who": "Priya", "text": "It was at four o'clock, [pause 0.5] but today it's at half past four."} ],
  "target_wpm": [115, 145], "target_duration_s": [10, 20],
  "plays": 2, "preview_s": 10, "gap_ms": 3000,
  "questions": [ {"q_id": "l-a2-dlg-007-q1", "key": "B",
                  "evidence": "turn 2: 'half past four'",
                  "distractors": {"A": "four o'clock (mentioned, superseded)"}} ],
  "delivery_notes": "Friendly, normal pace for A2; stress 'today'.",
  "status": "approved-script"
}
```

Rules: Indian, everyday settings; no Hindi words in the English audio apart
from names; the answer must not be guessable without listening. Dialogues
have exactly two speakers at A1–B1, easy to tell apart in mono (different
genders or clearly different voices), recorded together in one session, with
no overlapping speech. Instructions are separate (on-screen text and a Hindi
audio clip), so the app controls the timing.

### 8.4 Technical spec

In plain words: every clip is recorded cleanly in a quiet room and set to
the same loudness, so students never need to change the volume between
items.

| | Spec |
|---|---|
| Capture | WAV, 48 kHz, 24-bit, mono; automatic gain and noise suppression off |
| Shipped in packs | **WAV PCM, 16-bit, 48 kHz, mono.** Java plays WAV with no extra library; MP3, Opus or FLAC would need one [64]. The app already plays 16-bit mono PCM |
| Loudness | **−18 LUFS** integrated (measured as dual mono), true peak at most **−1.5 dBTP**, every clip in a form within ±0.5 LU [61][66][63]. A reasoned choice, not a standard: check it on the real computers in a real room before recording the first form |
| Noise floor | At most −60 dBFS on room tone (re-record above −50) [62] |
| Room tone | 2–3 s of silence at the start of each take |
| Microphone | Best: a USB dynamic microphone 10–15 cm from the mouth with a pop filter. Acceptable: a recent phone set to WAV, on a stand, airplane mode. Not a laptop's built-in microphone |
| Room | Small, furnished room; a quilt behind the speaker; **fans, coolers and air conditioners off**; record early or late to avoid traffic |
| File names | `listening/<item-id>.wav` in packs; raw takes as `raw/<item-id>__<speaker-code>__<yyyymmdd>__t<take>.wav` on the staff computer only |
| Metadata | Pack manifest per clip: sha256, duration, rate, loudness, peak, measured wpm. Speaker details (pseudonymous code, gender, age band, accent, release form) in a staff-only file, never in packs |

Normalising (two passes, linear): `ffmpeg -i in.wav -af
loudnorm=I=-18:TP=-1.5:LRA=11:dual_mono=true:print_format=json -f null -`,
then apply with the measured values and `linear=true`, output `-ar 48000 -ac
1 -c:a pcm_s16le` [63].

### 8.5 Checks

1. **Automatic** (one script on the staff computer): format, checksum,
   loudness and peak, clipping, noise floor, silence at head and tail, speech
   rate, and a Whisper transcript compared with the script (flag if more than
   5% of words differ or any word carrying the answer is missing). Whisper is
   a flagging tool here, not a judge: it misses some misreads and invents some
   errors [46].
2. **Two human listeners per clip** (a teacher who did not write the item,
   and one other person), on a MacBook's speakers and on cheap headphones:
   words match, the key is clearly audible, natural delivery, speed right for
   the level, voices distinct, no fan, horn or clicks.
3. **Record again** on any clipping, a noise floor above −50 dBFS, a
   misread that touches the answer (any misread at A1–A2), rate more than 15%
   off, or an unclear key word. Never fix by stretching, pitch-shifting or
   splicing words from another session.
4. **Once a form ships, its audio is frozen.** Any change means a new pack
   version.

### 8.6 Size and effort

About 12–15 minutes of unique audio per 36-item full form, about **70–90
MB** as shipped. A check form's 18 items need about 6–8 minutes, but its 6
shared items reuse the full form's clips, so only about 4–5 minutes are
new (about 25–30 MB). Three full forms and three check forms come to about
0.3–0.4 GB per computer. Effort is a rough guess: about 6–8 hours of
recording, editing and checks per full form for a practised team, much more
for the first, and about 3–4 hours per check form.

### 8.7 Hindi instruction clips

The test's Hindi instructions are recorded too, to the same technical spec
(§8.4), by one or two Hindi-speaking adults. A teacher checks every clip
against the reviewed Hindi text. They ship in the same packs.

---

## 9. Platforms: Mac and Ubuntu

### 9.1 Mac

The assessment ships inside the existing app. Practice stays Mac-only
because it uses macOS voices; the assessment uses only the recordings in its
packs, so it does not need them. Hindi instructions in the test are recorded
audio too, identical on both platforms. App updates follow the existing
packaging rules: each update needs Open Anyway and the microphone prompt
again, so **app updates never happen in a test week**. New forms come as
packs and need neither.

### 9.2 Ubuntu

**Packaging.** Linux packages must be built on Linux: neither jpackage nor
Compose can cross-build [69][70]. Compose's "AppImage" target is not an
`.AppImage` file but an unpacked folder [68]; a real AppImage needs FUSE 2,
which Ubuntu 22.04 and later don't install, and staff would have to mark the
download executable [71]. So:

- **A `.deb` as the installer**, assembled by a `scripts/package_linux.sh`
  like the Mac's: our own control file listing dependencies that resolve on
  all three Ubuntu releases (jpackage would copy the build machine's package
  names), and our own menu entry with the right window class so the dock
  shows the app's icon and name.
- **A `.tar.gz` fallback** for PCs where nobody has the admin password.
- **Not Snap or Flatpak:** their sandboxes need extra microphone permissions
  (Snap's is not connected automatically) and move the data folder [81].
- **Built on Ubuntu 22.04 x86_64**, the oldest supported release, on a
  GitHub-hosted runner [80] or a real PC; install tests on 22.04, 24.04 and
  26.04 containers.

**Releases supported:** Ubuntu 22.04, 24.04 and 26.04 LTS, x86_64 [78]. The
libraries need very little from the system (glibc 2.17); ONNX Runtime
needs only SSE2 and uses AVX2 when present, so old PCs run, more slowly [79].
22.04's standard support ends in May 2027, so fresh installs should use 24.04
or 26.04. 26.04's desktop is Wayland-only; the app runs through XWayland.

**Speech library.** In short: we rebuild the speech library on Ubuntu
without the voice part, as we did for the Mac, and check that it runs on all
three Ubuntu versions. The detail: the prebuilt Linux sherpa-onnx library
also contains eSpeak NG (GPL-3.0) [72]. It is rebuilt without text-to-speech on Ubuntu
22.04, with the same checks as the Mac build: no eSpeak or Piper code, only
`$ORIGIN` as the library path, no build-machine paths, nothing newer than
glibc 2.35. Linux needs no export-list change (it uses a wildcard). The
release's ONNX Runtime is reused.

**Microphone.** In short: the app always uses the microphone chosen in
Ubuntu's Sound settings; Ubuntu never asks permission, so a muted
microphone shows up only as silence, which the app checks for. The detail:
Java Sound on Linux goes through ALSA; its "default" device
leads to PipeWire (24.04, 26.04) or PulseAudio (22.04), which converts
formats [73]. The app always records from the default device and hides the
raw hardware devices, which bypass the sound server and are often busy.
Choosing the microphone happens in Ubuntu's Settings › Sound › Input. There
is **no permission prompt** for a normal installed app, so a muted microphone
shows only as silence; the existing silent-microphone check runs at the start
of Speaking. If the sound server is down, Java sees no devices or can hang
opening a line [74], so lines open on a worker thread with a timeout and a
clear message. Real-hardware testing is required (§12, M8).

**Playback** uses the same streaming path as the Mac (not `Clip`, which has
crashed in libasound in OpenJDK's tests).

**One bug to fix first:** the build always passes `-Xdock:name`, which only
the macOS launcher accepts; on Linux the JVM refuses to start [75]. Mac-only
options must be set only when building on a Mac.

**Data folder:** `$XDG_DATA_HOME/English Saathi`, or
`~/.local/share/English Saathi` [76]. Package updates never touch it. On
Linux a saved file is only safe after the folder itself is synced as well
[77]; the app's safe-write helper gains that step (Appendix A).

**Speed:** there are no published figures for sherpa-onnx Whisper on
low-end x86 Linux PCs. It must be measured on the actual PCs before choosing
tiny.en or small.en there.

**Updates:** a new `.deb` from Drive or USB, installed over the old one with
an administrator password. Test forms come separately as packs through the
app, so a new month never needs the administrator.

---

## 10. Test-form packs

Test forms and their recordings come in packs, like lesson packs: versioned,
checked before install, and never breaking saved results.

- **Container:** a `.zip` (`<id>-<version>.saathi-pack`) holding
  `pack.json` (format 2), `audio/*.wav` and optionally `images/*.png`. Lesson
  packs stay format 1; today's app can't even select a `.zip`, so it never
  sees one.
- **`pack.json`** lists every file with its checksum and size; the blueprint
  per skill and kind of form (full or check); the rubrics; the scoring
  rules and cut scores (with the ID of the study that set them); the
  schedule (which sitting has which skills and which group takes which
  form); the routing rule (later); and the forms with their kind and items
  (skill, level, type, audio, plays, key, accepted answers, whether the
  item is shared with another form or an unscored trial item). A shared
  item has one ID and one recording in every form that uses it.
- **Checks before install** (refused with a clear message, nothing changed):
  safe zip entry names, size and entry limits; UTF-8 and nesting limits;
  unknown fields refused below the top level; every listed file present with
  a matching checksum and no extras; audio is 16-bit mono WAV within length
  limits; item IDs unique within a form; **each form matches its kind's
  blueprint count for every skill, level and type**; every check form
  shares its items only with the full form it is paired with (§4.6); every closed item has a key and every open task a
  rubric; a newer format asks for a newer app.
- **Versions:** the same or an older version is refused. Unlike lessons,
  **every installed version is kept**: a session pins the pack version and
  checksum, and a session started on version 3 still finds version 3 after
  version 4 arrives.
- **Released-pack fixtures,** as for lessons: every released test pack is
  kept in the repo and a test checks each still loads in every later app.
- **Scoring-only updates:** after calibration, a pack version can change
  only the conversion tables and cut scores, and the dashboard re-scores
  history from stored answers. The original result file is never changed:
  re-scored results are added beside it with the scoring version, and the
  dashboard shows which version each score comes from.
- **Answer keys** sit inside the pack on a shared computer; hiding them is
  not real security. Install a new pack close to the test week (§14).

---

## 11. Cohort dashboard

### 11.1 Where it runs

A facilitator computer (Mac or Ubuntu) imports files from every student
computer. Offline, no server. Recommended: a small separate app, "English
Saathi Dashboard", built from the same code, so student computers never
contain it (§13).

### 11.2 One student, many computers

- Each computer gets a random **computer ID** the first time it runs the
  assessment version.
- Each student gets a random **student ID** (a UUID). Today's learner IDs
  are made from the clock and are unique only on one computer, so they cannot
  be used across computers.
- The reliable link between computers is a **Hikmat roll number**, typed by
  staff with a check character to catch typos. **Every student gets one
  before their first test** (owner, 2026-10-23): the app does not start an
  assessment for a student without a roll number.
- Two student IDs with the same roll number merge into one person
  automatically. So the no-duplicates promise holds, provided roll numbers
  are typed correctly; the check character catches most typos.
- For the typos it misses, the dashboard still lists likely duplicates
  (same class and, if names reach the dashboard, Q34, same name, but
  different roll numbers) and two records with the
  same roll number but different names, and the facilitator confirms or
  separates them; that link is recorded and can be undone.
- A renamed student keeps the same IDs; the name is shown from the newest
  record. Names travel only if the owner allows (a separate roster file);
  otherwise the dashboard shows roll numbers.

### 11.3 Imports never duplicate and never lose

- Every record has a global ID (`<computer ID>:<sequence number>`) and a
  content checksum.
- **Each export contains everything still kept on that computer**: scores
  and results always, answer text until 30 days after it was imported
  (§6.5). Results are small (about 5 MB a year, no audio; if Q42 sends
  speaking audio to the dashboard, it travels in a separate audio file per
  sitting, about 1.7 GB across the centre for a full test at 16 kHz, and is not
  repeated in later exports). A lost USB stick
  loses nothing, and importing an old file again changes nothing.
- On import: a new ID is added; the same ID with the same content is
  ignored; the same ID with different content is a **conflict**, kept and
  flagged, never overwritten (a copied data folder or a hand edit).
- Gaps in a computer's sequence numbers are shown, so a missing file is
  noticed.
- Every imported file is kept unchanged, and an import log records what each
  added. The dashboard rebuilds its view from these files at every start, so
  a damaged index loses nothing. No database is needed.
- Consent withdrawals and deletions arrive as tombstones and are applied on
  every import (§6.4).
- **The dashboard computer is backed up.** It holds the only full history,
  so after each import the dashboard writes a copy of its whole folder to a
  second USB drive kept elsewhere at the centre.

### 11.4 Views

**One student:** every assessment with date, kind (full test or check),
form and status; the overall score and each skill's score with its error
band; estimated levels from full tests (or why not); change over time as a
line per skill, full-test points and check points drawn differently;
interruptions and flags; open answers with AI and teacher scores side by
side; **areas needing support**: skills below the class median or not
improving between two full tests, and the
rubric criteria the student scores lowest on (accuracy, range…). Per-block or
per-item-type scores are too unreliable to show as numbers [24], so they
appear only as hints for the teacher.

**Cohort:** every student in a table, filterable by month, kind of
sitting, skill, estimated level, centre and computer; a progress graph of
each skill's average with its error band, sitting by sitting (two points a
quarter per skill); how many students are at each level at each full test;
scoring status (waiting for the teacher, waiting for AI, in teacher sample,
scored, flagged); teacher scoring progress (answers left, by skill);
per-form reliability and the SEM, and the reason each suppressed level was
suppressed.

**Teacher tools:** score open answers with the rubric (every answer while
AI scoring is not allowed; later the sample and the flagged ones). Proposed
(§13, Q49): answers are shown without name or roll number, in random order,
and where possible scored by a teacher who does not teach that student, so
the teacher's score is an independent reference;
the agreement report (QWK, differences, drift alarms); later, a calculator
for the standard-setting workshop (Angoff sums, the standard error of a cut,
the Body of Work regression), so no one builds spreadsheets by hand.

### 11.5 Export

One combined CSV (UTF-8 with BOM, local times, as the practice results CSV
already does), one row per student × month × skill taken:

`month, sitting (full | check), roll_no, student_id, name (if allowed),
computer, session_id, form,
pack_version, skill, score_0_100, raw, max, sem, estimated_level, level_note
(range | not enough evidence: why), score_source (rules | ai | teacher),
ai_band, teacher_band, status (complete | partial | expired | voided),
interruptions, completed_at, scoring_rules, app_version`

A second CSV gives counts per full test × skill × estimated level; a third, the
item-by-item answers for analysis.

---

## 12. Milestones (smallest first)

Each milestone ends with tests passing, a commit, and a demo the owner can
watch. Milestones M2a–M7 are on the Mac; if most computers turn out to be
Ubuntu PCs, M8 moves up to follow M2a (§3.1).

**Needed before the first full test (month 1):**

- the §13 questions that block M1–M5, and M8 if Ubuntu PCs take part
  (§13.1);
- M1 for the full forms A–C, the Listening and Speaking check forms (month
  2) and the Hindi instruction clips; the trial and the panel's
  before-month-1 steps (§4.4);
- if the owner agrees to provisional month-1 levels (Q12), the part of M7
  that shows them (first-estimate cuts in packs, ranges, the
  "provisional" label);
- M2a, M2b, M3, M4 and M5 (teacher scoring included), and M8 if any Ubuntu
  PCs take part;
- at the centre: every student's roll number and group entered, consent
  collected, headsets, the dashboard computer and its backup drive, a
  computer for recording and checking audio, and the named teacher
  scorers.

By month 3: the Reading and Writing check forms. By about month 2: M7, when
the panel's cuts arrive. M6 and M9 wait for the lawyer's review and the AI
consent; M10 for month 4.

### 12.1 Before any code

| | Milestone | Who | Done when |
|---|---|---|---|
| M0 | **Decisions** | Owner | The §13 questions that block M1–M5 (and M8 if Ubuntu PCs take part) answered and recorded in `docs/decisions.md` (first answers recorded 2026-10-23); the rest before the milestone they block (§13.1) |
| M1 | **Content start** | Hikmat teachers, coordinator | Full and check blueprints agreed; specification forms; full forms A–C for every skill written and reviewed, and the Listening and Speaking check forms a–c (needed in month 2), then the Reading and Writing ones (month 3); rubrics drafted from the CEFR grids; consent notice for the first consent forms reviewed by counsel (§6.2, Q44); the full forms recorded and checked (§8); a think-aloud check with 10–20 students and a trial with 30 or more students outside the cohort: two full forms each in one week (§4.4, §4.6) and the Listening and Speaking check forms, which also times a full test and a check; the Reading and Writing check forms trialled the same way before month 3 |

### 12.2 Code

| | Milestone | Main work | Done when |
|---|---|---|---|
| M2a | **One Reading form, save and resume** (smallest slice) | One built-in Reading form; session engine with save after every answer and resume; Reading items scored by rules; result file; score shown; crash tests (the app killed at every write point) | A student takes the Reading form, the app is killed mid-test, reopened, and resumes on the right item with nothing lost |
| M2b | **Test packs and the schedule** | Test-pack format 2: loading, checks before install, all versions kept, released-pack fixtures; full and check forms; the schedule and the three random groups (which sitting, which skills, which form for which student); the roster file from the dashboard (roll numbers, groups) and its import, with a facilitator screen to enter them until the dashboard exists (M5) | A new Reading form arrives as a pack; a broken pack is refused with nothing changed; a session started on version 1 still finishes after version 2 is installed; each student is offered the right sitting and form |
| M3 | **Listening** | Recorded-audio playback through the existing path, play counts written before playing, the sound check, flags | Listening form runs with fixed double play and resume rules |
| M4 | **Writing and Speaking capture, consent** | Long recordings (the recorder's 8-second limit lifted for tests); transcripts in a background queue with long answers cut at pauses into pieces of at most 28 s; fluency timing rules; consent records and the first scopes (§6.2; the AI scopes stay off until the lawyer's review); local name and number redaction | All four skills run, in full tests and checks; open answers saved locally with transcripts; no answer exported without consent |
| M5 | **Export, dashboard v1 and teacher scoring** | Computer and student IDs, roll numbers required; cumulative export with checksum and tombstones; dashboard import log, conflicts, links; **teacher scoring of every open answer in the dashboard**, with the speaking audio played where Q42 decides; student and cohort views; CSV | Two computers' files import with no duplicates; re-importing changes nothing; a withdrawal removes data everywhere; a teacher scores a full test's open answers and the scores reach the student's results |
| M6 | **AI scoring and teacher agreement** (after the lawyer's review and the AI consent) | Scorer tool on the staff computer (prepare, submit, collect, delete, purge); scoring request built by the dashboard; the agreement report and drift alarms | The first AI study (§5.5) can be run end to end on real answers |
| M7 | **Estimated levels** | Cut scores in packs; the "justified" rules (§4.3) on full tests; check reports (score, band, change); ranges near cuts; re-scoring history after a pack update; the standard-setting calculator | Month 1 re-scored with the panel's cuts; levels shown only where justified; checks never show a level |
| M8 | **Ubuntu** | The `-Xdock` fix; platform layer (data folder, microphone, no voices, Ctrl instead of ⌘); speech library without TTS built on Ubuntu; `.deb` and `.tar.gz`; Linux self-test; install tests on 22.04, 24.04, 26.04 | A staff member installs from the guide on a real Ubuntu PC; microphone, playback and crash tests pass; Whisper speed measured |
| M9 | **Local model** (after the lawyer's review and the AI consent) | Local mode in the scorer (Ollama, resumable); same agreement study | Decision recorded: second opinion, consent fallback, or main scorer |
| M10 | **Calibration and two-stage forms** | Joint Rasch calibration after month 4, full and check forms together; conversion tables as a pack update; optional routing | Full and check forms on one scale; history re-scored once; routing only if it shortens tests without losing accuracy |

### 12.3 People and time (the real cost)

| Task | Who | Time |
|---|---|---|
| Writing and reviewing items | Teachers | About 160 items per receptive skill (full forms A–C and check forms a–c, with the margin), plus 21 speaking and 15 writing prompts (§4.6) |
| Recording | Volunteers, a recordist | About 6–8 hours per full form and 3–4 per check form once practised, plus the Hindi instruction clips |
| Standard setting | 6–8 judges | About 5–6 working days each, months 0–2 |
| Scoring every open answer (until AI is allowed) | Teachers | About 30–45 hours in a full-test month, 15–23 in a Listening and Speaking month, 8–10 in a Reading and Writing month; about 18–26 a month on average (§7.1); plus 2–3 hours once for the second scorer |
| Spot checks (once AI is allowed) | Teacher | About 3–4.5 hours in a full-test month, 1–2.5 in a check month, plus flagged answers, plus every answer from families who said no to AI scoring |
| Consent | Facilitators | Reading the notice to each family, recording forms, and again for the AI scopes after the lawyer's review; a monthly 10% audit |
| Test administration | Facilitators | About 300 computer-hours in a full-test month and 90 in a check month for 150 students, about 160 a month on average (§3.1) |

---

## 13. Open questions for the owner

Numbers stay as first published, so earlier references still point to the
right question. Answers given on 2026-10-23 are marked **Answered** and
recorded in `docs/decisions.md`; Q39–Q50 are new, raised by those answers
and by the change to monthly checks. §13.1 groups the open questions by
the milestone each one blocks.

**Data and consent**

1. **Answered:** results may leave the student computers for the
   facilitator dashboard. Answer text with names removed may go to
   Anthropic, but only with recorded consent, and only after an Indian
   data-protection lawyer reviews the consent design. Until then, open
   answers are teacher-scored only. (Two readings to confirm: Q43.)
2. Batch API (half price, stored up to 29 days unless deleted right away,
   which the scorer does) or ordinary requests (about $4.50 a month instead
   of $2.30; the API docs say not kept by default, the privacy page says up
   to 30 days; only Zero Data Retention, which Anthropic must approve,
   guarantees no storage)? Or local scoring only? (Recommended: batches,
   deleted at once; revisit if Hikmat prefers no storage at all.) Needed
   only once AI scoring is allowed.
3. US-only processing (10% dearer, the notice can say "USA") or global (the
   notice must say "USA and other countries")? Needed only for the AI
   consent wording.
4. If a family says no to voice recording: speaking scored live by a teacher,
   or skipped?
5. Students turning 18 mid-pilot: pause AI scoring until they sign
   (recommended), or continue on the parent's consent? Needed only once AI
   scoring is allowed.
6. Retention periods in §6.5, to confirm with counsel (now including the
   answer text the dashboard keeps); and whether Rule 8(3) requires keeping
   answer text for a year.
7. Notice languages: Hindi only, or also Urdu or Maithili? Who reads it to
   parents, who witnesses thumb impressions, and who is the named contact?
8. Where is consent entered: once on the facilitator's dashboard and
   copied to student computers in a roster file with the roll numbers and
   groups (recommended), or on each student computer (§6.3)?
9. Will Hikmat publish a short privacy and child-safety statement on its
   website, as Anthropic's guidance for organisations serving minors
   expects [56]? Needed before AI scoring starts.

**Use of results**

10. What will results be used for: feedback and teaching only, grouping,
    reports to funders, certificates? Higher stakes need more rigour, and
    certificates trigger Anthropic's high-risk rules.
11. Overall level: show none (recommended until the panel agrees a rule), or
    agree a combination rule with the panel?
12. Month 1: show scores with levels marked provisional, from the first
    estimate of the cuts, until the panel's cuts (about month 2), then
    re-score? (Recommended. It needs the level display of M7 before month 1;
    otherwise month 1 shows scores only.)
13. Speaking and Writing results appear only once a teacher has scored them
    (about one to two weeks after the sitting), while Listening and Reading
    appear at once (recommended); or show the rule-scored parts (read
    aloud, repeat, form answers, tiles) first as a partial result?

**Scoring**

14. **Answered:** exports go through the facilitator's dashboard before the
    staff computer, as proposed (§5.3). Whether the facilitator and staff
    computers are the same machine is part of Q31.
15. **Answered:** yes, speed and pause measures for open speaking answers
    are calculated on the student computer, with no score shown there
    (§5.2).
16. Changed by Q1. In the first AI study month (§5.5), the teacher scores
    every consented answer, as she already does (recommended: no extra
    hours, and a firm answer at once), or only a sample of about 200 or 400
    answers, so that AI scoring takes over sooner?

**Test design**

17. Accent: Indian English only, or plus one fixed item per full form in
    another variety at B1–B2? Are there volunteers with a natural British or
    American accent?
18. **Partly answered** by the change of 2026-10-23: a monthly check of
    30–40 minutes in one sitting. Still open: the full test in two sittings
    of about an hour (recommended), or must it fit one sitting (shorter
    sections, less precise)?
19. The stopping rule for beginners: use it in full tests (shorter,
    slightly less precise; items not reached count as wrong, the same on
    every form), or show everyone every item? In checks, no stopping rule
    is recommended (§4.2).
20. Forms: three full forms and three check forms per skill (about 160
    items per receptive skill) cover nine months without a repeat. For
    month 10 onwards, a second set (D–F, d–f; about the same again) ready by
    month 10 (recommended, §4.6: reusing a form after nine months would read
    as growth), or reuse the first set? How long will the pilot run?
21. Can the 30 or more trial students outside the cohort take two full forms
    of Listening and Reading in one week before month 1, to measure how
    often the level agrees (§4.4)? (In the cohort, a second form breaks the
    rotation.)
22. Can teachers give independent level judgments of their students before
    seeing month-1 results?
23. Start with fixed (non-adaptive) forms and show ranges near boundaries
    (recommended), or build the two-stage form (a short routing block, then
    a block at the student's level) from the start (more precise levels, but
    it needs calibrated items first) (§4.3)?
24. **Answered:** both proposed limits stand: a facilitator confirms after
    two interruptions in a section, and an unfinished section gets no level
    once the sitting's window closes (§4.7).
25. **Still open: the figures did not come through.** What are the
    students' current EF SET results: how many at each level (below A1, A1,
    A2, B1, B2), per skill if available, with dates? Can Hikmat share them,
    with consent, so we can choose the level range of the first forms
    (§3.2) and use them as an outside check in month 1?

**People**

26. **Still open: the figures did not come through.** How many judges (6–8
    asked for: teachers plus 1–2 outside people) for how many days (about
    5–6 each over two months)? And, newly sized by Q1: which teachers score
    every open answer, about 30–45 hours in a full-test month, 15–23 in a
    Listening and Speaking month and 8–10 in a Reading and Writing month
    (§7.1), within one to two weeks of each sitting (Q13)? Is there budget for a few
    hours of a testing specialist's advice?
27. Who writes and reviews items, who records, and who signs off audio?

**Devices**

28. **Still open: the figures did not come through.** How many Macs and
    Ubuntu PCs? Also their models, memory, Ubuntu version and headphones,
    and can one Ubuntu PC be lent for testing?
29. **Still open: the name did not come through.** Who holds the
    administrator password on each computer?
30. One shared login per computer, or one per student or class?
31. Which computer is the facilitator's (dashboard), which records and
    checks the test audio (§8.4, §8.5; needed before month 1, no internet
    needed), and which is the staff scoring computer (needs internet; Apple
    silicon or a graphics card if the local model is wanted; needed only
    once AI scoring is allowed)? Can some of them be the same machine?
32. Is GitHub Actions on the private repo acceptable for building the Linux
    release?

**Identity and records**

33. **Answered:** every student gets a Hikmat roll number before their
    first test (§11.2). Follow-up questions: Q47.
34. May names reach the dashboard, or roll numbers only?
35. When a student is deleted on a computer: keep their assessment records on
    the dashboard marked "removed" (recommended), or purge them?
36. A separate dashboard app (recommended) or a facilitator mode in English
    Saathi?
37. Export after each sitting, or weekly too? The data folder is excluded
    from Time Machine, so exports are the only copy off each computer.
38. How long should speaking audio stay on each computer (practice keeps only
    the latest recording per sentence)?

**New, from the 2026-10-23 answers**

39. Check pairing: Listening and Speaking one month, Reading and Writing the
    next (recommended: one receptive and one productive skill each month,
    even scoring load, headsets needed only in one), or another pairing
    (§4.2)?
40. Start with the full test in month 1, then every three months
    (recommended: it gives the baseline and the standard-setting data, but
    all four skills' full forms must be ready first), or start with a check?
    And a short untimed practice sitting in the week before month 1, so the
    first full test does not also measure getting used to the format
    (recommended, §4.3)?
41. Monthly checks show a score and its error band but no level
    (recommended: a half-length Listening or Reading check has a
    reliability of about 0.75, below the 0.80 the level rules need, and a
    Speaking or Writing check is probably lower, §4.3), or make checks about
    two-thirds of full length (about 21 scored items per receptive skill;
    about 40 minutes for Listening and Speaking, 45–50 for Reading and
    Writing, beyond the 30–40 minutes asked for) so they could reach about
    0.80 and, with cuts carried onto them, show levels? Either way, the comparison with the last full test
    appears only after calibration (from the month-5 check), and on the
    facilitator's chart.
42. Teacher scoring of speaking needs the audio, which is on the student
    computers (§5.3). Either the teacher listens on each student computer
    in a facilitator-only screen and enters scores on the dashboard (audio
    never leaves the computer, but slow: she moves between computers), or
    the audio travels to the dashboard computer in a separate file per
    sitting (about 1.7 GB for a full test across the centre), stays inside
    the centre and is deleted on the same schedule (faster and simpler; it
    changes the rule that audio never leaves the student computer, and the
    lawyer should confirm `audio_local` covers it, so it depends on Q44).
    (Recommended: the dashboard computer.)
43. Two readings of Q1 to confirm: "results" include the answer text and
    transcripts (the teacher scores them on the dashboard), but not audio
    unless Q42 says so; and "teacher-scored only" also rules out the local
    model of §5.7 until the lawyer's review.
44. Does the lawyer review the whole consent design before month 1, AI part
    included (recommended: the document has said from the start that the
    consent and data-protection parts need a lawyer before the pilot, and
    Q42's recommended option depends on it), or only the AI part, later?
    Do the AI scopes then go on the first consent forms, unused until
    scoring starts, or on a second form later (recommended, §6.2)? Who is
    the lawyer, and by when?
45. When AI consent is given later: (a) AI scores only answers given after
    it (recommended, as §6.4 says), or may it cover earlier answers if the
    lawyer agrees? (b) The AI's prompts need benchmark answers as examples
    (§5.4), and the month 1–2 benchmarks were given before any AI consent:
    ask those families for a separate consent to send them to Anthropic as
    examples, or collect new benchmarks after the AI consent?
46. While teachers score alone, does a second teacher double-score about 10%
    of open answers at every sitting (recommended: about 3–5 more hours in
    a full-test month), so that a Speaking or Writing level rests on a
    measured teacher-teacher agreement, or is the one-off baseline of §5.5
    enough?
47. Roll numbers: use Hikmat's existing numbers (the app adds a check
    character) or issue new ones? Who issues them, and are they entered once
    on the dashboard and carried to student computers in the roster file
    with the student's group (recommended, Q8), or typed on each computer?
    Who supplies the class lists the random groups are drawn from? What
    does a facilitator do for a student with no roll number on test day?
48. A student absent from a full test, or joining after month 1: a make-up
    full test within the testing window, on their group's form and left out
    of equating (recommended), or wait for the next quarter? How long is
    each testing window?
49. Teacher scoring on the dashboard: answers shown without name or roll
    number, in random order, and where possible scored by a teacher who does
    not teach that student (recommended, §11.4), or by the class teacher
    with names shown?
50. Benchmark consent: do the first consent forms include `benchmark_local`
    (an answer kept longer and shown to the teacher panel as a benchmark,
    needed for the month 1–2 panel, §4.4) (recommended), or are benchmarks
    asked for separately?

### 13.1 What each open question blocks

A question is listed under the first milestone it blocks; M0 is done when
everything up to M5 (and M8, if there are Ubuntu PCs) is answered.

| Blocks | Questions | Why |
|---|---|---|
| **Planning and M1** (content, consent notice, trial) | 4, 6, 7, 10, 17, 18, 20, 21, 22, 25, 26, 27, 28, 29, 31 (recording computer), 39, 40, 41, 44, 50 | What to write and record (level range, accent, number of forms, check pairing and length), the notice's content, who does the work, the order of milestones (28: if most PCs are Ubuntu, M8 moves up), who can install and update the app (29) |
| **M2a** (one Reading form, save and resume) | none beyond the go-ahead | The answered Q24 settles the resume rules |
| **M2b** (packs and schedule) | 8 (roster file), 19, 23, 47 (groups), 48 (window, make-ups) | The blueprints, the stopping rule, the schedule, the testing window, and how a student's group reaches every computer |
| **M4** (capture and consent) | 38 | How long audio stays |
| **M5** (export, dashboard, teacher scoring) | 12, 13, 30, 31 (dashboard), 34, 35, 36, 37, 42, 43, 46, 47, 49 | What month 1 shows (12: provisional levels need part of M7 before month 1), data folders per login, which computers, names, deletion, export rhythm, where the teacher listens, how teacher scoring works, roll numbers |
| **M7** (levels) | 11 | Overall level |
| **M8** (Ubuntu) | 32 (and 28, 29 above) | The build machine; `.deb` or `.tar.gz` follows from 29 |
| **M6 and M9** (AI scoring, after the lawyer's review) | 2, 3, 5, 9, 16, 45, and the staff-computer part of 31 | Only once AI scoring is to start |

---

## 14. Risks

| Risk | Effect | Mitigation |
|---|---|---|
| Content takes longer than code | Month 1 slips | Start M1 now; write the full forms and the Listening and Speaking check forms first, and the Reading and Writing check forms by month 3; reuse item types across levels |
| Panel smaller than the Council of Europe asks | Cuts less precise; weaker claim | Check each cut's standard error against the SEM; extra round or ranges; document the adaptation |
| Short sections can't separate 5 levels | Students placed a level off | Ranges near cuts; "not enough evidence" rule; two-stage forms after calibration |
| Month-to-month noise read as progress or decline | False alarms for students and teachers | Trends with error bands; no headline for one-level changes; cohort-level claims |
| Forms not equally hard | Fake growth or decline | One blueprint per kind of form; random groups at every sitting; shared items between check and full forms; calibration after month 4; re-scoring once |
| Check scores read as levels or as real change | Students or teachers over-read a half-length test | Checks show no level; change shown only beyond the error band; never flagged on one check alone (§4.3) |
| Item and audio leaks (shared computers, staff voices, reused forms) | Inflated scores | Rotation; confidentiality clause; speakers who don't teach the cohort; install packs near test week |
| AI disagrees with teachers on this population | Open-answer levels unusable for a skill | First AI study before trusting; teacher scoring fallback; examples per level; drift alarms |
| Whisper flatters grammar and fluency, invents text, drops audio after about 29.5 s | Lenient or wrong speaking scores | Timing rules for fluency; accuracy weighted low; chunking at pauses; flags for empty or odd transcripts |
| Non-English answers mistranscribed | Unfair scores | Flag and send to the teacher |
| DPDP dates brought forward; exemption unclear; Rule 8(3) vs erasure | Non-compliance | Comply now; parental consent always; counsel review |
| Children's text stored abroad up to 29 days | Privacy | Delete batches at once; pseudonymous IDs; redaction; consent; option of ordinary requests |
| Disclosure of abuse in an answer seen only by AI | Missed reporting duty | Keyword hold for teacher review before export |
| App update re-triggers Open Anyway and the microphone prompt (Mac) | Computer records silence on test day | No app updates in test weeks; forms as packs |
| No backups (Time Machine excluded) | A dead disk loses unexported tests | Cumulative exports, weekly if possible |
| Facilitator computer lost or broken | Cohort history lost | Backup to a second drive after every import; every imported file kept |
| Many families say no to AI scoring | Teacher scoring time stays high after AI is allowed | Measure the share at consent time; local-model consent as a middle option; plan teacher hours at M0 |
| Linux untested on real hardware; speech speed unknown; admin rights | Ubuntu PCs not ready | Early Linux spike if many PCs; borrow a PC; `.tar.gz` fallback; background transcription |
| Copied data folders duplicate IDs | Wrong merges | Conflicts flagged by sequence numbers, never merged silently |
| Model retirement or prompt changes | Scores shift | Pinned models; revalidation on any change |
| Teacher time not available once AI scoring is allowed | Spot checks lapse; levels unsafe | Decide capacity at M0; scale the AI sample and spot checks to it |
| Teachers score every open answer until the lawyer's review (about 30–45 hours in a full-test month) | Scores late; teachers rushed; month 1 levels delayed | Name the scorers before month 1 (Q26); scoring progress shown on the dashboard; the lawyer's review started early (Q44); Speaking and Writing results shown when scored (Q13) |

---

## 15. Later (not in scope now)

| Item | What it would need |
|---|---|
| **Online sync** | Internet on student computers (today they stay offline); an authenticated sync service; conflict handling (the import design in §11.3 already gives every record a global ID, so it would carry over); new consent wording for continuous transfer; DPDP security duties for data in transit |
| **A central server** | Hosting and maintenance, preferably in India; encryption, access control and logs [52, r.6]; breach reporting within 72 hours [52, r.7]; backups; someone responsible for it; a new privacy notice |
| **Windows** | A Windows build machine (no cross-building [69]); the speech library rebuilt without text-to-speech for Windows; installer and code signing (Windows SmartScreen warns about unsigned apps); microphone permissions in Windows privacy settings; testing on the actual PCs |
| **Phones** | The paused Android app brought up to the assessment design; the recognizer and packs shared; offline storage and export on phones; the Android speech library rebuilt without text-to-speech before any release (noted in `docs/later.md`); small-screen layouts; much more audio and touch testing |
| **Two-stage (adaptive) testing for everyone** | Calibrated items (M10) and a larger bank |
| **Pronunciation scoring by machine** | An audio-based model or features beyond Whisper's text, validated against teachers |
| **Spoken interaction** | A live partner (teacher or recorded turns with timing), and the CEFR interaction scales |

---

## 16. Sources

Simulations (by the research pass; scripts not kept in the repo, so treat
their figures as rough estimates, to be replaced by real data after
month 4): **S1** Rasch simulation of level classification: 32 items, levels
1.2 logits wide, abilities spread as N(−0.6, 1.2). **S2** Simulation of QWK
precision by sample size on a 6-point scale (4,000 replications).

The CEFR and standard setting

1. Council of Europe (2020), CEFR Companion Volume. https://rm.coe.int/common-european-framework-of-reference-for-languages-learning-teaching/16809ea0d4
2. Council of Europe (2009), Relating Language Examinations to the CEFR: A Manual. https://rm.coe.int/1680667a2d
3. Kaftandjieva, F. (2004), Reference Supplement, Section B: Standard Setting. https://rm.coe.int/1680667a1d
4. Council of Europe, Relating examinations to the CEFR. https://www.coe.int/en/web/common-european-framework-reference-languages/relating-examinations-to-the-cefr
5. British Council, UKALTA, EALTA, ALTE (2022), Aligning Language Education with the CEFR: A Handbook. https://www.ealta.eu.org/documents/resources/CEFR%20alignment%20handbook.pdf
6. AERA, APA, NCME (2014), Standards for Educational and Psychological Testing. https://www.testingstandards.net/uploads/7/6/6/4/76643089/standards_2014edition.pdf
7. Linacre, J. M. (1994), Sample size and item calibration stability. https://www.rasch.org/rmt/rmt74m.htm
8. ALTE for the Council of Europe (2026), New revised Manual for Language Test Development and Examining. https://rm.coe.int/new-revised-manual-for-language-test-development-and-examining-joint-p/48802cc30d
9. Council of Europe, Illustrations of the European levels of language proficiency. https://www.coe.int/en/web/common-european-framework-reference-languages/illustrations-of-levels

Test design

10. EF SET FAQ. https://www.efset.org/faq
11. EF SET score scale. https://www.efset.org/english-score/
12. O'Sullivan et al. (2020), Aptis General Technical Manual v2.2. https://www.britishcouncil.org/sites/default/files/aptis_technical_manual_v_2.2_final.pdf
13. Papageorgiou et al. (2021), Design Framework for the TOEFL Essentials Test (ETS RM-21-03). https://www.ets.org/Media/Research/pdf/RM-21-03.pdf
14. Pearson, Versant English Placement Test guide. https://jtest.haui.edu.vn/media/VEPT%20Official%20Test%20Guide_M%E1%BB%9Bi%20nh%E1%BA%A5t%2011.2024.pdf
15. Cambridge English, Linguaskill results. https://www.cambridgeenglish.org/exams-and-tests/linguaskill/results/
16. Livingston, S. (2014), Equating Test Scores (Without IRT), ETS. https://www.ets.org/Media/Research/pdf/LIVINGSTON2ed.pdf
17. Livingston & Kim (2010), Equating with randomly equivalent groups of 50 to 400 (ETS RR-10-05). https://www.ets.org/research/policy_research_reports/publications/report/2010/ibno.html
18. Hausknecht et al. (2007), Retesting in selection: a meta-analysis. https://ecommons.cornell.edu/server/api/core/bitstreams/4fbba765-0d9c-4940-8492-66fecaca5227/content
19. Stocking (1994), Three practical issues for modern adaptive testing item pools (ETS RR-94-05). https://www.ets.org/research/policy_research_reports/publications/report/1994/hxnr.html
20. Lee (2005), Dependability of scores for a new ESL speaking test (ETS RM-04-07). https://www.ets.org/research/policy_research_reports/publications/report/2005/imdp.html
21. Kostromitina & Plonsky (2022), Elicited imitation tasks as a measure of L2 proficiency. https://www.cambridge.org/core/journals/studies-in-second-language-acquisition/article/elicited-imitation-tasks-as-a-measure-of-l2-proficiency-a-metaanalysis/C52297B9096EF1DA34FA28639242E3ED
22. International Test Commission (2005), Guidelines on Computer-Based and Internet Delivered Testing. https://www.intestcom.org/files/guideline_computer_based_testing.pdf
23. ASER 2023, Bihar Muzaffarpur district report. https://asercentre.org/wp-content/uploads/2022/12/4_Bihar_Muzaffarpur.pdf
24. Sinharay (2010), When can subscores be expected to have added value? (ETS RR-10-16). https://www.ets.org/research/policy_research_reports/publications/report/2010/issf.html

Scoring with Claude and with people

25. Anthropic, Pricing. https://platform.claude.com/docs/en/about-claude/pricing
26. Anthropic, Batch processing. https://platform.claude.com/docs/en/build-with-claude/batch-processing
27. Anthropic, Models overview. https://platform.claude.com/docs/en/about-claude/models/overview
28. Anthropic, Structured outputs. https://platform.claude.com/docs/en/build-with-claude/structured-outputs
29. Anthropic, Thinking. https://platform.claude.com/docs/en/build-with-claude/thinking
30. Anthropic, API and data retention. https://platform.claude.com/docs/en/manage-claude/api-and-data-retention
31. Anthropic, Data residency. https://platform.claude.com/docs/en/manage-claude/data-residency
32. Anthropic, Commercial Terms of Service. https://www.anthropic.com/legal/commercial-terms
33. Anthropic, Data Processing Addendum. https://www.anthropic.com/legal/data-processing-addendum
34. Anthropic, Usage Policy. https://www.anthropic.com/legal/aup
35. Anthropic Privacy Center, How long do you store my organization's data? https://privacy.claude.com/en/articles/7996866-how-long-do-you-store-my-organization-s-data
36. Anthropic, Rate limits. https://platform.claude.com/docs/en/api/rate-limits
37. Ramineni, Trapani & Williamson (2015), Evaluation of e-rater for the Praxis I Writing Test (ETS RR-15-03), pp.6–7. https://files.eric.ed.gov/fulltext/EJ1109680.pdf
38. Williamson, Xi & Breyer (2012), A framework for evaluation and use of automated scoring, Educational Measurement: Issues and Practice 31(1). https://doi.org/10.1111/j.1745-3992.2011.00223.x
39. Li et al. (2025), Agreement between LLMs and human raters in essay scoring: a research synthesis. https://arxiv.org/abs/2512.14561
40. Yancey, LaFlair, Verardi & Burstein (2023), Rating short L2 essays on the CEFR scale with GPT-4. https://aclanthology.org/2023.bea-1.49
41. Voskoboinik et al. (2025), Finnish L2 speech scoring with LLMs. https://aclanthology.org/2025.aaas-1.2.pdf
42. Ormerod & Kwako (2024), Automated text scoring for the GPU-poor. https://arxiv.org/abs/2407.01873
43. Wagner et al. (2024), CrisperWhisper. https://arxiv.org/abs/2408.16589
44. Kim et al. (2024), LearnerVoice. https://www.isca-archive.org/interspeech_2024/kim24v_interspeech.pdf
45. Kopparapu, Bhat & Panda (2024), Spoken grammar assessment using LLM. https://arxiv.org/abs/2410.01579
46. Koenecke et al. (2024), Careless Whisper: speech-to-text hallucination harms. https://arxiv.org/abs/2402.08021
47. Bosker et al. (2013), What makes speech sound fluent? https://research-portal.uu.nl/en/publications/what-makes-speech-sound-fluent-the-contributions-of-pauses-speed-/
48. sherpa-onnx v1.13.8, Whisper recognizer (30-second limit). https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/v1.13.8/sherpa-onnx/csrc/offline-recognizer-whisper-impl.h
49. llama.cpp, performance on Apple silicon. https://github.com/ggml-org/llama.cpp/discussions/4167
50. Ollama model library and structured outputs. https://ollama.com/library/qwen3.5 · https://docs.ollama.com/capabilities/structured-outputs

Consent and data protection

51. Digital Personal Data Protection Act, 2023. https://www.meity.gov.in/static/uploads/2024/06/2bf1f0e9f04e6fb4f8fef35e82c42aa5.pdf
52. Digital Personal Data Protection Rules, 2025 (G.S.R. 846(E)). https://www.meity.gov.in/static/uploads/2025/11/53450e6e5dc0bfa85ebd78686cadad39.pdf
53. S.S. Rana & Co., MeitY plans to cut short the DPDP compliance timeline. https://ssrana.in/articles/meity-plans-to-cut-short-dpdp-compliance-timeline-and-notify-cross-border-restrictions-for-sdfs/
54. ICMR (2017), National Ethical Guidelines for Biomedical and Health Research. https://ethics.ncdirindia.org/asset/pdf/ICMR_National_Ethical_Guidelines.pdf
55. Protection of Children from Sexual Offences Act 2012, s.19. https://www.advocatekhoj.com/library/bareacts/childrenprotection/19.php
56. Anthropic, Guidelines for organizations serving minors. https://support.claude.com/en/articles/9307344-responsible-use-of-anthropic-s-models-guidelines-for-organizations-serving-minors

Listening audio

57. Field, J. (2013), Cognitive validity, in Examining Listening (Studies in Language Testing 35). https://www.cambridgeenglish.org/Images/735101-studies-in-language-testing-volume-35.pdf
58. Cambridge English, A2 Key Handbook for Teachers. https://www.cambridgeenglish.org/Images/610842-a2-key-handbook-for-teachers.pdf
59. Cambridge English, B1 Preliminary Handbook for Teachers. https://www.cambridgeenglish.org/Images/168150-b1-preliminary-teachers-handbook.pdf
60. Kang, Yan & Kostromitina (2024), Fairness of using different English accents (Duolingo English Test). https://experts.illinois.edu/en/publications/fairness-of-using-different-english-accents-the-effect-of-shared-
61. EBU R 128-2023, Loudness normalisation. https://tech.ebu.ch/docs/r/r128.pdf
62. ACX Audio Submission Requirements. https://help.acx.com/s/article/acx-audio-submission-requirements
63. FFmpeg filters documentation (loudnorm, astats, silencedetect). https://ffmpeg.org/ffmpeg-filters.html#loudnorm
64. Java SE, AudioFileFormat.Type. https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/sound/sampled/AudioFileFormat.Type.html
65. Field, J. (2015), The effects of single and double play upon listening test outcomes, British Council. https://www.britishcouncil.org/exam/aptis/research/publications/effects-single-and-double-play
66. AES loudness recommendations for streaming speech (secondary coverage). https://www.production-expert.com/production-expert-1/aes-update-loudness-recommendations-for-audio-only-streaming
67. Vuković-Stamatović & Čarapić (2024), on speech-rate bands (Tauroza & Allison 1990; Griffiths). https://doi.org/10.17398/2340-2784.47.201

Linux

68. Compose Multiplatform v1.10.3, TargetFormat.kt. https://github.com/JetBrains/compose-multiplatform/blob/v1.10.3/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/desktop/application/dsl/TargetFormat.kt
69. Kotlin docs, Compose native distributions. https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html
70. JEP 392, Packaging Tool. https://openjdk.org/jeps/392
71. AppImage docs, FUSE. https://docs.appimage.org/user-guide/troubleshooting/fuse.html
72. sherpa-onnx v1.13.8, linux-jni workflow (text-to-speech on). https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.8/.github/workflows/linux-jni.yaml
73. OpenJDK 17, Java Sound ALSA device code. https://github.com/openjdk/jdk17u/blob/master/src/java.desktop/linux/native/libjsound/PLATFORM_API_LinuxOS_ALSA_PCMUtils.c
74. JDK-8334549, sound test timeout with PipeWire down. https://bugs.openjdk.org/browse/JDK-8334549
75. OpenJDK 17, HotSpot argument parsing (unknown options refused). https://github.com/openjdk/jdk17u/blob/master/src/hotspot/share/runtime/arguments.cpp
76. XDG Base Directory Specification. https://specifications.freedesktop.org/basedir/latest/
77. fsync(2), Linux manual. https://man7.org/linux/man-pages/man2/fsync.2.html
78. Ubuntu release cycle. https://ubuntu.com/about/release-cycle
79. ONNX Runtime v1.28.2, CPU feature detection. https://github.com/microsoft/onnxruntime/blob/v1.28.2/onnxruntime/core/mlas/lib/platform.cpp
80. GitHub-hosted runners. https://docs.github.com/en/actions/reference/runners/github-hosted-runners
81. Snapcraft, audio-record interface. https://snapcraft.io/docs/reference/interfaces/audio-record-interface/

Other

82. Liang et al. (2023), GPT detectors are biased against non-native English writers. https://arxiv.org/abs/2304.02819
83. ULID specification. https://github.com/ulid/spec
84. Anthropic Java SDK (Apache-2.0). https://github.com/anthropics/anthropic-sdk-java

---

## Appendix A: How it fits the existing code

### A.1 Modules

| Module | What |
|---|---|
| `:domain`, `:speech` | Unchanged. The normalizer and aligner score typed answers and read-aloud; the recorder records answers with test-specific limits |
| `:assessment-core` (new) | Pure Kotlin: item types, forms, the pack loader and checks, the session engine and resume rules, scoring rules, IDs, and the roster, export, import and scoring file formats. Shared by the app, the dashboard and the scorer, so the formats cannot drift |
| platform layer (new) | One interface with a Mac and a Linux implementation: data folder, backup exclusion, machine summary, microphone checks (codesign and the AVFoundation query are Mac-only), native library folder, shortcut key, whether practice is available |
| `:desktop` | Practice (Mac) plus the assessment (Mac and Ubuntu) |
| `:dashboard` (new) | The facilitator app |
| `:scorer` (new) | A small command-line tool for the staff computer, in Kotlin on the same Java runtime, using Anthropic's Java SDK (Apache-2.0) [84], at a release that has been out for some months |

### A.2 Data on disk (additions)

```
<data folder>/
  computer.json                                   computer ID and label
  assessments/packs/<packId>/<version>/           unpacked test packs, never overwritten
  assessments/exports/                            a copy of every export written
  roster.json                                     from the dashboard: roll numbers, groups, consent (§6.3)
  learners/<id>/identity.json                     student ID, roll number, group (a new file: today's app
                                                  rewrites learner.json and would drop new fields)
  learners/<id>/consent.json                      consent events, append-only
  learners/<id>/assessment/sessions/<session>.events.jsonl   one line per answer, the source of truth
  learners/<id>/assessment/sessions/<session>.json           snapshot after every answer
  learners/<id>/assessment/responses/<response>.wav          speaking audio, never leaves the centre (Q42)
  learners/<id>/assessment/responses/<response>.json         text or transcript, status
  learners/<id>/assessment/results/<session>.json            final result, never edited
```

Writes: append the answer to the log and flush; then rewrite the snapshot
with the existing safe-write helper (temporary file, sync, atomic rename),
which gains a sync of the folder for Linux [77]. On load, a snapshot that
doesn't match the log is rebuilt from the log and the bad copy kept aside,
as the app already does for damaged progress files. Session and response IDs
are ULIDs, sortable by time and safe as file names [83].

### A.3 Decisions in `docs/decisions.md` this touches

| Decision | Change needed |
|---|---|
| "All data stays on that Mac. No tracking or sharing for now." | Changed by the owner on 2026-10-23 (§13, Q1): results go to the dashboard; answer text to Anthropic only with consent, after the lawyer's review |
| "No LLM in the app" | The app stays LLM-free. The owner allowed (2026-10-23, §13, Q1) scoring name-removed answer text with Claude on a staff computer, with recorded consent and after the lawyer's review |
| Deleting a student removes everything | Warn about unexported results first, and send a pseudonymous tombstone to the dashboard (§13, Q35) |
| Keep only the latest recording per sentence | Assessment audio kept per answer, then deleted on a schedule (§6.5) |
| Data folder excluded from Time Machine | Exports become the only copy off the computer (§13, Q37) |
| Profiles: the name is recorded as audio, no typing | Staff type a roll number for every student before the first test (§13, Q33) |
| Every Hindi string reviewed by a teacher | Applies to every new screen, the consent notice, instruction audio and the rubric's Hindi lines |
| Licences: nothing copyleft beyond the Java runtime | The Linux speech library rebuilt without text-to-speech; new libraries permissive only |
