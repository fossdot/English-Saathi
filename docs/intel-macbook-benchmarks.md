# Intel MacBook check: dependencies, speech models and small LLMs

Asked by the owner on 2026-10-19. Target machine: a 2019 16-inch MacBook Pro
(Intel Core i7/i9, 6–8 cores, 16 GB RAM), running Ollama on the CPU only.
Nothing here changes the app; it is a report to decide on.

**Every speed figure in this report was measured on an Apple M4 running the
Intel builds under Rosetta, and is unconfirmed for the 2019 MacBook Pro.**
Section 4 says how to measure on the real machine.

## Summary

- **Dependencies:** everything the desktop app needs runs on an Intel Mac
  today. The one real risk is Compose Multiplatform: from 1.11.0 on, its
  supported-platforms list no longer names macOS x64, although the Intel
  artifacts are still published. Proposal: pin **Compose Multiplatform
  1.10.3**, the newest release that still officially supports Intel Macs
  (section 1). macOS Tahoe 26 is the last macOS for these MacBooks, and
  JDK 25 is the last Temurin LTS built for them.
- **Speech: no default chosen yet.** Keep both tiny.en (the app's 44 MB
  build) and small.en (int8) in the desktop app until student recordings
  have been tested. On correctly read synthetic sentences, tiny.en marks at
  least one word wrong 43% of the time and small.en 22%. Both are too high
  to accept without real data. base.en is barely better than tiny.en.
- **LLMs: recommendation for now, no LLM in the app.** Use LLMs only
  offline, to draft lesson content (Task A), and have a teacher review
  every draft. Of 14 models, gemma3:4b (10 of 10 drafts well-formed) and
  qwen3:4b-instruct (8 of 10) are the candidates on format, at about 1.5–2
  minutes per sentence. The Hindi-focused Nemotron-4-Mini-Hindi-4B, rerun
  with a fixed stop marker, is the third (6 of 10). Nothing has been
  judged for Hindi yet: a fluent speaker should score
  `tools/llm-bench/results/short-scoring-sheet.csv` (these three plus
  Param-1, 72 outputs). Live explanations (Task B) are not recommended:
  only the smallest models answered in time. Every model fitted in 3.3 GB
  of RAM.

## 1. Dependencies on macOS x86_64

Checked against primary sources (release pages, Maven Central, vendor docs)
on 2026-10-19.

| Piece | Intel macOS status | Notes |
|---|---|---|
| macOS | Tahoe 26 supports the 2019 16-inch MacBook Pro | Apple: "macOS Tahoe will be the final release for Intel Macs". macOS 27 is Apple silicon only. |
| JDK | Temurin 17, 21 and 25 (x64) are current, released 2026-08-19 | Adoptium won't build JDK 27 for macOS x64, and JEP 541 deprecates the port in JDK 28, so JDK 25 is the last Temurin LTS for these Macs. JetBrains Runtime 21 and 25 also ship x64 builds. |
| Kotlin 2.4.21, Gradle | Fine on JDK 17 or later | Kotlin's Gradle plugin supports Gradle 7.6.3–9.7.0. |
| Compose Multiplatform (desktop, JVM) | **1.10.3 officially supports Intel Macs; 1.11.0 and later work but don't.** See below. | Metal rendering on the Radeon Pro 5300M/5500M is unconfirmed; `SKIKO_RENDER_API=SOFTWARE` is the fallback. Packaging (jpackage) has to run on an Intel JDK; running from Gradle doesn't care. |
| sherpa-onnx 1.13.8 (speech) | **Supported.** `sherpa-onnx-jvm` plus `sherpa-onnx-native-lib-osx-x64` (and `-osx-aarch64` for Apple silicon) from JitPack, not Maven Central | Offline Whisper works on the JVM, and the existing speech code can sit behind the `Transcriber` interface. The bundled onnxruntime 1.28.2 chooses AVX2 kernels at run time. The benchmarks used the Python wheel, which ships the byte-identical onnxruntime library; the JVM path itself has not been run yet. |
| whisper.cpp 1.9.5 | Builds for Intel from source (no binary releases) | Its Java binding on Maven Central is from 2023 and bundles only a Windows library. It was no better than sherpa-onnx here (below), so it's only a fallback. |
| Ollama 0.40.6 (the benchmark used 0.34.0) | One universal app; on Intel it runs on the **CPU only** (no AMD GPU support) and needs macOS 14 or later | It picks the AVX2 ("haswell") kernels on an i7/i9 automatically. |

