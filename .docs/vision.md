# Vision and Features

Audio Presenter turns written articles into podcast episodes. Each episode has a recorded narration, and the article's images show up as chapter art at the moment the narration reaches them. You listen in a podcast app you already use. You only look at the screen when there's something to see.

## How It Feels to Use

- You give it an article URL. A few minutes later there's a new episode in your podcast app.
- You hit play and put the phone in your pocket.
- When the narration reaches a chart or photo, you hear a short chime and a spoken pointer, for example "There's a chart on screen." The image is the current chapter art in the player.
- If you don't look, you still get the gist: the narration describes what the image shows.

## First Iteration

From one article URL to one episode, playable in AntennaPod with images in sync.

- **Extraction.** Pull the article body into ordered blocks: headings, paragraphs, images, captions, quotes, code, tables.
- **Narration.** Claude Code, on your Anthropic subscription, rewrites the article for listening. It stays faithful to the text, describes images, and summarizes code and tables instead of reading them out.
- **Recorded voice.** The TTS engine picked in the [TTS spike](spikes/tts.md) renders one segment per paragraph.
- **Synced images.** An MP3 with embedded chapters, where each chapter carries the image on screen. A chime is mixed into the audio before each new image.
- **Podcast package.** Episode folder plus a podcast feed that AntennaPod can subscribe to.

## Later

- **Sources.** Configured sites checked for new articles, with Anthropic research first.
- **Scheduled generation.** A Claude Code cloud routine that runs without the PC.
- **Own player, or a patch to AntennaPod.** Only if vibration on images or auto-pause at images turn out to be worth it.

## Not Planned

- TTS on the phone. The voice quality isn't good enough.
- Paywalled or login-only sources.
