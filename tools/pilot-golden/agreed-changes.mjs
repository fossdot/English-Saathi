// The owner-agreed changes to the pilot's domain rules (docs/decisions.md,
// "Domain rules: where the Kotlin port differs from the pilot"), as a small
// overlay on the pilot's own code. generate.mjs records golden cases from
// "pilot + agreed changes" with it. Everything the overlay does not replace
// runs exactly as in the pilot.
//
// This is a second implementation of those rules, written from the decisions
// document alone, so the golden cases check the Kotlin port against it.

/**
 * Patches a pilot loaded by loadPilot() (pilot-harness.mjs) in place and returns it.
 * Afterwards P.api has the agreed versions of the replaced pilot functions and the
 * new ones (agreedNormalize, agreedHeard, openApp, streakView, startIndex, ...).
 */
export function applyAgreedChanges(P) {
  const fns = P.run("(" + overlay.toString() + ")()", "agreed-changes.js");
  Object.assign(P.api, fns);
  return P;
}

/**
 * Runs inside the pilot's vm context (never called in this module), so the pilot's
 * top-level names are in scope: S, D, St, CONFIG, LESSON, WORDS, today, addDays,
 * dayDiff, ymd, doneToday, sent, save, go, addXP, align, score, logAttempt, fixTarget.
 * Pilot functions are replaced by assigning their global bindings, which the
 * pilot's own code (go, align, ...) looks up by name on every call.
 */
