# Presentation Format

A presentation is one narrated article: a folder with the audio, the images, and a `presentation.json` that ties them together on a timeline. The generator writes it, and the app and the podcast export read it.

## Feed Repo Layout

```text
audio-presenter-feed/
  sources.yaml
  index.json
  podcast.xml
  state/seen.json
  episodes/
    2026-11-20-anthropic-formal-verification/
      script.json
      presentation.json
      audio.m4a
      chapters.json
      media/
        m1.webp
        m2.webp
```

Episode folders are named `<publish date>-<source id>-<slug>`, so they sort by date and never collide.

`script.json` is what Claude wrote: the segments with their text and media references, without timings. Everything else in the folder is built from it, so swapping the voice means rendering again from the script.

## presentation.json

```json
{
  "formatVersion": 1,
  "id": "2026-11-20-anthropic-formal-verification",
  "source": { "id": "anthropic-research", "name": "Anthropic Research" },
  "article": {
    "url": "https://www.anthropic.com/research/formal-verification",
    "title": "Formal verification with Claude",
    "authors": [],
    "publishedAt": "2026-11-20"
  },
  "generatedAt": "2026-11-20T19:02:11Z",
  "voice": { "engine": "kokoro", "voice": "af_heart" },
  "audio": { "file": "audio.m4a", "durationMs": 612000 },
  "segments": [
    { "id": "s1", "startMs": 0, "endMs": 4100, "kind": "title", "text": "Formal verification with Claude." },
    { "id": "s2", "startMs": 4100, "endMs": 31800, "kind": "paragraph", "text": "..." },
    { "id": "s3", "startMs": 31800, "endMs": 52300, "kind": "media", "text": "There's a chart on screen. It shows...", "media": "m1" }
  ],
  "media": [
    {
      "id": "m1",
      "type": "image",
      "file": "media/m1.webp",
      "caption": "Proof success rate by model",
      "alt": "Bar chart",
      "description": "Success rises from 12% to 61% across three model generations.",
      "sourceUrl": "https://www-cdn.anthropic.com/..."
    }
  ]
}
```

The example values are illustrative.

## Rules

- `segments` cover the audio end to end, in order, with no gaps or overlaps.
- `text` is what's spoken, so the app's transcript view shows exactly what you hear.
- `kind` is one of `title`, `heading`, `paragraph`, `media`, `quote`, `list`, `code-summary`, `table-summary`, `outro`.
- An image stays on screen from its segment's start until the next segment that brings new media.
- Every `media` reference must match an entry in `media`, and every file path is relative to the episode folder.
- A breaking change bumps `formatVersion`. The app skips presentations with a version it doesn't know.

## index.json

The app polls this file. It lists every episode, newest first, with just enough to render the queue without opening each presentation.

```json
{
  "formatVersion": 1,
  "updatedAt": "2026-11-20T19:02:30Z",
  "episodes": [
    {
      "id": "2026-11-20-anthropic-formal-verification",
      "title": "Formal verification with Claude",
      "source": "anthropic-research",
      "publishedAt": "2026-11-20",
      "durationMs": 612000,
      "mediaCount": 4,
      "path": "episodes/2026-11-20-anthropic-formal-verification/"
    }
  ]
}
```

## sources.yaml

```yaml
sources:
  - id: anthropic-research
    name: Anthropic Research
    type: sitemap
    url: https://www.anthropic.com/sitemap.xml
    include: /research/
    firstRunLimit: 5
    voice: default
```

## Podcast Export

- `chapters.json` is derived from the segments that bring media, in the Podcasting 2.0 JSON chapters format, with `img` pointing to the image.
- `podcast.xml` lists every episode with an enclosure for `audio.m4a` and a `<podcast:chapters>` link.
