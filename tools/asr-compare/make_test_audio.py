"""Builds the test clips for the model comparison with macOS `say` (macOS only).

Writes 16 kHz mono 16-bit WAVs plus `manifest.jsonl` into --out. Output is
deterministic for the same macOS voices.

Clip kinds:
- correct: the lesson sentence, said right
- mistake: a typical learner mistake (a wrong, missing or reordered word)
- nonspeech: no speech at all (room tone, noise, background talk)

Every clip has 0.4 s of lead-in and 1.2 s of trailing silence, as the app's
recorder captures it (it stops after 1.2 s of silence).
"""

import argparse
import json
import subprocess
import tempfile
import zlib
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy.signal import butter, sosfiltfilt

SR = 16000
LEAD_S, TAIL_S = 0.4, 1.2

SENTENCES = {
    "bus_01": ("Where is the bus stop?", "वेयर इज़ द बस स्टॉप?"),
    "bus_02": ("When will the bus come?", "वेन विल द बस कम?"),
    "bus_03": ("One ticket to Patna, please.", "वन टिकट टू पटना, प्लीज़।"),
    "bus_04": ("How much is the ticket?", "हाउ मच इज़ द टिकट?"),
    "bus_05": ("Does this bus go to Bettiah?", "डज़ दिस बस गो टू बेतिया?"),
}

# (sentence id, what is said in English, the same in Devanagari for the Hindi voice)
MISTAKES = [
    ("bus_01", "Where is bus stop?", "वेयर इज़ बस स्टॉप?"),
    ("bus_01", "Where is the boss stop?", "वेयर इज़ द बॉस स्टॉप?"),
    ("bus_01", "Where is the bus shop?", "वेयर इज़ द बस शॉप?"),
    ("bus_02", "When the bus will come?", "वेन द बस विल कम?"),
    ("bus_02", "When will the bus came?", "वेन विल द बस केम?"),
    ("bus_03", "Van ticket to Patna, please.", "वैन टिकट टू पटना, प्लीज़।"),
    ("bus_03", "One ticket Patna, please.", "वन टिकट पटना, प्लीज़।"),
    ("bus_04", "How much the ticket?", "हाउ मच द टिकट?"),
    ("bus_04", "How match is the ticket?", "हाउ मैच इज़ द टिकट?"),
    ("bus_05", "Does this bus goes to Bettiah?", "डज़ दिस बस गोज़ टू बेतिया?"),
    ("bus_05", "Do this bus go to Bettiah?", "डू दिस बस गो टू बेतिया?"),
    ("bus_05", "Does this bus go Bettiah?", "डज़ दिस बस गो बेतिया?"),
]

# name: (macOS voice, script the voice reads, group)
VOICES = {
    "aman": ("Aman", "latin", "indian-english"),
    "rishi": ("Rishi", "latin", "indian-english"),
    "tara": ("Tara", "latin", "indian-english"),
    "lekha-latin": ("Lekha", "latin", "hindi-voice"),
    "lekha-deva": ("Lekha", "deva", "hindi-voice"),
    "samantha": ("Samantha", "latin", "us-control"),
}
MISTAKE_VOICES = ["aman", "rishi", "tara", "lekha-deva"]

# name: (words per minute or None for the voice default, pause between words in ms)
STYLES = {"normal": (None, 0), "slow": (120, 0), "halting": (140, 350)}

CONDITIONS = ["clean", "noisy", "phone"]
MISTAKE_CONDITIONS = ["clean", "noisy"]

# Background talk for the "noisy" condition: unrelated sentences, none of the lesson words.
BABBLE = [
    ("Samantha", "The weather has been very warm this week and the farmers are waiting for rain. "
                 "My sister bought fresh vegetables from the market in the morning. "
                 "The children played cricket near the school until it got dark."),
    ("Daniel", "Please remember to close the window before you leave the house. "
               "The shop near the temple sells sweets, rice and cooking oil. "
               "Our neighbour is building a new room on the roof of his house."),
    ("Moira", "I think the meeting will start a little late today. "
              "She keeps her phone in a small cloth bag. "
              "The tea stall opens early, before the sun comes up."),
    ("Karen", "We need two kilos of potatoes and some onions for dinner. "
              "His brother works in a factory in another state. "
              "The river is full after last month's heavy rain."),
    ("Rishi", "Can you hear me? The line is not very clear. "
              "I will call you again in the evening after work. "
              "My mother is cooking dal and rice for everyone."),
    ("Lekha", "आज मौसम बहुत अच्छा है। बाज़ार में बहुत भीड़ है। "
              "मेरे भाई ने नया मोबाइल खरीदा है। शाम को हम सब मंदिर जाएँगे। "
              "खेत में गेहूँ की फसल तैयार हो रही है।"),
]


def say(voice, text, rate=None):
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "x.wav"
        cmd = ["say", "-v", voice, "-o", str(out), f"--data-format=LEI16@{SR}"]
        if rate:
            cmd += ["-r", str(rate)]
        subprocess.run(cmd + [text], check=True)
        audio, sr = sf.read(out, dtype="float32")
    assert sr == SR, sr
    return audio