function overlay() {
  const pilot = { nextIndex, updateRevision };
  const stats = { pullBacks: 0 };

  // ---------- Text matching: one normalizer ----------
  const ONES = ["zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve",
    "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"];
  const TENS = ["", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"];
  /** 1..999 in words, without "and". */
  function underThousand(n) {
    const w = [];
    if (n >= 100) { w.push(ONES[Math.floor(n / 100)], "hundred"); n %= 100; }
    if (n >= 20) { w.push(TENS[Math.floor(n / 10)]); n %= 10; }
    if (n > 0) w.push(ONES[n]);
    return w;
  }
  /** 0..999,999,999 in words, without "and" ("120" -> one hundred twenty). */
  function spell(n) {
    if (n === 0) return ["zero"];
    const w = [], millions = Math.floor(n / 1e6), thousands = Math.floor(n / 1e3) % 1000, rest = n % 1000;
    if (millions) w.push(...underThousand(millions), "million");
    if (thousands) w.push(...underThousand(thousands), "thousand");
    if (rest) w.push(...underThousand(rest));
    return w;
  }
  /**
   * Lowercase; "-" separates words; split on JavaScript \s; keep a-z and 0-9 in each
   * token; drop tokens with nothing left; spell out digit-only tokens up to
   * 999,999,999 (by value, so leading zeros don't count); longer ones stay as they are.
   */
  function agreedNormalize(text) {
    const out = [];
    for (const raw of String(text).toLowerCase().replace(/-/g, " ").split(/\s+/)) {
      const tok = raw.replace(/[^a-z0-9]/g, "");
      if (!tok) continue;
      if (/^[0-9]+$/.test(tok)) {
        const digits = tok.replace(/^0+/, "");
        if (digits.length <= 9) { out.push(...spell(digits ? Number(digits) : 0)); continue; }
      }
      out.push(tok);
    }
    return out;
  }
  /**
   * Bracketed non-speech tags in recognizer output: innermost [...] and (...) pairs, so an
   * unclosed bracket can't swallow the speech after it. Each tag becomes a space (it is never
   * part of a word), repeated until none is left, so nested tags go too.
   */
  const TAG = /\[[^\[\]]*\]|\([^()]*\)/g;
  const agreedHasTag = text => /\[[^\[\]]*\]|\([^()]*\)/.test(String(text));
  function stripTags(text) {
    let t = String(text);
    for (;;) { const u = t.replace(TAG, " "); if (u === t) return t; t = u; }
  }
  /** Words of recognizer output: tags removed, then the one normalizer. [] means "Didn't hear you". */
  const agreedHeard = text => agreedNormalize(stripTags(text));

  /** Accept entries are normalized too; only entries that normalize to one word count. */
  globalThis.same = function (t, h) {
    if (t === h) return true;
    const acc = (WORDS[t] && WORDS[t].accept) || [];
    return acc.some(e => { const n = agreedNormalize(e); return n.length === 1 && n[0] === h; });
  };

  // ---------- Word to fix: first WRONG word, else first MISSING word ----------
  function agreedWordToFix() {
    const r = S.result;
    if (!r) return null;
    let i = r.res.findIndex(x => x.status === "wrong");
    if (i < 0) i = r.res.findIndex(x => x.status === "missing");
    return i < 0 ? null : { word: r.res[i].word, index: i };
  }
  globalThis.firstWrong = function () { const w = agreedWordToFix(); return w ? w.word : null; };

  // ---------- Streak ----------
  const shiftDay = (day, n) => { const d = new Date(day + "T00:00"); d.setDate(d.getDate() + n); return ymd(d); };
  globalThis.markDone = function (id, starsN) {
    const s = St(), t = today(), list = doneToday();
    if (!list.includes(id)) list.push(id);
    s.doneByDay[t] = list;
    s.best[id] = Math.max(s.best[id] || 0, starsN);
    if (s.lastDay && t < s.lastDay) {
      // Phone date earlier than the last practice day: the streak ignores it.
    } else if (s.lastDay !== t) {
      s.streak = !s.lastDay ? 1 : dayDiff(s.lastDay, t) <= 3 ? s.streak + 1 : 1;
      s.lastDay = t;
    }
    s.pausedSince = null; // cleared by practising
    save();
  };
  /**
   * The app sees today's date: 1-2 missed days store pausedSince; 3 or more break the
   * streak, which is not paused (pausedSince null). The stored count is kept: streakView()
   * shows 0 for a broken streak, and the next practice starts again at 1.
   */
  function openApp() {
    const s = St();
    if (!s.lastDay) return;
    const diff = dayDiff(s.lastDay, today());
    if (diff <= 1) return; // practised today or yesterday, or an earlier phone date
    s.pausedSince = diff <= 3 ? shiftDay(s.lastDay, 1) : null;
    save();
  }
  /** What the learner sees. */
  function streakView() {
    const s = St();
    if (!s.lastDay) return { days: 0, status: "none" };
    const diff = dayDiff(s.lastDay, today());
    if (diff <= 1) return { days: s.streak, status: "active" };
    if (diff <= 3) return { days: s.streak, status: "paused" };
    return { days: 0, status: "broken" };
  }

  // ---------- Leitner box: pull back due dates further ahead than their box's interval ----------
  const latestDue = e => addDays(CONFIG.leitnerDays[e.box - 1]);
  /** Stores pulled-back due dates (only before a Leitner update). Returns how many changed. */
  function agreedPullBack() {
    const s = St();
    let n = 0;
    for (const w of Object.keys(s.revise)) {
      const e = s.revise[w], max = latestDue(e);
      if (e.due > max) { e.due = max; n++; }
    }
    if (n) { stats.pullBacks += n; save(); }
    return n;
  }
  globalThis.updateRevision = function (results) { agreedPullBack(); pilot.updateRevision(results); };
  /** A due date as read today: pulled back if further ahead than its box's interval (nothing stored). */
  const dueAsOfToday = e => { const max = latestDue(e); return e.due > max ? max : e.due; };
  /** Words due today or earlier, reading through the pull-back without storing it. */
  function agreedDueWords() {
    const s = St(), t = today();
    return Object.keys(s.revise).filter(w => dueAsOfToday(s.revise[w]) <= t);
  }
  /** "Practise again tomorrow": due tomorrow or earlier, reading through the pull-back. */
  function agreedTomorrowView() {
    const s = St(), tomorrow = addDays(1);
    return Object.keys(s.revise).filter(w => dueAsOfToday(s.revise[w]) <= tomorrow);
  }
  /** "Practise again tomorrow" as the lesson-done screen shows it. */
  function agreedTomorrow() { return agreedTomorrowView(); }

  // ---------- Sentence choice and the lesson-done screen ----------
  /** First sentence, in lesson order, not done today that contains a due word; -1 if none. */
  function dueSentence() {
    const due = agreedDueWords(), d = doneToday();
    return LESSON.sentences.findIndex(x => !d.includes(x.id) && agreedNormalize(x.text).some(w => due.includes(w)));
  }
  /** Next, Skip, and after a fix. */
  globalThis.nextIndex = function () { const i = dueSentence(); return i >= 0 ? i : pilot.nextIndex(); };
  /** Start practice / One more sentence. */
  function startIndex() {
    const i = dueSentence();
    if (i >= 0) return i;
    const d = doneToday(), k = LESSON.sentences.findIndex(x => !d.includes(x.id));
    return k >= 0 ? k : (S.idx + 1) % LESSON.sentences.length;
  }
  globalThis.afterSentence = function () {
    const s = St(), t = today();
    if (doneToday().length >= CONFIG.goal && s.celebratedOn !== t) {
      s.celebratedOn = t; // remembered in the learner's record, not in memory
      save();
      S.tomorrow = agreedTomorrow();
      go("done");
      return;
    }
    S.idx = nextIndex();
    go("practice");
  };

  // ---------- Attempts ----------
  /** Returns { nothingHeard }. Nothing heard: no score, XP, done mark, Leitner or streak change. */
  globalThis.handleAttempt = function (heard) {
    const s = sent(), H = agreedHeard(heard);
    if (!H.length) return { nothingHeard: true };
    const a = align(agreedNormalize(s.text), H), sc = score(a.res), xp = CONFIG.xpAttempt + CONFIG.xpStar * sc.stars;
    addXP(xp); markDone(s.id, sc.stars); updateRevision(a.res); logAttempt("sentence", s.text, heard, sc);
    S.result = Object.assign({ res: a.res, extra: a.extra, heard, xp }, sc);
    go("feedback");
    return { nothingHeard: false };
  };

  // ---------- Fixing loop ----------
  /**
   * Step-4 chunk: the chunk holding the fixed word's position, with chunks located in
   * order as contiguous runs of the sentence's words; else the first chunk containing
   * the word; else the full sentence. Returns the chunk text and the word's position in it.
   */
  function chooseChunk(s, wf) {
    const W = agreedNormalize(s.text);
    let from = 0, located = true;
    const spans = [];
    for (const c of s.chunks) {
      const C = agreedNormalize(c);
      let at = -1;
      for (let i = from; i + C.length <= W.length; i++) if (C.every((w, k) => W[i + k] === w)) { at = i; break; }
      if (at < 0) { located = false; break; }
      spans.push({ c, at, len: C.length });
      from = at + C.length;
    }
    if (located) {
      const sp = spans.find(x => wf.index >= x.at && wf.index < x.at + x.len);
      if (sp) return { chunk: sp.c, pos: wf.index - sp.at };
    }
    for (const c of s.chunks) {
      const pos = agreedNormalize(c).indexOf(wf.word);
      if (pos >= 0) return { chunk: c, pos };
    }
    return { chunk: s.text, pos: wf.index };
  }
  globalThis.startFix = function () {
    const wf = agreedWordToFix(), s = sent();
    if (!wf) return;
    const { chunk, pos } = chooseChunk(s, wf);
    S.fix = { word: wf.word, wordIndex: wf.index, chunk, chunkPos: pos, step: 1, fails: 0 };
    go("fix");
  };
  /**
   * Steps 3-5. Passes when the fixed word is said correctly at its position (step 3: the
   * word alone, step 4: its place in the chunk, step 5: its place in the sentence).
   * Nothing heard is not a try. Returns { nothingHeard, passed }.
   */
  globalThis.handleFix = function (heard) {
    const f = S.fix, target = fixTarget(), H = agreedHeard(heard);
    if (!H.length) return { nothingHeard: true, passed: false };
    const a = align(agreedNormalize(target), H), sc = score(a.res);
    const pos = f.step === 3 ? 0 : f.step === 4 ? f.chunkPos : f.wordIndex;
    const ok = !!a.res[pos] && a.res[pos].status === "ok";
    logAttempt("fix step " + f.step, target, heard, { c: sc.c, n: sc.n });
    if (ok) {
      addXP(CONFIG.xpFixStep);
      if (f.step === 5) { updateRevision(a.res); afterSentence(); return { nothingHeard: false, passed: true }; }
      f.step++; f.fails = 0;
    } else {
      f.fails++;
      if (f.fails >= 2) { f.step = Math.max(1, f.step - 1); f.fails = 0; }
    }
    render();
    return { nothingHeard: false, passed: ok };
  };

  // The screens are not part of the domain rules, and the pilot's feedback screen
  // assumes one target word per whitespace token, which the one normalizer changes.
  globalThis.render = function () {};

  const added = { agreedNormalize, agreedHeard, agreedHasTag, agreedWordToFix, agreedPullBack, agreedDueWords,
    agreedTomorrow, agreedTomorrowView, agreedStats: stats, openApp, streakView, startIndex };
  Object.assign(globalThis, added);
  const replaced = {};
  for (const name of ["same", "firstWrong", "markDone", "updateRevision", "nextIndex", "afterSentence",
    "handleAttempt", "startFix", "handleFix", "render"]) replaced[name] = globalThis[name];
  return Object.assign(added, replaced);
}
