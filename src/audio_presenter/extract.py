"""Fetch an article page and write article.json: metadata, ordered content blocks, footnotes, and media."""
import json
import re
from datetime import datetime
from pathlib import Path
from urllib.parse import urljoin, urlparse

import httpx
from bs4 import BeautifulSoup, Tag

from . import media as media_io

SKIP_TAGS = {"nav", "aside", "button", "script", "style", "noscript", "svg", "header", "form", "footer"}
SKIP_CLASS = re.compile(r"TableOfContents|Footnotes|tab-group|buttonGroup", re.I)
HEADINGS = {"h2", "h3", "h4", "h5", "h6"}
DATE_FORMATS = ("%b %d, %Y", "%B %d, %Y", "%Y-%m-%d")


def clean(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def inline_text(el: Tag) -> str:
    return clean(el.get_text(""))


def parse_date(text: str | None) -> str | None:
    if not text:
        return None
    text = clean(text)
    match = re.match(r"\d{4}-\d{2}-\d{2}", text)
    if match:
        return match.group(0)
    for fmt in DATE_FORMATS:
        try:
            return datetime.strptime(text, fmt).date().isoformat()
        except ValueError:
            continue
    return None


def published_date(soup: BeautifulSoup) -> str | None:
    for dt in soup.find_all("dt"):
        if clean(dt.get_text()).lower() == "published" and dt.find_next_sibling("dd"):
            return parse_date(dt.find_next_sibling("dd").get_text())
    meta = soup.find("meta", property="article:published_time")
    if meta:
        return parse_date(meta.get("content"))
    time = soup.find("time")
    return parse_date(time.get("datetime") or time.get_text()) if time else None


def meta(soup: BeautifulSoup, key: str) -> str | None:
    tag = soup.find("meta", property=key) or soup.find("meta", attrs={"name": key})
    return clean(tag["content"]) if tag and tag.get("content") else None


def content_root(soup: BeautifulSoup) -> Tag:
    """The element with the most direct <p> children is the article body on most sites."""
    return max(soup.find_all(True), key=lambda el: len(el.find_all("p", recursive=False)))


class Extractor:
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.blocks: list[dict] = []
        self.media: list[dict] = []

    def add(self, block: dict) -> None:
        block["id"] = f"b{len(self.blocks) + 1}"
        self.blocks.append(block)

    def add_media(self, kind: str, src: str, alt: str | None, caption: str | None, **extra) -> None:
        media_id = f"m{len(self.media) + 1}"
        self.media.append({"id": media_id, "type": kind, "sourceUrl": urljoin(self.base_url, src),
                           "alt": clean(alt or "") or None, "caption": caption, **extra})
        self.add({"type": "media", "media": media_id})

    def figure(self, fig: Tag) -> None:
        cap = fig.find("figcaption")
        caption = inline_text(cap) if cap else None
        for el in fig.find_all(["img", "video", "iframe"]):
            self.element_media(el, caption)

    def element_media(self, el: Tag, caption: str | None = None) -> None:
        if el.name == "img" and el.get("src"):
            kind = "animation" if ".gif" in el["src"].lower() else "image"
            self.add_media(kind, el["src"], el.get("alt"), caption)
        elif el.name == "video":
            src = el.get("src") or (el.find("source") or {}).get("src")
            if src:
                loop = el.has_attr("loop")
                self.add_media("animation" if loop else "video", src, el.get("aria-label"), caption,
                               loop=loop, poster=el.get("poster"))
        elif el.name == "iframe" and el.get("src"):
            self.add_media("embed", el["src"], el.get("title"), caption)

    def walk(self, el: Tag) -> None:
        for child in el.children:
            if not isinstance(child, Tag):
                continue
            classes = " ".join(child.get("class", []))
            if child.name in SKIP_TAGS or SKIP_CLASS.search(classes):
                continue
            name = child.name
            if child.get("role") == "table":
                self.table(child)
            elif name in HEADINGS:
                if text := inline_text(child):
                    self.add({"type": "heading", "level": int(name[1]), "text": text})
            elif name == "p":
                if text := inline_text(child):
                    self.add({"type": "paragraph", "text": text})
            elif name in ("ul", "ol"):
                items = [inline_text(li) for li in child.find_all("li", recursive=False)]
                self.add({"type": "list", "ordered": name == "ol", "items": [i for i in items if i]})
            elif name == "blockquote":
                paras = [inline_text(p) for p in child.find_all("p")] or [inline_text(child)]
                self.add({"type": "quote", "text": "\n".join(p for p in paras if p)})
            elif name == "pre":
                code = child.get_text().strip("\n")
                if not code.lstrip().startswith("@"):  # BibTeX citation blocks
                    self.add({"type": "code", "text": code})
            elif name == "figure":
                self.figure(child)
            elif name in ("img", "video", "iframe"):
                self.element_media(child)
            elif name != "h1":
                self.walk(child)

    def table(self, el: Tag) -> None:
        rows = []
        for row in el.select("[role=row]"):
            cells = row.select("[role=columnheader], [role=cell], [role=rowheader]")
            rows.append([inline_text(c) for c in cells])
        if rows:
            self.add({"type": "table", "header": rows[0], "rows": rows[1:]})


def mark_footnotes(soup: BeautifulSoup) -> dict[str, str]:
    """Replace footnote markers with [^n] and return the footnote texts."""
    for ref in soup.select("[role=doc-noteref]"):
        n = clean(ref.get_text())
        (ref.find_parent("sup") or ref).replace_with(f"[^{n}]")
    notes = {}
    for item in soup.select("[id^=footnote-]"):
        if item.get("id", "").startswith("footnote-ref"):
            continue
        for junk in item.select("[role=doc-backlink], [class*=Sup]"):
            junk.decompose()
        notes[item["id"].removeprefix("footnote-")] = inline_text(item)
    return notes


def extract(url: str, html: str) -> dict:
    soup = BeautifulSoup(html, "html.parser")
    footnotes = mark_footnotes(soup)
    h1 = soup.find("h1")
    root = content_root(soup)
    ex = Extractor(url)
    # Intro text such as bylines often sits in a sibling just before the body.
    ex.walk(root.parent if root.parent else root)
    return {
        "url": url,
        "title": inline_text(h1) if h1 else meta(soup, "og:title"),
        "site": meta(soup, "og:site_name") or urlparse(url).hostname.removeprefix("www."),
        "description": meta(soup, "og:description") or meta(soup, "description"),
        "publishedAt": published_date(soup),
        "coverUrl": meta(soup, "og:image"),
        "blocks": ex.blocks,
        "footnotes": footnotes,
        "media": ex.media,
    }


def download_media(article: dict, episode_dir: Path) -> None:
    media_dir = episode_dir / "media"
    media_dir.mkdir(parents=True, exist_ok=True)
    with httpx.Client(follow_redirects=True, timeout=60) as client:
        if article["coverUrl"]:
            media_io.download_still(article["coverUrl"], episode_dir / "cover.jpg", client)
        for m in article["media"]:
            dest = media_dir / f"{m['id']}.jpg"
            if m["type"] == "embed":
                continue  # embeds have no still to show
            is_video = m["sourceUrl"].lower().split("?")[0].endswith((".mp4", ".webm", ".mov"))
            try:
                if m.get("poster"):
                    media_io.download_still(urljoin(article["url"], m["poster"]), dest, client)
                elif is_video:
                    media_io.video_still(m["sourceUrl"], dest, m.get("loop", False))
                else:
                    media_io.download_still(m["sourceUrl"], dest, client)
                m["file"] = f"media/{dest.name}"
            except Exception as err:  # one broken asset shouldn't sink the episode
                m["error"] = str(err)
    if not (episode_dir / "cover.jpg").exists():
        first = next((m for m in article["media"] if m.get("file")), None)
        if first:
            (episode_dir / "cover.jpg").write_bytes((episode_dir / first["file"]).read_bytes())


def episode_id(article: dict) -> str:
    slug = urlparse(article["url"]).path.rstrip("/").rsplit("/", 1)[-1].lower()
    slug = re.sub(r"[^a-z0-9-]+", "-", slug).strip("-")[:60]
    return f"{article['publishedAt'] or datetime.now().date().isoformat()}-{slug}"


def run(url: str, output: Path) -> Path:
    html = httpx.get(url, headers={"User-Agent": media_io.UA}, follow_redirects=True, timeout=30) \
        .raise_for_status().text
    article = extract(url, html)
    episode_dir = output / "episodes" / episode_id(article)
    episode_dir.mkdir(parents=True, exist_ok=True)
    download_media(article, episode_dir)
    (episode_dir / "article.json").write_text(json.dumps(article, indent=1, ensure_ascii=False), encoding="utf-8")
    return episode_dir