### Which Compose Multiplatform version

- **1.10.3** (2026-03-19) is the last release whose supported-platforms page
  lists "macOS 12 x64, macOS 13 arm64". The docs change that removed
  "macOS 12 x64" came with 1.11.0 (kotlin-multiplatform-dev-docs PR #687,
  merged 2026-05-13). It needs Kotlin 2.1.0 or later, and the Compose
  compiler plugin must match the Kotlin version.
- **Proposal: pin 1.10.3.** It matches "stability over newest" and is the
  only version JetBrains says works on these Macs.
  - Not yet checked: that 1.10.3 builds with the project's Kotlin 2.4.21,
    which is newer than 1.10.3. If it doesn't, the desktop module needs an
    older Kotlin and the domain module has to compile with it too.
  - Its Material 3 library is a pre-release (`material3:1.10.0-alpha05`).
    The plan has to choose between that, plain Material, or a hand-made
    theme.
- **The risk of 1.11.0 or later (1.12.2 is current).**
  - The Intel desktop artifacts (`desktop-jvm-macos-x64`,
    `skiko-awt-runtime-macos-x64`) are still published, and the 1.13
    alpha still bundles the Intel Skia library. The 1.11.0 release notes
    remove only the Kotlin/Native `macosX64` target, not the JVM desktop
    one.
  - But Intel is no longer tested or promised. A rendering or input bug
    that shows only on Intel Macs would not be treated as a supported
    platform issue, and a later release could drop the Intel library
    without notice. Since Apple stops at macOS Tahoe 26 for Intel and
    Kotlin has deprecated `macosX64`, that is the likely direction.
  - Being on 1.10.3 also means no fixes after March 2026 and nothing from
    1.11/1.12, so moving later costs more the longer we stay.
- **Either way:** check the chosen version on a real 2019 MacBook Pro
  (window, Metal rendering on the AMD GPU, microphone, keyboard shortcuts)
  before students use it.

## 2. Speech models

Scored with the app's rules (place names unscored; to/two/too, for/four,
one/won equal) on the 375 synthetic clips. The accuracy columns don't depend
on the machine. Times are sherpa-onnx per 3-second clip, median of 7 runs
with 4 and 6 threads, **M4 under Rosetta, unconfirmed**. RAM is the peak of
the whole process.

| Model | Files MB | Words right | Correct readings fully right | Mistakes caught | False alarms | 3 s clip, 4 / 6 threads (M4 under Rosetta, unconfirmed) | Peak RAM MB |
|---|---|---|---|---|---|---|---|
| tiny.en, the app's 44 MB build | 44.3 | 83.4% | 57.4% | 90.6% | 13.3% | 0.15 / 0.19 s | 276 |
| base.en int8 | 160.6 | 86.7% | 65.2% | 86.5% | 14.9% | 0.33 / 0.43 s | 543 |
| base.en fp32 | 292.5 | 87.7% | 65.2% | 86.5% | 11.7% | 0.31 / 0.38 s | 863 |
| small.en int8 | 375.5 | 91.9% | 78.1% | 89.6% | 8.4% | 1.40 / 1.52 s | 1,033 |
| small.en fp32 | 969.5 | 92.4% | 78.5% | 89.6% | 7.6% | 1.42 / 1.58 s | 1,780 |

**No default is chosen yet.** Keep tiny.en and small.en (int8) both in the
desktop app's model switch, log which one each attempt used, and decide once
student recordings have been run through `tools/asr-compare`. The numbers
that decide it are how often a correct reading is marked wrong and whether
small.en's wait is acceptable on the real MacBook.

- **What the columns mean for a student.** "Correct readings fully right"
  is how often a sentence read correctly comes back with no word marked
  wrong. With tiny.en, 43% of correct readings show the student a mistake
  they didn't make; with small.en, 22%. "False alarms" counts the same thing
  per word on clips that had a planted mistake elsewhere.
