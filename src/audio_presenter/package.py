"""Build chapters from presentation.json and embed them, with images, as ID3 chapters in audio.mp3."""
import json
from pathlib import Path

from mutagen.id3 import APIC, CHAP, CTOC, ID3, TALB, TDRC, TIT2, TPE1, WXXX, CTOCFlags

CHAPTER_KINDS = {"title", "heading"}


def build_chapters(presentation: dict) -> list[dict]:
    """A chapter starts at the title, each heading, and each segment that brings media.

    Every chapter carries the image that should be on screen, so players never fall back to
    the cover mid-article.
    """
    media = {m["id"]: m for m in presentation["media"]}
    cover = presentation.get("cover")
    title = presentation["article"]["title"]
    chapters, image = [], cover
    for seg in presentation["segments"]:
        ref = seg.get("media")
        if ref:
            image = media[ref]["file"]
            label = seg.get("chapter") or media[ref].get("caption") or "Figure"
        elif seg["kind"] in CHAPTER_KINDS:
            label = title if seg["kind"] == "title" else seg.get("chapter") or seg["text"].rstrip(".")
        else:
            continue
        if chapters and chapters[-1]["startMs"] == seg["startMs"]:
            chapters.pop()
        chapters.append({"startMs": seg["startMs"], "title": label[:120], "image": image})
    for current, following in zip(chapters, chapters[1:] + [None]):
        current["endMs"] = following["startMs"] if following else presentation["audio"]["durationMs"]
    return chapters


def embed(episode_dir: Path, presentation: dict, chapters: list[dict]) -> None:
    def picture(rel: str | None, desc: str, kind: int) -> list[APIC]:
        if not rel:
            return []
        return [APIC(encoding=3, mime="image/jpeg", type=kind, desc=desc, data=(episode_dir / rel).read_bytes())]

    article = presentation["article"]
    tags = ID3()
    tags.add(TIT2(encoding=3, text=article["title"]))
    tags.add(TPE1(encoding=3, text=article["site"]))
    tags.add(TALB(encoding=3, text="Audio Presenter"))
    if article.get("publishedAt"):
        tags.add(TDRC(encoding=3, text=article["publishedAt"]))
    tags.add(WXXX(encoding=3, desc="article", url=article["url"]))
    for frame in picture(presentation.get("cover"), "cover", 3):
        tags.add(frame)

    ids = [f"ch{i}" for i in range(len(chapters))]
    tags.add(CTOC(element_id="toc", flags=CTOCFlags.TOP_LEVEL | CTOCFlags.ORDERED, child_element_ids=ids,
                  sub_frames=[TIT2(encoding=3, text="Chapters")]))
    for cid, ch in zip(ids, chapters):
        tags.add(CHAP(element_id=cid, start_time=ch["startMs"], end_time=ch["endMs"],
                      sub_frames=[TIT2(encoding=3, text=ch["title"]), *picture(ch["image"], cid, 0)]))
    # ID3v2.3 is the version every podcast app reads.
    tags.save(episode_dir / "audio.mp3", v2_version=3)


def run(episode_dir: Path) -> list[dict]:
    path = episode_dir / "presentation.json"
    presentation = json.loads(path.read_text(encoding="utf-8"))
    chapters = build_chapters(presentation)
    presentation["chapters"] = chapters
    path.write_text(json.dumps(presentation, indent=1, ensure_ascii=False), encoding="utf-8")
    embed(episode_dir, presentation, chapters)
    return chapters
