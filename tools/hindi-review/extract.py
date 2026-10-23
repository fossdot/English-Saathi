#!/usr/bin/env python3
"""Collects every Hindi line in English Saathi into one sheet for the teacher review.

Writes docs/hindi-review.csv (UTF-8 with BOM, so Excel shows the Hindi):
one row per Hindi text, with where it is, the English beside it, and whether
the web pilot already had it ("pilot") or it was written for the desktop app
("drafted"). Run from the repo root: python3 -I tools/hindi-review/extract.py
"""
import csv
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
DEVANAGARI = re.compile(r"[ऀ-ॿ]")
PILOT = (ROOT / "docs/pilot/english-practice-pilot.html").read_text(encoding="utf-8")
SOURCES = sorted((ROOT / "desktop/src/main/kotlin").rglob("*.kt")) + [
    ROOT / "domain/src/main/kotlin/net/bodhya/speak/domain/Content.kt",
    ROOT / "desktop/build.gradle.kts",
]


def origin(hindi):
    """'pilot' when every fixed part of the text (between ${...}) is in the pilot.

    A part shorter than 4 letters must match a whole quoted text there, so a
    short word doesn't match by chance inside a longer one.
    """
    parts = [p.strip(" ,.:।") for p in re.split(r"\$\{[^}]*\}|\$\w+", hindi)]
    parts = [p for p in parts if DEVANAGARI.search(p)]

    def in_pilot(part):
        if len(part) >= 4:
            return part in PILOT
        return any(f"{q}{part}{e}" in PILOT for q, e in [('"', '"'), ("'", "'"), (">", "<")])

    return "pilot" if parts and all(in_pilot(p) for p in parts) else "drafted"


def word_origin(word, hint):
    """'pilot' when the pilot's word table has the word with the same hint."""
    pattern = r"\b" + re.escape(word) + r':\s*\{[^}]*\bhi:\s*"' + re.escape(hint) + '"'
    return "pilot" if re.search(pattern, PILOT) else "drafted"


def literals(line):
    """The Kotlin string literals on one line, with their start, as written.

    Follows ${...} templates, so quotes inside them ("lesson" in
    ${if (n == 1) "lesson" else "lessons"}) don't end the outer string.
    """
    found = []

    def string(i):  # line[i] is the opening quote; returns the index after the closing one
        j = i + 1
        while j < len(line):
            c = line[j]
            if c == "\\":
                j += 2
            elif c == '"':
                return j + 1
            elif line.startswith("${", j):
                j = template(j + 2)
            else:
                j += 1
        return j

    def template(j):  # just after "${"; returns the index after the matching "}"
        depth = 1
        while j < len(line) and depth:
            c = line[j]
            if c == '"':
                j = string(j)
                continue
            depth += {"{": 1, "}": -1}.get(c, 0)
            j += 1
        return j

    i = 0
    while i < len(line):
        if line.startswith("//", i):
            break
        if line[i] == '"':
            end = string(i)
            found.append((i, line[i + 1:end - 1].replace('\\"', '"')))
            i = end
        else:
            i += 1
    return found


# Hindi whose English isn't a string beside it: what it labels.
OVERRIDES = {
    "तेज़": "fast (the tiny.en speech model's button)",
    "ज़्यादा सही": "more accurate (the small.en speech model's button)",
    "चलाइए": "Play (a test clip's button)",
    "यह माइक": "this microphone (a microphone's button)",
    "Mac में लगा माइक": "the Mac's built-in microphone (its button)",
    "“${p.titleHi}”, संस्करण ${p.version}": "“${p.titleEn}”, version ${p.version}",
    " (संस्करण $it की जगह)": " (replaces version $it)",
}


def is_english(text):
    return not DEVANAGARI.search(text) and len(re.findall(r"[A-Za-z]{2,}", text)) > 0


