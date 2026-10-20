"""Writes cases.json: the sentences for Task A and the mistake cases for Task B.

Task B's "first mistake" comes from the app's own aligner (tools/asr-compare/appmatch.py),
using transcripts that Whisper tiny.en actually produced for the mistake clips in
docs/asr-model-comparison.md, so the LLM gets exactly what the app would know.
"""

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "asr-compare"))
from appmatch import align, normalize, normalize_heard  # noqa: E402

PILOT = {
    "bus_01": {"text": "Where is the bus stop?", "hi": "बस स्टॉप कहाँ है?", "chunks": ["Where is", "the bus stop"]},
    "bus_02": {"text": "When will the bus come?", "hi": "बस कब आएगी?", "chunks": ["When will", "the bus come"]},
    "bus_03": {"text": "One ticket to Patna, please.", "hi": "पटना का एक टिकट दीजिए।", "chunks": ["One ticket", "to Patna", "please"]},
    "bus_04": {"text": "How much is the ticket?", "hi": "टिकट कितने का है?", "chunks": ["How much", "is the ticket"]},
    "bus_05": {"text": "Does this bus go to Bettiah?", "hi": "क्या यह बस बेतिया जाती है?", "chunks": ["Does this bus", "go to Bettiah"]},
}

# Task A: the four bus-stand sentences the pilot already has human versions of
# (so a reviewer can compare), plus six new ones from the locked situations.
TASK_A = [
    ("bus_02", "When will the bus come?"),
    ("bus_03", "One ticket to Patna, please."),
    ("bus_04", "How much is the ticket?"),
    ("bus_05", "Does this bus go to Bettiah?"),
    ("market_01", "How much is one kilo of rice?"),
    ("market_02", "Please give me two kilos of onions."),
    ("bank_01", "I want to open a bank account."),
    ("bank_02", "Where do I sign?"),
    ("hospital_01", "My child has had a fever since yesterday."),
    ("hospital_02", "Which doctor should I see?"),
]

# Task B: (sentence id, what Whisper heard) from real transcripts of the mistake clips.
TASK_B = [
    ("bus_01", "Where is bus stop?"),
    ("bus_01", "Where is the boss stop?"),
    ("bus_01", "Where is the bus shop?"),
    ("bus_02", "When the bus will come?"),
    ("bus_03", "Van ticket to Patna, please."),
    ("bus_04", "How much the ticket?"),
    ("bus_05", "Does this bus goes to Bettiah?"),
    ("bus_05", "Do this bus go to Bettiah?"),
]


def first_mistake(target_text, heard_text):
    target = normalize(target_text)
    heard = normalize_heard(heard_text)
    statuses, extra = align(target, heard)
    heard_as = dict(pair_words(target, heard, statuses))
    for i, (word, status) in enumerate(zip(target, statuses)):
        if status == "WRONG":
            return {"index": i, "word": word, "status": status, "heard_as": heard_as.get(i)}
    for i, (word, status) in enumerate(zip(target, statuses)):
        if status == "MISSING":
            return {"index": i, "word": word, "status": status, "heard_as": None}
    return None


def pair_words(target, heard, statuses):
    """Which heard word landed on each WRONG target word (re-walks the alignment)."""
    # Same DP as appmatch.align, but keeping the matched heard word.
    n, m = len(target), len(heard)
    d = [[0] * (m + 1) for _ in range(n + 1)]
    for i in range(n + 1):
        d[i][0] = i
    for j in range(m + 1):
        d[0][j] = j
    for i in range(1, n + 1):
        for j in range(1, m + 1):
            cost = 0 if target[i - 1] == heard[j - 1] else 1
            d[i][j] = min(d[i - 1][j - 1] + cost, d[i - 1][j] + 1, d[i][j - 1] + 1)
    pairs = []
    i, j = n, m
    while i > 0 or j > 0:
        if i > 0 and j > 0 and d[i][j] == d[i - 1][j - 1] + (0 if target[i - 1] == heard[j - 1] else 1):
            if statuses[i - 1] == "WRONG":
                pairs.append((i - 1, heard[j - 1]))
            i -= 1
            j -= 1
        elif i > 0 and d[i][j] == d[i - 1][j] + 1:
            i -= 1
        else:
            j -= 1
    return pairs


def main():
    cases = {
        "example": {"id": "bus_01", **PILOT["bus_01"], "words": {
            "where": {"syllables": ["where"], "hint_hi": "वेयर", "hint_tag": "v_w"},
            "the": {"syllables": ["the"], "hint_hi": "द", "hint_tag": "th"},
            "bus": {"syllables": ["bus"], "hint_hi": "बस", "hint_tag": "short_vowel"},
            "stop": {"syllables": ["stop"], "hint_hi": "स्टॉप", "hint_tag": "s_cluster"},
        }},
        "task_a": [{"id": i, "text": t, "pilot_hi": PILOT.get(i, {}).get("hi"), "pilot_chunks": PILOT.get(i, {}).get("chunks")} for i, t in TASK_A],
        "task_b": [],
    }
    for sid, heard in TASK_B:
        s = PILOT[sid]
        mistake = first_mistake(s["text"], heard)
        assert mistake, (sid, heard)
        cases["task_b"].append({"id": f"{sid}-{len(cases['task_b']) + 1}", "text": s["text"], "hi": s["hi"], "heard": heard, "mistake": mistake})
    out = Path(__file__).with_name("cases.json")
    out.write_text(json.dumps(cases, ensure_ascii=False, indent=1))
    for c in cases["task_b"]:
        print(c["id"], c["heard"], "->", c["mistake"])


if __name__ == "__main__":
    main()
