"""Benchmarks small Ollama models on the two lesson tasks, with the app's speech model loaded alongside.

    arch -x86_64 python bench.py --host 127.0.0.1:11435 --server-pid 12345 \
        --models llama3.2:1b gemma3:4b --speech-model ../../app/src/main/assets/asr/whisper-tiny.en \
        --clip ../../app/src/androidTest/assets/asr-test/librispeech-0.wav --out results

Task A: draft lesson data (Hindi translation, chunks, syllables, sound hints) for an
English sentence, as JSON in the lesson format.
Task B: explain the first mistake in 1-2 sentences of simple Hindi.

For every answer it records time to first token, tokens per second, prompt
processing speed and the peak memory of the Ollama server and its runner
(sampled every 0.25 s), plus the memory of this process, which holds the
speech model. All outputs go to <out>/review.md and <out>/scoring-sheet.csv
for a fluent Hindi speaker to score; nothing here judges the Hindi.
"""

import argparse
import csv
import json
import platform
import re
import statistics
import subprocess
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path

DEVANAGARI = re.compile(r"[ऀ-ॿ]")
HINT_TAGS = {"v_w", "th", "s_cluster", "short_vowel", "generic"}

TASK_A_SYSTEM = """You write lesson content for "English Saathi", an app where Hindi speakers from villages in Bihar practise speaking English. For the English sentence given, produce lesson data as a JSON object with exactly these keys:
- "text": the sentence exactly as given.
- "hi": its Hindi translation in Devanagari, polite (आप form) and natural, as a villager would say it.
- "chunks": the sentence split into 2 or 3 natural phrases, in order, covering every word exactly once, keeping the original spelling.
- "words": an object. Its keys are the 3 to 5 words of the sentence that are hardest for a Hindi speaker to pronounce, in lowercase without punctuation. Each value has:
  - "syllables": the word split into syllables, as a list of strings;
  - "hint_hi": how to say the word, written in Devanagari;
  - "hint_tag": one of "v_w" (v/w confusion), "th" (the th sound), "s_cluster" (a word starting with s plus a consonant, where Hindi speakers add "i" in front), "short_vowel" (a short vowel that Hindi speakers make long), "generic" (anything else).
Output only the JSON object, with no explanation and no code fence.

Example for the sentence "Where is the bus stop?":
{example}"""

TASK_B_SYSTEM = """You are a kind English teacher who speaks to a learner in simple Hindi. The learner lives in a village in Bihar, may be a child, and reads Hindi slowly. The app showed them an English sentence, they said it aloud, and a speech recognizer wrote down what it heard. You are told the first mistake. Write 1 or 2 short sentences in simple Hindi (Devanagari) that tell the learner what went wrong with that one word and how to say it right. Use the polite आप form. Use no English except the English words themselves. No greetings, no lists, nothing about other words. Output only the Hindi text."""


def task_b_user(case):
    m = case["mistake"]
    n = len(case["text"].split())
    if m["status"] == "MISSING":
        mistake = f'the word "{m["word"]}" (word {m["index"] + 1} of {n}) was missing.'
    else:
        mistake = f'the word "{m["word"]}" (word {m["index"] + 1} of {n}) was said as "{m["heard_as"]}".'
    return (f'Sentence: "{case["text"]}"\nMeaning in Hindi: {case["hi"]}\n'
            f'The recognizer heard: "{case["heard"]}"\nFirst mistake: {mistake}')


