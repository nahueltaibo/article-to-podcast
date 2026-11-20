"""Writes out/index.html: every sample under a random letter, with names hidden until you reveal them."""
import json
import random
from pathlib import Path

OUT = Path(__file__).parent / "out"
samples = sorted(OUT.glob("*.json"))
random.shuffle(samples)

rows = []
for letter, stats_file in zip("ABCDEFGHIJKLMNOPQRSTUVWXYZ", samples):
    s = json.loads(stats_file.read_text(encoding="utf-8"))
    rows.append(f"""<section><h2>Sample {letter}</h2>
<audio controls preload="none" src="{stats_file.stem}.mp3"></audio>
<p class="name">{s['engine']} · {s['voice']} · {s['audio_s']}s · RTF {s['rtf']}</p></section>""")

(OUT / "index.html").write_text(f"""<!doctype html><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>TTS Blind Test</title>
<style>
:root {{ --bg: #fff; --fg: #1a1a1a; --muted: #666; }}
@media (prefers-color-scheme: dark) {{ :root {{ --bg: #141414; --fg: #eee; --muted: #999; }} }}
body {{ background: var(--bg); color: var(--fg); font: 16px system-ui, sans-serif; margin: 0 auto; max-width: 640px; padding: 16px; }}
audio {{ width: 100%; }}
.name {{ color: var(--muted); display: none; }}
body.revealed .name {{ display: block; }}
button {{ font: inherit; padding: 8px 16px; }}
</style>
<h1>TTS Blind Test</h1>
<p>Score each sample 1–5 for naturalness before revealing.</p>
{''.join(rows)}
<button onclick="document.body.classList.toggle('revealed')">Reveal engines</button>
""", encoding="utf-8")
print(f"{len(rows)} samples -> {OUT / 'index.html'}")
