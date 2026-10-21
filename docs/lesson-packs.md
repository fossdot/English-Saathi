# Lesson packs

Lessons are kept separate from the app (`docs/decisions.md`, "Distribution,
defaults and lessons: owner answers"). English Saathi ships with the first
pack inside (`content/packs/starter.json`: Bus stand). More lessons come as
pack files that a facilitator adds in Settings › **Add lessons**. Added packs
live in the data folder (`~/Library/Application Support/English Saathi/lessons/`),
so they and all progress survive app updates.

## The file

One UTF-8 `.json` file, format 1 (text only; a later format may add pictures
in a `.zip`):

```json
{
  "format": 1,
  "id": "bihar-places-1",
  "version": 1,
  "order": 1,
  "title_en": "Places in town",
  "title_hi": "शहर की जगहें",
  "hints": {
    "z_j": { "text_hi": "…", "reviewed": false }
  },
  "lessons": [
    {
      "id": "market",
      "title_en": "Market", "title_hi": "बाज़ार",
      "words": {
        "bananas": { "syllables": ["ba", "na", "nas"], "hint_hi": "बनानाज़", "hint_tag": "generic", "reviewed": false }
      },
      "sentences": [
        { "id": "mkt_01", "text": "How much are the bananas?", "hi": "केले कितने के हैं?", "chunks": ["How much", "are the bananas"], "picture": null }
      ]
    }
  ]
}
```

- **`id`**: names the pack across versions: lower-case letters, digits, `-`
  or `_`, up to 40. Never change it between versions.
- **`version`**: a whole number, 1 or more. Raise it for every new copy you
  send out. English Saathi refuses a version that is the same as, or older
  than, the one it has ("nothing was changed").
- **`order`**: where the pack's lessons go in the unlock order, after the
  packs that come with the app: lower first, then by `id`.
- **`hints`**: tip templates for this pack's words. Optional: the app's own
  templates (`v_w`, `th`, `s_cluster`, `short_vowel`, `generic`) are always
  there, and a pack's template with the same tag replaces the app's for that
  pack's lessons.
- **`lessons`**: in the order learners open them. Each lesson is exactly the
  lesson format agreed in `docs/decisions.md` ("Word data scope"): one word
  table, sentences with `chunks`, `"score": false` for words the recognizer
  can't be expected to hear (place names), `"unscored"` for a sentence-only
  override.

## What the app checks

A pack is refused, and nothing changes, when:

- it isn't valid JSON, or a field is missing or of the wrong kind;
- it has no lessons, or a lesson fails the lesson checks (a sentence without
  English words, a sentence with nothing to score, word keys that aren't
  normalized words, …);
- a lesson id or sentence id is used twice in the pack, or by another pack
  (ignoring case), or isn't made of letters, digits, `-` and `_` (sentence
  ids name the learners' recordings);
- its `format` is newer than the app reads ("needs a newer version of
  English Saathi");
- the same pack is already there in the same or a newer version;
- the file is over 20 MB.

What the lesson checks only warn about (an unused word entry, a word
without one, an unknown hint tag, …) is written to the log, and the pack is
added.

## Unlocking and progress

- Lessons open in order: the next lesson opens once every sentence of the one
  before it has been said at least once, with any stars. "Didn't hear you"
  doesn't count. Settings › Students › **Open all lessons** opens everything
  for one student.
- A lesson that has opened for a student stays open, even if a later version
  of a pack adds a sentence to the lesson before it.
- Progress is kept by sentence id, so **never reuse a sentence id for a
  different sentence**. A new version may change a sentence's wording, add
  sentences and lessons, or drop them; dropped lessons disappear from Home but
  the students' progress for them stays in the data folder (and returns if
  the lesson comes back).

## Size

Bus stand is about 3 KB as written. A 100-lesson pack of lessons like it is
about 310 KB, about 80 KB compressed, and the app checks it in about 50 ms
(`PackSizeTest`).