def code_rows():
    """Yields [where, english, hindi, from, how sure the English is (0-2)]."""
    for path in SOURCES:
        lines = path.read_text(encoding="utf-8").splitlines()
        rel = path.relative_to(ROOT)
        function = ""
        for i, line in enumerate(lines):
            if line.lstrip().startswith(("//", "*", "/*")):
                continue
            named = re.search(r"\bfun (\w+)", line)
            if named:
                function = named.group(1)
            strings = literals(line)
            for start, hindi in strings:
                if not DEVANAGARI.search(hindi):
                    continue
                # The English is the nearest English string before it on the
                # same line; in a `X -> "…"` line, X; else up to 3 lines above.
                english, sure = "", 0
                before = [t for s0, t in strings if s0 < start and is_english(t)]
                arrow = re.match(r"\s*(.+?)\s*->\s*\"", line)
                if hindi in OVERRIDES:
                    english, sure = OVERRIDES[hindi], 2
                elif before:
                    english, sure = before[-1], 2
                elif re.match(r"\s*CardSymbol\.(\w+)\s*->", line):
                    name = re.match(r"\s*CardSymbol\.(\w+)", line).group(1).lower()
                    english, sure = f"{name} (a card's picture, named by the facilitator)", 2
                elif function == "helpText" and not arrow:
                    english, sure = "(the Hindi help button, after a sentence is scored)", 1
                elif arrow and function == "helpText":
                    case = arrow.group(1).replace("is ", "").replace("Screen.", "")
                    step = "5" if case == "else" else case
                    where = f"fixing step {step}" if step.isdigit() else f"the {case} screen"
                    english, sure = f"(the Hindi help button, on {where})", 1
                elif arrow:
                    english, sure = f"({arrow.group(1)})", 1
                elif re.search(r"[A-Za-z]{2,}.* · [\u0900-\u097F]|\([\u0900-\u097F][^)]*\)\s*$", hindi):
                    english, sure = "(in the same text)", 1
                elif (pair := re.fullmatch(r'\s*(?:en\s*=\s*)?"(.*)",?\s*', lines[i - 1])) and is_english(pair.group(1)):
                    # Message(\n "English",\n "Hindi",\n): the line above is the pair.
                    english, sure = literals(lines[i - 1])[0][1], 2
                else:
                    for j in range(i - 1, max(i - 4, -1), -1):
                        above = [t for _, t in literals(lines[j]) if is_english(t)]
                        if above:
                            english = f"nearby: {above[-1]}"
                            break
                yield [f"{rel}:{i + 1}", english, hindi, origin(hindi), sure]


def pack_rows():
    path = ROOT / "content/packs/starter.json"
    pack = json.loads(path.read_text(encoding="utf-8-sig"))
    rel = path.relative_to(ROOT)
    yield [f"{rel}: pack title", pack["title_en"], pack["title_hi"], origin(pack["title_hi"])]
    for tag, hint in pack.get("hints", {}).items():
        yield [f"{rel}: tip “{tag}”", "", hint["text_hi"], origin(hint["text_hi"])]
    for lesson in pack["lessons"]:
        yield [f"{rel}: lesson {lesson['id']}", lesson["title_en"], lesson["title_hi"], origin(lesson["title_hi"])]
        for s in lesson["sentences"]:
            yield [f"{rel}: sentence {s['id']}", s["text"], s["hi"], origin(s["hi"])]
        for word, w in lesson["words"].items():
            if w.get("hint_hi"):
                syllables = w.get("syllables", [])
                english = f"{word} ({'-'.join(syllables)})" if len(syllables) > 1 else word
                yield [f"{rel}: word “{word}”", english, w["hint_hi"], word_origin(word, w["hint_hi"])]


def main():
    rows = list(pack_rows())
    # The same Hindi in several places is one row, listing every place.
    merged = {}
    for where, english, hindi, source, sure in code_rows():
        if hindi in merged:
            row = merged[hindi]
            row[0] += f"; {where}"
            if sure > row[4]:
                row[1], row[4] = english, sure
        else:
            merged[hindi] = [where, english, hindi, source, sure]
    rows += [row[:4] for row in merged.values()]
    out = ROOT / "docs/hindi-review.csv"
    with out.open("w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(["no", "where", "english", "hindi", "from", "ok (yes/no)", "teacher's correction", "note"])
        for n, row in enumerate(rows, 1):
            w.writerow([n, *row, "", "", ""])
    drafted = sum(1 for r in rows if r[3] == "drafted")
    print(f"{out.relative_to(ROOT)}: {len(rows)} rows, {drafted} drafted, {len(rows) - drafted} from the pilot")


if __name__ == "__main__":
    main()
