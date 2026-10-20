"""Scores the recorded transcripts the way the app would, and prints Markdown tables.

    python score.py --models WORK/models --audio WORK/audio --results WORK/results > report.md
"""

import argparse
import json
import math
import statistics
import zlib
from collections import Counter, defaultdict
from pathlib import Path

from appmatch import align, normalize, normalize_heard

PLACES = {"patna", "bettiah"}

# The rules the owner decided after milestone b (docs/decisions.md), used with --app-rules:
# place names are not scored, and these groups count as the same word.
UNSCORED = {"patna", "bettiah"}
HOMOPHONES = [{"to", "two", "too"}, {"for", "four"}, {"one", "won"}]
ACCEPT = {w: group - {w} for group in HOMOPHONES for w in group}


def pct(n, d):
    return f"{100 * n / d:.1f}%" if d else "–"


def sign_test(a, b):
    """Two-sided exact binomial test of a vs b successes at p = 0.5."""
    n = a + b
    if n == 0:
        return 1.0
    k = min(a, b)
    tail = sum(math.comb(n, i) for i in range(k + 1)) / 2**n
    return min(1.0, 2 * tail)


def deflated_mb(paths):
    total = 0
    for p in paths:
        total += len(zlib.compress(p.read_bytes(), 6))
    return total / 1e6


def load(results_dir, audio_dir):
    manifest = {r["clip"]: r for r in map(json.loads, open(audio_dir / "manifest.jsonl"))}
    runs = {}
    for summary_path in sorted(results_dir.glob("*.summary.json")):
        summary = json.loads(summary_path.read_text())
        name = summary_path.name[: -len(".summary.json")]
        rows = [json.loads(line) for line in open(results_dir / f"{name}.jsonl")]
        runs[name] = (summary, [(manifest[r["clip"]], r) for r in rows])
    return runs


def score_clip(meta, result, app_rules=False):
    heard = normalize_heard(result["text"])
    out = {"heard": heard, "nothing": not heard}
    if meta["kind"] == "nonspeech":
        return out
    target = normalize(meta["target"])
    accept = ACCEPT if app_rules else None
    statuses, extra = align(target, heard, accept)
    expected, _ = align(target, normalize(meta["said"]), accept)
    if app_rules:
        # Unscored words stay in the alignment but are dropped from every count.
        keep = [w not in UNSCORED for w in target]
        target = [w for w, k in zip(target, keep) if k]
        statuses = [st for st, k in zip(statuses, keep) if k]
        expected = [st for st, k in zip(expected, keep) if k]
    out.update(target=target, statuses=statuses, extra=extra, expected=expected)
    return out


def summarize(clips):
    s = defaultdict(int)
    unexpected = Counter()
    for meta, result, sc in clips:
        if meta["kind"] == "nonspeech":
            s["ns"] += 1
            s["ns_nothing"] += sc["nothing"]
            continue
        if meta["kind"] == "correct":
            s["clips"] += 1
            s["words"] += len(sc["target"])
            ok = [st == "CORRECT" for st in sc["statuses"]]
            s["correct"] += sum(ok)
            s["all_ok"] += all(ok)
            for w, good in zip(sc["target"], ok):
                if w in PLACES:
                    s["place"] += 1
                    s["place_ok"] += good
                else:
                    s["other"] += 1
                    s["other_ok"] += good
            s["nothing_on_speech"] += sc["nothing"]
            if meta["sentence"] in ("bus_03", "bus_05"):
                unexpected.update(w for w in sc["heard"] if w not in sc["target"])
        else:
            s["m_clips"] += 1
            for w, exp, got in zip(sc["target"], sc["expected"], sc["statuses"]):
                if exp != "CORRECT":
                    s["m_planted"] += 1
                    s["m_caught"] += got != "CORRECT"
                elif w not in PLACES:
                    s["m_other"] += 1
                    s["m_false"] += got != "CORRECT"
            s["m_exact"] += sc["statuses"] == sc["expected"]
    return s, unexpected