class Ollama:
    def __init__(self, host):
        self.base = f"http://{host}"

    def _post(self, path, body, stream=False):
        req = urllib.request.Request(self.base + path, data=json.dumps(body).encode(), headers={"Content-Type": "application/json"})
        return urllib.request.urlopen(req, timeout=1800)

    def show(self, model):
        with self._post("/api/show", {"model": model}) as r:
            return json.load(r)

    def unload(self, model):
        with self._post("/api/generate", {"model": model, "keep_alive": 0}) as r:
            r.read()

    def chat(self, model, system, user, options, think=None, override=None):
        """One answer. `override` (from model_overrides.json) can add stop markers, and can replace the
        model's own chat template with a raw prompt in which {system} and {user} are filled in."""
        override = override or {}
        if override.get("stop"):
            options = {**options, "stop": override["stop"]}
        if override.get("prompt"):
            prompt = override["prompt"].replace("{system}", system).replace("{user}", user)
            path, body = "/api/generate", {"model": model, "prompt": prompt, "raw": True}
        else:
            path, body = "/api/chat", {"model": model, "messages": [{"role": "system", "content": system},
                                                                  {"role": "user", "content": user}]}
        body.update({"stream": True, "options": options, "keep_alive": "10m"})
        if think is not None:
            body["think"] = think
        start = time.perf_counter()
        first = None
        content = []
        thinking = []
        final = None
        with self._post(path, body, stream=True) as r:
            for line in r:
                chunk = json.loads(line)
                if "error" in chunk:
                    raise RuntimeError(chunk["error"])
                message = chunk.get("message", chunk)
                text = message.get("content", chunk.get("response", ""))
                # Ollama moves text inside <think> tags out of the answer; kept so an empty answer can be explained.
                thinking.append(message.get("thinking", ""))
                if text and first is None:
                    first = time.perf_counter() - start
                content.append(text)
                if chunk.get("done"):
                    final = chunk
        wall = time.perf_counter() - start
        metrics = {
            "done_reason": final.get("done_reason"),
            "ttft_s": round(first, 3) if first is not None else None,
            "wall_s": round(wall, 3),
            "load_s": round(final.get("load_duration", 0) / 1e9, 3),
            "prompt_tokens": final.get("prompt_eval_count"),
            "prompt_tokens_per_s": round(final["prompt_eval_count"] / (final["prompt_eval_duration"] / 1e9), 1) if final.get("prompt_eval_duration") else None,
            "output_tokens": final.get("eval_count"),
            "tokens_per_s": round(final["eval_count"] / (final["eval_duration"] / 1e9), 2) if final.get("eval_duration") else None,
            "thinking_chars": len("".join(thinking)),
        }
        return "".join(content), metrics


class RamSampler:
    """Peak resident memory of the Ollama server and its runner, and of this process, sampled in a thread."""

    def __init__(self, server_pid, interval=0.25):
        self.server_pid = server_pid
        self.interval = interval
        self.peak = {"ollama_total_mb": 0.0, "children_mb": 0.0, "self_mb": 0.0}
        self._stop = threading.Event()
        self._thread = None

    def sample(self):
        out = subprocess.run(["ps", "-axo", "pid=,ppid=,rss=,command="], capture_output=True, text=True).stdout
        total = children = me = 0
        import os
        for line in out.splitlines():
            parts = line.split(None, 3)
            if len(parts) < 4:
                continue
            pid, ppid, rss = int(parts[0]), int(parts[1]), int(parts[2])
            if pid == self.server_pid or ppid == self.server_pid:
                total += rss
                if ppid == self.server_pid:
                    children += rss
            if pid == os.getpid():
                me = rss
        # The server plus any model runner it spawned; "children" is the runner alone when there is one.
        return {"ollama_total_mb": total / 1024, "children_mb": children / 1024, "self_mb": me / 1024}

    def _run(self):
        while not self._stop.is_set():
            s = self.sample()
            for k in self.peak:
                self.peak[k] = max(self.peak[k], s[k])
            self._stop.wait(self.interval)

    def start(self):
        self.peak = {k: 0.0 for k in self.peak}
        self._stop.clear()
        self._thread = threading.Thread(target=self._run, daemon=True)
        self._thread.start()

    def stop(self):
        self._stop.set()
        self._thread.join()
        return {k: round(v) for k, v in self.peak.items()}


def extract_json(text):
    """The JSON object in the output; (data, repaired). Repaired means closing braces had to be added."""
    text = text.strip()
    text = re.sub(r"^```(?:json)?\s*|\s*```$", "", text)
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end < 0:
        return None, False
    candidate = text[start:end + 1]
    try:
        return json.loads(candidate), False
    except json.JSONDecodeError:
        pass
    missing = candidate.count("{") - candidate.count("}")
    if missing > 0:
        try:
            return json.loads(candidate + "}" * missing), True
        except json.JSONDecodeError:
            pass
    return None, False


def words_of(text):
    return [w for w in re.sub(r"[^a-z0-9 ]", " ", text.lower()).split() if w]


def check_task_a(sentence, output):
    """Mechanical checks only: format and coverage, not the Hindi."""
    data, repaired = extract_json(output)
    checks = {"valid_json": data is not None and not repaired, "valid_after_brace_repair": data is not None}
    if not data:
        return checks
    checks["has_keys"] = all(k in data for k in ("text", "hi", "chunks", "words"))
    checks["hi_is_devanagari"] = bool(DEVANAGARI.search(str(data.get("hi", ""))))
    chunks = data.get("chunks") or []
    checks["chunks_cover_sentence"] = isinstance(chunks, list) and words_of(" ".join(map(str, chunks))) == words_of(sentence)
    words = data.get("words") or {}
    sentence_words = set(words_of(sentence))
    checks["word_keys_in_sentence"] = isinstance(words, dict) and bool(words) and all(k in sentence_words for k in words)
    checks["hints_devanagari"] = isinstance(words, dict) and all(DEVANAGARI.search(str(v.get("hint_hi", ""))) for v in words.values() if isinstance(v, dict))
    checks["tags_valid"] = isinstance(words, dict) and all(isinstance(v, dict) and v.get("hint_tag") in HINT_TAGS for v in words.values())
    checks["syllables_lists"] = isinstance(words, dict) and all(isinstance(v, dict) and isinstance(v.get("syllables"), list) and v["syllables"] for v in words.values())
    return checks


