import json
import xml.etree.ElementTree as ET

from audio_presenter import feed


def test_feed_links_each_episode_presentation(tmp_path):
    episode = tmp_path / "episodes" / "2026-01-01-post"
    episode.mkdir(parents=True)
    (episode / "audio.mp3").write_bytes(b"x" * 10)
    (episode / "presentation.json").write_text(json.dumps({
        "id": "2026-01-01-post",
        "article": {"url": "https://x.test/post", "title": "Post", "site": "x.test"},
        "description": "About the post.",
        "generatedAt": "2026-01-02T00:00:00+00:00",
        "audio": {"durationMs": 61000},
        "cover": None,
        "chapters": [{"startMs": 0, "endMs": 61000, "title": "Post", "image": None}],
    }), encoding="utf-8")

    rss = ET.parse(feed.run(tmp_path, "http://host:8000/"))
    link = rss.find(f".//item/{{{feed.PAGECAST_NS}}}presentation")
    assert link.get("url") == "http://host:8000/episodes/2026-01-01-post/presentation.json"


def test_feed_tags_source_and_categories_and_sorts_by_publish_date(tmp_path):
    for name, day in (("old", "2025-05-01"), ("new", "2026-05-01")):
        episode = tmp_path / "episodes" / name
        episode.mkdir(parents=True)
        (episode / "audio.mp3").write_bytes(b"x")
        (episode / "presentation.json").write_text(json.dumps({
            "id": name,
            "article": {"url": f"https://x.test/{name}", "title": name, "site": "x.test", "publishedAt": day},
            "source": "Anthropic",
            "categories": ["Science", "Economics"],
            "description": "d",
            # Generated in the opposite order, so the sort has to use publishedAt.
            "generatedAt": "2026-06-01T00:00:00+00:00" if name == "old" else "2026-01-01T00:00:00+00:00",
            "audio": {"durationMs": 1000},
            "cover": None,
            "chapters": [],
        }), encoding="utf-8")

    items = ET.parse(feed.run(tmp_path, "http://host")).findall(".//item")
    assert [i.findtext("title") for i in items] == ["new", "old"]
    assert [c.text for c in items[0].findall("category")] == ["Science", "Economics"]
    assert items[0].findtext(f"{{{feed.PAGECAST_NS}}}source") == "Anthropic"
