# Speech on the device (milestone b)

Recording and recognition run fully on the phone. This page has the
measurements from the emulators (2026-10-19) and what still needs a real
phone.

## What runs

- **Recording:** `AudioRecord`, 16 kHz mono 16-bit, from the `VOICE_RECOGNITION`
  source. Android requires that source to support this format with noise
  suppression and automatic gain off, so the recognizer gets unprocessed
  audio.
- **When recording stops:** about 1.2 s after the learner stops speaking, or
  at 8 s, or when Stop is tapped. Audio counts as speech when it lasts 60 ms
  and is at least 12 dB above the room's background level and above
  −55 dBFS. These levels are starting values, tested only on synthetic and
  emulator audio.
- **"Didn't hear you":** if nothing in the recording counted as speech, the
  recognizer is not run, so Whisper can't invent words from silence. If it
  runs and returns only tags like `[BLANK_AUDIO]`, the result is also
  "Didn't hear you".
- **A blocked microphone is told apart from silence.** A real microphone
  always picks up some noise. So if a second or more of recording is exactly
  zero, the app says the microphone sends no sound: it may be switched off in
  the phone's privacy settings, or taken by a call.
- **Leaving the app stops listening:** pressing Home, a call or the screen
  turning off ends the recording. Turning the phone doesn't.
- **Failures are shown, not crashes:**
  - a microphone that won't open or stops delivering audio;
  - a model or speech library that won't load;
  - anything unexpected while checking.
- **Recognition:** Whisper tiny.en with the fully quantized decoder
  (`docs/asr-model-comparison.md`). It is loaded once per app process, on its
  own thread, from the app's assets, and every recognition runs on that
  thread. Settings: 2 threads, tail padding 1000, greedy decoding, no
  prompt. The target sentence is never given to the model.
- **App size:**
  - The model's 44.3 MB of files is compressed to 27.9 MB inside the APK.
  - The speech library adds 24.2 MB for 64-bit ARM and 16.4 MB for 32-bit
    ARM. It is stored uncompressed, so Android can load it without unpacking
    it.
  - The release APK is 75.9 MB.

## Emulator results

Two emulators, each with 3 GB RAM and 4 cores, running on a MacBook Pro M4.
The emulated phone uses the Mac's own processor, so **these times are far
faster than a cheap phone**. They show that the pipeline works and how the
settings compare, not what learners will see.

| | Android 15 (API 35) | Android 8.0 (API 26, the minimum) |
|---|---|---|
| LibriSpeech test clip (6.6 s) | transcribed word for word | transcribed word for word |
| 3 s clip, end of recording → scored result (median of 5) | 83–91 ms | 116–131 ms |
| 6.6 s clip, recognition only | 148–175 ms | 209–229 ms |
| 3 s clip with 1 / 2 / 4 threads | 120 / 80–82 / 77–81 ms | 188 / 116–128 / 88–120 ms |
| Model load | 0.3–0.4 s; 0.7–0.9 s at app start | 0.3 s |
| Process memory: before load → after load → peak after recognition | 157–175 → 270–287 → 430–447 MB | 105 → 238 → 372–374 MB |
| Microphone | opens; silence stops at 8 s, "Didn't hear you" | can't open: this emulator image has no audio input; the app shows an error, no crash |

- Ranges cover two or three runs. Memory comes from runs of the memory
  test on its own; in a full test run it logs "NOT ISOLATED".
- **Speed target:** the spec asks for under 3 s for a 3 s clip, from the end
  of the recording to the result. The emulator needs 83–131 ms of that.
- **Memory:** the model adds about 270 MB at its peak; the 430 MB and 372 MB
  above are whole-process figures.
- **Lesson sentences** (the macOS "Rishi" voice, `scripts/make_test_wavs.sh`):
  - Android 15 transcribed 4 of 5 exactly. "Bettiah" came out as "beta".
  - Android 8.0 transcribed 3 of 5. "One ticket to Patna, please" came out as
    "One tiktut-Tupatna, please".
  - The model, the library and the audio are the same on both, so a borderline
    clip can give different words on different phones.
- **Through the app's own screen** on Android 15:
  - the recorder stopped a test clip 1.2 s after the speech ended (2.4 s of
    audio);
  - the scored result appeared 259 ms later, which includes the first
    recognition's warm-up.

## What to test on a real phone

Use the debug build. Its "Speech check" screen shows, after every answer:
- how long the result took after the recording ended;
- the memory used;
- why the recording ended.

The same numbers are logged with the tag `EnglishBolo` (`adb logcat -s EnglishBolo`).

1. **Phones:** a cheap 2–3 GB phone with a 64-bit ARM processor, and one
   with a 32-bit (armeabi-v7a) processor. No emulator exists for 32-bit
   Android 8 or later, so the 32-bit build is untested.
2. **Speed:** say each lesson sentence a few times.
   - Is "Result ready" under 3,000 ms?
   - Does it get slower after 20 answers in a row, as the phone warms up?
3. **When recording stops:**
   - In a quiet room, does it stop about a second after you finish?
   - With a fan, a TV or children nearby, does it stop at all before 8 s?
   - Does a pause between words cut you off?
4. **Quiet voices:** speak softly, or hold the phone at arm's length. Does it
   say "Didn't hear you" when you did speak?
5. **Memory:** is the app killed in the background? What "peak" figure does
   the screen show?
6. **Start-up:** how long does "Loading the speech model…" stay on screen
   the first time?

If the phone is too slow, the first setting to try is tail padding 300.
`docs/asr-model-comparison.md` measured it as faster with no significant
loss of accuracy.
