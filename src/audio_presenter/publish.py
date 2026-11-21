"""Copy finished episodes into a static site folder and write its feed, ready for GitHub Pages."""
import json
import shutil
from pathlib import Path

from . import feed

# article.json and script.json are inputs to the pipeline. Listeners don't need them.
SERVED = ("presentation.json", "audio.mp3", "narration.mp3", "chime.wav", "cover.jpg")


def run(output: Path, site: Path, base_url: str) -> Path:
    for path in sorted(output.glob("episodes/*/presentation.json")):
        if "chapters" not in json.loads(path.read_text(encoding="utf-8")):
            continue  # not packaged yet
        src, dest = path.parent, site / "episodes" / path.parent.name
        dest.mkdir(parents=True, exist_ok=True)
        for name in SERVED:
            if (src / name).exists():
                shutil.copy2(src / name, dest / name)
        if (src / "media").is_dir():
            shutil.copytree(src / "media", dest / "media", dirs_exist_ok=True)
    if (output / "cover.jpg").exists():
        shutil.copy2(output / "cover.jpg", site / "cover.jpg")
    # Pages would otherwise run Jekyll, which skips nothing here but slows every deploy.
    (site / ".nojekyll").touch()
    return feed.run(site, base_url)