def check_task_b(case, output):
    text = output.strip()
    sentences = [s for s in re.split(r"[।.!?]\s*", text) if s.strip()]
    return {
        "devanagari": bool(DEVANAGARI.search(text)),
        "sentences": len(sentences),
        "at_most_2_sentences": 0 < len(sentences) <= 2,
        "mentions_word": case["mistake"]["word"].lower() in text.lower(),
        "latin_words": len(re.findall(r"[A-Za-z]+", text)),
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="127.0.0.1:11434")
    ap.add_argument("--server-pid", type=int, required=True, help="PID of the `ollama serve` process to measure")
    ap.add_argument("--models", nargs="+", required=True)
    ap.add_argument("--cases", default=str(Path(__file__).with_name("cases.json")))
    ap.add_argument("--speech-model", help="directory with the app's Whisper files; loaded in this process for the whole run")
    ap.add_argument("--clip", help="16 kHz WAV transcribed once after each model, to show both models work together")
    ap.add_argument("--speech-threads", type=int, default=4)
    ap.add_argument("--think-models", nargs="*", default=["qwen3"], help="model name prefixes that take think=false")
    ap.add_argument("--overrides", default=str(Path(__file__).with_name("model_overrides.json")),
                    help="per-model stop markers and raw prompt templates, for model files whose own template is wrong")
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    cases = json.load(open(args.cases, encoding="utf-8"))
    overrides = json.load(open(args.overrides, encoding="utf-8")) if Path(args.overrides).exists() else {}
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    ollama = Ollama(args.host)
    sampler = RamSampler(args.server_pid)

    speech = None
    speech_info = {}
    if args.speech_model:
        import sherpa_onnx
        import soundfile as sf
        before = sampler.sample()["self_mb"]
        d = Path(args.speech_model)
        t0 = time.perf_counter()
        speech = sherpa_onnx.OfflineRecognizer.from_whisper(
            encoder=str(d / "tiny.en-encoder.int8.onnx"), decoder=str(d / "tiny.en-decoder.int8-embedding.onnx"),
            tokens=str(d / "tiny.en-tokens.txt"), num_threads=args.speech_threads)
        clip, rate = sf.read(args.clip, dtype="float32")
        clip = clip[: 3 * rate]
        speech_info = {"load_s": round(time.perf_counter() - t0, 2), "process_mb_before": round(before),
                       "process_mb_after_load": round(sampler.sample()["self_mb"])}

        def transcribe():
            start = time.perf_counter()
            s = speech.create_stream()
            s.accept_waveform(rate, clip)
            speech.decode_stream(s)
            return s.result.text, round(time.perf_counter() - start, 3)
        speech_info["warmup"] = transcribe()

    example = json.dumps({"text": cases["example"]["text"], "hi": cases["example"]["hi"], "chunks": cases["example"]["chunks"],
                          "words": cases["example"]["words"]}, ensure_ascii=False, indent=1)
    system_a = TASK_A_SYSTEM.replace("{example}", example)
    options_a = {"temperature": 0, "seed": 7, "num_predict": 800, "num_ctx": 4096, "num_gpu": 0}
    options_b = {"temperature": 0, "seed": 7, "num_predict": 160, "num_ctx": 4096, "num_gpu": 0}

    summary = {"arch": platform.machine(), "speech": speech_info, "models": []}
    for model in args.models:
        print(f"== {model}", flush=True)
        think = False if any(model.startswith(p) for p in args.think_models) else None
        override = overrides.get(model)
        info = ollama.show(model)
        details = info.get("details", {})
        record = {"model": model, "parameter_size": details.get("parameter_size"), "quantization": details.get("quantization_level"),
                  "family": details.get("family"), "task_a": [], "task_b": []}
        if override:
            record["override"] = override
        sampler.start()
        try:
            # First request: the model loads. Its load time is reported by Ollama.
            warm, warm_metrics = ollama.chat(model, "Reply with the single word: ready", "ready?", {"num_predict": 5, "num_gpu": 0}, think, override)
            record["load_s"] = warm_metrics["load_s"]
            record["first_response_wall_s"] = warm_metrics["wall_s"]
            for case in cases["task_a"]:
                try:
                    text, metrics = ollama.chat(model, system_a, f'Sentence: "{case["text"]}"', options_a, think, override)
                except RuntimeError as e:
                    text, metrics = f"ERROR: {e}", {}
                record["task_a"].append({"id": case["id"], "text": case["text"], "output": text, "metrics": metrics,
                                         "checks": check_task_a(case["text"], text)})
                print(f"  A {case['id']}: {metrics.get('tokens_per_s')} tok/s, ttft {metrics.get('ttft_s')} s", flush=True)
            for case in cases["task_b"]:
                try:
                    text, metrics = ollama.chat(model, TASK_B_SYSTEM, task_b_user(case), options_b, think, override)
                except RuntimeError as e:
                    text, metrics = f"ERROR: {e}", {}
                record["task_b"].append({"id": case["id"], "output": text, "metrics": metrics, "checks": check_task_b(case, text)})
                print(f"  B {case['id']}: {metrics.get('tokens_per_s')} tok/s, ttft {metrics.get('ttft_s')} s", flush=True)
            if speech:
                record["speech_after_llm"] = transcribe()
        finally:
            record["peak_ram_mb"] = sampler.stop()
            try:
                ollama.unload(model)
            except (urllib.error.URLError, OSError):
                pass
            time.sleep(2)

        def med(task, key):
            vals = [c["metrics"][key] for c in record[task] if c["metrics"].get(key) is not None]
            return round(statistics.median(vals), 2) if vals else None
        record["medians"] = {t: {k: med(t, k) for k in ("ttft_s", "tokens_per_s", "prompt_tokens_per_s", "wall_s", "output_tokens")} for t in ("task_a", "task_b")}
        record["format"] = {
            "task_a_valid_json": sum(c["checks"].get("valid_json", False) for c in record["task_a"]),
            "task_a_valid_after_brace_repair": sum(c["checks"].get("valid_after_brace_repair", False) for c in record["task_a"]),
            "task_a_all_checks": sum(all(c["checks"].values()) for c in record["task_a"]),
            "task_b_devanagari": sum(c["checks"]["devanagari"] for c in record["task_b"]),
            "task_b_at_most_2_sentences": sum(c["checks"]["at_most_2_sentences"] for c in record["task_b"]),
        }
        summary["models"].append(record)
        (out / f"{model.replace('/', '_').replace(':', '_')}.json").write_text(json.dumps(record, ensure_ascii=False, indent=1))
        (out / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=1))

    write_review(cases, summary, out)