- **Where small.en helps most:** the Indian English voices and noise. With
  noisy recordings, sentences fully right go from 50% (tiny) to 73%
  (small int8), and with the "phone" condition from 42% to 74%. It is the
  only model that sometimes catches "How match is the ticket?" (4 of 8;
  every other model hears "much").
- **int8 vs fp32:** the same accuracy within noise, at less than half the
  size and RAM. Use int8.
- **whisper.cpp** gave the same accuracy as sherpa-onnx (tiny.en 82.9%,
  small.en 91.4% words right) and was not faster: 0.23 s (tiny.en), 0.45 s
  (base.en) and 1.38 s (small.en) per 3-second clip, even with its
  shortened audio context (`-ac 650`). Without it, small.en took 3.2 s
  (all M4 under Rosetta, unconfirmed). sherpa-onnx stays.
- **Silence and noise:** every model returned nothing (or only a bracketed
  sound tag) for the 6 silence and noise clips, except whisper.cpp base.en
  with its full audio context (5 of 6). Babble clips are excluded from that
  count.
- **whisper.cpp's AVX2 build** scored exactly the same as its SSE4.2 build
  for tiny.en and small.en, so the instruction set changes only speed.
- **Speed on the real machine is unknown.** Under Rosetta onnxruntime ran
  its SSE4.2 code; an i7/i9 will run AVX2 code but on slower cores. The
  ratio between models should hold better than the absolute times: here
  small.en cost about 8–9× tiny.en. A student waits for the result after
  the 1.2 s silence that ends recording, so 1–1.5 s on top may be
  acceptable in a facilitated session and 3 s would not be; only the real
  MacBook can say which side small.en lands on.
- **Limits.** The clips are macOS voices, not students. The Devanagari-input
  voice (lekha-deva) is the hardest for every model (tiny.en 22% of
  sentences fully right, small.en 51–53%). The student recordings the owner
  will send are the real test.

Full tables (by voice, speaking style and recording condition, per planted
mistake, and output on silence) are in
`tools/llm-bench/results/report-speech--app-rules.md`, with the raw results
in `tools/llm-bench/results/results-speech/`. They come from
`tools/asr-compare/score.py --app-rules`.

## 3. Small LLMs through Ollama

Separate from the app. Ollama 0.34.0 (x86_64, the version installed here;
0.40.6 is current) on the CPU, Q4_K_M weights, temperature 0, one
request at a time, with the app's speech model loaded alongside. 10
sentences for Task A (draft the Hindi translation, chunks, syllables and a
sound hint per word in the lesson JSON format; up to 800 tokens) and 8
mistake cases for Task B (explain the first mistake in 1–2 sentences of
simple Hindi; up to 160 tokens).

Times are medians, **M4 under Rosetta, unconfirmed**. "Well-formed" is
mechanical only: valid JSON with the right keys, Devanagari present, chunks
that cover the sentence, word keys from the sentence, valid hint tags. It
says nothing about whether the Hindi is right.

