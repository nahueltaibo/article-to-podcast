---
name: setup
description: Set up the local environment the article-to-podcast skill needs - uv, ffmpeg, PyTorch, and the Kokoro TTS model - and check it with a test render. Use when the user installs the pagecast plugin, asks to set it up, or a podcast build fails on a missing dependency.
allowed-tools: Bash(uv run --project "${CLAUDE_PLUGIN_ROOT}" audio-presenter doctor) Bash(uv sync --project "${CLAUDE_PLUGIN_ROOT}") Bash(uv --version) Bash(ffmpeg -version) Bash(nvidia-smi)
---

# Setup

Get the user from a fresh install to a working test render. Narration is written by Claude itself, so no API key or LLM server is needed. The only model that runs locally is Kokoro-82M for speech.

## Steps

1. Check `uv --version`. If it's missing, show the install command for their OS and ask before running it:
   - macOS and Linux: `curl -LsSf https://astral.sh/uv/install.sh | sh`
   - Windows: `powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"`
2. Check `ffmpeg -version`. If it's missing, show the install command and ask before running it:
   - macOS: `brew install ffmpeg`
   - Debian and Ubuntu: `sudo apt install ffmpeg`
   - Windows: `winget install Gyan.FFmpeg`, then restart the terminal so PATH picks it up.
3. Check `nvidia-smi`. It's fine if it fails: Kokoro runs on CPU too, only slower. Tell the user which one they'll get.
4. `uv sync --project "${CLAUDE_PLUGIN_ROOT}"`. Warn first that it downloads PyTorch, which is about 3 GB with CUDA. uv installs Python 3.12 on its own if needed.
5. `uv run --project "${CLAUDE_PLUGIN_ROOT}" audio-presenter doctor`. The first run downloads the Kokoro weights, about 330 MB. It must end with `ok`.
6. Tell the user they're ready, and that `/pagecast:article-to-podcast <url>` makes an episode. Episodes go to `output/` in the folder they run it from, so suggest a fixed folder, or setting `AUDIO_PRESENTER_OUTPUT`.

## When Something Fails

- `uv sync` fails on torch on an Intel Mac: PyTorch dropped Intel Mac wheels. Tell the user it's unsupported.
- `doctor` says ffmpeg is missing right after installing it: the terminal running Claude Code still has the old PATH. Ask them to restart it.
- Kokoro download fails: it comes from Hugging Face. Check the network or a proxy.
