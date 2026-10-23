# Campus server and progress dashboard: plan

**Status: plan only, for the owner's OK. Nothing is built.** Written
2026-10-23 from the owner's brief of the same day, then revised after three
independent reviews. Facts from outside the repo cite their source at the
end (§14). Anything not yet checked on a real Hikmat MacBook or the Airtel
router is marked **untested**. Where the plan needs a decision, it
recommends one and lists it in §12.

This replaces the dashboard ideas in `docs/assessment-design.md` §11 for the
practice app only. The assessment stays paused; if it resumes, its results
can travel on the same connection.

Contents: 1 [Summary](#1-summary) · 2 [What staff do](#2-what-staff-do) ·
3 [How it works](#3-how-it-works) · 4 [Joining and security](#4-joining-and-security) ·
5 [Sending progress](#5-sending-progress) · 6 [The server Mac](#6-the-server-mac) ·
7 [Fixing problems](#7-fixing-problems) · 8 [macOS questions and signing](#8-macos-questions-and-signing) ·
9 [Resource use](#9-resource-use) · 10 [Staff guide](#10-staff-guide) ·
11 [Milestones](#11-milestones-smallest-first) · 12 [Questions for the owner](#12-questions-for-the-owner) ·
13 [Risks](#13-risks) · 14 [Sources](#14-sources) · A [For the developer](#appendix-a-for-the-developer)

---

## 1. Summary

- **One of the 20 MacBooks becomes the server.** It runs the same English
  Saathi app with **server mode** turned on behind a PIN. In server mode the
  app opens the dashboard instead of the student picker. It never loads the
  speech models, the voices or the microphone, and the Mac is not used for
  practice. Any students already on that Mac are copied into the dashboard
  first (§2.1).
- **Student Macs find the server by themselves** on the campus Wi-Fi with
  Bonjour (Apple's own "find devices nearby" system). They find it again on
  their own after a router restart gives it a new address. Typing an address
  is a hidden last resort.
- **Joining takes one approval.** The student Mac shows a 6-digit number.
  The server shows "Mac 07 wants to join · 482 913 · Approve". Staff check
  that the numbers match and press Approve. It works whichever Mac they go
  to first. Everything sent after that is encrypted, with no further steps.
- **Practice never waits.** Each student Mac keeps everything on its own
  disk, as today. It sends what's new every 60 minutes (30–120, set once on
  the server), and once more when the app closes, at a slightly random moment
  so the Macs don't all connect at once. If the server can't be reached, the
  Mac just tries again later: through Wi-Fi drops, router restarts and power
  cuts. Every record has its own ID, so sending it twice never counts it
  twice. A Mac stops resending a record only once the server has confirmed
  it was saved. Sync never deletes anything on the student Mac.
- **Text only.** Students, their progress and their attempts (what was said,
  what was heard, stars) travel. Voice recordings never leave the Mac.
- **The server** stores what arrives and does nothing else in the
  background. Its pages are worked out when someone opens them:
  - one page per student, and a cohort page;
  - a **Macs** page, green or red, with the reason;
  - a CSV export;
  - a warning when a Mac falls behind;
  - a daily backup to an external drive, with restore.
- **When something is red**, the student Mac's **Connection** screen says in
  plain English and Hindi what is wrong and what to do. It has one-button
  fixes: Find server again, Connect again, Send now. Both sides have **Save
  problem report**. It shows a short summary on screen to photograph for
  WhatsApp, and saves a small file. Neither has names or recordings.
- **"Export progress" to a USB stick** stays as the fallback when there's no
  Wi-Fi. The server imports the file with the same no-duplicates rule.
- **New parts:**
  - one new library, **JNA** (Apache-2.0), so the app can use macOS's own
    Bonjour and keep the server Mac awake;
  - two more parts of the Java runtime the app already ships (about 1.4 MB).
  - No database, no cloud, no internet.
- **The biggest risk is macOS's permission system, not our code.** From
  macOS 15, each Mac must allow English Saathi to "find devices on local
  networks". Apple warns that apps signed the way ours is can have that
  answer forgotten, or asked for again after updates. Ours is signed by us,
  without an Apple certificate. So the first milestone is a one-day test on
  real Hikmat Macs and the real router, before any screens are built (§11,
  S0). An Apple Developer ID would remove most of this risk (§12, Q1). That
  would reverse the owner's earlier "no notarization".
- **This changes two earlier decisions** (§12, Q2 and Q14):
  - "All data stays on that Mac": names and practice text are copied to the
    server Mac on campus.
  - The staff guide: it becomes English and Hindi, several pages, as the
    brief asks. The existing one-page setup guide stays English only.

---

## 2. What staff do

### 2.0 Once, before anything: number the Macs

Put a sticker with a number, 01 to 20, on every MacBook. The number is how
staff and the dashboard tell the Macs apart.

### 2.1 Once: make a Mac the server (about 15 minutes)

1. On the chosen Mac, open English Saathi › Settings › **Make this Mac the
   server**. If another server is already on the Wi-Fi, the app says so and
   stops: "Mac 01 is already the server. Only one Mac can be the server."
   It also offers "This Mac replaces it", for moving the server.
2. If this Mac has students, the app says: "This Mac has 6 students. Their
   progress will be copied to the dashboard. From now on they practise on
   another Mac." Staff press **Copy and continue** (§12, Q11).
3. Choose a 6-digit PIN. The app shows a **recovery code** to write down and
   keep in the office.
4. Type the Wi-Fi's name (for example HIKMAT-WIFI) exactly as staff see it.
   Apps can't read it themselves on these macOS versions (§7.2). The name
   appears in every message.
5. macOS may ask up to three times. Click **Allow** or **OK** each time:
   - "find devices on local networks" (macOS 15 and later);
   - "accept incoming network connections" (only if the Mac's firewall is
     on);
   - later, "files on a removable volume", when the backup drive is first
     used.
6. A checklist turns green as each item is done:
   - plugged in, lid open;
   - staying awake;
   - **can be found on the Wi-Fi**;
   - **firewall lets the student Macs in**;
   - starts by itself after a restart (one button, if S0 shows it works;
     otherwise a button opens the Login Items page of System Settings);
   - backup drive plugged in.

### 2.2 Once per student Mac: connect it (about 3 minutes)

The order doesn't matter: either Mac can be done first.

1. On the student Mac: Settings › **Connect to the server**. Type the Mac's
   sticker number.
2. If macOS asks to find devices on local networks, click **Allow**.
3. The student Mac shows "Waiting for the server Mac. On the server Mac, open
   **Macs** and press Approve" and a 6-digit number. It keeps asking for 10
   minutes.
4. On the server Mac, open **Macs** (PIN). A card shows "Mac 07 wants to
   join" and the same number. If the numbers match, press **Approve**.
5. The student Mac turns green and sends everything it holds, even practice
   from before it was connected.
6. Settings › Students: type each student's roll number, if there is one
   (§3.3).

If a second Mac asks to join with a number already in use, the card says:
"Mac 07 is already connected. Is this a new Mac 07 replacing the old one?
**Replace** / **Cancel**". Cancel means: give it another number.

### 2.3 Day to day

Nothing. Students practise as today. The server Mac stays on, plugged in,
lid open. Someone looks at the **Macs** page now and then; a red card says
what to do.

---

## 3. How it works

### 3.1 Finding the server

| Step | When | How |
|---|---|---|
| 1. Bonjour | Always, first | The server announces itself on the Wi-Fi, with the Wi-Fi name staff typed. Student Macs listen, and macOS notices a new address after a router restart by itself |
| 2. Last address | When Bonjour finds nothing | Try the address that worked last time; usually the router gives the same one back |
| 3. Look around | Automatically, at most once per failed send, and on **Find server again** | Ask each address on this Wi-Fi (at most 254) on English Saathi's own port, in a few seconds. Only on the network the server was last seen on; never on a hotspot or another Wi-Fi. Only the real server can answer correctly (§4), so no other device is ever trusted |
| 4. Type an address | Last resort, behind **More…** | The server's Macs page shows its address for this |

Bonjour starts only on Macs that are already connected, and only once the
student picker is showing. Each connected student Mac also announces a tiny
"student Mac" record with no names. That tells "the server is off" (other
student Macs are visible) apart from "this Wi-Fi keeps devices apart"
(nothing is visible) (§7.2).

### 3.2 What runs where

```
Student Mac (practice, as today)                 Server Mac (server mode, no practice)
--------------------------------                 -------------------------------------
practice saves to disk as today; nothing         announces itself (Bonjour)
  is ever deleted because of sending             listens on one port, encrypted
every 60 min ± a random delay, and on quit:      checks the Mac's key, stores new records,
  find server, send records not yet confirmed ─► replies with the IDs it saved,
  mark those IDs as confirmed  ◄───────────────  its settings, its Wi-Fi details
Connection screen: green/red, why, fixes         Dashboard, Macs page, CSV, backups, warnings
Export progress → USB (fallback)  ─────USB────►  Import file (same rules)
```

### 3.3 Recognising the same student on several Macs

Today each Mac has its own students and cards; a card on Mac 03 means
nothing on Mac 07. Recommendation (§12, Q3):

- **Each student practises on one Mac.** Progress is not copied back to
  other Macs. That would need two-way sync and conflict rules, with much
  more to go wrong. The cost: staff make sure each student always sits at
  the same Mac. A student who uses another Mac gets a second profile there.
  It isn't known yet whether students sit at fixed Macs today (§12, Q3).
- **The server recognises a student by roll number.** Staff type it once
  per student, as an optional field in New student and in Settings ›
  Students. Two records with the same roll number and the same name are one
  student on the dashboard. The owner already chose Hikmat roll numbers for
  the paused assessment (Q33), so it is the same number.
- **The server never merges records silently** when one roll number arrives
  with two names. It asks: "Roll 1234 is used for Aman (Mac 03) and Sana
  (Mac 07). Same student? **Yes** / **No, fix the number**."
- **Without a roll number**, the dashboard shows "Name (Mac 07)". It offers
  "same student?" when one name appears on two Macs. Staff confirm with one
  tap, and can undo it.
- **When one student's records come from two Macs**, attempts, XP and stars
  are added up. Words to revise and open lessons come from the Mac used most
  recently, labelled with its number.

---

## 4. Joining and security

**Recommendation: Approve, with the same number shown on both screens.**

| | Short code typed on the student Mac | "Approve" only | **Approve + matching number (recommended)** |
|---|---|---|---|
| Staff steps | Read a code on the server, type it on the student Mac | Press Approve | Glance that both numbers match, press Approve |
| Stops an unknown Mac joining | Yes | Yes | Yes |
| Stops a fake "server" on the Wi-Fi at the first connection | No: it can learn or guess the code | No | **Yes**, unless both numbers match by chance (1 in a million) |
| If staff don't compare the numbers | n/a | n/a | Still as safe as "Approve only" |

- **Join requests wait.** A request that arrives while the server is
  locked is held for 10 minutes and shown when the Macs page is next
  unlocked. The student Mac keeps asking meanwhile, so the order doesn't
  matter.
- On approval, the student Mac receives its own secret key; the server keeps
  only a fingerprint of it.
- **Remove** (this Mac is gone for good) cancels the key in one step.
- **Reconnect** (the same Mac needs to join again) is also one step on the
  server. The server cancels the old key and lets that same Mac rejoin on its
  own within 30 minutes, with no approval and no one at the student Mac. The
  Mac is recognised by a hidden ID it made when it was first connected. Its
  practice is never lost: it is still on its disk.
- **All traffic is encrypted** with TLS 1.3, the standard websites use. The
  server's identity is fixed at joining. Nothing depends on names or
  addresses, so router restarts don't matter. There are no certificate dates
  to expire on Macs whose clocks drift without internet.
- **The PIN** (6 digits) guards server mode, the dashboard and the Macs
  page. It doesn't encrypt the files on the server's disk: every Hikmat user
  is an administrator and could open them directly.
  - **Forgot the PIN?** on the PIN screen: type the recovery code, then
    choose a new PIN.
  - If the code is lost too, **Reset PIN** asks for this Mac's own login
    password, keeps all data, and notes the reset on the dashboard.
- **Backups include the server's identity**, so a server restored onto the
  same or a replacement Mac carries on without reconnecting the student
  Macs. Macs connected after that backup was made must connect again; the
  restore screen lists them. The cost: whoever has the backup drive could
  pretend to be the server. Keep the drive in the office (§12, Q8).

---

## 5. Sending progress

### 5.1 What is sent

| Record | Repeat-safe because |
|---|---|
| **Mac hello**, every send: Mac number, app version, lesson packs, the Mac's own clock time, records not yet confirmed | Replaces the previous one |
| **Student**: added, renamed, roll number set | Has its own revision; the server keeps the newest |
| **Attempt**: every row of the attempt log, as written today | Has its own ID; the same ID twice is stored once |
| **Progress snapshot**: words due for revision, open lessons, "Open all lessons", current lesson | Read from the student's files at each send when they changed; latest wins |
| **Student deleted** | The server keeps a permanent "deleted" note and refuses anything later about that student, even from an old Export file or after a restore |

Never sent: recordings, the speech-check clips, settings, logs. Attempts
include what the speech recogniser heard, as text; the owner confirms that
is fine (§12, Q2).

**Practice from before connecting** is sent too, the first time. Existing
attempt rows get an ID made from their own content, so sending them again
can never duplicate them.

### 5.2 Timing

- Every **60 minutes** while the app is open, adjustable from 30 to 120 on the
  server. It is one setting for all Macs, sent to them with each reply.
- **The first send after the app opens** waits a random 0–60 minutes. Later
  sends move by up to 10% either way.
- **On closing the app**: one quick send, at most a few seconds. If it can't
  finish, the next send covers it. Nothing waits for it.
- **After a failed send**: try again as soon as Bonjour sees the server come
  back, after a random 0–5 minutes (so 19 Macs don't all arrive together
  after a power cut). Otherwise, wait a little longer each time, never more
  than the normal interval.
- **Send now** on the Connection screen sends at once.
- A send is a few kilobytes. 20 Macs a few times an hour is nothing for the
  Wi-Fi or the server.

### 5.3 Nothing lost, nothing doubled

- **Sync never deletes or changes practice data** on the student Mac. The
  attempt log, students and progress files stay exactly as today. The
  Results screen and CSV keep working.
- The student Mac keeps a small list of which records the server has
  confirmed. At start-up and before each send it checks its own files for
  anything not yet confirmed. So a crash at any moment can't leave a record
  unsent.
- The server confirms **the exact IDs** it saved, and only after its own copy
  is safely on disk.
- **The same record twice is stored once.** Two records with the same ID
  but different content (a copied data folder, a hand edit) are both kept
  and flagged, never overwritten.
- **Mac clocks can be wrong** without internet. The server keeps each Mac's
  records in the order the Mac made them, and stores when they arrived. The
  Macs page flags a Mac whose clock is far from the server's.
- **Deletions are remembered for good** on both sides. The student Mac keeps
  a small, permanent list of students it deleted. It resends that list
  whenever it sees that the server has lost data, for example after a
  restore. So a deleted student can never come back on the dashboard
  (§12, Q12).

### 5.4 Export and import (no Wi-Fi)

Settings › **Export progress** writes everything the Mac holds that the
server hasn't confirmed to one file on a USB stick (everything, if the Mac
was never connected). On the server, Macs › **Import file** applies the same
rules, so importing a file twice, or after the same data came over Wi-Fi,
changes nothing.

---

## 6. The server Mac

### 6.1 Storage

Plain files in the server's own folder, one folder per student per Mac.
Records are added, never rewritten. Each student also has their latest
progress snapshot. Deleting a student removes their folder and keeps the
"deleted" note. There is no database: fewer parts, and backups are plain
copies. A year for 150 students is likely tens to a few hundred megabytes.
The dashboard reads the files when opened. The existing Results code handles
100,000 attempts in under a second on an M4, so seconds at most on the Intel
Mac (**to measure**).

### 6.2 Dashboard

- **Students:** every student, with Mac, roll number, last practice,
  sentences said, stars, XP, days practised and current lesson. Filter by Mac
  or class.
- **One student:** the same over time, plus lessons opened, words due for
  revision and recent attempts.
- **Cohort:** students practising per day and week, average sentences and
  stars, how far through the lessons.
- **Macs:** each Mac **green or red**, with the reason in plain words. It
  also shows last seen, records waiting, app version and any clock warning.
  **Remove** (gone for good) and **Reconnect** (same Mac, join again) are
  one step each.
- **Warnings** (thresholds in §12, Q6):
  - **A Mac that has fallen behind:** other Macs have sent today but this one
    hasn't sent for 2 such days. Weekends and holidays, when no Mac sends,
    don't count, so there is no false alarm every Monday.
  - **A Mac whose last send failed.**
  - **No backup for 3 days.** The drive is meant to stay plugged in.
  - **The server's own checks** (§7.3).
- **Export CSV:** the same columns as today's Results CSV, with Mac, roll
  number and student ID added at the end.

### 6.3 Backups

- To an external USB drive that **stays plugged in**. The drive is
  recognised by a small "English Saathi backups" folder, whatever its name.
- **When:** daily, on **Back up now**, and also each time a Mac is approved
  or removed.
- **How a backup is written:**
  1. Write under a temporary name.
  2. Force it all the way onto the drive (USB drives often say "done" too
     early).
  3. Read it back and check it.
  4. Only then rename it.
  So a drive pulled out mid-way never damages the previous backup.
- **Kept:** the last 7 daily backups and one a month (how long monthly
  ones stay: §12, Q12).
- A drive the Mac can't write to (NTFS) gets a plain message.
- **Restore:**
  1. Backups › Restore › pick a date.
  2. The current data is set aside as "before restore".
  3. The screen lists the Macs connected since that backup, which must
     connect again.
  4. Each student Mac's next send fills in whatever the backup was missing,
     deletions included.
- If **Don't Allow** was clicked for the backup drive, the backup line turns
  red. It says "English Saathi can't use the backup drive", with an **Open
  Mac settings** button.

### 6.4 Keeping it running

- **Staying awake:** while server mode is on, the app tells macOS not to
  sleep from idleness. The screen still turns off. This also stops macOS from
  slowing the app down while its window is hidden.
- **Where it sits:**
  - lid open, plugged in;
  - **no external display**: it switches on the 2019 16-inch's hot graphics
    chip;
  - shaded room, vents clear. Apple's limit is 35 °C.
- **Power cuts:** the MacBook's battery keeps it running. The Wi-Fi box goes
  off, and the student Macs wait. After a long cut that empties the battery:
  1. Plug the Mac in and open the lid.
  2. If the screen stays dark, press the power button (top right of the
     keyboard).
  3. Type the Mac's password if asked.
  4. Check that English Saathi opens on the dashboard.
- **After a restart:** the app starts again if it is set to start
  automatically. Whether someone must type a password first depends on
  automatic login (§12, Q7).
- **What the server's main screen shows:** "Staying awake: yes", "Backup:
  today", "Can be found on the Wi-Fi: yes", and the firewall state, all in
  plain words. Anything red shows on the main screen, not only on the
  dashboard.

---

## 7. Fixing problems

### 7.1 The Connection screen (student Mac)

One screen: Settings › Connection, plus a small dot on the student picker.
It shows:

- **green** or **red**;
- "Last sent: today 10:42";
- "Waiting to send: 37";
- buttons: **Send now**, **Find server again**, **Connect again**, **Save
  problem report**.

It is in English with Hindi below, like every other screen. The Hindi needs
the teacher review.

### 7.2 What the red light says

macOS no longer lets apps read the Wi-Fi's name without asking for Location
permission. So instead:

- the server records its network's details: the Wi-Fi box's address and
  hardware ID, and the address range;
- it gives them, with the name staff typed, to every student Mac;
- each student Mac compares them with its own.

Checks run only when a send fails or the Connection screen is open, never on
a timer. The first row that matches is shown. Staff-facing words always say
"Wi-Fi box" for the router.

| What the Mac finds | What it says (English; Hindi below it) | Button |
|---|---|---|
| macOS blocks English Saathi (macOS 15+; Bonjour reports the block) | English Saathi needs permission to talk to the server Mac. Click Open Mac settings, find English Saathi in the list and turn its switch on. If it is already on, turn it off and on again. | Open Mac settings (falls back to showing: System Settings › Privacy & Security › Local Network) |
| Wi-Fi is off | Wi-Fi is switched off on this Mac. Click the Wi-Fi symbol at the top right and turn it on. | |
| Not on any Wi-Fi | This Mac is not on any Wi-Fi. Join HIKMAT-WIFI. | |
| No address from the Wi-Fi box | The Wi-Fi box isn't giving this Mac an address. If the power just came back, wait 5 minutes, then press Check again. | Check again |
| On a phone hotspot | This Mac is on a phone's hotspot. Join HIKMAT-WIFI. | |
| Same Wi-Fi box, different network | This Mac is on the guest Wi-Fi. Join HIKMAT-WIFI. | |
| A different Wi-Fi box, and it isn't the server's | This Mac may be on a different Wi-Fi. Check it is on HIKMAT-WIFI. If it is, press Find server again. | Find server again |
| Wi-Fi box doesn't answer | The Wi-Fi box isn't answering. Check its lights are on. After a power cut, wait 5 minutes. | Check again |
| The server Mac is there, but English Saathi isn't answering | English Saathi is closed on the server Mac, or server mode is off. Open it there. | |
| The server Mac is there, but blocks the connection | The server Mac is blocking the student Macs. Look at the server Mac's screen and follow its red message. | |
| The server doesn't know this Mac | This Mac isn't connected yet. Press Connect again, then press Approve on the server Mac. | Connect again |
| Waiting for approval | Waiting for the server Mac. On the server Mac, open Macs and press Approve. Number: 482 913. | |
| A different server answers | This Mac knows the server Mac 01, but Mac 12 answered. If Mac 12 is the new server Mac, press Connect again. | Connect again |
| Never connected, server not found | Can't find the server Mac. Make sure this Mac is on the same Wi-Fi as the server Mac. The server Mac's Macs page shows the Wi-Fi name. | Find server again |
| Server not found; other student Macs visible | Can't reach the server Mac. Check it is on, awake, and English Saathi is open there. If it is, look at the server Mac's screen and follow its red message. | Find server again |
| Server not found; nothing visible; Wi-Fi box fine | Can't find the server Mac or any other Mac. 1) Check the server Mac is on. 2) Check this Mac is on HIKMAT-WIFI, not the guest Wi-Fi. 3) If every Mac is on HIKMAT-WIFI and this still shows, the Wi-Fi box is keeping devices apart and needs a technical person: press Save problem report and send it. Until then, use Export progress. | Find server again |
| Green, records waiting | (green) 37 waiting to send. | Send now |

How well these checks tell the cases apart on the Airtel router is
**untested**. §11 S0 measures it before the wording is final.

### 7.3 The server's own checks and the Macs page

The server checks itself when its screen opens and every few minutes while
it is showing; each check is quick and runs only then. Each line is green or
red, in plain words:

- **Wi-Fi:** on, and the same Wi-Fi box as at setup. If the box seems to
  have changed (Airtel replaced it, or a reset), the server asks: "The Wi-Fi
  box seems to have changed. Is this still HIKMAT-WIFI? **Yes** / **No**".
  Yes saves the new details, and each student Mac gets them with its next
  send.
- **Can be found:** macOS allows English Saathi on the local network. If
  not, the line says "Student Macs can't find this Mac. Click Open Mac
  settings and turn on English Saathi", with that button.
- **Firewall:** off, or on and allowing English Saathi. The server can read
  this without an administrator password.
- **Staying awake.**
- **Backup drive, and the last backup.**
- **Devices kept apart:** only when the server can tell for sure. For
  example, staff press **Check** on the server while English Saathi is open
  on one student Mac, and that Mac still can't be seen. Then the server
  says: "The Wi-Fi box is keeping devices apart. This needs a technical
  person. Press Save problem report and send it. Until then, use Export
  progress."

**Macs page:** a card per Mac, **green or red**, with the reason in one
line ("Not heard from for 2 school days", "Last send failed: …", "Removed").
It also shows last seen, records waiting, app version, and **Remove** or
**Reconnect**. During joining, the Wi-Fi name is shown in large letters.

### 7.4 Save problem report

On both sides, with no names and no recordings:

- **On screen:** a short summary to photograph with a phone and send on
  WhatsApp, which needs no internet on the Mac. It shows the Mac number,
  what is wrong, last send, records waiting, and app and macOS version. A
  QR code carries the same lines.
- **In a file:** a small text file, saved to the Desktop with the Mac's
  number in its name, for when a USB stick or AirDrop is at hand. It holds:
  - the network details;
  - the Local Network and firewall state;
  - what Bonjour can see;
  - the last 20 send attempts, with their errors and timing;
  - record counts;
  - the end of the app log, with student names and recognised text removed
    and the home folder hidden.

---

## 8. macOS questions and signing

| What macOS asks | Which Macs | When | Staff do |
|---|---|---|---|
| "Allow English Saathi to find devices on local networks?" | All, on macOS 15 and later (not 13 or 14) | First connection, or the first time server mode is on. **Again after each update**: it may appear as soon as the app opens, in front of a student | Allow (whoever sees it) |
| "Accept incoming network connections?" | Server only, and only if its firewall is on | First time server mode is on; probably again after each update | Allow |
| "Access files on a removable volume?" | Server | First backup; probably after updates | OK |
| Microphone (as today) | Student Macs | After each update | Allow |

- **Why they come back after updates.** macOS remembers these answers by
  the app's signature. Ours is made by us, without an Apple certificate
  (called "ad hoc"), and it changes with every build. So macOS treats each
  update as a new app.
- **What Apple warns about.** Apple's note on Local Network permission says
  answers given to such apps can be unreliable. It also warns about apps
  that share an internal ID number with other programs. Ours shares it with
  every app built by the same Java packaging tool (checked). Apple's forums
  also report the permission being forgotten after a restart.
- **The plan:**
  - give the app its own internal ID number when packaging. This worked on a
    test copy. Whether one fixed number or a new one per version is better is
    **untested**, and S0 tests both;
  - test, on one macOS 15 and one macOS 26 Hikmat Mac, from the `.dmg`:
    - allow → restart → send;
    - update → restart → send;
    - Don't Allow after an update → recover (§11, S0);
  - keep "turn it off and on again" in the checklist; it is the fix
    reported for the restart bug;
  - put a Developer ID to the owner again (§12, Q1).
- **Testing trap:** an app started from Terminal skips the Local Network
  check entirely. All testing must use the app from the `.dmg`, opened in
  Finder.

---

## 9. Resource use

- **Measured on an Apple M4** with a headless test program: the built-in
  Java web server, idle, used about 0.09% of one core and about 37 MB of
  memory in all.
- **Not yet measured:** the real app's window on a 2019 Intel Mac. It will be
  most of the memory. S0 and S2 measure it with the window open and
  minimised.
- **What server mode skips:** the speech models (small.en alone is about
  375 MB on disk), the voices and the microphone. So it should use far less
  memory than a practice Mac.
- **A memory cap.** Java today may take up to a quarter of the Mac's memory
  (about 4 GB). Server mode gets a cap, set from the S2 measurement.
- **No background work:** the server only stores what arrives. Its pages
  are worked out when opened.
- **Reported in plain words** on the server screen and in the problem
  report: "This app is using about _% of the processor and _ MB of memory",
  with the real figures measured on the Mac.

---

## 10. Staff guide

English and Hindi (the Hindi teacher-reviewed), one page per task. Each page
has screenshots from macOS 13 and 26 where they differ, taken from the real
app on a Hikmat Mac. Words match the app's: "Wi-Fi box", "server Mac",
"student Mac". The existing one-page setup guide stays English only, as
decided on 2026-10-22, unless the owner wants them merged (§12, Q14).

1. **Making a Mac the server**, keeping it awake and cool:
   - numbering the Macs;
   - what happens to its students;
   - PIN and recovery code;
   - the Wi-Fi name;
   - the three macOS questions;
   - starting automatically;
   - where to put it, and what to do after a long power cut.
2. **Connecting a student Mac:** its number, Allow, compare the number,
   Approve, then roll numbers.
3. **"The light is red":** the checklist in order of likelihood:
   1. Wi-Fi is on, and on HIKMAT-WIFI.
   2. The server Mac is on, awake, and the app is open.
   3. After a power cut: check the Wi-Fi box's lights (photo) and wait 5
      minutes.
   4. Local Network switch: turn it off and on.
   5. Connect again.
   6. Save problem report: photograph the summary and send it.
4. **Backups and restoring.**
5. **The macOS firewall question:** click Allow; how to fix it if Deny was
   clicked.
6. **After an update:** which questions come back, on which Macs.
7. **No Wi-Fi:** copy progress with a USB stick (Export progress, Import
   file).
8. **Moving the server to another Mac:**
   1. Make the new Mac the server ("This Mac replaces it").
   2. Backups › Restore.
   3. The student Macs carry on.
9. **Forgot the PIN.**

---

## 11. Milestones (smallest first)

Each ends with tests passing, a commit, and a demo on the Mac.

| | Milestone | Done when |
|---|---|---|
| **S0** | **Field test, one day on site.** Needs two Hikmat Macs, one on macOS 15+, and the real router | A throwaway test build finds, joins and sends between two Macs from the `.dmg`. Tested: Local Network allow → restart → send, update → restart → send, Don't Allow after an update → recover; firewall on and off; 2.4 vs 5 GHz, the guest Wi-Fi, after a router restart; whether the main Wi-Fi keeps devices apart; one fixed and one per-version internal ID; one-button "start automatically"; idle CPU and memory. The results decide the wording in §7.2 and whether the plan holds |
| **S1** | **Records on the student Mac** (no network) | Every attempt, student change and deletion has its own ID; older practice gets IDs too; the confirmed list and the permanent deletion list work; Export progress writes a file; practice is unchanged and no slower |
| **S2** | **Server mode and dashboard from files** | PIN, recovery code, Reset PIN; server mode opens without speech but keeps the app log; the Mac's own students copied in; Import file stores records with no duplicates; Students, One student, Cohort, CSV; memory cap set; resource figures shown |
| **S3** | **Encrypted connection and joining** | Approve with the matching number in either order; per-Mac key; Remove and one-step Reconnect; a duplicate Mac number asks Replace/Cancel; sends every 60 min ± random, on quit, on Send now and after the server returns; a killed app or server loses nothing and doubles nothing |
| **S4** | **Finding the server** | Bonjour both ways; last address; automatic look-around only on the paired network; Find server again; manual address under More…; finds the server again after its address changes; a second server is refused |
| **S5** | **Connection screen, Macs page, problem reports** | Every row of §7.2 and every line of §7.3 shown in a test; problem reports with no names; the on-screen summary and QR code |
| **S6** | **Backups and restore** | Daily, Back up now and on approve/remove; a drive pulled mid-backup leaves the last one intact; restore, then the student Macs fill the gap, and a student deleted after the backup stays deleted |
| **S7** | **Staff guide and trial** | The nine pages with real screenshots, Hindi reviewed; a Program Assistant sets up the server and connects a Mac from the guide alone, and fixes three staged problems |

Packaging changes along the way:

- JNA, with its Intel library signed inside the app;
- two more Java runtime parts;
- the Local Network texts in English and Hindi;
- the app's own internal ID number;
- self-test checks for each.

---

## 12. Questions for the owner

1. **Apple Developer ID.** On 2026-10-22 the owner chose no notarization
   and no Developer ID (decisions.md, "Choices made while packaging"). This
   asks again, because of the Local Network risk.
   - Apply for Apple's nonprofit fee waiver (or pay US $99 a year), so the
     app is signed and notarized? Recommended: it is the one change that
     makes the macOS permissions reliable, and it also removes Open Anyway.
   - If the answer stays no, the plan still works, but expects the Allow
     questions after every update and relies on S0 passing.
   - *Decide after S0.*
2. **Data leaving each Mac.** These go to the server Mac on campus: student
   names, roll numbers, practice text, and what the recogniser heard.
   Recordings never do. This changes "All data stays on that Mac". OK?
   *(Blocks S1.)*
3. **Same student on several Macs.** Two options:
   - one home Mac per student, with roll numbers to join them on the
     dashboard (recommended);
   - progress that follows a student to any Mac (two-way sync, much more
     work).
   Also: do students sit at fixed Macs today? *(Blocks S1.)*
4. **Joining:** Approve with a matching number (recommended), a typed code,
   or Approve only? *(Blocks S3.)*
5. **JNA** as the one new library (Apache-2.0), for Bonjour and staying
   awake? The alternatives are worse:
   - a second, Java-only Bonjour system. It currently has a macOS bug, and
     competes with macOS for the same network port;
   - running macOS's `dns-sd` tool, whose manual says scripts that use it are
     fragile.
   *(Blocks S4.)*
6. **Warnings:** a Mac is late after 2 school days without sending. The
   backup drive stays plugged in, with a warning after 3 days without a
   backup. OK? *(Blocks S5.)*
7. **After a restart of the server Mac.** Two options:
   - automatic login, so the app comes back with no one present. This needs
     FileVault off, and anyone who restarts the Mac gets an administrator
     account;
   - someone types the password.
   *(Blocks S2's setup checklist.)*
8. **Backups include the server's identity**, so a replacement server needs
   no reconnecting (recommended; keep the drive in the office)? *(Blocks
   S6.)*
9. **The site visit for S0:** who goes, when, and which two Macs (one on
   macOS 15 or later)? Which macOS versions do the 20 Macs run? *(Blocks
   S0.)*
10. **Wi-Fi:** is there one name for both 2.4 and 5 GHz, and is there a
    guest network? No router changes are needed; this only sets what S0
    tests.
11. **Which Mac becomes the server**, leaving 19 for practice? Its students'
    progress is copied to the dashboard, and they move to another Mac.
    There they start a new profile, because progress doesn't move between
    Macs (Q3). OK? *(Blocks S2.)*
12. **Deleting a student on a student Mac** (Settings has no PIN): should it
    also delete them from the dashboard, the pilot's evidence?
    - Recommended: yes, as the app's delete already promises.
    - Deleted students stay in older backups until those expire. Recommended:
      monthly backups are kept for 6 months.
    *(Blocks S1.)*
13. **Settings PIN on student Macs:** Settings is open to anyone today. It
    could show Delete, Connect again and Export behind the same kind of PIN.
    Recommended: not now, to keep staff steps simple. *(Blocks nothing.)*
14. **Guides:** a new English and Hindi staff guide, several pages, as the
    brief asks, beside the existing English-only setup guide. Or merge them?
    *(Blocks S7.)*

---

## 13. Risks

| Risk | Effect | What the plan does |
|---|---|---|
| Local Network permission forgotten or asked again (signed without an Apple certificate; shared internal ID) | A Mac goes red after a restart or update | Own ID number; S0 tests it before building; the Connection screen names it, with an Open Mac settings button; Developer ID (Q1) |
| The main Wi-Fi keeps devices apart, or filters Bonjour | Server not found | Last address and look-around work without Bonjour. S0 measures it. The server confirms it with a staff check, and the message says to escalate and use Export progress |
| Server Mac off, asleep, lid closed, flat battery | Nothing arrives | Student Macs keep everything and retry. The server keeps itself awake. Guide and Macs page say what to do |
| Firewall: Deny clicked | Server found but blocked | The server reads its firewall state and says how to fix it |
| Two copies of the app installed | macOS confused about permissions | Guide: delete the old copy from Applications and Downloads |
| Server mode turned on by mistake on a second Mac | Data split across two servers | Refused while another server is on the Wi-Fi |
| Copied data folder or hand edits | Same ID, different content | Kept and flagged, never overwritten |
| Server disk failure | History lost | Daily backups. Student Macs resend anything the restored server lacks, deletions included |
| Mac clocks wrong | Odd dates | Each Mac's own order kept, arrival time stored, the clock flagged |
| Roll number typos | Two students merged, or one split | Never merged silently when names differ |
| Staff approve without comparing numbers | A fake server could join at first contact | Still as safe as "Approve only"; joining only through the PIN-unlocked Macs page |
| Dashboard slow on a year of data | A wait when opening | Measure in S2; keep a per-Mac summary if needed |

---

## 14. Sources

1. Apple, TN3179: Understanding local network privacy. https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy
2. Apple, TN3178: Checking for and resolving build UUID problems. https://developer.apple.com/documentation/technotes/tn3178-checking-for-and-resolving-build-uuid-problems
3. Apple, NSLocalNetworkUsageDescription. https://developer.apple.com/documentation/bundleresources/information-property-list/nslocalnetworkusagedescription
4. Apple, NSBonjourServices. https://developer.apple.com/documentation/bundleresources/information-property-list/nsbonjourservices
5. Apple Developer Forums: Java apps sharing a Local Network entry (766129); permission ignored after reboot (792453); ad hoc signing and the firewall (747298, 776479). https://developer.apple.com/forums/thread/792453 · https://developer.apple.com/forums/thread/766129 · https://developer.apple.com/forums/thread/747298 · https://developer.apple.com/forums/thread/776479
6. Apple, Firewall settings and security. https://support.apple.com/guide/mac-help/change-firewall-settings-on-mac-mh11783/mac · https://support.apple.com/guide/security/firewall-security-seca0e83763f/web
7. Apple, Membership fee waiver for nonprofits. https://developer.apple.com/support/membership-fee-waiver/
8. Apple, dns_sd.h (DNS Service Discovery API), in the macOS SDK; dns-sd(1) man page. https://keith.github.io/xcode-man-pages/dns-sd.1.html
9. RFC 6762 (Multicast DNS) and RFC 6763 (DNS-SD). https://www.rfc-editor.org/rfc/rfc6762 · https://www.rfc-editor.org/rfc/rfc6763
10. RFC 9119, Multicast considerations over IEEE 802 wireless media. https://www.rfc-editor.org/rfc/rfc9119
11. JmDNS issues #373 (3.6.4 breaks macOS) and #203. https://github.com/jmdns/jmdns/issues/373
12. JNA (Apache-2.0 or LGPL-2.1+). https://github.com/java-native-access/jna
13. Apple Developer Forums: SSID needs Location permission since macOS 14. https://developer.apple.com/forums/thread/732431 · https://developer.apple.com/forums/thread/748518
14. Apple, IOPMLib.h (power assertions) and App Nap. https://developer.apple.com/documentation/iokit/1557134-iopmassertioncreatewithname · https://developer.apple.com/library/archive/documentation/Performance/Conceptual/power_efficiency_guidelines_osx/AppNap.html
15. Apple, Open items automatically when you log in; SMAppService; automatic login. https://support.apple.com/guide/mac-help/open-items-automatically-when-you-log-in-mh15189/mac · https://developer.apple.com/documentation/servicemanagement/smappservice/mainapp · https://support.apple.com/102316
16. Apple, MacBook Pro (16-inch, 2019) technical specifications. https://support.apple.com/kb/SP809
17. JDK 17: com.sun.net.httpserver, java.net.http, X509ExtendedTrustManager, PBKDF2. https://docs.oracle.com/en/java/javase/17/docs/api/jdk.httpserver/module-summary.html · https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/module-summary.html
18. RFC 8446 (TLS 1.3); RFC 5280 (X.509); RFC 6189 (ZRTP short authentication strings). https://www.rfc-editor.org/rfc/rfc8446
19. OWASP Password Storage Cheat Sheet. https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
20. SQLite, How to corrupt a database (USB drives and sync). https://sqlite.org/howtocorrupt.html

---

## Appendix A: For the developer

**Code anchors (read from the repo).**

- **Server mode branches in `Main.kt`** right after `AppLock`, from a
  PIN-gated flag in the data folder. Split `Setup.create` so server mode
  still runs `DataFolder.prepare` and the session log (sync and joining
  events go there too). It skips the speech library, models, voices,
  microphone and lessons. Server data lives in its own subfolder.
- **Practice files are the source of truth** and sync never writes to them.
  Read only from an allow-list (`learner.json`, `progress.json`,
  `lessons.json`, `attempts/*.jsonl`); never walk `learners/<id>/`, which
  holds recordings.
- **Sync state lives in a new `sync/` folder:** `mac.json` (random install
  ID), the confirmed-ID set, the server's pinned identity and key, the
  permanent deletion log. At start-up and before each send, scan for
  records not yet confirmed.
- **Record IDs:**
  - `AttemptRecord` gains `id` (`<installId>:<counter>`, counter saved with
    `SafeFiles` first) and the pack version.
  - Existing rows get `sha256(installId, learnerId, raw line)`, ordered by
    file name and line number.
  - The server confirms exact IDs; IDs, not counters, prevent duplicates.
  - `learner.json` gains a random `uid`, a `revision` and an optional
    `roll`.
- **Deletion:** `LearnerStore.delete` appends to the deletion log before
  renaming the folder. The server keeps tombstones permanently and refuses
  later records for that learner. When the server's reply shows it lacks
  records the Mac had confirmed (a restore), the Mac resends the deletion
  log and clears the confirmed set for re-checking.
- **Snapshots** are read at send time when `progress.json` or `lessons.json`
  changed since the last confirmed snapshot. They carry a per-learner
  snapshot number.
- **The hello** carries the Mac's clock with its offset.
- **Send on quit** needs `Desktop.setQuitHandler` and the window's close
  hook, with a timeout; no hook exists today.
- **The dashboard** reuses `Results.summary`/`csv`, adding columns at the
  end.

**Network.**

- **Server:** JDK `HttpsServer`, TLS 1.3 only, 2 threads.
  - `sun.net.httpserver.maxReqTime/maxRspTime=30`, `maxConnections≈64`.
  - Request bodies capped.
- **Client:** one long-lived `HttpClient`, HTTP/1.1.
- **Server key:** EC P-256, with a self-signed certificate made by a small
  public-API DER encoder.
  - No BouncyCastle; `keytool` is stripped from the jlinked runtime.
  - Stored as PKCS12, `notAfter` 99991231235959Z.
- **Pinning:** the client pins SHA-256 of the whole certificate with an
  `X509ExtendedTrustManager`. A plain `X509TrustManager` triggers hostname
  checks.
- **Joining:**
  - commitment C=SHA-256(nc), then the nonces;
  - code = first 4 bytes of SHA-256("ES-pair-v1"‖F‖installId‖nc‖ns) mod
    10⁶;
  - pending requests are queued while the server is locked, kept for 10
    minutes, rate-limited;
  - token: 32 random bytes; the server stores its SHA-256;
  - Reconnect revokes the token and pre-approves that `installId` for 30
    minutes.
- **The server's identity:** each reply includes the server's Wi-Fi MAC
  address. The "app closed on the server" row needs `arp -n <last address>`
  to match it, so a recycled address isn't mistaken for the server.
- **PIN:**
  - PBKDF2WithHmacSHA256, with salt and iteration count stored, tuned to
    about 0.5–1 s on the Intel Mac;
  - a growing delay after 5 wrong tries;
  - Reset PIN checks the macOS login password (method to confirm in S2).

**Bonjour.**

- **JNA** (pin 5.17.0 or 5.18.1) to `dns_sd` in libSystem:
  `DNSServiceRegister/Browse/Resolve/GetAddrInfo`.
- **One owner thread** polls `DNSServiceRefSockFD` and calls
  `DNSServiceProcessResult`; the API isn't thread-safe.
- **Errors:** recreate everything on `ServiceNotRunning`. Treat
  `PolicyDenied` (-65570) as "Local Network off"; this is the only signal
  used for that row. `NoRouteToHost` is ambiguous.
- **Service:** type `_saathi._tcp` (≤15 characters). TXT: role, server ID,
  protocol version, Wi-Fi name. Prefer IPv4.
- **Look-around:** /24 cap, one port, about 32 parallel connects with a
  300 ms timeout. Only when the network details match the server's last
  ones.
- **Ship JNA's x86-64 `libjnidispatch` inside the bundle** so the signing
  loop signs it, with `-Djna.boot.library.path` and `-Djna.nounpack=true`.
  Strip the other natives, and have the self-test confirm where it loaded
  from.
- **Staying awake:** `IOPMAssertionCreateWithName("PreventUserIdleSystemSleep")`
  through JNA. Also try `SMAppService.mainApp` in S0.

**Diagnosis inputs** (all unprivileged, checked on macOS 26.5.1):

- `route -n get default`;
- `arp -n <gateway>`;
- `ipconfig getoption <if> router|subnet_mask|server_identifier|lease_time`;
- `networksetup -getairportpower` and `-listallhardwareports` (don't
  hard-code en0);
- `system_profiler -json SPAirPortDataType` (about 4 s; Connection screen
  only);
- on the server: `socketfilterfw --getglobalstate/--getblockall/--getappblocked`
  and `pmset -g assertions`.

Probe the Wi-Fi box with a DNS query when it is the DNS server (DHCP option
6; exempt from Local Network privacy), not ping. The Wi-Fi name comes back
redacted.

**Backups:** `FileChannel.force(true)` (F_FULLFSYNC on macOS) on the file
and its folder before the rename; read back and check before the previous
backup can expire.

**Packaging:**

- Add `jdk.httpserver` and `java.net.http` to `modules(...)`, about 1.4 MB.
  `jdk.crypto.ec` stays; TLS needs it.
- Add `NSLocalNetworkUsageDescription` (English + Hindi) and
  `NSBonjourServices` = [`_saathi._tcp`] to `extraKeysRawXml`.
- Patch the launcher's LC_UUID before ad hoc signing: fixed or per version,
  as S0 decides.
- Set a `-Xmx` from the S2 measurement.
- The self-test checks the keys, the modules, one local TLS round trip and
  that JNA loads from inside the app.
