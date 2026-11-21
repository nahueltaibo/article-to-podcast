# Pagecast

Turns an article URL into a podcast episode. You get a recorded narration, and the article's images show up as chapter art when the narration reaches them. You listen in the Pagecast Android app, or in any podcast app such as AntennaPod.

Claude Code writes the narration on your own Claude subscription. Speech comes from [Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M), which runs on your machine, so there's no API key and no TTS bill.

## Get Started

You need [Claude Code](https://claude.com/claude-code) on a computer and an Android phone on the same Wi-Fi.

1. In Claude Code, install the plugin:

   ```text
   /plugin marketplace add nahueltaibo/pagecast
   /plugin install pagecast@pagecast
   ```

2. Make an episode:

   ```text
   /pagecast:article-to-podcast https://www.anthropic.com/research/harness-design-long-running-apps
   ```

The first run sets things up for you. It asks before installing [uv](https://docs.astral.sh/uv/), then downloads PyTorch and the Kokoro voice. That's about 3 GB with an NVIDIA GPU, or a few hundred MB on CPU. When the episode is ready, a subscribe page opens in your browser. Scan its QR code with your phone. From the phone, the page installs the [Pagecast app](https://github.com/nahueltaibo/pagecast/releases/latest/download/pagecast.apk) and adds your feed with one tap. Later episodes show up when you tap refresh in the app.

The computer serves the feed, so it has to be on while the phone downloads an episode. Downloaded episodes play offline. Episodes land in `output/` in the folder you run Claude from. Set `AUDIO_PRESENTER_OUTPUT` to keep them in one place.

## What Works Where

- **Sites:** the extractor is tuned on Anthropic's research posts. Other well-structured article pages usually work. Paywalled and login-only pages don't.
- **Platforms:** Windows, Linux, and Apple Silicon Macs. Intel Macs aren't supported, since PyTorch no longer ships wheels for them.
- **Players:** Pagecast shows the images full screen. AntennaPod shows them as chapter art, using the same feed URL. Pocket Casts needs a public feed, which this doesn't host.

## Develop

```bash
uv sync                       # add --no-default-groups --group dev to skip PyTorch
uv run pytest
claude --plugin-dir .         # load the plugin from this checkout
```

The Android app lives in [android](android). Build a debug APK with `./gradlew assembleDebug` from that folder, using JDK 17 and the Android SDK 36. Release builds need your own signing key. See [Build and release](.docs/release.md).

Design docs live in [.docs](.docs):

- [Vision and features](.docs/vision.md)
- [Architecture](.docs/architecture.md)
- [Presentation format](.docs/presentation-format.md)
- [Roadmap](.docs/roadmap.md)
- [TTS spike](.docs/spikes/tts.md)

## License

[MIT](LICENSE)
