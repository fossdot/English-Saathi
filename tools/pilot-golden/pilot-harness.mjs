// Runs the web pilot's own JavaScript (docs/pilot/english-practice-pilot.html)
// inside a Node vm with minimal DOM stubs and a controllable clock, so its
// domain behaviour can be recorded as golden test cases for the Kotlin port.
//
// Run with TZ=Asia/Kolkata so local-date arithmetic is deterministic (no DST).

import fs from "node:fs";
import vm from "node:vm";

const EXPORTS = [
  "St", "CONFIG", "WORDS", "HINTS", "LESSON",
  "normTok", "normText", "same", "align", "score",
  "markDone", "updateRevision", "addXP", "doneToday", "xpToday",
  "today", "addDays", "dayDiff",
  "handleAttempt", "startFix", "handleFix", "afterSentence", "nextIndex",
  "firstWrong", "fixTarget", "sent", "tokens", "addStudent", "celebrated",
];

function stubElement() {
  return {
    innerHTML: "", textContent: "", value: "", dataset: {},
    addEventListener() {}, focus() {}, querySelector() { return null; },
    appendChild() {}, remove() {}, click() {},
  };
}

/**
 * Loads a fresh copy of the pilot (empty storage) with the clock set to
 * [startMs]. Returns the pilot's functions/state plus clock controls.
 */
export function loadPilot(htmlPath, startMs) {
  const html = fs.readFileSync(htmlPath, "utf8");
  const scripts = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)];
  if (scripts.length !== 1) throw new Error(`expected 1 inline <script>, found ${scripts.length}`);
  const src = scripts[0][1];

  let nowMs = startMs;
  const RealDate = Date;
  class FakeDate extends RealDate {
    constructor(...a) { if (a.length === 0) super(nowMs); else super(...a); }
    static now() { return nowMs; }
  }

  const elements = { app: stubElement(), live: stubElement(), newname: stubElement() };
  const storage = {};
  const window = { scrollTo() {} };
  const ctx = vm.createContext({
    window,
    document: {
      getElementById: id => elements[id] || null,
      addEventListener() {}, createElement: () => stubElement(),
      body: stubElement(), activeElement: null,
    },
    localStorage: {
      getItem: k => (k in storage ? storage[k] : null),
      setItem: (k, v) => { storage[k] = String(v); },
    },
    Date: FakeDate,
    setTimeout: f => f(),
    URL: { createObjectURL: () => "" },
    Blob: class {},
    console,
  });
  window.window = window;

  const exportCode = "\n;globalThis.__pilot = { get S() { return S; }, get D() { return D; }, " +
    EXPORTS.map(n => `${n}: ${n}`).join(", ") + " };";
  vm.runInContext(src + exportCode, ctx, { filename: "english-practice-pilot.js" });

  const api = ctx.__pilot;
  return {
    api,
    /** The pilot's vm context. Its top-level function declarations are properties of it. */
    ctx,
    /**
     * Runs more code in the pilot's context and returns its value. The code sees the
     * pilot's top-level names (including const ones), and assigning one of the pilot's
     * global functions changes what the pilot's own code calls.
     */
    run(code, filename = "overlay.js") { return vm.runInContext(code, ctx, { filename }); },
    /** Moves the fake clock to [ms]. */
    setNow(ms) { nowMs = ms; },
    /** Moves the fake clock forward by whole days (keeps the time of day). */
    advanceDays(n) { const d = new RealDate(nowMs); d.setDate(d.getDate() + n); nowMs = d.getTime(); },
    /** Adds a student through the pilot's own addStudent() and selects them. */
    addStudent(name) { elements.newname.value = name; nowMs += 1; api.addStudent(); },
    /** Deep copy of plain pilot data (strips functions / prototypes). */
    snapshot(v) { return JSON.parse(JSON.stringify(v)); },
  };
}
