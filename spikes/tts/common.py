"""Shared spike harness: render each segment separately, join with pauses, write a normalized MP3."""
import json
import subprocess
import time
from pathlib import Path

import numpy as np
import soundfile as sf

ROOT = Path(__file__).parent
OUT = ROOT / "out"
PAUSE_S = 0.6


def load_segments():
    return json.loads((ROOT / "test-script.json").read_text(encoding="utf-8"))["segments"]


def run(engine: str, voice: str, synth):
    """`synth(text) -> (np.ndarray mono float32, sample_rate)`. Writes out/<engine>-<voice>.mp3 and .json."""
    OUT.mkdir(exist_ok=True)
    clips, timings, rate = [], [], None
    started = time.perf_counter()
    for seg in load_segments():
        t0 = time.perf_counter()
        audio, sr = synth(seg["text"])
        rate = rate or sr
        if sr != rate:
            raise ValueError(f"sample rate changed mid-run: {sr} != {rate}")
        audio = np.asarray(audio, dtype=np.float32).reshape(-1)
        clips += [audio, np.zeros(int(PAUSE_S * sr), dtype=np.float32)]
        timings.append({"id": seg["id"], "audio_s": round(len(audio) / sr, 2), "synth_s": round(time.perf_counter() - t0, 2)})
        print(f"  {seg['id']}: {timings[-1]['audio_s']}s audio in {timings[-1]['synth_s']}s")
    synth_total = time.perf_counter() - started

    name = f"{engine}-{voice}"
    wav = OUT / f"{name}.wav"
    sf.write(wav, np.concatenate(clips), rate)
    # Loudness-normalize so engines compare fairly by ear.
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-af", "loudnorm=I=-16:TP=-1.5:LRA=11",
                    "-ac", "1", "-ar", "44100", "-b:a", "96k", str(OUT / f"{name}.mp3")], check=True)
    wav.unlink()

    audio_total = sum(t["audio_s"] for t in timings)
    stats = {"engine": engine, "voice": voice, "audio_s": round(audio_total, 1), "synth_s": round(synth_total, 1),
             "rtf": round(synth_total / audio_total, 3), "segments": timings}
    (OUT / f"{name}.json").write_text(json.dumps(stats, indent=1), encoding="utf-8")
    print(f"{name}: {stats['audio_s']}s audio, synth {stats['synth_s']}s, RTF {stats['rtf']}")
