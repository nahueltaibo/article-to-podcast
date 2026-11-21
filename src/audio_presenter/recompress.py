"""Re-encode existing episodes to the current MP3 settings and re-embed the ID3 chapters."""
import json
import subprocess
from pathlib import Path

from . import package
from .render import MP3_BITRATE, SAMPLE_RATE


def reencode(src: Path) -> None:
    tmp = src.with_suffix(".tmp.mp3")
    # Drop old tags and cover art. The chapters are embedded again afterwards.
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(src), "-map", "0:a", "-map_metadata", "-1",
                    "-ar", str(SAMPLE_RATE), "-ac", "1", "-b:a", MP3_BITRATE, str(tmp)], check=True)
    tmp.replace(src)


def run(output: Path) -> None:
    for presentation_path in sorted(output.glob("episodes/*/presentation.json")):
        episode = presentation_path.parent
        before = sum(f.stat().st_size for f in episode.glob("*.mp3"))
        for name in ("audio.mp3", "narration.mp3"):
            if (episode / name).exists():
                reencode(episode / name)
        presentation = json.loads(presentation_path.read_text(encoding="utf-8"))
        package.embed(episode, presentation, presentation["chapters"])
        after = sum(f.stat().st_size for f in episode.glob("*.mp3"))
        print(f"{episode.name}: {before / 1e6:.1f} -> {after / 1e6:.1f} MB")
