from audio_presenter.extract import extract

HTML = """<html><head><meta property="og:image" content="https://x.test/cover.png"></head><body>
<article>
 <header><h1>My Title</h1><nav class="TableOfContents"><ul><li>01 Intro</li></ul></nav>
  <dl><div><dt>Published</dt><dd>Mar 24, 2026</dd></div></dl></header>
 <div class="wrap">
  <div class="intro"><p>Written by Someone.</p></div>
  <aside><ul><li>Related post</li></ul></aside>
  <div class="body">
   <p>First paragraph with a note<sup><a role="doc-noteref" href="#footnote-1">1</a></sup>.</p>
   <h2>Section</h2>
   <p>Second.</p>
   <ul><li><strong>A:</strong> one</li><li>two</li></ul>
   <div role="table">
    <div role="row"><div role="columnheader"><p>Name</p></div><div role="columnheader"><p>Cost</p></div></div>
    <div role="row"><div role="cell"><p>Solo</p></div><div role="cell"><p>$9</p></div></div>
   </div>
   <figure><img src="/a.png" alt=""><figcaption>A caption</figcaption></figure>
   <figure><video loop autoplay src="https://x.test/v.mp4"></video><figcaption>Loop</figcaption></figure>
   <blockquote><p>Quoted.</p></blockquote>
   <pre>@techreport{x, title={y}}</pre>
   <pre>print("hi")</pre>
  </div>
  <div class="Footnotes"><p id="footnote-1"><span class="Sup">01</span>The note.<a role="doc-backlink" href="#">back</a></p></div>
 </div>
</article></body></html>"""


def test_extract_blocks_and_metadata():
    a = extract("https://x.test/research/my-post", HTML)
    assert a["title"] == "My Title"
    assert a["publishedAt"] == "2026-03-24"
    assert a["site"] == "x.test"
    types = [b["type"] for b in a["blocks"]]
    assert types == ["paragraph", "paragraph", "heading", "paragraph", "list", "table", "media", "media", "quote", "code"]
    assert a["blocks"][0]["text"] == "Written by Someone."
    assert a["blocks"][1]["text"] == "First paragraph with a note[^1]."
    assert a["blocks"][4]["items"] == ["A: one", "two"]
    assert a["blocks"][5] == {"type": "table", "header": ["Name", "Cost"], "rows": [["Solo", "$9"]], "id": "b6"}
    assert a["blocks"][9]["text"] == 'print("hi")'
    assert a["footnotes"] == {"1": "The note."}


def test_extract_media():
    m = extract("https://x.test/research/my-post", HTML)["media"]
    assert m[0] | {} == {"id": "m1", "type": "image", "sourceUrl": "https://x.test/a.png", "alt": None, "caption": "A caption"}
    assert m[1]["type"] == "animation" and m[1]["loop"] is True and m[1]["caption"] == "Loop"
