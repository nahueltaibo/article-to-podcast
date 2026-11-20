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
