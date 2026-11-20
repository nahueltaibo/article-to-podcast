"""Turn remote images, GIFs, and videos into still JPEGs that podcast apps accept as chapter art."""
import json
import subprocess
from io import BytesIO
from pathlib import Path

import httpx
from PIL import Image

MAX_PX = 1400
UA = "Mozilla/5.0 (compatible; audio-presenter)"


def save_jpeg(img: Image.Image, dest: Path) -> None:
    img = img.convert("RGB")
    img.thumbnail((MAX_PX, MAX_PX))
    img.save(dest, "JPEG", quality=85)


def download_still(url: str, dest: Path, client: httpx.Client) -> None:
    """Images and GIFs. For a GIF, the last frame usually shows the animation's final state."""
    data = client.get(url, headers={"User-Agent": UA}).raise_for_status().content
    img = Image.open(BytesIO(data))
    if getattr(img, "n_frames", 1) > 1:
        img.seek(img.n_frames - 1)
    save_jpeg(img, dest)


def video_still(url: str, dest: Path, loop: bool) -> None:
    """Grab one frame with ffmpeg straight from the URL, without downloading the whole video."""
    probe = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "json", url],
                           capture_output=True, text=True, check=True)
    duration = float(json.loads(probe.stdout)["format"]["duration"])
    # Looping animations tend to end on their final state. Demo videos are most representative mid-way.
    at = max(duration - 0.5, 0) if loop else duration / 2
    tmp = dest.with_suffix(".png")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-ss", f"{at:.2f}", "-i", url, "-frames:v", "1", str(tmp)],
                   check=True)
    save_jpeg(Image.open(tmp), dest)
    tmp.unlink()
