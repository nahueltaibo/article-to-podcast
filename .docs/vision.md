# Vision and Features

Audio Presenter is an Android app that turns written articles into a personal podcast. You pick the sources, it fetches new posts, narrates them with AI, and shows each article's images at the moment the narration reaches them.

You listen to articles the way you'd listen to a podcast. You only look at the screen when there's something to see.

## How It Feels to Use

- New posts from your sources show up in a queue, already narrated and downloaded.
- You hit play, put the phone in your pocket, and listen. The queue auto-advances like a podcast.
- When the narration reaches a chart or photo, a short chime and a vibration tell you to look. The image is on screen and on the lock screen.
- If you don't look, you still get the gist: the narration describes what the image shows.

## Features

### MVP

- **Sources.** Add a source by URL. The app detects RSS/Atom, a sitemap, or a listing page. Anthropic research ships as a built-in preset.
- **Discovery.** Periodic background checks for new articles. Configurable interval per source.
- **Extraction.** Pull the article body into ordered blocks: headings, paragraphs, images, captions, quotes, code, tables.
- **Narration script.** AI rewrites the blocks for listening. It keeps the text faithful, describes images, and summarizes code and tables instead of reading them out.
- **Speech.** Text-to-speech per segment, cached on the device for offline listening.
- **Synced media.** Images appear on screen in sync with the narration, with the chime and vibration cue.
- **Player.** Background playback, lock screen and Bluetooth controls, speed control, skip by section.
- **Queue.** Played and unplayed state, auto-advance, remove or reorder episodes.

### Later

- "Pause on media" mode: auto-pause at each image until you tap or a timeout passes.
- Narration styles: verbatim, adapted (default), or summary.
- Per-source voices.
- Android Auto.
- Embedded video: announce it and save it for later.
- Highlight the sentence being spoken in a transcript view.

## Not Planned

- Social or sharing features.
- Paywalled or login-only sources.
- Publishing episodes as a public podcast feed.
