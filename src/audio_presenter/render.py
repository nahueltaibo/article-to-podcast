"""Render script.json to audio.mp3, one TTS clip per segment, and write the timeline to presentation.json."""
import json
import subprocess
import tempfile
from datetime import datetime, timezone
from pathlib import Path

import numpy as np
import soundfile as sf

SAMPLE_RATE = 24000
PAUSE_AFTER = {"title": 1.0, "heading": 0.8}
DEFAULT_PAUSE = 0.6
CHIME_GAP = 0.25
DEFAULT_VOICE = "af_heart"


def chime(sr: int = SAMPLE_RATE) -> np.ndarray:
    """Two soft rising notes. Short enough not to break the narration's flow."""
    notes = []
    for freq in (880.0, 1318.5):
        t = np.arange(int(0.16 * sr)) / sr
        envelope = np.exp(-t * 18) * np.minimum(1, t * 400)
        notes.append(0.25 * envelope * (np.sin(2 * np.pi * freq * t) + 0.3 * np.sin(4 * np.pi * freq * t)))
    return np.concatenate(notes).astype(np.float32)


class KokoroVoice:
    def __init__(self, voice: str = DEFAULT_VOICE, speed: float = 1.0):
        import torch
        from kokoro import KPipeline

        self.name, self.speed = voice, speed
        device = "cuda" if torch.cuda.is_available() else "cpu"
        self.pipe = KPipeline(lang_code=voice[0], repo_id="hexgrad/Kokoro-82M", device=device)

    def __call__(self, text: str) -> np.ndarray:
        chunks = [a.cpu().numpy() if hasattr(a, "cpu") else a for _, _, a in self.pipe(text, voice=self.name, speed=self.speed)]
        return np.concatenate(chunks).astype(np.float32)


def silence(seconds: float) -> np.ndarray:
    return np.zeros(int(seconds * SAMPLE_RATE), dtype=np.float32)


def run(episode_dir: Path, voice: str = DEFAULT_VOICE) -> dict:
    script = json.loads((episode_dir / "script.json").read_text(encoding="utf-8"))
    article = json.loads((episode_dir / "article.json").read_text(encoding="utf-8"))
    tts = KokoroVoice(voice)
    bell = chime()

    parts, segments, cursor = [], [], 0
    for seg in script["segments"]:
        clip = [bell, silence(CHIME_GAP)] if seg.get("media") else []
        clip += [tts(seg["text"]), silence(PAUSE_AFTER.get(seg["kind"], DEFAULT_PAUSE))]
        audio = np.concatenate(clip)
        parts.append(audio)
        start_ms = round(cursor * 1000 / SAMPLE_RATE)
        cursor += len(audio)
        segments.append({**seg, "startMs": start_ms, "endMs": round(cursor * 1000 / SAMPLE_RATE)})
        print(f"  {seg['id']} {seg['kind']:<13} {segments[-1]['endMs'] - start_ms:>6} ms")

    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "audio.wav"
        sf.write(wav, np.concatenate(parts), SAMPLE_RATE)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-af", "loudnorm=I=-16:TP=-1.5:LRA=11",
                        "-ar", "44100", "-ac", "1", "-b:a", "96k", str(episode_dir / "audio.mp3")], check=True)

    used = {s["media"] for s in segments if s.get("media")}
    presentation = {
        "formatVersion": 1,
        "id": episode_dir.name,
        "article": {k: article[k] for k in ("url", "title", "site", "publishedAt")},
        "description": script["description"],
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "voice": {"engine": "kokoro", "voice": voice},
        "audio": {"file": "audio.mp3", "durationMs": segments[-1]["endMs"]},
        "cover": "cover.jpg" if (episode_dir / "cover.jpg").exists() else None,
        "segments": segments,
        "media": [m for m in article["media"] if m["id"] in used],
    }
    (episode_dir / "presentation.json").write_text(json.dumps(presentation, indent=1, ensure_ascii=False), encoding="utf-8")
    return presentation
