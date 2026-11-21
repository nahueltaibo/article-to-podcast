# Presentation Format

One article becomes one episode folder. `presentation.json` is the full description of the episode, and everything a podcast app reads is built from it.

## Output Layout

```text
output/
  podcast.xml
  cover.jpg
  episodes/
    2026-03-24-harness-design-long-running-apps/
      article.json
      script.json
      presentation.json
      audio.mp3
      narration.mp3
      chime.wav
      chapters.json
      cover.jpg
      media/
        m1.jpg
        m2.jpg
```

| File | Written by | Purpose |
| --- | --- | --- |
| `article.json` | `extract` | Metadata, ordered content blocks, footnotes, media |
| `media/*.jpg`, `cover.jpg` | `extract` | Still images for chapter art |
| `script.json` | Claude | Episode description and narration segments, no timings |
| `presentation.json` | `build` | Segments with timings, the media shown, chapters |
| `audio.mp3` | `build` | Narration plus chimes, with embedded ID3 chapters and images |
| `narration.mp3`, `chime.wav` | `build` | The same audio with silence in place of each chime, and the chime alone, so Pagecast can make the chime optional |
| `chapters.json` | `feed` | Podcasting 2.0 chapters with absolute image URLs |
| `podcast.xml` | `feed` | Feed listing every built episode, newest first |

Episode folders are named `<publish date>-<URL slug>`, so they sort by date.

## script.json

The contract between Claude and the pipeline. The schema is [script.schema.json](../src/audio_presenter/schemas/script.schema.json), and the [skill](../skills/article-to-podcast/SKILL.md) has the writing rules.

```json
{
  "description": "One or two sentences for the podcast app.",
  "segments": [
    { "id": "s1", "kind": "title", "text": "Harness design for long-running application development. By ..." },
    { "id": "s23", "kind": "media", "media": "m1", "chapter": "The Dutch art museum website", "text": "There's a still from a video on screen. ..." }
  ]
}
```

- `kind` is one of `title`, `heading`, `paragraph`, `media`, `quote`, `list`, `code-summary`, `table-summary`, `footnote`, `outro`.
- `media` can be set on any segment. It plays the chime and changes the chapter image.
- `chapter` overrides the chapter title. It's useful for headings and for media without a caption.

## presentation.json

`script.json` plus timings, article metadata, and the chapters. Abridged from the real test episode:

```json
{
  "formatVersion": 1,
  "id": "2026-03-24-harness-design-long-running-apps",
  "article": { "url": "https://www.anthropic.com/research/...", "title": "...", "site": "anthropic.com", "publishedAt": "2026-03-24" },
  "description": "...",
  "generatedAt": "2026-11-20T19:07:02+00:00",
  "voice": { "engine": "kokoro", "voice": "af_heart" },
  "audio": { "file": "audio.mp3", "narrationFile": "narration.mp3", "chimeFile": "chime.wav", "durationMs": 2054125 },
  "cover": "cover.jpg",
  "segments": [
    { "id": "s1", "kind": "title", "text": "...", "startMs": 0, "endMs": 11900 }
  ],
  "media": [
    { "id": "m2", "type": "image", "file": "media/m2.jpg", "caption": "Initial screen when opening the app created by the solo harness.", "alt": null, "sourceUrl": "https://assets.claude.com/..." }
  ],
  "chapters": [
    { "startMs": 0, "endMs": 102325, "title": "Harness design for long-running application development", "image": "cover.jpg" }
  ]
}
```

- `segments` cover the audio end to end, in order, with no gaps. A segment's range includes its chime and the pause after it.
- `media[].type` is `image`, `animation` (a GIF or looping video), `video`, or `embed`.
- An `animation` also has `animatedFile`, a GIF that Pagecast plays in place of the still. Looping videos are converted to 720 px, 12 fps GIFs.
- `narration.mp3` shares `audio.mp3`'s timeline, so every timing applies to both. Pagecast plays it and adds the chime at each segment with media, unless the listener turns the chime off. Older episodes without `narrationFile` play `audio.mp3`, chime included.
- A breaking change bumps `formatVersion`.

## Chapters

Podcast apps show the current chapter's image and fall back to the episode cover when a chapter has none. That fallback would make images flicker away mid-article, so:

- A new chapter starts at the title, at each heading, and at each segment with media.
- Every chapter carries the image that should be on screen: its own, or the last one shown. Before the first image, that's the episode cover.
- Chapter titles are the article title, the heading text, or the media caption, in that order of kind. `chapter` in the script overrides them.
- Images are JPEGs no larger than 1400 px, since some apps reject large or WebP chapter art.

The ID3 chapters in `audio.mp3` and `chapters.json` hold the same chapters.

## podcast.xml

A standard RSS 2.0 feed with the iTunes and Podcasting 2.0 namespaces. Each item has:

- `enclosure` pointing to `audio.mp3`
- `podcast:chapters` pointing to `chapters.json`
- `pagecast:presentation` pointing to `presentation.json`, which is how the Pagecast app finds the timeline and media. Other apps ignore it.
- the episode cover as `itunes:image`
- the article URL as the link and in the description
- `generatedAt` as `pubDate`, so new episodes show up on top even for older articles
