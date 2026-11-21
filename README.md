# Pagecast

Turns an article URL into a podcast episode. You get a recorded narration, and the article's images show up as chapter art when the narration reaches them. You listen in a podcast app you already use, such as AntennaPod.

Claude Code writes the narration on your own Claude subscription. Speech comes from [Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M), which runs on your machine, so there's no API key and no TTS bill.

## Install the Claude Code Plugin

In Claude Code:

```text
/plugin marketplace add nahueltaibo/article-to-podcast
/plugin install pagecast@pagecast
/pagecast:setup
```

`setup` checks for [uv](https://docs.astral.sh/uv/) and ffmpeg, installs PyTorch and Kokoro, and renders a test sentence. Expect about 3 GB of downloads with CUDA. An NVIDIA GPU makes rendering fast, but CPU works too.

Then make an episode:

```text
/pagecast:article-to-podcast https://example.com/some-article
```

Episodes land in `output/` in the folder you run it from. Set `AUDIO_PRESENTER_OUTPUT` to keep them in one place. Ask Claude to start the feed server, then subscribe to the URL it prints from a podcast app on the same network.

## What Works Where

- **Sites:** the extractor is tuned on Anthropic's research posts. Other well-structured article pages usually work. Paywalled and login-only pages don't.
- **Platforms:** Windows, Linux, and Apple Silicon Macs. Intel Macs aren't supported, since PyTorch no longer ships wheels for them.
- **Players:** AntennaPod shows the chapter images. Pocket Casts needs a public feed, which this doesn't host.

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
