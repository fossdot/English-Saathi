# English speaking practice (working title)

Offline Android app that helps Hindi speakers in rural Bihar practise spoken
English. A learner says a sentence; the app finds the words that went wrong
and walks them through fixing them.

**Status:** milestone a of v0.1 is done: the domain logic and its tests. The
Android app (recording, speech recognition, screens) comes in later
milestones. This README will grow into the full one at the end of v0.1.

## Layout

| Path | What |
|---|---|
| `domain/` | Pure Kotlin rules, with no Android imports: normalizer, aligner, scoring, tip selection, fixing loop, streak, Leitner box, sentence choice |
| `docs/pilot/` | The web pilot; the reference implementation for the rules |
| `docs/decisions.md` | Everything agreed with the product owner, including where the Kotlin rules deliberately differ from the pilot |
| `docs/later.md` | Ideas kept out of v0.1 |
| `design/` | Screen designs (one HTML file with all six screens) |
| `tools/pilot-golden/` | Records the pilot's behaviour as golden test cases for the Kotlin port |

## Running the tests

Requires JDK 17. The Gradle wrapper downloads Gradle 9.7.1.

```sh
export JAVA_HOME=/path/to/jdk-17
./gradlew :domain:test
```

The tests are hand-written unit tests plus `PilotGoldenTest`. That test
replays several thousand cases recorded from the pilot's own JavaScript, with
the agreed changes applied. To re-record them after the pilot changes, see
`tools/pilot-golden/README.md`.
