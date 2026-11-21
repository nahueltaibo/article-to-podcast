"""Write podcast.xml for every finished episode, plus each episode's chapters.json."""
import json
from datetime import datetime
from email.utils import format_datetime
from pathlib import Path
from xml.sax.saxutils import escape

from PIL import Image, ImageDraw

FEED_TITLE = "Pagecast"
# Other podcast apps ignore this namespace. Pagecast uses it to find each episode's images and timeline.
PAGECAST_NS = "https://github.com/nahueltaibo/article-to-podcast/ns/1"


def default_cover(path: Path, title: str) -> None:
    img = Image.new("RGB", (1400, 1400), (24, 24, 32))
    draw = ImageDraw.Draw(img)
    draw.text((120, 600), title, fill=(240, 240, 240), font_size=140)
    img.save(path, "JPEG", quality=90)


def chapters_json(presentation: dict, base: str) -> dict:
    return {"version": "1.2.0", "chapters": [
        {"startTime": round(ch["startMs"] / 1000, 3), "title": ch["title"],
         **({"img": f"{base}/{ch['image']}"} if ch["image"] else {}),
         "url": presentation["article"]["url"]}
        for ch in presentation["chapters"]]}


def published(presentation: dict) -> datetime:
    """The article's date when known, so a catalog built in one day still lists in publish order."""
    day = presentation["article"].get("publishedAt")
    generated = datetime.fromisoformat(presentation["generatedAt"])
    return datetime.fromisoformat(day).replace(tzinfo=generated.tzinfo) if day else generated


def item(presentation: dict, episode_dir: Path, base: str) -> str:
    article = presentation["article"]
    audio = episode_dir / "audio.mp3"
    seconds = presentation["audio"]["durationMs"] // 1000
    image = f'<itunes:image href="{escape(base)}/cover.jpg"/>' if presentation.get("cover") else ""
    description = f"{presentation['description']} Original article: {article['url']}"
    tags = "".join(f"\n   <category>{escape(c)}</category>" for c in presentation.get("categories", []))
    source = presentation.get("source") or article.get("site")
    if source:
        tags += f"\n   <pagecast:source>{escape(source)}</pagecast:source>"
    return f"""  <item>
   <title>{escape(article['title'])}</title>
   <link>{escape(article['url'])}</link>
   <guid isPermaLink="false">{escape(presentation['id'])}</guid>
   <pubDate>{format_datetime(published(presentation))}</pubDate>
   <description>{escape(description)}</description>{tags}
   <enclosure url="{escape(base)}/audio.mp3" length="{audio.stat().st_size}" type="audio/mpeg"/>
   <itunes:duration>{seconds // 3600:02d}:{seconds % 3600 // 60:02d}:{seconds % 60:02d}</itunes:duration>
   {image}
   <podcast:chapters url="{escape(base)}/chapters.json" type="application/json+chapters"/>
   <pagecast:presentation url="{escape(base)}/presentation.json"/>
  </item>"""


def run(output: Path, base_url: str, title: str = FEED_TITLE) -> Path:
    base_url = base_url.rstrip("/")
    if not (output / "cover.jpg").exists():
        default_cover(output / "cover.jpg", title)
    items = []
    for path in sorted(output.glob("episodes/*/presentation.json")):
        presentation = json.loads(path.read_text(encoding="utf-8"))
        if "chapters" not in presentation:
            continue  # not packaged yet
        episode_base = f"{base_url}/episodes/{path.parent.name}"
        (path.parent / "chapters.json").write_text(json.dumps(chapters_json(presentation, episode_base), indent=1),
                                                   encoding="utf-8")
        items.append((published(presentation), item(presentation, path.parent, episode_base)))
    items.sort(reverse=True)
    xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:podcast="https://podcastindex.org/namespace/1.0" xmlns:pagecast="{PAGECAST_NS}">
 <channel>
  <title>{escape(title)}</title>
  <link>{escape(base_url)}/</link>
  <description>Articles narrated into podcast episodes, with images in sync.</description>
  <language>en</language>
  <itunes:image href="{escape(base_url)}/cover.jpg"/>
  <itunes:explicit>false</itunes:explicit>
{chr(10).join(x for _, x in items)}
 </channel>
</rss>
"""
    feed = output / "podcast.xml"
    feed.write_text(xml, encoding="utf-8")
    return feed
