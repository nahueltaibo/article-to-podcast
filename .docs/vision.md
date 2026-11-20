# Vision and Features

Audio Presenter turns written articles into a personal podcast. You pick the sources. A scheduled Claude Code routine on your Anthropic subscription narrates new posts into presentations. The Android app plays them with each article's images shown at the moment the narration reaches them.

You listen to articles the way you'd listen to a podcast. You only look at the screen when there's something to see.

## How It Feels to Use

- New posts from your sources show up in the app's queue, already narrated and downloaded.
- You hit play, put the phone in your pocket, and listen. The queue auto-advances like a podcast.
- When the narration reaches a chart or photo, a short chime and a vibration tell you to look. The image is on screen and on the lock screen.
- If you don't look, you still get the gist: the narration describes what the image shows.

## Features

### MVP

- **Sources.** A config file lists the sources. Each one is an RSS/Atom feed, a sitemap, or a listing page. Anthropic research is the first.
- **Scheduled generation.** A cloud routine checks the sources, narrates new articles, renders the audio, and publishes them to a private feed repo.
- **Narration.** Claude rewrites the article for listening. It keeps the text faithful, describes images, and summarizes code and tables instead of reading them out.
- **Quality voice.** A local TTS model or a paid voice API renders the audio. A spike decides which.
- **Presentation format.** One self-contained package per article: audio, images, transcript, and the timeline that syncs them. See [presentation-format.md](presentation-format.md).
- **Player app.** Background playback, lock screen and Bluetooth controls, speed control, images in sync with the chime and vibration cue.
- **Queue.** Played and unplayed state, auto-advance, offline downloads.

### Later

- Edit sources from the app. It commits the config change to the feed repo.
- "Pause on media" mode: auto-pause at each image until you tap or a timeout passes.
- Narration styles: verbatim, adapted (default), or summary.
- Per-source voices.
- Android Auto.
- Transcript view that highlights the segment being spoken.

## Not Planned

- TTS on the phone. The voice quality isn't good enough.
- Paywalled or login-only sources.
- A public podcast feed.
