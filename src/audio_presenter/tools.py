"""Find ffmpeg: the one on PATH, else the static build that ships in the imageio-ffmpeg wheel."""
import functools
import re
import shutil
import subprocess


@functools.cache
def ffmpeg() -> str:
    if found := shutil.which("ffmpeg"):
        return found
    import imageio_ffmpeg

    return imageio_ffmpeg.get_ffmpeg_exe()


def duration(url: str) -> float:
    """Seconds, read from ffmpeg's input summary, since the bundled build has no ffprobe."""
    # With no output file ffmpeg exits non-zero after printing the summary, so the exit code means nothing here.
    info = subprocess.run([ffmpeg(), "-hide_banner", "-i", url], capture_output=True, text=True).stderr
    match = re.search(r"Duration: (\d+):(\d\d):(\d\d(?:\.\d+)?)", info)
    if not match:
        raise RuntimeError(f"ffmpeg couldn't read the duration of {url}")
    h, m, s = match.groups()
    return int(h) * 3600 + int(m) * 60 + float(s)
