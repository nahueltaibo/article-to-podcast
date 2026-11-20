---
name: article-to-podcast
description: Turn an article URL into a podcast episode with narration and chapter images in sync. Use when the user gives an article URL and wants it as a podcast episode, or says /article-to-podcast <url>.
---

# Article to Podcast

You do one thing the scripts can't: write the narration. Everything else is a command.

## Steps

1. `uv run audio-presenter extract <url>`. It prints the episode folder, for example `output/episodes/2026-03-24-harness-design-long-running-apps`.
2. Read `article.json` in that folder.
3. Open every still in `media/` with the Read tool. You can't describe an image you haven't looked at.
4. Write `script.json` in the episode folder, following the rules below.
5. `uv run audio-presenter validate <folder>`. Fix every error. Each warning means an image is never shown, so make sure skipping it was deliberate.
6. `uv run audio-presenter build <folder>`. It renders the audio and embeds the chapters.
7. `uv run audio-presenter feed`.
8. Tell the user the episode length and chapter count. If the server isn't running, tell them to start it with `uv run audio-presenter serve` and subscribe in AntennaPod to the URL it prints.

## script.json

```json
{
  "description": "One or two sentences on what the article is about, for the podcast app.",
  "segments": [
    { "id": "s1", "kind": "title", "text": "Harness design for long-running application development. By Prithvi Rajasekaran, from Anthropic. Published March 24, 2026." },
    { "id": "s2", "kind": "paragraph", "text": "..." },
    { "id": "s3", "kind": "heading", "text": "Running the harness." },
    { "id": "s4", "kind": "media", "media": "m2", "text": "There's a screenshot on screen. It shows ..." }
  ]
}
```

Kinds: `title`, `heading`, `paragraph`, `media`, `quote`, `list`, `code-summary`, `table-summary`, `footnote`, `outro`. Any segment can carry `media`, but use `kind: media` when the segment is mainly about the image.

## Narration Rules

**Stay faithful.** Keep the author's words and voice, in first person if they wrote that way. Paragraphs map one to one onto segments. Don't summarize, editorialize, or skip arguments.

**Only rewrite what doesn't work spoken:**

- URLs and link text: drop the URL, keep the words.
- Footnote markers `[^n]`: drop the marker. If the footnote adds something a listener needs, add it as a short `footnote` segment right after: "A side note: ...". Otherwise skip it.
- Acronyms: expand on first use when the article doesn't, for example "Generative Adversarial Networks, or GANs".
- Symbols: write "5 to 15", not "5–15". Write "about 20 times", not "20x". Dollar amounts and decimals are fine as digits.
- Lists: one `list` segment that reads naturally, for example "First, ... Second, ... And finally, ...".
- Tables: one `table-summary` segment that says what the table compares and the numbers that matter. Never read it cell by cell.
- Code: one `code-summary` sentence on what the code does or shows. Never read code aloud. Long code like an appendix plan gets one sentence, or is skipped.
- Quotes: introduce them, for example "Quote: ... End quote." Use `kind: quote`.

**Images, animations, videos.** Put a `media` segment where the image appears in the article, after the paragraph that introduces it.

- Open with a pointer: "There's a chart on screen.", "There's a screenshot on screen.", or "There's an animation on screen."
- Then describe what the image shows that the text doesn't already say: the trend, the comparison, the key numbers. Two or three sentences, from what you actually saw.
- For animations and videos, the listener only sees a still. Say what happens in motion, and that the full version is in the original article.
- In carousels of similar screenshots, show each one, but keep each description to a sentence.
- Skip decorative images. An image that's just the article's hero art doesn't need a segment.
- Embeds without a still, such as iframes, get one spoken sentence pointing to the original article, with no `media`.

**Structure.**

- `s1` is the title, then authors if known, then the publish date in words.
- A `heading` segment for every heading, as its text with a period. Add `chapter` when the spoken text differs from the heading.
- Skip boilerplate: author bios, acknowledgements, citation blocks, "related articles", "read more" links, and subscription prompts.
- End with an `outro`: "That was <title>, from <site>."