| Model | Hindi (model card) | Licence: commercial use? | Peak RAM, Ollama | A: s per sentence | A: well-formed | B: first token | B: s per answer | B: ≤ 2 sentences |
|---|---|---|---|---|---|---|---|---|
| llama3.2:1b (q4_K_M) | officially supported | Llama 3.2 Community: yes, with conditions | 1.2 GB | 24 | 2/10 | 1.0 s | 3.7 | 7/8 |
| gemma3:1b | 140 languages, Hindi not named | Gemma Terms: yes, with conditions | 1.1 GB | 28 | 2/10 | 2.0 s | 4.5 | 2/8 |
| EuroLLM-1.7B-Instruct | listed | Apache 2.0: yes | 1.6 GB | 43 | 2/10 | 1.8 s | 9.1 | 0/8 |
| qwen3:1.7b (thinking off) | listed | Apache 2.0: yes | 2.0 GB | 34 | 4/10 | 1.5 s | 10.0 | 6/8 |
| qwen2.5:3b | Hindi not named | Qwen Research: **no** | 2.3 GB | 42 | 10/10 | 2.8 s | 14.4 | 6/8 |
| llama3.2:3b | officially supported | Llama 3.2 Community: yes, with conditions | 2.6 GB | 55 | 0/10 | 2.7 s | 9.4 | 8/8 |
| Falcon-H1-3B-Instruct | listed | TII Falcon License: yes, with conditions | 3.0 GB | 32 | 0/10 | 5.4 s | 18.7 | 2/8 |
| phi4-mini (3.8B) | not listed | MIT: yes | 3.2 GB | 51 | 4/10 | 3.5 s | 7.6 | 7/8 |
| gemma3:4b | 140 languages, Hindi not named | Gemma Terms: yes, with conditions | 2.9 GB | 111 | **10/10** | 15.9 s | 25.8 | 6/8 |
| qwen3:4b-instruct | listed | Apache 2.0: yes | 3.3 GB | 96 | **8/10** | 5.2 s | 22.8 | 7/8 |
| nemotron-mini (4B) | English | NVIDIA Open Model: yes, with conditions | 2.7 GB | 53 | 4/10 | 2.5 s | 4.0 | 8/8 |
| Nemotron-4-Mini-Hindi-4B (rerun ‡) | Hindi, English, Hinglish | NVIDIA Open Model: yes, with conditions | 2.7 GB | 94 | **6/10** | 3.6 s | 6.5 | 8/8 |
| Param-1-2.9B-Instruct (rerun ‡) | English and Hindi | Apache 2.0 or MIT (the card says both): yes | 2.3 GB | 74 | 4/10 | 1.8 s | 3.1 | 5/8 |
| qwen3:4b (thinking) † | listed | Apache 2.0: yes | 3.2 GB | 224 | 0/10 | 3.5 s | 39.4 | 0/8 |

‡ **Rerun on 2026-10-20 with fixes for their community GGUF files**, set in
`tools/llm-bench/model_overrides.json`. The first run's records are kept
in `results/results-llm/`. The rerun ran while this Mac was busier, so its
times are less comparable than the other rows'.

- **Nemotron-4-Mini-Hindi-4B** (`itlwas/...`): the file's chat template has
  no stop marker, so in the first run the model went on writing invented
  `<extra_id_1> User` turns until the token limit (0 of 10 well-formed).
  With `<extra_id_1>` as a stop marker it stops properly. One thing the
  checks don't test: in 7 of its 10 drafts the `"text"` field holds
  something other than the English sentence (usually the Hindi).
- **Param-1-2.9B-Instruct** (`arunmcops/...`, converted from a custom
  architecture): Ollama gave the file a Qwen3-style template that ends the
  prompt with an open `<think>` tag, so the model stopped at once with an
  empty answer. The rerun uses the model's own ChatML format, from
  `chat_template.jinja` in `bharatgenai/Param-1-2.9B-Instruct`. It now
  answers, but in Task A it often repeats chunks until the token limit.

† **qwen3:4b** now resolves to the "thinking" build, which reasons at length
despite `think: false` and is cut off every time. qwen3:4b-instruct is the
model to use.

### Licences

Checked against each model's own card and licence text on 2026-10-20. The
community GGUF files (`itlwas`, `arunmcops`, `QuantFactory`) carry the
original model's licence. This is a summary for choosing, not legal advice;
read the full licence before shipping anything built with a model.

| Licence | Models | Commercial use | Restrictions |
|---|---|---|---|
| Apache 2.0 | qwen3:1.7b, qwen3:4b-instruct, qwen3:4b, EuroLLM-1.7B; Param-1 (card metadata) | Yes | Keep the licence and notices when redistributing; no trademark rights. |
| MIT | phi4-mini; Param-1 (card text) | Yes | Keep the copyright notice. Param-1's card says Apache 2.0 in its metadata and MIT in its text, and links a LICENSE file that isn't in the repo; both allow commercial use. |
| Llama 3.2 Community License | llama3.2:1b, llama3.2:3b | Yes, with conditions | Follow Meta's Acceptable Use Policy; a product with over 700 million monthly users needs Meta's permission; anyone distributing the model must include the licence and show "Built with Llama"; a fine-tuned model's name must start with "Llama". |
| Gemma Terms of Use | gemma3:1b, gemma3:4b | Yes, with conditions | Follow Google's Gemma Prohibited Use Policy; pass the same restrictions on to anyone given the model; Google may restrict use it believes breaks the terms. |
| NVIDIA Open Model License (June 2024) | nemotron-mini, Nemotron-4-Mini-Hindi-4B | Yes ("Models are commercially useable") | When distributing, include the agreement and the notice "Licensed by NVIDIA Corporation under the NVIDIA Open Model License"; the licence ends for anyone who sues over IP in the model; NVIDIA may update the terms, and users must comply or stop. NVIDIA claims no ownership of outputs. |
| TII Falcon License (Apache 2.0 based) | Falcon-H1-3B-Instruct | Yes, with conditions | Follow TII's Acceptable Use Policy, which TII can change; public statements about a derived model must say it is "built using Falcon LLM technology from the Technology Innovation Institute". The card links TII's terms page without naming a version. |
| Qwen Research License | qwen2.5:3b | **No** | "Research or evaluation purposes only"; commercial use needs a licence from Alibaba Cloud. Dropped from every recommendation. |

