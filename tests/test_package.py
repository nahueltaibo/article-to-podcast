from audio_presenter.package import build_chapters


def presentation(segments):
    return {
        "article": {"title": "T"},
        "cover": "cover.jpg",
        "audio": {"durationMs": 1000},
        "media": [{"id": "m1", "file": "media/m1.jpg", "caption": "Chart"}],
        "segments": segments,
    }


def seg(id, kind, start, **extra):
    return {"id": id, "kind": kind, "text": f"{id}.", "startMs": start, **extra}


def test_chapters_carry_the_image_on_screen():
    chapters = build_chapters(presentation([
        seg("s1", "title", 0),
        seg("s2", "paragraph", 100),
        seg("s3", "heading", 200),
        seg("s4", "media", 300, media="m1"),
        seg("s5", "heading", 600, chapter="Short name"),
    ]))
    assert [(c["startMs"], c["endMs"], c["title"], c["image"]) for c in chapters] == [
        (0, 200, "T", "cover.jpg"),
        (200, 300, "s3", "cover.jpg"),
        (300, 600, "Chart", "media/m1.jpg"),
        (600, 1000, "Short name", "media/m1.jpg"),  # last image stays on screen
    ]


def test_script_chapter_title_beats_caption():
    chapters = build_chapters(presentation([seg("s1", "title", 0), seg("s2", "media", 100, media="m1", chapter="Mine")]))
    assert chapters[1]["title"] == "Mine"


def test_media_at_same_start_replaces_previous_chapter():
    chapters = build_chapters(presentation([seg("s1", "title", 0, media="m1")]))
    assert len(chapters) == 1 and chapters[0]["image"] == "media/m1.jpg"
