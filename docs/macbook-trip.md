# MacBook trip: D1 speech check and D3 microphone test

One visit to a 2019 16-inch Intel MacBook Pro covers both milestones that
need the real machine (`docs/desktop-plan.md`). Allow about an hour, most
of it waiting for downloads and the D1 timings. Tick each box and note what
you saw; the notes and two log files are what to send back.

## A. Set up (once per MacBook, about 20 minutes)

- [ ] Plug the MacBook into power and close other apps.
- [ ] Check the machine: in Terminal, `uname -m` prints `x86_64` and
      `sysctl -n machdep.cpu.brand_string` names the Intel i7 or i9.
      macOS version (Apple menu › About This Mac): ________
- [ ] Install **Amazon Corretto 17** for macOS x64 (the `.pkg`) from
      https://aws.amazon.com/corretto/. Not Temurin: its Java can't use the
      microphone.
- [ ] If the speech benchmark ran on this MacBook, Xcode's tools and Python
      are already there. Otherwise: `xcode-select --install`, then Homebrew
      (https://brew.sh) and `brew install python@3.12`.
- [ ] Get the repo: `gh repo clone fossdot/English-Saathi ~/English-Saathi`,
      or `cd ~/English-Saathi && git pull`. Let the scripts download the
      large files on the MacBook itself; files copied over AirDrop are
      marked as downloaded, and macOS may refuse to load the speech library.
- [ ] Voices: System Settings › Accessibility › Read & Speak › System voice
      › ⓘ. Note which English (India) and Hindi voices are listed as
      installed: ________. Don't download any yet; the app should work with
      what a fresh Mac has.

## B. D1: speech check (about 10 minutes, unattended)

```sh
cd ~/English-Saathi
caffeinate -i scripts/desktop_speech_check.sh
```

- [ ] It ends with "Done. Send back: desktop/build/speech-check/x86_64.log".
      It needs internet the first time (about 1 GB). If it stops part way,
      run it again: downloads are kept.

## C. D3: microphone and the window (about 25 minutes)

Do this in the room where sessions happen, at a normal time of day.
`--check` opens the facilitator's speech check instead of the student
picker.

```sh
cd ~/English-Saathi
scripts/run_desktop.sh --check
```

Starting up:

- [ ] A macOS prompt asks: "“Terminal” would like to access the
      Microphone." Click **Allow**. (No prompt and the microphone stays
      silent: note it; check that Terminal is on in System Settings ›
      Privacy & Security › Microphone.)
- [ ] The window opens and draws cleanly: text sharp, Hindi joined up
      correctly, no flicker or black areas. Note anything odd: ________
- [ ] The grey box at the bottom ("For the facilitator") shows:
      microphone "working", voices English and Hindi (not "missing"), and,
      within a minute, "lesson speech ready".
- [ ] No red notice at the top. (A red notice about Java means it isn't
      running on Corretto: note what it says.)

Speaking, with the model the app starts on (tiny.en):

- [ ] For each of the 5 sentences (click it on the left): press **Space**,
      say the sentence normally, then stop talking.
  - [ ] Recording stops about a second after you finish (not at 8 s, not
        in the middle of the sentence). Times it went wrong: ____
  - [ ] The result appears; note the "result after … s" figures: ____
- [ ] Press Space and say nothing: after 8 s it says "Didn't hear you".
- [ ] Press Space, start speaking, and press Space again: it stops at once.
- [ ] Have someone talk nearby (as students waiting their turn would)
      while you say a sentence. Does recording still stop after you
      finish? ____

The other model:

- [ ] Press **⌘,** (Settings), choose **small.en**, close Settings, and
      wait until the mic button works again (the model takes a few seconds
      to load).
- [ ] Repeat the 5 sentences. Note the "result after … s" figures: ____
      Is that wait acceptable for a student? ____
- [ ] Quit the app (⌘Q) and run `scripts/run_desktop.sh --check` again.
      The top of the window says **Speech model: small.en**, and Settings
      still has the microphone chosen before.

Listening and keys:

- [ ] **L** plays the sentence; **S** plays it clearly slower, at the same
      pitch. Both start at once (no wait).
- [ ] **Hindi help** reads the Hindi instruction in a Hindi voice.
- [ ] **Enter** starts recording like Space.
- [ ] Start a recording and click on another app's window: recording
      stops.

When the microphone is blocked:

- [ ] Quit the app (⌘Q). In System Settings › Privacy & Security ›
      Microphone, turn **Terminal** off (macOS may ask to quit Terminal:
      do it, and open Terminal again).
- [ ] Run `scripts/run_desktop.sh --check` again. The app shows a red
      message that the microphone is blocked, naming Terminal and the
      setting.
- [ ] Turn Terminal back on in System Settings, quit and restart the app,
      and check that recording works again.

Test clips and battery:

- [ ] ⌘, › Test clips: run bus_01 to bus_05. Each gives a result (these
      are the recorded test sentences, without the microphone).
- [ ] Open Activity Monitor › Energy. In the "Requires High Perf GPU"
      column, java says **No** (the app asks for the Intel graphics to save
      battery). Value seen: ____

Optional, if you have one:

- [ ] A wired or USB headset, chosen under ⌘, › Microphone: recording works.
- [ ] AirPods or another Bluetooth headset: recording works, and note
      whether speech sounds worse while the microphone is in use.

## D. The student screens (about 5 minutes)

```sh
scripts/run_desktop.sh
```

- [ ] The window opens at **Who is practising?**, with the speech model at
      the top. Text is in the pilot's font (Mukta), Hindi joined up
      correctly.
- [ ] **New student**: choose a colour and a picture, type a name, press
      **Add student**. Home shows the name, "0 / 5", "0 days" and "0 XP".
- [ ] **Start practice**: the first sentence. Press Space, say it, stop
      talking: the result shows stars, the words as chips, what was heard
      and XP. If a word is marked, the tip names it as written.
- [ ] Press Space and say nothing: "Didn't hear you", and no stars.
- [ ] **Next sentence** (or **Skip for now**) moves on to the next
      sentence; the cross at the top left goes home, where the sentence
      now shows its stars and the goal counts it.
- [ ] Click the name at the top left: back at the picker, the new card is
      there with "1 day".
- [ ] In a second Terminal window, run `scripts/run_desktop.sh` again: a
      small window says English Saathi is already open. Press OK.
- [ ] ⌘, › Students › **Delete…** › **Delete …**: the student disappears
      from the picker.

## E. Send back

- [ ] `~/English-Saathi/desktop/build/speech-check/x86_64.log` (D1)
- [ ] `~/Library/Application Support/English Saathi/logs/speech-check.log`
      (D3: the machine, Java, permission, voices, graphics and every result
      with its model). It holds what the recognizer heard, not recordings.
- [ ] This checklist with your notes, and a screenshot of the window
      (⇧⌘4, then Space, then click the window).