For offline drafting the distribution clauses don't arise, because we don't
ship the model. The use policies (Meta, Google, TII) still apply.

### Recommendation

**No LLM in the app for now.** Use LLMs only offline, to draft lesson
content (Task A), and have a teacher review every draft before it reaches
students. The Hindi is unjudged, and a wrong explanation shown live to a
learner can't be caught in time.

- **Candidates for drafting:** gemma3:4b (10 of 10 well-formed),
  qwen3:4b-instruct (8 of 10) and Nemotron-4-Mini-Hindi-4B (6 of 10, and
  the only one trained for Hindi). Drafting is our job, not the students',
  so 1.5–2 minutes per sentence is fine. The Hindi reviewer's scores decide
  between them. qwen2.5:3b is out: its licence forbids commercial use.
- **Not now: live explanations (Task B).** Only llama3.2:1b (3.7 s),
  Param-1 (3.1 s, after its fix), nemotron-mini (4.0 s) and gemma3:1b
  (4.5 s) answered within a few seconds, and gemma3:1b broke the
  two-sentence limit in 6 of 8 cases. The other models took 6.5–26 s. The
  real MacBook's AVX2 kernels may change this, so measure it there if live
  explanations come back up.

Other findings:

- **Memory is not the limit:** no model needed more than 3.3 GB, leaving
  plenty of the 16 GB, and the speech model transcribed its clip correctly
  after every model.
- **gemma3:4b read its prompts unusually slowly** (30 tokens/s against
  127–338 for the other 3–4B models, hence its 16–18 s first token). In
  llama-bench, using the same file outside Ollama, it read prompts at the
  same speed as phi4-mini. So this may come from Ollama under Rosetta;
  check it on the real Mac.
- **Not run:** gemma3n (7.8–9.8 GB downloads), ministral-3 (no Hindi),
  BharatGPT-3B-Indic (research-only licence, gated), Sarvam-1 (a base model,
  not instruction-tuned) and Ganga-2-1B (no GGUF).

Files in `tools/llm-bench/results/`:

- `summary.md`: the full table, with load time, tokens per second and
  prompt speed.
- `review.md`: every output, by task and case.
- `scoring-sheet.csv`: one row per output (252) for the Hindi reviewer.
- `short-review.md` and `short-scoring-sheet.csv`: the same for only
  gemma3:4b, qwen3:4b-instruct, Nemotron-4-Mini-Hindi-4B and Param-1
  (72 outputs). Start with these.
- `results-llm/` and `results-llm2/`: the raw per-model results of the
  first run; `rerun/`: the two Hindi models after their fixes. The merged
  files above use the rerun for those two.
- `results-llm/llama-bench-x64avx2.jsonl`: raw llama.cpp speeds from an
  AVX2 build. Under Rosetta its prompt speeds are far below Ollama's, so it
  is kept only for comparison with the real Mac.

## 4. Running the benchmarks on a real 2019 MacBook Pro

`tools/bench-intel/run_on_intel_mac.sh` repeats the speech and LLM runs
natively on the Intel Mac and writes the same tables. It takes two to four
hours (the LLMs are most of it) and about 25 GB of disk.

