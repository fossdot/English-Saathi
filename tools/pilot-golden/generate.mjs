// Records golden test cases for the Kotlin port
// (domain/src/test/resources/pilot-golden/*.json).
//
//   TZ=Asia/Kolkata node tools/pilot-golden/generate.mjs
//
// Every case comes from running the pilot's own code (pilot-harness.mjs),
// patched with the owner-agreed changes from docs/decisions.md
// (agreed-changes.mjs). So the cases record "pilot + agreed changes". Fields
// named pilot* (and normalize.json's `pilot`) come from the unpatched pilot.
// `features` tags mark inputs that touch an agreed change. Before writing,
// the generator checks that the patched pilot still gives the unpatched
// pilot's results wherever no agreed change applies.

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { loadPilot } from "./pilot-harness.mjs";
import { applyAgreedChanges } from "./agreed-changes.mjs";

if (process.env.TZ !== "Asia/Kolkata") {
  console.error("Run with TZ=Asia/Kolkata so dates are deterministic.");
  process.exit(1);
}

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const PILOT = path.join(ROOT, "docs/pilot/english-practice-pilot.html");
const OUT = path.join(ROOT, "domain/src/test/resources/pilot-golden");
const START = new Date(2026, 9, 19, 10, 0).getTime(); // 2026-10-19 10:00 local

