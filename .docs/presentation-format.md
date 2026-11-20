# Presentation Format

One article becomes one episode folder. `presentation.json` is the full description of the episode, and everything a podcast app reads is built from it.

## Output Layout

```text
output/
  podcast.xml
  cover.jpg
  episodes/
    2026-11-20-formal-verification/
      article.json
      script.json
      presentation.json
      audio.mp3
      chapters.json
      media/
        m1.jpg
        m2.jpg
```

| File | Written by | Purpose |
| --- | --- | --- |
| `article.json` | Extract | Ordered content blocks from the page |
| `script.json` | Claude | Narration segments with text and image references, no timings |
| `presentation.json` | Render + Package | Segments with timings, image list, metadata |
| `audio.mp3` | Render + Package | Narration plus chimes, with embedded ID3 chapters and images |
| `chapters.json` | Package | Podcasting 2.0 chapters for apps that read them from the feed |
| `podcast.xml` | Package | Feed listing every episode folder |

Episode folders are named `<publish date>-<slug>`, so they sort by date.

## presentation.json

```json
{
  "formatVersion": 1,
  "id": "2026-11-20-formal-verification",
  "article": {
    "url": "https://www.anthropic.com/research/formal-verification",
    "title": "Formal verification with Claude",
    "authors": [],
    "publishedAt": "2026-11-20"
  },
  "generatedAt": "2026-11-20T19:02:11Z",
  "voice": { "engine": "kokoro", "voice": "af_heart" },
  "audio": { "file": "audio.mp3", "durationMs": 612000 },
  "segments": [
    { "id": "s1", "startMs": 0, "endMs": 4100, "kind": "title", "text": "Formal verification with Claude." },
    { "id": "s2", "startMs": 4100, "endMs": 31800, "kind": "paragraph", "text": "..." },
    { "id": "s3", "startMs": 31800, "endMs": 52300, "kind": "media", "text": "There's a chart on screen. It shows...", "media": "m1" }
  ],
  "media": [
    {
      "id": "m1",
      "type": "image",
      "file": "media/m1.jpg",
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

- `segments` cover the audio end to end, in order, with no gaps or overlaps. A segment's time range includes its chime and the pause after it.
- `text` is exactly what's spoken.
- `kind` is one of `title`, `heading`, `paragraph`, `media`, `quote`, `list`, `code-summary`, `table-summary`, `outro`.
- Every `media` reference matches an entry in `media`. All paths are relative to the episode folder.
- A breaking change bumps `formatVersion`.

## Chapters

Podcast apps show the current chapter's image and fall back to the episode cover when a chapter has none. That fallback would make images flicker away mid-article, so:

- A new chapter starts at the title, at each heading, and at each segment that brings an image.
- Every chapter carries the image that should be on screen: its own, or the last one shown. Before the first image, that's the episode cover.
- The episode cover is the article's lead image, or a default cover.
- Chapter titles are the heading text, or the image caption for image chapters.
- Images are resized to at most 1400 px on the long side and saved as JPEG, since some apps reject large or WebP chapter images.

The ID3 chapters in `audio.mp3` and `chapters.json` hold the same chapters.

## podcast.xml

A standard RSS 2.0 feed with the iTunes and Podcasting 2.0 namespaces. Each item has:

- `enclosure` pointing to `audio.mp3`
- `podcast:chapters` pointing to `chapters.json`
- The original article URL as the item link, with the article summary as the description
