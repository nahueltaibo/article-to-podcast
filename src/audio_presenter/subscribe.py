"""Write output/index.html: the page a phone opens to install Pagecast and add this feed."""
from html import escape
from pathlib import Path
from urllib.parse import quote

import segno

APK_URL = "https://github.com/nahueltaibo/pagecast/releases/latest/download/pagecast.apk"


def add_link(feed_url: str) -> str:
    return f"pagecast://add?feed={quote(feed_url, safe='')}"


def write(output: Path, base: str) -> Path:
    base = base.rstrip("/")
    page_url = f"{base}/"
    feed_url = f"{base}/podcast.xml"
    qr = segno.make(page_url, error="m").svg_inline(scale=8, border=2, dark="#111", light="#fff")
    html = TEMPLATE.format(qr=qr, page=escape(page_url), feed=escape(feed_url),
                           add=escape(add_link(feed_url)), apk=escape(APK_URL))
    path = output / "index.html"
    output.mkdir(parents=True, exist_ok=True)
    path.write_text(html, encoding="utf-8")
    return path


TEMPLATE = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Add Pagecast feed</title>
<style>
  :root {{ --bg: #f6f6f4; --fg: #1b1b1f; --muted: #5c5c66; --card: #fff; --accent: #3a5bd9; --on-accent: #fff; }}
  @media (prefers-color-scheme: dark) {{ :root {{ --bg: #141418; --fg: #ececf1; --muted: #a0a0ab; --card: #1f1f26; --accent: #8aa2ff; --on-accent: #141418; }} }}
  body {{ margin: 0; background: var(--bg); color: var(--fg); font: 16px/1.5 system-ui, sans-serif; }}
  main {{ max-width: 34rem; margin: 0 auto; padding: 24px 16px 48px; }}
  h1 {{ font-size: 1.6rem; margin: 0 0 4px; }}
  .muted {{ color: var(--muted); }}
  .card {{ background: var(--card); border-radius: 12px; padding: 16px; margin: 16px 0; }}
  .qr {{ text-align: center; }} .qr svg {{ max-width: 100%; height: auto; }}
  ol {{ padding-left: 1.2rem; }} li {{ margin: 8px 0; }}
  a {{ color: var(--accent); }}
  a.button {{ display: inline-block; background: var(--accent); color: var(--on-accent); padding: 10px 16px;
             border-radius: 8px; text-decoration: none; font-weight: 600; }}
  code {{ word-break: break-all; }}
</style>
</head>
<body>
<main>
  <h1>Listen on your phone</h1>
  <p class="muted">Scan this from your phone's camera. It opens this page there.</p>
  <div class="card qr">{qr}</div>
  <div class="card">
    <ol>
      <li>Connect the phone to the same Wi-Fi as this computer.</li>
      <li>Install Pagecast, if you haven't: <a href="{apk}">download the APK</a>. Android asks you to allow installs from your browser once.</li>
      <li><a class="button" href="{add}">Add to Pagecast</a></li>
      <li>Tap an episode to download it, then play. New episodes show up when you tap refresh.</li>
    </ol>
  </div>
  <div class="card">
    <strong>Not working?</strong>
    <ul>
      <li>The page won't open on the phone: the computer's firewall may block port 8000, or the phone is on another network.</li>
      <li>The button does nothing: add the feed by hand with + in Pagecast, using <code>{feed}</code></li>
      <li>Episodes stop loading later: the computer is off or asleep, or its address changed. Run a new episode and scan again.</li>
    </ul>
    <p class="muted">Other podcast apps, such as AntennaPod, can subscribe to the same feed URL.</p>
  </div>
</main>
</body>
</html>
"""
