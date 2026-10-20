"""Merges the per-model result files of one or more bench.py runs into one report.

    python report.py --cases cases.json --out results results-run2

Writes, into the first --out directory:
- summary.md: speed, memory and format checks for every model;
- review.md and scoring-sheet.csv: every output, for a fluent Hindi speaker
  to score (nothing here judges the Hindi).

When a model appears in several directories, the last one wins (a rerun goes last).
--models keeps only the models named, in that order, and --prefix names the
review files, e.g. a shorter sheet for the reviewer:

    python report.py --out results results/results-llm results/rerun \
        --models gemma3:4b qwen3:4b-instruct --prefix short-
"""

import argparse
import json
from pathlib import Path

from bench import write_review


def load_models(dirs):
    models = {}
    for d in dirs:
        for f in sorted(Path(d).glob("*.json")):
            if f.name == "summary.json":
                continue
            record = json.loads(f.read_text(encoding="utf-8"))
            if "model" in record and "task_a" in record:
                models[record["model"]] = record
    return list(models.values())


def summary_table(models, speech):
    lines = ["# LLM benchmark summary", ""]
    if speech:
        lines += [f"Speech model in the same process: loaded in {speech.get('load_s')} s, process "
                  f"{speech.get('process_mb_before')} → {speech.get('process_mb_after_load')} MB.", ""]
    lines += ["| Model | Params | Quant | Load s | A: first token s | A: tokens/s | A: time per sentence s | "
              "B: first token s | B: tokens/s | B: time per answer s | Prompt tokens/s | Peak RAM Ollama MB | "
              "Peak RAM speech process MB | A: valid JSON (repaired) | A: all checks | B: Devanagari | B: ≤2 sentences | Speech still works |",
              "|---" * 18 + "|"]
    for m in models:
        a, b = m["medians"]["task_a"], m["medians"]["task_b"]
        fmt = m["format"]
        n_a, n_b = len(m["task_a"]), len(m["task_b"])
        speech_ok = "yes" if m.get("speech_after_llm") and m["speech_after_llm"][0] else "–"
        lines.append(
            f"| {m['model']} | {m.get('parameter_size')} | {m.get('quantization')} | {m.get('load_s')} | "
            f"{a['ttft_s']} | {a['tokens_per_s']} | {a['wall_s']} | {b['ttft_s']} | {b['tokens_per_s']} | {b['wall_s']} | "
            f"{a['prompt_tokens_per_s']} | {m['peak_ram_mb']['ollama_total_mb']} | {m['peak_ram_mb']['self_mb']} | "
            f"{fmt['task_a_valid_json']}/{n_a} ({fmt.get('task_a_valid_after_brace_repair', '–')}) | "
            f"{fmt['task_a_all_checks']}/{n_a} | {fmt['task_b_devanagari']}/{n_b} | {fmt['task_b_at_most_2_sentences']}/{n_b} | {speech_ok} |")
    lines += ["", "Medians over the cases of each task. \"All checks\" is mechanical only: valid JSON, the right keys, "
              "Devanagari present, chunks covering the sentence, word keys from the sentence, valid hint tags. "
              "It says nothing about whether the Hindi is good.", ""]
    stops = {m["model"]: sorted({c["metrics"].get("done_reason") for c in m["task_a"] + m["task_b"] if c["metrics"]} - {None})
             for m in models}
    cut = [f"{k}: {', '.join(v)}" for k, v in stops.items() if v and v != ["stop"]]
    if cut:
        lines += ["Answers cut off by the token limit (done_reason \"length\"): " + "; ".join(cut), ""]
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cases", default=str(Path(__file__).with_name("cases.json")))
    ap.add_argument("--out", nargs="+", required=True)
    ap.add_argument("--models", nargs="+", help="only these models, in this order")
    ap.add_argument("--prefix", default="", help="prefix for the review files; the summary is not written")
    args = ap.parse_args()
    cases = json.load(open(args.cases, encoding="utf-8"))
    models = load_models(args.out)
    if args.models:
        by_name = {m["model"]: m for m in models}
        missing = [n for n in args.models if n not in by_name]
        if missing:
            raise SystemExit(f"no results for: {', '.join(missing)}")
        models = [by_name[n] for n in args.models]
    speech = {}
    for d in args.out:
        p = Path(d) / "summary.json"
        if p.exists():
            speech = json.loads(p.read_text(encoding="utf-8")).get("speech") or speech
    out = Path(args.out[0])
    write_review(cases, {"models": models}, out, args.prefix)
    if args.prefix:
        print(f"{len(models)} models -> {out}/{args.prefix}review.md, {args.prefix}scoring-sheet.csv")
        return
    (out / "summary.md").write_text(summary_table(models, speech), encoding="utf-8")
    print(f"{len(models)} models -> {out}/summary.md, review.md, scoring-sheet.csv")


if __name__ == "__main__":
    main()
