from audio_presenter import subscribe


def test_page_links_the_app_to_the_feed(tmp_path):
    page = subscribe.write(tmp_path, "http://192.168.1.10:8000/").read_text(encoding="utf-8")
    assert 'href="pagecast://add?feed=http%3A%2F%2F192.168.1.10%3A8000%2Fpodcast.xml"' in page
    assert "<svg" in page
    assert subscribe.APK_URL in page
