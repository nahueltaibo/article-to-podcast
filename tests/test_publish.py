import json

from audio_presenter import publish


def test_publish_copies_only_served_files_and_writes_the_feed(tmp_path):
    episode = tmp_path / "output" / "episodes" / "2026-01-01-post"
    (episode / "media").mkdir(parents=True)
    presentation = {
        "id": "2026-01-01-post", "generatedAt": "2026-01-02T00:00:00+00:00", "description": "d", "cover": None,
        "article": {"url": "https://x.test/post", "title": "Post"}, "audio": {"durationMs": 1000},
        "chapters": [{"startMs": 0, "title": "Post", "image": None}],
    }
    (episode / "presentation.json").write_text(json.dumps(presentation))
    for name in ("audio.mp3", "narration.mp3", "chime.wav", "article.json", "script.json", "media/m1.jpg"):
        (episode / name).write_bytes(b"x")
    site = tmp_path / "site"

    feed = publish.run(tmp_path / "output", site, "https://example.github.io/feed/")

    published = site / "episodes" / "2026-01-01-post"
    assert sorted(p.name for p in published.iterdir()) == [
        "audio.mp3", "chapters.json", "chime.wav", "media", "narration.mp3", "presentation.json"]
    assert (published / "media" / "m1.jpg").exists()
    assert "https://example.github.io/feed/episodes/2026-01-01-post/presentation.json" in feed.read_text()
    assert (site / ".nojekyll").exists()
