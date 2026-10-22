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
  heard word, so a word heard as two words cannot be accepted. Whisper
  usually hears "Patna" as "but now" (`docs/asr-model-comparison.md`); for
  v0.1 the owner chose not to score place names instead. Matching would need
  phrase-level alternatives in the aligner.
- **End-of-speech detection with a model.** If the loudness-based detector
  stops too late or too early in noisy homes, sherpa-onnx's Silero VAD (a
  small extra model) could tell speech from noise.
- **The Android app's speech library has text-to-speech inside.**
  `sherpa-onnx-static-link-onnxruntime-1.13.8.aar` compiles in eSpeak NG
  (GPL-3.0), as the desktop library did before it was rebuilt
  (`docs/decisions.md`, "Licences: owner answers"). Before an Android build
  is shared, rebuild it without text-to-speech too, or move to sherpa-onnx
  2.0, which drops eSpeak NG.
- **Recording at 48 kHz as a fallback.** Android requires phones to record
  at 16 kHz, but if a cheap phone can't, the app could record at 48 kHz and
  let sherpa-onnx resample.

## macOS desktop app

- **Notarize the app** (`docs/packaging.md`, "Signing"): with an Apple
  Developer ID, signing with the hardened runtime and the four entitlements
  listed there, and notarization, macOS would open the app without the
  one-time Open Anyway, and keep the microphone permission across updates.
  (Packaging itself was done on 2026-10-21, before D9: the owner wants
  students on a normal Mac app now.)
- **Remove an added lesson pack** in Settings. Not needed yet: packs are
  only ever replaced by newer versions.

## App size

- **Smaller APK for sharing directly.** The single APK that works on every
  phone is about 76 MB. It carries the speech library twice (64-bit and
  32-bit ARM), uncompressed so that Android can load it without unpacking.
  Separate APKs per processor type, or compressed libraries, would make it
  much smaller to pass between phones. Play Store installs already get only
  the library their phone needs.

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
- **Words removed from the content.** Leitner entries are keyed by word. If
  a lesson edit removes a word, its entry stays in the box. Sentence choice
  already ignores such words, but they would still show in "Practise again
  tomorrow". The app could drop entries for words no longer in any unlocked
  lesson.
