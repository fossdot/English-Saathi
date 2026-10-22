# Packaging English Saathi for the MacBooks

Students get English Saathi as a normal Mac app from a download link
(`docs/decisions.md`, "Distribution, defaults and lessons: owner answers"):
no Terminal, Gradle, Java or internet once it is downloaded. The staff guide
is `docs/setup-guide.pdf` (source: `docs/setup-guide.html`); it is also on
the `.dmg`.

## Building the .dmg

On any Mac with the repo (an Apple silicon Mac needs Rosetta):

```sh
scripts/package_mac.sh
```

The result is `desktop/build/packaging/English-Saathi-<version>.dmg`. The
first run needs internet for Gradle, the libraries and models
(`scripts/fetch_deps.sh`) and Amazon Corretto 17 for Intel (checked against
Amazon's published SHA-256). The version is `appVersion` in
`desktop/build.gradle.kts`; raise it for every copy sent out.

What the script does:

1. Downloads Corretto 17.0.20.12.1 for Intel into `desktop/build/packaging/`.
2. Runs `scripts/fetch_deps.sh`.
3. Runs Gradle on that Intel JDK (under Rosetta on Apple silicon) with
   `-Pdistribution`, so Compose builds for Intel: the Intel graphics library,
   the Intel speech library, and a Corretto runtime made with jlink.
4. Takes the Apple silicon graphics library out of Skiko's Intel jar (the
   app never loads it), checks that every program and library in the app,
   also inside jars, is for Intel, and that every file in it can be read by
   everyone (otherwise macOS would call the app "damaged" for some
   accounts).
5. Signs the app ad hoc (below), and checks that no program or library kept
   the hardened runtime.
6. Runs the app's self-test (below), then checks the signature is still
   intact: the app never writes inside itself.
7. Builds the `.dmg` (LZMA-compressed) with the app, a link to Applications
   and the setup guide, verifies it, and checks the app on it.
8. Downloads Corretto's source code, `corretto-17-<version>.tar.gz`, next to
   the `.dmg` (below, "Licences").

## What is inside

`English Saathi.app/Contents/`:

- `MacOS/English Saathi`: the launcher.
- `runtime/`: Corretto 17's runtime, with only the modules the app needs and
  its own licences in `runtime/Contents/Home/legal/`.
- `app/`: the app's code, and in `app/resources/`:
  - `content/packs/starter.json`: the first lesson pack;
  - `models/whisper-tiny.en/`, `models/whisper-small.en/`: both speech models;
  - `native/osx-x64/`: the speech library (sherpa-onnx, built without
    text-to-speech, and ONNX Runtime 1.28.2);
  - `clips/librispeech-0.wav`: the speech check's test clip (CC BY 4.0);
  - `THIRD-PARTY-NOTICES.txt`: the licences of everything above, with ONNX
    Runtime's own `onnxruntime-LICENSE.txt` and
    `onnxruntime-ThirdPartyNotices.txt`.
- `Info.plist`: bundle ID `net.bodhya.speak` (never change it: macOS keeps
  the microphone permission under it), version, macOS 11 or later (Corretto
  17.0.20's Intel programs need it; the staff guide's steps are for macOS 13
  to 26), and the microphone text macOS shows in its prompt.

The bus-stand test clips are not inside: they are made with the macOS voices,
whose licence doesn't allow sharing recordings of them.

Sonic, Mukta and the app's icons are part of the app's code. The icon
(`desktop/packaging/English Saathi.icns`, from `icon-1024.png`) is
provisional: the app's mic button on the pilot's blue, until the designs
arrive.

## Data and updates

Everything a learner does is in the data folder,
`~/Library/Application Support/English Saathi`: learners, progress, the
attempt log, recordings, added lesson packs (`lessons/`), settings and logs.
The app never writes inside `English Saathi.app`. An update is a new `.dmg`:
drag the new app onto Applications and replace the old one; everything in the
data folder stays. `AppUpdateTest` checks this.

## Signing

The app is **not notarized** and has **no Apple Developer ID**. It is signed
ad hoc (`codesign --sign -`), which needs no Apple account:

- **The microphone works without a paid signature.** What it needs is the
  `NSMicrophoneUsageDescription` text in `Info.plist` (without it macOS
  refuses the microphone) and a signature that doesn't forbid recording.