def latency(clips):
    speech = [(r["decode_s"], m["seconds"]) for m, r, _ in clips if m["kind"] != "nonspeech"]
    times = sorted(t for t, _ in speech)
    p90 = times[int(0.9 * (len(times) - 1))]
    rtf = statistics.mean(t / sec for t, sec in speech)
    three = [t for t, sec in speech if 2.5 <= sec <= 3.5]
    return statistics.median(times), p90, rtf, (statistics.median(three) if three else None)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--models", required=True)
    ap.add_argument("--audio", required=True)
    ap.add_argument("--results", required=True)
    ap.add_argument("--threads", type=int, default=2)
    ap.add_argument("--app-rules", action="store_true",
                    help="score as the app will: place names unscored, to/two/too, for/four, one/won equal")
    args = ap.parse_args()
    models, audio_dir = Path(args.models), Path(args.audio)
    runs = load(Path(args.results), audio_dir)

    scored = {}
    for name, (summary, rows) in runs.items():
        if summary["threads"] != args.threads:
            continue
        scored[name] = (summary, [(m, r, score_clip(m, r, args.app_rules)) for m, r in rows])

    rules = " (app rules: place names unscored, homophones equal)" if args.app_rules else ""
    print(f"## Overview ({args.threads} threads){rules}\n")
    print("| Model | Files MB | Zipped MB | Words right | Sentences all right | Place names right | "
          "Other words right | Mistakes caught | False alarms | Silence/noise → nothing | "
          "Median ms | p90 ms | 3 s clips ms | RTF | Load s | Peak RAM +MB |")
    print("|---" * 16 + "|")
    for name, (summary, clips) in scored.items():
        s, _ = summarize(clips)
        files = [models / f for f in summary["files"] if (models / f).exists()]
        mb = sum(summary["files"].values()) / 1e6
        med, p90, rtf, three = latency(clips)
        ns = [c for c in clips if c[0]["kind"] == "nonspeech" and c[0]["condition"] != "babble"]
        ns_nothing = sum(c[2]["nothing"] for c in ns)
        zipped = f"{deflated_mb(files):.1f}" if files else "–"
        ram = f"{summary['rss_peak_mb'] - summary['rss_before_mb']:.0f}" if summary.get("rss_before_mb") is not None else "–"
        print(f"| {summary['config']} | {mb:.1f} | {zipped} | {pct(s['correct'], s['words'])} | "
              f"{pct(s['all_ok'], s['clips'])} | {pct(s['place_ok'], s['place'])} | {pct(s['other_ok'], s['other'])} | "
              f"{pct(s['m_caught'], s['m_planted'])} | {pct(s['m_false'], s['m_other'])} | {ns_nothing}/{len(ns)} | "
              f"{med * 1000:.0f} | {p90 * 1000:.0f} | {three * 1000:.0f} | {rtf:.3f} | {summary['load_s']:.2f} | {ram} |")

    def breakdown(title, key, kind="correct"):
        values = sorted({c[0][key] for _, cl in scored.values() for c in cl if c[0]["kind"] == kind})
        print(f"\n## Words right by {title} (correct readings)\n")
        print("| Model | " + " | ".join(values) + " |")
        print("|---" * (len(values) + 1) + "|")
        for name, (summary, clips) in scored.items():
            cells = []
            for v in values:
                sub = [c for c in clips if c[0]["kind"] == kind and c[0][key] == v]
                s, _ = summarize(sub)
                cells.append(f"{pct(s['correct'], s['words'])} ({pct(s['all_ok'], s['clips'])})")
            print(f"| {summary['config']} | " + " | ".join(cells) + " |")
        print("\nEach cell: words right (sentences fully right).")

    breakdown("voice", "voice")
    breakdown("speaking style", "style")
    breakdown("recording condition", "condition")

    print("\n## What was heard instead of the place names (correct readings of sentences 3 and 5)\n")
    for name, (summary, clips) in scored.items():
        _, unexpected = summarize(clips)
        common = ", ".join(f"{w} ×{n}" for w, n in unexpected.most_common(12))
        print(f"- **{summary['config']}:** {common or 'nothing unexpected'}")

    print("\n## Mistakes caught, per planted mistake (8 clips each: 4 voices × clean/noisy)\n")
    said = sorted({c[0]["said"] for _, cl in scored.values() for c in cl if c[0]["kind"] == "mistake"})
    names = list(scored)
    print("| Said | " + " | ".join(scored[n][0]["config"] for n in names) + " |")
    print("|---" * (len(names) + 1) + "|")
    for text in said:
        cells = []
        for n in names:
            sub = [c for c in scored[n][1] if c[0]["kind"] == "mistake" and c[0]["said"] == text]
            s, _ = summarize(sub)
            cells.append(f"{s['m_caught']}/{s['m_planted']}")
        print(f"| {text} | " + " | ".join(cells) + " |")

    baseline = next((n for n in scored if scored[n][0]["config"] == "whisper-int8"), None)
    if baseline:
        print("\n## Paired comparison with whisper-int8 (same clips, word by word)\n")
        print("Target words in correct readings and mistake clips, scored against what was really said. "
              "\"Better\" counts words this model got right and whisper-int8 got wrong; \"worse\" the reverse. "
              "p is a two-sided exact sign test (McNemar) on those two counts.\n")
        print("| Model | Better | Worse | p |")
        print("|---|---|---|---|")
        base = {m["clip"]: sc for m, _, sc in scored[baseline][1] if m["kind"] != "nonspeech"}
        for name, (summary, clips) in scored.items():
            better = worse = 0
            for m, _, sc in clips:
                if m["kind"] == "nonspeech":
                    continue
                b = base[m["clip"]]
                for exp, got, ref in zip(sc["expected"], sc["statuses"], b["statuses"]):
                    ok, ref_ok = (got == "CORRECT") == (exp == "CORRECT"), (ref == "CORRECT") == (exp == "CORRECT")
                    better += ok and not ref_ok
                    worse += ref_ok and not ok
            print(f"| {summary['config']} | {better} | {worse} | {sign_test(better, worse):.3g} |")

    print("\n## Output on clips without speech\n")
    for name, (summary, clips) in scored.items():
        outs = Counter(r["text"].strip() or "(nothing)" for m, r, _ in clips if m["kind"] == "nonspeech")
        print(f"- **{summary['config']}:** " + "; ".join(f"\"{t}\" ×{k}" for t, k in outs.most_common()))


if __name__ == "__main__":
    main()