def with_pauses(text, pause_ms):
    if not pause_ms:
        return text
    return f" [[slnc {pause_ms}]] ".join(text.split())


def rng_for(name):
    return np.random.default_rng(zlib.crc32(name.encode()))


def pink(n, rng):
    spectrum = np.fft.rfft(rng.standard_normal(n))
    f = np.fft.rfftfreq(n)
    f[0] = f[1]
    x = np.fft.irfft(spectrum / np.sqrt(f), n)
    return (x / np.sqrt(np.mean(x**2))).astype(np.float32)


def active_rms(x):
    frames = x[: len(x) // 160 * 160].reshape(-1, 160)
    e = np.sqrt(np.mean(frames**2, axis=1))
    loud = e[e > 10 ** (-50 / 20)]
    return float(np.sqrt(np.mean(loud**2))) if len(loud) else float(np.sqrt(np.mean(x**2)))


def db(level):
    return 10 ** (level / 20)


def make_babble(seconds=40):
    n = SR * seconds
    rng = rng_for("babble")
    mix = np.zeros(n, np.float32)
    for voice, text in BABBLE:
        talk = say(voice, text + " " + text)
        talk = talk / active_rms(talk)
        start = int(rng.integers(0, SR * 3))
        while start < n:
            end = min(n, start + len(talk))
            mix[start:end] += talk[: end - start] * float(rng.uniform(0.6, 1.0))
            start = end + int(rng.integers(SR // 4, SR))
    return mix / np.sqrt(np.mean(mix**2))


def finish(speech, condition, name, babble):
    """Pads the speech with room tone and applies the recording condition."""
    rng = rng_for(name)
    speech = speech / active_rms(speech) * db(-23)
    x = np.concatenate([np.zeros(int(LEAD_S * SR)), speech, np.zeros(int(TAIL_S * SR))]).astype(np.float32)
    n = len(x)
    if condition == "clean":
        x = x + pink(n, rng) * db(-65)
    elif condition == "noisy":
        start = int(rng.integers(0, len(babble) - n))
        x = x + babble[start : start + n] * db(-23 - 10) + pink(n, rng) * db(-50)
    elif condition == "phone":
        sos = butter(4, [300, 3400], btype="bandpass", fs=SR, output="sos")
        x = sosfiltfilt(sos, x).astype(np.float32)
        x = x / active_rms(x) * db(-23) + pink(n, rng) * db(-23 - 15)
        x = x * db(-32 + 23)
    else:
        raise ValueError(condition)
    return np.clip(x, -1, 1).astype(np.float32)


def nonspeech(kind, k, babble):
    name = f"nonspeech-{kind}-{k}"
    rng = rng_for(name)
    n = 2 * SR
    if kind == "silence":
        x = pink(n, rng) * db(-65)
    elif kind == "noise":
        x = pink(n, rng) * db(-40)
    elif kind == "babble":
        start = int(rng.integers(0, len(babble) - n))
        x = babble[start : start + n] * db(-33) + pink(n, rng) * db(-50)
    else:
        raise ValueError(kind)
    return name, np.clip(x, -1, 1).astype(np.float32)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    out = Path(ap.parse_args().out)
    (out / "wav").mkdir(parents=True, exist_ok=True)
    babble = make_babble()
    rows = []

    def write(name, audio, **meta):
        path = out / "wav" / f"{name}.wav"
        sf.write(path, audio, SR, subtype="PCM_16")
        rows.append({"clip": f"wav/{name}.wav", "seconds": round(len(audio) / SR, 3), **meta})

    for sid, (en, deva) in SENTENCES.items():
        for vname, (voice, script, group) in VOICES.items():
            for style, (rate, pause) in STYLES.items():
                speech = say(voice, with_pauses(deva if script == "deva" else en, pause), rate)
                for cond in CONDITIONS:
                    name = f"correct-{sid}-{vname}-{style}-{cond}"
                    write(name, finish(speech, cond, name, babble), kind="correct", sentence=sid, target=en,
                          said=en, voice=vname, group=group, style=style, condition=cond)

    for k, (sid, en, deva) in enumerate(MISTAKES):
        for vname in MISTAKE_VOICES:
            voice, script, group = VOICES[vname]
            speech = say(voice, deva if script == "deva" else en)
            for cond in MISTAKE_CONDITIONS:
                name = f"mistake{k:02d}-{sid}-{vname}-{cond}"
                write(name, finish(speech, cond, name, babble), kind="mistake", sentence=sid,
                      target=SENTENCES[sid][0], said=en, voice=vname, group=group, style="normal", condition=cond)

    for kind in ["silence", "noise", "babble"]:
        for k in range(3):
            name, audio = nonspeech(kind, k, babble)
            write(name, audio, kind="nonspeech", sentence=None, target=None, said="", voice=None,
                  group=None, style=None, condition=kind)

    with open(out / "manifest.jsonl", "w") as f:
        for r in rows:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")
    print(f"{len(rows)} clips in {out}")


if __name__ == "__main__":
    main()