def write_review(cases, summary, out, prefix=""):
    lines = ["# LLM outputs for review", "",
             "Please score each output from 1 (unusable) to 5 (ready to use) in scoring-sheet.csv. "
             "Task A: is the Hindi natural for a villager in Bihar, are the chunks sensible, are the sound hints right? "
             "Task B: would a slow Hindi reader understand what went wrong and how to fix it?", ""]
    lines += ["## Task A: lesson content", ""]
    for case in cases["task_a"]:
        lines += [f"### {case['id']}: {case['text']}", ""]
        if case.get("pilot_hi"):
            lines += [f"Reference from the pilot (human): hi = {case['pilot_hi']}; chunks = {case['pilot_chunks']}", ""]
        for m in summary["models"]:
            item = next(c for c in m["task_a"] if c["id"] == case["id"])
            lines += [f"**{m['model']}**", "", "```", item["output"].strip(), "```", ""]
    lines += ["## Task B: explaining the first mistake", ""]
    for case in cases["task_b"]:
        lines += [f"### {case['id']}", "", f"- Sentence: {case['text']} ({case['hi']})", f"- Heard: {case['heard']}",
                  f"- First mistake: {case['mistake']}", ""]
        for m in summary["models"]:
            item = next(c for c in m["task_b"] if c["id"] == case["id"])
            lines += [f"**{m['model']}**: {item['output'].strip()}", ""]
    (out / f"{prefix}review.md").write_text("\n".join(lines), encoding="utf-8")
    with open(out / f"{prefix}scoring-sheet.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["task", "case", "model", "score_1_to_5", "notes"])
        for m in summary["models"]:
            for c in cases["task_a"]:
                w.writerow(["A", c["id"], m["model"], "", ""])
            for c in cases["task_b"]:
                w.writerow(["B", c["id"], m["model"], "", ""])


if __name__ == "__main__":
    main()