- **Compose doesn't leave a usable signature.** With no Developer ID,
  Compose 1.10.3 removes the signatures jpackage made from the runtime and
  the app, and signs them again only when Gradle runs on Apple silicon, and
  then with the hardened runtime and no
  `com.apple.security.device.audio-input` entitlement: such an app records
  only silence (as with Temurin's `java`, `docs/desktop-plan.md`). Built on
  the Intel JDK, as here, it leaves them unsigned, and macOS treats an
  unsigned or half-signed app as "damaged" or won't open it at all. So the
  script re-signs every program and library ad hoc, **without** the
  hardened runtime; no entitlements are then needed.
- **"Damaged" means a broken signature**: a file changed after signing, or
  files some accounts can't read. Open Anyway can't get past it; the `.dmg`
  has to be built again. The script checks both before building the `.dmg`.
- **Because it isn't notarized**, macOS refuses to open it the first time; a
  staff member allows it once per new version in System Settings › Privacy &
  Security › Open Anyway (the setup guide shows how).
- **An ad hoc signature changes with every build**, so after an update macOS
  asks again for the microphone, and Open Anyway is needed again.
- **To remove those steps later:** an Apple Developer ID (Apple Developer
  Program), signing with the hardened runtime, and notarization. The
  hardened runtime then needs these entitlements for the JVM and the speech
  library: `com.apple.security.device.audio-input`,
  `com.apple.security.cs.allow-jit`,
  `com.apple.security.cs.allow-unsigned-executable-memory` and
  `com.apple.security.cs.disable-library-validation`.

## The self-test

`English Saathi.app/Contents/MacOS/English Saathi --self-test` checks, without
a window, the microphone or the data folder:

- the app's signature lets it record;
- `Info.plist` has the microphone text;
- the licence notices are there, the Java runtime's included;
- the lessons load;
- the speech library loads, and both models hear the LibriSpeech clip word
  for word on this Mac's processor;
- the window draws, with the Mukta font;
- which voices it would use (missing voices are a setup step, not a failure).

It is the quickest check on a new MacBook too (in Terminal, for whoever
looks after the Macs; staff never need it).

## Licences

`THIRD-PARTY-NOTICES.txt` lists everything inside the app with its licence.
Two choices keep the app free of copyleft obligations beyond the Java
runtime's (`docs/decisions.md`, "Licences: owner answers"):

- The speech library is sherpa-onnx rebuilt without text-to-speech
  (`scripts/build_sherpa_onnx.sh`): the prebuilt one has eSpeak NG
  (GPL-3.0) inside.
- Corretto (GPLv2 with the Classpath Exception) needs its source available
  from the same place as the app. `scripts/package_mac.sh` downloads
  `corretto-17-<version>.tar.gz` (the source of the Corretto release whose
  runtime is inside the app, from github.com/corretto/corretto-17) next to
  the `.dmg`. **Put both files in the Google Drive folder** and share the
  folder; staff only download the `.dmg`.

## Checking which app macOS asks about

The prompt must name English Saathi, not Terminal. When the app is opened
from Finder (or with `open`), macOS treats the app as responsible for its own
microphone use, and keeps the answer under its bundle ID. To see it in
macOS's log, answer the prompt, then within a few minutes run:

```sh
/usr/bin/log show --last 10m --info --style compact --predicate 'subsystem == "com.apple.TCC" AND eventMessage CONTAINS "AUTHREQ_PROMPTING"'
```

It should show `service=kTCCServiceMicrophone, subject=Sub:{net.bodhya.speak}`.
(`--info` is needed: these are info messages. `/usr/bin/log`, because in a
zsh script plain `log` is zsh's own command.) The app's log
(`logs/speech-check.log` in the data folder) then has
`microphone check: Working`.

Checked on 2026-10-22 with the packaged app on macOS 26.5.1 (Apple silicon,
the Intel app under Rosetta): the prompt named English Saathi, macOS logged
it for `net.bodhya.speak` and recorded Allow two seconds later, and the
start-up microphone check, which had waited for the answer, said Working.
Quit and opened again, the app recorded at once, with no prompt.

(Run from Terminal with `scripts/run_desktop.sh`, the developer build is
Terminal's responsibility, which is why its messages say "Terminal".)
