# Ideas for after v0.1

Out of scope for v0.1. Collected here so they are not lost.

## Speech recognition

- **UNCLEAR words (low confidence).** sherpa-onnx's Whisper decoder takes
  the plain argmax of the logits and exposes no per-token probability in its
  Kotlin API (checked against v1.13.8). Options: patch the native decoder to
  return log-probs, or switch to a model whose Kotlin result carries them
  (streaming transducers expose `ysProbs`). The aligner would then mark a
  CORRECT word with low confidence as UNCLEAR.
- **Multi-word alternate spellings.** An `accept` entry is compared with one
  heard word, so a place name heard as two words ("betty ah") cannot be
  accepted. Matching would need phrase-level alternatives in the aligner.

## Text matching

- **Ordinals.** "1st" is compared as written, so a lesson's "first" does not
  match a transcript's "1st". Spell out ordinals like cardinal numbers.
- **Indian number words, times, decimals and fractions.** Digits are
  spelled out the international way ("100000" → "one hundred thousand", not
  "one lakh"). Punctuation inside a number joins its digits: "10:30" becomes
  "one thousand thirty", "1.5" becomes "fifteen" and "1/2" becomes "twelve".
  Lessons should write prices and times as digits until this is handled.
- **Other dash characters.** Only the ASCII hyphen separates words. A
  word processor's non-breaking hyphen or en dash joins the words ("busstop"),
  as in the pilot.

## Content and teaching

- **Contractions.** "Where's the bus stop?" is marked wrong against "Where is
  the bus stop?". We could accept common contractions as equal to their
  expanded forms.
- **Lesson-level word table.** Accept lists and hints are per sentence (spec
  format). Words that recur across sentences (bus, the, ticket) need their
  data repeated; a lesson-level table would remove the duplication.
- **Words removed from the content.** Leitner entries are keyed by word. If
  a lesson edit removes a word, its entry stays in the box. Sentence choice
  already ignores such words, but they would still show in "Practise again
  tomorrow". The app could drop entries for words no longer in any unlocked
  lesson.