// ---------- Deterministic randomness ----------
function mulberry32(seed) {
  return function () {
    let t = (seed += 0x6d2b79f5);
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const rand = mulberry32(20261019);
const pick = a => a[Math.floor(rand() * a.length)];
const int = (lo, hi) => lo + Math.floor(rand() * (hi - lo + 1));

// ---------- Pilots ----------
/** The unpatched pilot, with one learner added. */
const fresh = name => { const P = loadPilot(PILOT, START); P.addStudent(name); return P; };
/** The pilot with the agreed changes, with one learner added. */
const agreed = name => applyAgreedChanges(fresh(name));
const AG = agreed("features").api;

// ---------- Checks ----------
const eq = (x, y) => JSON.stringify(x) === JSON.stringify(y);
function check(ok, what) { if (!ok) throw new Error("Sanity check failed: " + what); }

// ---------- Feature tags (inputs that touch an agreed change) ----------
const UNICODE_SPACE = /[\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000\uFEFF]/g;
/** Tags for a lesson text [target] (with its sentence-level [unscored] words) and recognizer outputs [texts]. */
function features({ target = null, texts = [], unscored = [] }) {
  const f = new Set();
  const all = target === null ? texts : [target, ...texts];
  for (const t of all) {
    if (/['\u2018\u2019]/.test(t)) f.add("apostrophe");
    if (/[^\x00-\x7F]/.test(t.replace(UNICODE_SPACE, "").replace(/[\u2018\u2019]/g, ""))) f.add("nonAscii");
    for (const tok of t.toLowerCase().replace(/-/g, " ").split(/\s+/)) {
      const s = tok.replace(/[^a-z0-9']/g, "");
      if (/^\d+$/.test(s) && +s > 10) f.add("bigNumber");
    }
  }
  for (const t of texts) {
    if (AG.agreedHasTag(t)) f.add("bracketTag");
    if (AG.agreedHeard(t).length === 0) f.add("nothingHeard");
  }
  if (target !== null) {
    const T = AG.agreedNormalize(target);
    if (T.some(w => AG.agreedUnscored({ unscored }, w))) f.add("unscored");
    if (texts.some(t => AG.agreedHeard(t).some(h => T.some(w => AG.agreedHomophones(w, h))))) f.add("homophone");
    if (target.includes("-")) f.add("hyphenInTarget");
    if (target.split(/\s+/).some(tok => tok.toLowerCase().replace(/[^a-z0-9']/g, "") === "")) f.add("emptyTargetToken");
  }
  return [...f].sort();
}
/** Tags where the agreed result may differ from the pilot's. */
const DEVIATIONS = ["apostrophe", "bigNumber", "bracketTag", "emptyTargetToken", "homophone", "hyphenInTarget", "nothingHeard", "unscored"];

// ---------- Heard-text variants ----------
const CONFUSABLE = {
  where: ["were", "wear", "vere", "ware"], is: ["es", "his", "iz"], the: ["da", "de", "a", "that"],
  bus: ["bas", "boss", "bass", "buzz"], stop: ["stap", "step", "top", "shop"],
  when: ["ven", "then", "wen"], will: ["vill", "well", "wheel"], come: ["came", "kam", "cum"],
  one: ["won", "1", "van"], ticket: ["tikat", "tickets", "picket", "tiket"], to: ["too", "two", "2", "ta"],
  patna: ["patana", "pattna", "putna", "partner"], please: ["plz", "plis", "police", "pleas"],
  how: ["hao", "who"], much: ["mach", "match", "march"], does: ["das", "dose", "this"],
  this: ["dis", "these", "the"], go: ["goes", "gone", "ko"], bettiah: ["betia", "betiya", "bethia", "betty", "beauty"],
};
const FILLERS = ["uh", "please", "sir", "the", "a", "excuse", "me", "bhaiya", "is", "bus"];
/** Recognizer outputs with nothing to score once tags and punctuation are removed. */
const NOTHING = ["", " ", "...", "?", "[BLANK_AUDIO]", "(music)", "[ Silence ]"];

function variant(text) {
  let words = text.toLowerCase().replace(/[.,?!]/g, "").split(/\s+/).filter(Boolean);
  const ops = int(0, 3);
  for (let k = 0; k < ops; k++) {
    const op = int(0, 6);
    const i = int(0, Math.max(0, words.length - 1));
    if (op === 0 && words.length) { const c = CONFUSABLE[words[i]]; if (c) words[i] = pick(c); }
    else if (op === 1 && words.length) words.splice(i, 1);
    else if (op === 2) words.splice(int(0, words.length), 0, pick(FILLERS));
    else if (op === 3 && words.length > 1 && i < words.length - 1) [words[i], words[i + 1]] = [words[i + 1], words[i]];
    else if (op === 4 && words.length) words.splice(i, 0, words[i]);
    else if (op === 5 && words.length > 1 && i < words.length - 1) words.splice(i, 2, words[i] + "-" + words[i + 1]);
    else if (op === 6 && words.length) words[i] = words[i].toUpperCase();
  }
  let out = words.join(" ");
  if (rand() < 0.3) out = out.charAt(0).toUpperCase() + out.slice(1) + pick([".", "?", "", "!"]);
  return out;
}

/** Heard text for a fixing step: correct most of the time, otherwise a variant (never empty by accident). */
function fixHeard(target, pCorrect) {
  if (rand() < pCorrect) return rand() < 0.5 ? target : target.toLowerCase().replace(/[.,?!]/g, "");
  const v = variant(target);
  if (v === target) {
    const rest = v.split(" ").slice(1).join(" ");
    return rest ? pick(["", "uh"]) + " " + rest : "uh";
  }
  return /[a-z0-9]/i.test(v) ? v : "uh";
}

// ---------- Helpers ----------
const STATUS = { ok: "CORRECT", wrong: "WRONG", missing: "MISSING" };
const statuses = res => res.map(r => STATUS[r.status]);
/** Which results count: false for unscored words. */
const scoredFlags = res => res.map(r => r.scored !== false);
/** Learner state after an event: today's XP and done list, plus everything that is not per day. */
function studentSnapshot(P) {
  const a = P.api, s = P.snapshot(a.St()), t = a.today();
  return {
    xpTotal: s.xpTotal, xpToday: s.xpByDay[t] || 0, doneToday: s.doneByDay[t] || [], streak: s.streak, lastDay: s.lastDay,
    pausedSince: s.pausedSince ?? null, celebratedOn: s.celebratedOn ?? null, best: s.best,
    revise: Object.entries(s.revise).map(([word, e]) => [word, e.box, e.due]),
    // "Practise again tomorrow" as it would show now; computed without storing pulled-back dates.
    tomorrow: P.snapshot(a.agreedTomorrowView()),
    view: P.snapshot(a.streakView()),
  };
}
/** The whole learner record, including every day's XP and done list. */
function fullSnapshot(P) {
  const s = P.snapshot(P.api.St());
  return { ...studentSnapshot(P), xpByDay: s.xpByDay, doneByDay: s.doneByDay };
}
/** Writes JSON with one case per line: compact, and diffs stay readable. */
function write(name, data) {
  fs.mkdirSync(OUT, { recursive: true });
  const { cases, ...rest } = data;
  let text = JSON.stringify(rest, null, 1);
  if (Array.isArray(cases)) {
    text = text.replace(/\n}$/, ',\n "cases": [\n' + cases.map(c => JSON.stringify(c)).join(",\n") + "\n ]\n}");
  }
  fs.writeFileSync(path.join(OUT, name), text + "\n");
  console.log(`${name}: ${Array.isArray(cases) ? cases.length + " cases" : "written"}, ${(Buffer.byteLength(text) / 1024).toFixed(0)} KB`);
}
const header = P => ({
  generatedBy: "tools/pilot-golden/generate.mjs",
  pilot: "docs/pilot/english-practice-pilot.html",
  config: P.snapshot(P.api.CONFIG),
});

// ---------- 0. The pilot's content with the agreed content changes (for the Kotlin side to rebuild the same lesson) ----------
{
  const P = agreed("content");
  write("content.json", { ...header(P), lesson: P.snapshot(P.api.LESSON), words: P.snapshot(P.api.WORDS), hints: P.snapshot(P.api.HINTS) });
}

// ---------- 1. Normalization: pilot normText, the one agreed normalizer, and recognizer output ----------
{
  const P = fresh("norm");
  const A = agreed("norm-agreed").api;
  const fixed = [
    "Where is the bus stop?", "  where   is the BUS-stop ", "1 ticket to patana, please", "One ticket to Patna, please.",
    "0", "1", "2", "9", "10", "11", "007", "0010", "00", "2nd", "3.5", "1,000", "#2", "$5", "20 rupees", "twenty-one",
    "don't", "Don\u2019t", "it's", "o'clock", "'quoted'", "rock-'n'-roll", "e-mail", "--", "-", "?", "", " ", "\t\n",
    "\u00A0bus\u2003stop\u3000", "Caf\u00E9", "na\u00EFve", "\u0130stanbul", "\u212A", "bus\u2014stop", "bus\u2013stop",
    "bus_stop", "bus/stop", "Bettiah?!", "PATNA...", "\u092C\u0938 \u0938\u094D\u091F\u0949\u092A", "bus \u092C\u0938 stop", "Where is the bus stop ?", "a-b-c",
    // Numbers: spelled out without "and" up to 999,999,999; longer digit strings stay.
    "12", "13", "15", "19", "20", "21", "30", "40", "45", "99", "100", "101", "110", "120", "999", "1000", "1001", "1100",
    "12345", "21000", "100000", "1000000", "1000001", "100000000", "999999999", "1000000000", "9999999999",
    "12345678901234567890", "2,500", "10,00,000", "1,000,000,000", "0000000001", "00000000000", "01000000000",
    "1-800", "21-22", "\u20B920", "Rs. 500", "platform 3", "10:30", "24/7", "1.5", "20th", "1st", "bus 12 stop",
    // Apostrophes: straight, curly, and quotes around words.
    "don\u2019t", "dont", "\u2018quoted\u2019", "\u201Cquoted\u201D", "\"quoted\"", "rock 'n' roll", "'", "\u2019", "''",
    "Patna's", "bus\u2019", "\u2018bus\u2019 \u2018stop\u2019", "ma'am", "y'all're", "5'",
    // Hyphens: "-" separates words; other dashes are punctuation.
    "bus-stop", "Bus-Stop", "-bus-", "bus--stop", "bus -stop", "bus- stop", "co-op", "one-two-three", "where-is-the-bus-stop",
    "bus\u2010stop", "bus\u2011stop", "bus\u2212stop",
    // Bracketed non-speech tags (removed from recognizer output only).
    "[BLANK_AUDIO]", "(music) where is the bus stop", "[ Silence ]", "(upbeat music)", "[MUSIC PLAYING]", "(music)",
    "where is the bus stop [BLANK_AUDIO]", "(laughs) one ticket (coughs) to patna", "[BLANK_AUDIO] [BLANK_AUDIO]",
    "bus[noise]stop", "bus [noise] stop", "[]", "()", "( )", "(nested (tags))", "[BLANK_AUDIO", "BLANK_AUDIO]", "[bus] stop",
    // Lone punctuation.
    "!", ".", ",", "...", "\u2014", "\u2013", "\u2026", "?!", "\u00BF", "\u0964", "- - -", " ? \u2014 ",
    // Whitespace: JavaScript's \s, so Unicode spaces count (and nothing else does).
    "bus\u2009stop", "bus\u202Fstop", "bus\u2007stop", "bus\uFEFFstop", "bus\u00A0stop", "bus\u1680stop", "bus\u2028stop",
    "bus\u000Bstop", "bus\u000Cstop", "bus\r\nstop", "bus\u200Bstop", "bus\u0085stop", "bus\u180Estop", "bus\u001Cstop",
    // Non-ASCII digits are not 0-9.
    "bus \u0663", "\uFF13 tickets",
  ];
  const palette = ["a", "b", "s", "t", "o", "p", "A", "B", "S", "0", "1", "2", "5", "10", "11", " ", " ", " ", "-", "'", "\u2019",
    ",", ".", "?", "!", "\u00A0", "\t", "\u00E9", "\u2014", "bus", "stop", "Patna", "one", "two"];
  const inputs = [...new Set(fixed)];
  for (let k = 0; k < 250; k++) inputs.push(Array.from({ length: int(0, 12) }, () => pick(palette)).join(""));
  const cases = inputs.map(input => ({
    input, pilot: P.snapshot(P.api.normText(input)), agreed: P.snapshot(A.agreedNormalize(input)),
    heard: P.snapshot(A.agreedHeard(input)), features: features({ texts: [input] }),
  }));
  for (const c of cases) {
    if (!c.features.includes("apostrophe") && !c.features.includes("bigNumber")) {
      check(eq(c.agreed, c.pilot), `normalize ${JSON.stringify(c.input)}: agreed ${JSON.stringify(c.agreed)} != pilot ${JSON.stringify(c.pilot)}`);
    }
  }
  // The examples written in docs/decisions.md.
  const norm = t => cases.find(c => c.input === t).agreed;
  check(eq(norm("21"), ["twenty", "one"]) && eq(norm("120"), ["one", "hundred", "twenty"]) && eq(norm("1,000"), ["one", "thousand"]), "number examples");
  check(eq(norm("don't"), ["dont"]) && eq(norm("Don\u2019t"), ["dont"]) && eq(norm("dont"), ["dont"]), "apostrophe examples");
  check(eq(norm("bus-stop"), ["bus", "stop"]) && eq(norm("?"), []) && eq(norm("\u2014"), []), "hyphen and punctuation examples");
  check(eq(A.agreedHeard("[BLANK_AUDIO]"), []) && eq(A.agreedHeard("(music)"), []), "non-speech tag examples");
  write("normalize.json", { ...header(P), cases });
}

// ---------- 2. Alignment on given word lists (pure pilot: DP + backtrace tie-breaking, accept lists) ----------
{
  const P = fresh("align");
  const a = P.api;
  const cases = [];
  const fixed = [
    [["where", "is", "the", "bus", "stop"], ["where", "is", "bas", "stop"], {}],
    [["a", "b"], ["b", "a"], {}], [["a", "b", "c"], ["c", "b", "a"], {}], [["a"], [], {}], [[], ["a"], {}], [[], [], {}],
    [["a", "b", "c"], ["a", "x", "b", "c"], {}], [["a", "b", "c"], ["x", "y", "z"], {}], [["a", "a", "a"], ["a"], {}],
    [["patna"], ["patana"], { patna: ["patana"] }], [["patna"], ["patna"], { patna: ["patana"] }], [["patana"], ["patna"], { patna: ["patana"] }],
  ];
  for (const [T, H, acc] of fixed) cases.push({ target: T, heard: H, accept: acc });
  for (let k = 0; k < 500; k++) {
    const alpha = ["a", "b", "c", "d", "e"].slice(0, int(2, 5));
    const T = Array.from({ length: int(0, 7) }, () => pick(alpha));
    const H = Array.from({ length: int(0, 7) }, () => pick([...alpha, "x", "y"]));
    const acc = {};
    if (rand() < 0.35) acc[pick(alpha)] = Array.from({ length: int(1, 2) }, () => pick(["x", "y", "a", "b"]));
    cases.push({ target: T, heard: H, accept: acc });
  }
  for (const c of cases) {
    const keys = Object.keys(c.accept);
    for (const k of keys) a.WORDS[k] = { accept: c.accept[k] };
    const r = a.align(c.target, c.heard);
    const sc = a.score(r.res);
    for (const k of keys) delete a.WORDS[k];
    Object.assign(c, { statuses: statuses(r.res), extra: r.extra, correct: sc.c, total: sc.n, stars: sc.stars });
  }
  write("align.json", { ...header(P), cases });
}

// ---------- 3. Sentence attempts on the bus lesson (agreed: one normalizer, nothing heard, word to fix) ----------
{
  const A = agreed("attempts");
  const P = fresh("attempts-pilot");
  const a = A.api;
  const curated = {
    bus_01: ["where is bas stop", "Where is the bus-stop?", "where is the busstop", "", "excuse me where is the bus stop please",
      "is where the bus stop", "where where is the bus stop", "where is the the bus stop", "WHERE IS THE BUS STOP",
      "where's the bus stop", "where is that bus stop", "were is the bus stop", "Where is the bus stop?",
      "Where is bas stop?", "where is bas stop.", "WHERE IS BAS STOP", "where is bas stop please", "where is the bas stop",
      "where is bas-stop", "where's bas stop", "where is bas stop [BLANK_AUDIO]", "(music) where is bas stop",
      "[BLANK_AUDIO]", "...", "(music)", "[ Silence ]", "(upbeat music)", "(music) where is the bus stop",
      "where is the bus stop [BLANK_AUDIO]", "?", " "],
    bus_02: ["when will bus come", "ven will the bus come", "when the bus will come", "when will the bus came", "When will the bus come?",
      "[BLANK_AUDIO]", "(music)", "when will the bas come", "(laughs) when will the bus come"],
    bus_03: ["1 ticket to patana please", "one ticket to patna please", "one ticket two patna please", "1 ticket 2 patna plz",
      "won ticket to putna please", "one tickets to patna", "ticket to patna", "one ticket to patna please please",
      "One ticket to Patna, please.", "...", "1 ticket to patna, please [BLANK_AUDIO]", "11 ticket to patna please",
      "one ticket to patna-please"],
    bus_04: ["how much is ticket", "how much the ticket", "how match is the ticket", "how much is the tickets", "ho much is the ticket",
      "[ Silence ]", "how much is the ticket (coughs)", "how much is the ticket?!"],
    bus_05: ["does this bus go to betiya", "does this bus go to bethia", "does this bus go to betty a", "does this bus goes to bettiah",
      "this bus go to bettiah", "does dis bus go to betia", "Does this bus go to Bettiah?", "(upbeat music)",
      "does this bus go to Bettiah (upbeat music)", "does this bus go to Bettiah's"],
  };
  const cases = [];
  a.LESSON.sentences.forEach((s, i) => {
    const heards = [...(curated[s.id] || [])];
    for (let k = 0; k < 60; k++) heards.push(variant(s.text));
    const targetWords = P.snapshot(a.agreedNormalize(s.text));
    for (const heard of heards) {
      a.S.idx = i;
      const out = a.handleAttempt(heard);
      const feats = features({ target: s.text, texts: [heard] });
      if (out.nothingHeard) {
        cases.push({ sentenceId: s.id, heard, nothingHeard: true, targetWords, statuses: [], scored: [], extra: [], correct: null, total: null,
          stars: null, xp: 0, wordToFix: null, wordToFixIndex: null, features: feats });
        continue;
      }
      const r = a.S.result, wf = a.agreedWordToFix();
      const c = { sentenceId: s.id, heard, nothingHeard: false, targetWords, statuses: statuses(r.res), scored: scoredFlags(r.res), extra: P.snapshot(r.extra),
        correct: r.c, total: r.n, stars: r.stars, xp: r.xp, wordToFix: wf ? wf.word : null, wordToFixIndex: wf ? wf.index : null, features: feats };
      cases.push(c);
      if (!feats.some(f => DEVIATIONS.includes(f))) {
        P.api.S.idx = i;
        P.api.handleAttempt(heard);
        const p = P.api.S.result;
        check(eq([c.statuses, c.extra, c.stars, c.xp], [statuses(p.res), p.extra, p.stars, p.xp]),
          `attempt ${s.id} ${JSON.stringify(heard)} differs from the pilot`);
      }
    }
  });
  // The owner's examples.
  const find = (id, heard) => cases.find(c => c.sentenceId === id && c.heard === heard);
  const bas = find("bus_01", "where is bas stop");
  check(eq(bas.statuses, ["CORRECT", "CORRECT", "MISSING", "WRONG", "CORRECT"]) && bas.wordToFix === "bus" && bas.wordToFixIndex === 3, "where is bas stop");
  // Patna is not scored now: "patana" is still WRONG, but it doesn't count.
  const patana = find("bus_03", "1 ticket to patana please");
  check(patana.correct === 4 && patana.total === 4 && patana.stars === 3 && eq(patana.scored, [true, true, true, false, true]), "1 ticket to patana please");
  // Whisper's "2" for "to" matches.
  const two = find("bus_03", "1 ticket 2 patna plz");
  check(two.statuses[2] === "CORRECT" && two.wordToFix === "please", "1 ticket 2 patna plz");
  check(find("bus_01", "[BLANK_AUDIO]").nothingHeard && find("bus_01", "...").nothingHeard && find("bus_01", "(music)").nothingHeard, "nothing heard");
  write("attempts.json", { ...header(P), cases });
}

// ---------- 4. Probes: sentences outside the bus lesson that hit edge cases ----------
{
  const A = agreed("probes");
  const P = fresh("probes-pilot");
  // [text, heards, chunks (default: the whole text), accept lists for this probe (default: none),
  //  words unscored in this sentence (default: none)]
  const probes = [
    ["Where is the bus-stop?", ["where is the bus stop", "where is the bus-stop", "where is the busstop"]],
    ["Twenty rupees, please.", ["20 rupees please", "twenty rupees please"]],
    ["Ten rupees, please.", ["10 rupees please", "ten rupees please"]],
    ["Don\u2019t go.", ["don't go", "dont go", "don\u2019t go"]],
    ["Don't go.", ["don't go", "dont go"]],
    ["Where is it ?", ["where is it", "where is"]],
    [" Where is it?", ["where is it"]],
    ["Caf\u00E9 is open.", ["cafe is open", "caf\u00E9 is open"]],
    // Hyphenated chunks: chunks are located on the normalized words.
    ["Where is the bus-stop?", ["where is the bas stop", "where is the bus stap", "where is the stop"], ["Where is", "the bus-stop"]],
    ["Is this the Patna-Bettiah bus?", ["is this the patna betty bus", "is this the patna bettiah bas"], ["Is this the Patna", "Bettiah bus"]],
    // A repeated word: the chunk that holds the fixed word's position.
    ["The bus to the bus stand", ["the bus to the bas stand", "the bas to the bus stand", "the bus to the bus stand", "to the bus stand",
      "the bus to the bus"], ["The bus", "to the bus stand"]],
    // A chunk that can't be located: the first chunk containing the word.
    ["One ticket to Patna, please.", ["one tiket to patna please", "one ticket to patna plis"], ["One ticket", "to Bettiah", "please"]],
    // The position is in no chunk (chunks leave a gap): the first chunk containing the word.
    ["The bus to the bus stand", ["the bus to the bas stand", "the bus ta the bus stand"], ["The bus", "stand"]],
    // No chunk contains the word: the full sentence.
    ["Where is the bus stop?", ["where is the bas stop"], ["Where is"]],
    // Numbers in the lesson text.
    ["Twenty one rupees, please.", ["21 rupees please", "twenty-one rupees please", "20 1 rupees please"]],
    ["Platform 12, please.", ["platform twelve please", "platform 12 please", "platform 2 please"], ["Platform 12", "please"]],
    // Nothing heard.
    ["Where is the bus stop?", ["[BLANK_AUDIO]", "...", "(music)", "(music) where is the bus stop"]],
    // Accept entries are normalized; an entry that is not one word is ignored.
    ["Twenty rupees, please.", ["twenty rupiya please", "twenty ru pees please", "twenty rupees please"], null, { rupees: ["Rupiya!", "ru-pees"] }],
    // Homophones: to/two/too, for/four, one/won match both ways, after numbers are spelled out.
    ["One ticket to Patna, please.", ["1 ticket 2 patna please", "won ticket too patna please", "one ticket two patna please",
      "one ticket ta patna please", "for ticket to patna please"]],
    ["Two tickets for Bettiah.", ["to tickets four bettiah", "2 tickets 4 betia", "too tickets for bettiah", "three tickets for bettiah"],
      ["Two tickets", "for Bettiah"]],
    // A sentence can turn scoring off for a word that is scored elsewhere.
    ["Where is the bus stop?", ["where is the bas stop", "where is bas stop", "where is the bus stap", "where bus stop"],
      ["Where is", "the bus stop"], null, ["bus"]],
    // Only unscored words wrong: 3 stars and nothing to fix.
    ["Does this bus go to Bettiah?", ["does this bus go to betty", "does this bus go to"]],
  ];
  const cases = [];
  for (const [text, heards, chunksIn, acceptIn, unscoredIn] of probes) {
    const chunks = chunksIn || [text], accept = acceptIn || {}, unscored = unscoredIn || [];
    const saved = {};
    for (const X of [A, P]) {
      X.api.LESSON.sentences.push({ id: "probe", text, hi: "", chunks, unscored });
      for (const [w, list] of Object.entries(accept)) { saved[w] = X.api.WORDS[w]; X.api.WORDS[w] = { accept: list }; }
    }
    const i = A.api.LESSON.sentences.length - 1;
    for (const heard of heards) {
      A.api.S.idx = i;
      const out = A.api.handleAttempt(heard);
      P.api.S.idx = i;
      P.api.handleAttempt(heard);
      const p = P.api.S.result;
      const c = { text, chunks, accept, unscored, heard, nothingHeard: out.nothingHeard, targetWords: P.snapshot(A.api.agreedNormalize(text)) };
      if (out.nothingHeard) {
        Object.assign(c, { statuses: [], scored: [], extra: [], stars: null, wordToFix: null });
      } else {
        const r = A.api.S.result, wf = A.api.agreedWordToFix();
        Object.assign(c, { statuses: statuses(r.res), scored: scoredFlags(r.res), extra: P.snapshot(r.extra), stars: r.stars, wordToFix: wf ? wf.word : null });
        // fixChunk / fixWordIndex only when there is a word to fix: PilotGoldenTest starts a
        // fixing loop whenever the "fixChunk" key is present, even if its value is null.
        if (wf) {
          A.api.startFix();
          Object.assign(c, { fixChunk: A.api.S.fix.chunk, fixWordIndex: A.api.S.fix.wordIndex });
        }
      }
      Object.assign(c, { pilotTargetWords: P.api.tokens(P.api.LESSON.sentences[i]).map(P.api.normTok), pilotStatuses: statuses(p.res),
        pilotStars: p.stars, features: features({ target: text, texts: [heard], unscored }) });
      cases.push(c);
    }
    for (const X of [A, P]) {
      X.api.LESSON.sentences.pop();
      for (const w of Object.keys(accept)) { if (saved[w] === undefined) delete X.api.WORDS[w]; else X.api.WORDS[w] = saved[w]; }
    }
  }
  const twice = cases.find(c => c.text === "The bus to the bus stand" && c.chunks.length === 2 && c.chunks[1] === "to the bus stand"
    && c.heard === "the bus to the bas stand");
  check(twice.wordToFix === "bus" && twice.fixWordIndex === 4 && twice.fixChunk === "to the bus stand", "repeated word: fix the second bus");
  const override = cases.find(c => c.unscored.includes("bus") && c.heard === "where is bas stop");
  check(override.wordToFix === "the" && eq(override.scored, [true, true, true, false, true]), "a sentence's unscored word is never the word to fix");
  write("probes.json", { ...header(P), cases });
}

// ---------- 5. Learner traces: attempts over many days, fixing loops, skips, goal screen ----------
{
  const JUMPS = new Set([4, 17, 30, 43, 56]); // traces with a 300-400 day jump forward and back
  const stat = { events: {}, dayChangesForward: 0, dayChangesBackward: 0, bigJumps: 0, jumpsBack: 0, openAppViews: {},
    pullBacks: 0, tracesWithPullBacks: 0, nothingHeardAttempts: 0, nothingHeardFixes: 0, earlierDayAttempts: 0,
    dropBacks: 0, doneScreens: 0, nextPicksDifferFromPilot: 0 };
  const traces = [];
  let header0 = null, dayChanges = 0;
  for (let t = 0; t < 80; t++) {
    const P = agreed("trace" + t);
    const a = P.api;
    header0 = header0 || header(P);
    const events = [];
    const ev = e => { events.push(e); stat.events[e.type] = (stat.events[e.type] || 0) + 1; if (e.screen === "done") stat.doneScreens++; };
    const leave = type => {
      // For the coverage count only: where the pilot's order would have gone.
      const len = a.LESSON.sentences.length, done = a.doneToday();
      let pilotNext = (a.S.idx + 1) % len;
      for (let k = len; k >= 1; k--) if (!done.includes(a.LESSON.sentences[(a.S.idx + k) % len].id)) pilotNext = (a.S.idx + k) % len;
      a.afterSentence();
      if (a.S.screen === "practice" && a.S.idx !== pilotNext) stat.nextPicksDifferFromPilot++;
      ev({ type, day: a.today(), screen: a.S.screen, index: a.S.idx, after: studentSnapshot(P) });
    };
    const n = int(3, 26);
    // Some learners follow the app's own flow (Start practice, then Next), so they reach
    // the daily goal; others open random sentences.
    const followFlow = t % 2 === 0;
    const jump = JUMPS.has(t), jumpAt = jump ? int(2, 5) : -1, backAt = jump ? jumpAt + int(2, 5) : -1;
    let jumpSize = 0, prevDay = a.today();
    for (let e = 0; e < Math.max(n, jump ? backAt + 6 : 0); e++) {
      let gap;
      if (e === jumpAt) { gap = jumpSize = int(300, 400); stat.bigJumps++; }
      else if (e === backAt) { gap = -jumpSize; stat.jumpsBack++; }
      else {
        const r = rand();
        gap = followFlow
          ? (r < 0.85 ? 0 : r < 0.95 ? 1 : int(2, 5))
          : (r < 0.55 ? 0 : r < 0.8 ? 1 : r < 0.92 ? int(2, 3) : int(4, 9));
        if (gap && ++dayChanges % 25 === 0) gap = -int(1, 3); // 4% of day changes: the phone's date goes back
        if (gap > 0) stat.dayChangesForward++;
        if (gap < 0) stat.dayChangesBackward++;
      }
      if (gap) P.advanceDays(gap);
      const day = a.today();
      const newDay = day !== prevDay;
      if (newDay) {
        a.openApp();
        const s = a.St(), view = P.snapshot(a.streakView());
        stat.openAppViews[view.status] = (stat.openAppViews[view.status] || 0) + 1;
        ev({ type: "openApp", day, view, pausedSince: s.pausedSince ?? null, streak: s.streak });
      }
      prevDay = day;

      let i;
      if (followFlow) i = e === 0 || newDay ? a.startIndex() : a.S.idx;
      else i = int(0, a.LESSON.sentences.length - 1);
      const text = a.LESSON.sentences[i].text;
      const r = rand();
      const heard = r < 0.03 ? pick(NOTHING) : r < 0.33 ? text : variant(text);
      a.S.idx = i;
      const before = JSON.stringify(a.St());
      const earlier = !!a.St().lastDay && day < a.St().lastDay;
      const out = a.handleAttempt(heard);
      if (earlier && !out.nothingHeard) stat.earlierDayAttempts++;
      if (out.nothingHeard) {
        check(JSON.stringify(a.St()) === before, "nothing heard changed the learner record");
        stat.nothingHeardAttempts++;
        ev({ type: "attempt", day, sentenceIndex: i, heard, nothingHeard: true, stars: null, wordToFix: null, wordToFixIndex: null,
          after: studentSnapshot(P) });
        continue;
      }
      const wf = a.agreedWordToFix();
      ev({ type: "attempt", day, sentenceIndex: i, heard, nothingHeard: false, stars: a.S.result.stars, wordToFix: wf ? wf.word : null,
        wordToFixIndex: wf ? wf.index : null, after: studentSnapshot(P) });

      if (!wf) {
        if (rand() < 0.5) leave("next");
        continue;
      }
      const roll = rand();
      if (roll < 0.2) { leave("skip"); continue; }
      if (roll < 0.35) continue; // leaves the feedback screen without fixing

      a.startFix();
      ev({ type: "startFix", day, word: a.S.fix.word, wordIndex: a.S.fix.wordIndex, chunk: a.S.fix.chunk, step: a.S.fix.step, fails: a.S.fix.fails });
      const pCorrect = pick([0.3, 0.6, 0.85]);
      for (let k = 0; k < 25 && a.S.screen === "fix"; k++) {
        const f = a.S.fix;
        if (f.step <= 2) {
          if (rand() < 0.05) { leave("skip"); break; }
          f.step++; // the pilot's "fixnext" button
          ev({ type: "fixNext", step: f.step, fails: f.fails });
        } else {
          if (rand() < 0.04) { leave("skip"); break; }
          const target = a.fixTarget();
          const heardFix = rand() < 0.03 ? pick(NOTHING) : fixHeard(target, pCorrect);
          const stepBefore = f.step, failsBefore = f.fails, recordBefore = JSON.stringify(a.St());
          const res = a.handleFix(heardFix);
          if (res.nothingHeard) {
            check(a.S.fix.step === stepBefore && a.S.fix.fails === failsBefore && JSON.stringify(a.St()) === recordBefore,
              "nothing heard changed the fixing loop");
            stat.nothingHeardFixes++;
          } else if (!res.passed && a.S.fix.step < stepBefore) stat.dropBacks++;
          ev({ type: "fixSay", day, target, heard: heardFix, nothingHeard: res.nothingHeard, passed: res.passed, step: a.S.fix.step,
            fails: a.S.fix.fails, screen: a.S.screen, index: a.S.idx, after: studentSnapshot(P) });
        }
      }
    }
    stat.pullBacks += a.agreedStats.pullBacks;
    if (a.agreedStats.pullBacks) stat.tracesWithPullBacks++;
    traces.push({ followFlow, events, final: fullSnapshot(P) });
  }
  write("traces.json", { ...header0, cases: traces });
  console.log("traces coverage: " + JSON.stringify(stat));
}

// ---------- 6. Sentence picking (agreed: a sentence with a due word first) ----------
{
  const A = agreed("picker");
  const P = fresh("picker-pilot");
  const a = A.api;
  const ids = a.LESSON.sentences.map(s => s.id);
  const words = [...new Set(a.LESSON.sentences.flatMap(s => P.snapshot(a.agreedNormalize(s.text))))];
  const cases = [];
  for (let k = 0; k < 300; k++) {
    const done = ids.filter(() => rand() < 0.5);
    if (rand() < 0.3) { // shuffle (Fisher-Yates with the seeded rand, so any JS engine gives the same order)
      for (let i = done.length - 1; i > 0; i--) { const j = Math.floor(rand() * (i + 1)); [done[i], done[j]] = [done[j], done[i]]; }
    }
    const current = int(0, ids.length - 1);
    const dueWords = rand() < 0.35 ? [] : words.filter(() => rand() < 0.12);
    if (rand() < 0.15) dueWords.splice(int(0, dueWords.length), 0, "zebra"); // in no sentence
    const notDueWords = words.filter(w => !dueWords.includes(w) && rand() < 0.1);
    const s = a.St(), t = a.today(), tomorrow = a.addDays(1);
    s.revise = {};
    for (const w of dueWords) s.revise[w] = { box: 1, due: t };
    for (const w of notDueWords) s.revise[w] = { box: 1, due: tomorrow };
    s.doneByDay[t] = done;
    a.S.idx = current;
    const next = a.nextIndex(), start = a.startIndex();
    if (!dueWords.length) {
      P.api.St().doneByDay[t] = done;
      P.api.S.idx = current;
      check(next === P.api.nextIndex(), `picker ${JSON.stringify({ current, done })}: next differs from the pilot`);
    }
    cases.push({ current, doneToday: done, dueWords, notDueWords, next, start });
  }
  write("picker.json", { ...header(A), cases });
}
