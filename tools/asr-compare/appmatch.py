"""The app's word matching (normalizer + aligner), ported from the Kotlin domain.

Used to score recognizer output exactly the way the app will. `python
appmatch.py` checks this port against the golden cases that the Kotlin tests
use (domain/src/test/resources/pilot-golden).
"""

import json
import re
import sys
from pathlib import Path

# JavaScript's \s, as in Normalizer.kt.
WHITESPACE = re.compile(
    "[\t\n\u000b\f\r \u00a0\u1680\u2000-\u200a\u2028\u2029\u202f\u205f\u3000\ufeff]+"
)
NON_SPEECH_TAG = re.compile(r"\[[^\[\]]*\]|\([^()]*\)")

SMALL = [
    "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
]
TENS = ["", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"]


def _words(n):
    if n < 20:
        return [SMALL[n]]
    if n < 100:
        return [TENS[n // 10]] + _rest(n % 10)
    if n < 1000:
        return [SMALL[n // 100], "hundred"] + _rest(n % 100)
    if n < 1_000_000:
        return _words(n // 1000) + ["thousand"] + _rest(n % 1000)
    return _words(n // 1_000_000) + ["million"] + _rest(n % 1_000_000)


def _rest(n):
    return [] if n == 0 else _words(n)


def spell(digits):
    significant = digits.lstrip("0")
    if len(significant) > 9:
        return None
    return _words(int(significant or "0"))


def token_words(token):
    out = []
    for part in token.lower().split("-"):
        kept = "".join(c for c in part if "a" <= c <= "z" or "0" <= c <= "9")
        if not kept:
            continue
        if all("0" <= c <= "9" for c in kept):
            out.extend(spell(kept) or [kept])
        else:
            out.append(kept)
    return out


def normalize(text):
    return [w for token in WHITESPACE.split(text) for w in token_words(token)]


def normalize_heard(text):
    while True:
        stripped = NON_SPEECH_TAG.sub(" ", text)
        if stripped == text:
            return normalize(text)
        text = stripped


def align(target, heard, accept=None):
    """Statuses (CORRECT/WRONG/MISSING) per target word, plus extra heard words."""
    accept = accept or {}

    def same(t, h):
        return t == h or h in accept.get(t, ())

    n, m = len(target), len(heard)

    def cost(i, j):
        return 0 if same(target[i - 1], heard[j - 1]) else 1

    d = [[0] * (m + 1) for _ in range(n + 1)]
    for i in range(n + 1):
        d[i][0] = i
    for j in range(m + 1):
        d[0][j] = j
    for i in range(1, n + 1):
        for j in range(1, m + 1):
            d[i][j] = min(d[i - 1][j - 1] + cost(i, j), d[i - 1][j] + 1, d[i][j - 1] + 1)

    statuses = ["MISSING"] * n
    extra = []
    i, j = n, m
    while i > 0 or j > 0:
        if i > 0 and j > 0 and d[i][j] == d[i - 1][j - 1] + cost(i, j):
            statuses[i - 1] = "CORRECT" if same(target[i - 1], heard[j - 1]) else "WRONG"
            i -= 1
            j -= 1
        elif i > 0 and d[i][j] == d[i - 1][j] + 1:
            statuses[i - 1] = "MISSING"
            i -= 1
        else:
            extra.insert(0, heard[j - 1])
            j -= 1
    return statuses, extra


def _self_test():
    golden = Path(__file__).resolve().parents[2] / "domain/src/test/resources/pilot-golden"
    failures = 0
    cases = json.loads((golden / "normalize.json").read_text())["cases"]
    for c in cases:
        if normalize(c["input"]) != c["agreed"] or normalize_heard(c["input"]) != c["heard"]:
            failures += 1
            print("normalize mismatch:", repr(c["input"]), normalize(c["input"]), c["agreed"])
    aligns = json.loads((golden / "align.json").read_text())["cases"]
    for c in aligns:
        accept = {k: set(v) for k, v in c["accept"].items()}
        statuses, extra = align(c["target"], c["heard"], accept)
        if statuses != c["statuses"] or extra != c["extra"]:
            failures += 1
            print("align mismatch:", c["target"], c["heard"], statuses, c["statuses"])
    print(f"{len(cases)} normalize cases, {len(aligns)} align cases, {failures} failures")
    return failures


if __name__ == "__main__":
    sys.exit(1 if _self_test() else 0)
