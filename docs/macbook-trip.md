# MacBook trip: the packaged app on a Hikmat MacBook

One visit to one of Hikmat Foundation's 2019 16-inch Intel MacBook Pros.
It checks the app exactly as staff will get it (`docs/packaging.md`): the
`.dmg`, the setup guide, Open Anyway, the microphone, the voices, then the
speed of small.en (the default speech model) and the student screens. No
Terminal is needed except in part F, which is optional. Allow about an hour.
Tick each box and note what you saw; the notes, the log and the CSV are what
to send back.

Take: `English-Saathi-<version>.dmg` (or its Google Drive link) and the
setup guide (`docs/setup-guide.pdf`, also on the `.dmg`).

## A. The machine (5 minutes)

- [ ] Plug the MacBook into power and close other apps.
- [ ] Apple menu › About This Mac: macOS version ________, processor Intel
      i7 or i9 ________, memory 16 GB. (Every Hikmat account is an
      administrator, `docs/decisions.md`.) Needs macOS 13 to 26 for the
      guide's steps; note any of the 20 Macs on an older version: ________
- [ ] Voices: System Settings › Accessibility › Read & Speak (Spoken Content
      on older macOS) › System voice › ⓘ or Manage Voices. Note which English
      (India) and Hindi voices are already installed: ________. Don't
      download any yet.

## B. Install, following the setup guide (10 minutes)

Do exactly what the guide says, as a staff member would, and note anything
in the guide that didn't match the screen: ________

- [ ] Download the `.dmg` from the Google Drive link (in Safari or Chrome),
      open it, drag English Saathi onto Applications, eject the disk.
- [ ] Open the app from Applications. macOS refuses it the first time. Exact
      words of the message and its buttons: ________. (It must **not** say
      the app is "damaged".)
- [ ] System Settings › Privacy & Security › Open Anyway, then Open Anyway
      in the message that follows, then an administrator's Touch ID or Use
      Password… (macOS 13 or 14: Open Anyway, then Open): the app opens.
      Steps or words that differed from the guide: ________
- [ ] macOS asks for the microphone, and the prompt names **English Saathi**
      (not Terminal), with the text "English Saathi listens while a student
      says a sentence…". Click **Allow** (**OK** on macOS 13).
- [ ] The window opens at **Who is practising?**, text sharp, Hindi joined up
      correctly. The model at the top says **small.en**. No red notice about
      the microphone; one may show while macOS is asking, and it must go
      within a few seconds of Allow. What you saw: ________. A red notice
      about missing voices may appear: note it ________ (newer macOS has
      basic Rishi and Lekha voices built in).
- [ ] Quit (⌘Q) and open the app again from Applications: it opens at once,
      with no Open Anyway and no microphone prompt. With a stopwatch:
      seconds from double-click to **Who is practising?** ____; then pick a
      student and **Start practice**: seconds until the first sentence can
      be heard and recorded ____.

## C. Voices (5 minutes, needs internet)

- [ ] Follow step 4 of the guide: download **Rishi (Enhanced)** (English
      (India)) and **Lekha (Enhanced)** (Hindi). Quit and reopen the app: no
      voice notice, and Settings (⌘,) › Voices names Rishi (Enhanced) and
      Lekha (Enhanced) (the app takes the best voice installed, so a Premium
      voice already on the Mac would come first). Time the downloads took:
      ________

## D. Speech: is small.en fast enough? (15 minutes)

In the room where sessions happen, at a normal time of day.

- [ ] Settings (⌘,) › **Open the speech check**. For each of the 5 sentences
      (click it on the left): press **Space**, say it normally, stop talking.
  - [ ] Recording stops about a second after you finish (not at 8 s, not in
        the middle). Times it went wrong: ____
  - [ ] Note the "result after … s" figures: ____ Is that wait acceptable
        for a student? ____ (If not, the owner's rule applies: tiny.en.)
- [ ] Press Space and say nothing: after 8 s it says "Didn't hear you".
- [ ] Have someone talk nearby while you say a sentence. Does recording
      still stop after you finish? ____
- [ ] Settings › Microphone shows the MacBook's own microphone first, marked
      "(built-in)", and ticked.
- [ ] Settings › **tiny.en**, then the 5 sentences again: "result after"
      figures ____. Then back to **small.en**.
- [ ] Settings › Test clips › librispeech-0: a result appears.
- [ ] Activity Monitor › Energy: in "Requires High Perf GPU", java or English
      Saathi says **No**. Value seen: ____

## E. The student screens (10 minutes)

- [ ] Back to the picker (the arrow at the top left). **New student**:
      choose a colour and a picture, type a name, **Add student**. Home shows
      the name, "0 / 5", "0 days" and "0 XP".
- [ ] **Start practice**: press Space, say the sentence, stop talking: stars,
      the words as chips, what was heard, XP.
- [ ] Say a sentence with one word wrong on purpose, then **Fix**: steps 1 to
      5; saying the word wrong twice goes back one step.
- [ ] Say all five sentences: the goal screen appears once.
- [ ] Results › **Save as CSV…**: open the file in Numbers or Excel; the
      times are this Mac's.
- [ ] Settings › Add lessons…: choose `content/packs/starter.json` from a
      copy of the repo if one is at hand: it is refused as "already
      installed", and nothing changes. (Skip if no pack file is at hand.)
- [ ] Settings › Students › **Delete…** › **Delete …**: the student
      disappears.

## F. Optional, for whoever looks after the Macs (Terminal)

```sh
"/Applications/English Saathi.app/Contents/MacOS/English Saathi" --self-test
```

- [ ] It ends with "all checks passed". Paste its output: ________
- [ ] Which app macOS asked about (after part B's Allow, within a few
      minutes): `/usr/bin/log show --last 10m --info --style compact
      --predicate 'subsystem == "com.apple.TCC" AND eventMessage CONTAINS
      "AUTHREQ_PROMPTING"'` shows `subject=Sub:{net.bodhya.speak}`: ____
- [ ] A refused microphone: run `tccutil reset Microphone net.bodhya.speak`,
      open the app and click **Don't Allow**. Practise a sentence: the app
      says the microphone is blocked. Follow its steps (turn on English
      Saathi, then Quit & Reopen, or quit and open it again): recording
      works again. What happened: ________
- [ ] The earlier D1 timing script still works from a copy of the repo:
      `caffeinate -i scripts/desktop_speech_check.sh` (needs Corretto 17 and
      internet the first time; `docs/desktop-speech.md`).

## G. Send back

- [ ] `~/Library/Application Support/English Saathi/logs/speech-check.log`
      (Settings › Open the data folder › logs): the machine, the voices and
      every result with its model. It holds what was heard, not recordings.
- [ ] The CSV saved in part E.
- [ ] This checklist with your notes, and a screenshot of the window
      (⇧⌘4, then Space, then click the window).