1. **Plug the MacBook into power** and close other apps. The script keeps
   the Mac awake itself (step 6), but on battery a laptop slows down.
2. **Install the tools**, in Terminal:
   ```sh
   xcode-select --install                 # git, clang; skip if already installed
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"   # if Homebrew is missing
   brew install python@3.12 cmake
   ```
   Install Ollama from https://ollama.com/download/mac (needs macOS 14 or
   later) and open it once; allow it to install the `ollama` command line
   tool when asked (`which ollama` should then print a path). Then **quit
   it** from the menu bar icon: the script runs its own Ollama server.
3. **Install the voices** used for the test clips: System Settings >
   Accessibility > Spoken Content > System voice > Manage Voices. Add
   English (India) **Aman**, **Rishi** and **Tara**, Hindi **Lekha**, and
   English (US) **Samantha**. Check with `say -v '?' | grep -E 'Aman|Rishi|Tara|Lekha|Samantha'`.
4. **Get the repo** (it is private, so sign in first):
   ```sh
   brew install gh && gh auth login
   gh repo clone fossdot/English-Saathi ~/English-Saathi
   cd ~/English-Saathi
   ```
   Or copy the project folder over from this Mac on a USB drive.
5. **Check the machine**: `sysctl -n machdep.cpu.brand_string` should name
   the Intel i7/i9, and `uname -m` should print `x86_64`.
6. **Run it**, kept awake for the whole run:
   ```sh
   caffeinate -i tools/bench-intel/run_on_intel_mac.sh ~/bench-work 2>&1 | tee ~/bench-work.log
   ```
   The script stops at once if a voice, Python 3.12, cmake or Ollama is
   missing. It downloads the app's speech model itself
   (`scripts/fetch_deps.sh`). If it stops part way, run the same command
   again: downloaded models and built tools are reused.
7. **Send back** `~/bench-work/report-speech.md`, the folders
   `~/bench-work/results-speech` and `~/bench-work/results-llm`, and
   `~/bench-work.log`. Skip `~/bench-work/models` and `~/bench-work/ggml`
   (several GB of model files).

## How the measurements were made

- **Machine:** a MacBook Pro with an Apple M4 and 16 GB, running macOS 26.5.
  There is no Intel Mac here, so the Intel software ran through Rosetta 2:
  the x86_64 builds of Python, sherpa-onnx, whisper.cpp and Ollama, i.e. the
  same binaries an Intel MacBook would run.
- **What Rosetta does and doesn't tell you.** Rosetta advertises only
  SSE4.2 to programs, not AVX2 (`sysctl hw.optional.avx2_0` is 0 under
  Rosetta here), although it does translate AVX2 instructions when a program
  uses them anyway. So:
  - libraries that choose their kernels at run time ran their pre-AVX code
    here: SSE4.2 for onnxruntime inside sherpa-onnx, and only SSE3/SSSE3 for
    llama.cpp inside Ollama (its log says so). On a 2019 i7/i9 they would
    run AVX2 code, which is faster;
  - a whisper.cpp built with AVX2 ran its AVX2 code; it is measured both ways.
  - The M4's cores are faster than the i7's. The two effects pull in opposite
    directions, so **absolute times here are only a rough guide**. Ratios
    between models (tiny vs base vs small, 1B vs 4B) transfer much better.
  - `tools/bench-intel/run_on_intel_mac.sh` runs everything on a real Intel
    Mac and writes the same tables; that is the final word on speed.
- **Speech accuracy** does not depend on the machine. It was scored on the
  375 synthetic clips from `docs/asr-model-comparison.md` with the app's own
  matcher, both as milestone a scores and with the rules decided after
  milestone b (place names unscored; to/two/too, for/four, one/won equal).
- **LLMs** ran through Ollama with `num_gpu: 0`, temperature 0, one request
  at a time, with the app's speech model (Whisper tiny.en, quantized decoder)
  loaded in the measuring process the whole time and transcribing one clip
  after each model. Memory is the peak resident size of the Ollama server
  plus its runner, sampled every 0.25 s, with the speech process shown
  separately.
- **Hindi quality is not judged here.** All LLM outputs are in
  `tools/llm-bench/results/review.md` with a scoring sheet for a fluent Hindi
  speaker.
