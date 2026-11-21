---
name: setup
description: Set up the local environment the article-to-podcast skill needs - uv, PyTorch, and the Kokoro TTS model - and check it with a test render. Use when the user installs the pagecast plugin, asks to set it up, or a podcast command fails because the environment isn't installed.
allowed-tools: Bash(uv run --no-sync --project "${CLAUDE_PLUGIN_ROOT}" audio-presenter doctor) Bash(uv sync --project "${CLAUDE_PLUGIN_ROOT}") Bash(uv sync --project "${CLAUDE_PLUGIN_ROOT}" --no-group cuda --group cpu) Bash(uv --version) Bash(nvidia-smi)
---

# Setup

Get the user from a fresh install to a working test render. Narration is written by Claude itself, so no API key or LLM server is needed. The only model that runs locally is Kokoro-82M for speech. ffmpeg ships inside the Python environment, so there's nothing to install for it.

## Steps

1. Check `uv --version`. If it's missing, show the install command for their OS and ask before running it:
   - macOS and Linux: `curl -LsSf https://astral.sh/uv/install.sh | sh`
   - Windows: `powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"`
2. Check `nvidia-smi`, unless they're on a Mac. It decides which PyTorch build to install. Tell the user which one they'll get:
   - It works: the CUDA build, about 3 GB, and fast rendering.
   - It fails: the CPU build, a few hundred MB. Rendering works, just slower.
3. Install, after telling the user the download size. uv installs Python 3.12 on its own if needed.
   - NVIDIA GPU or a Mac: `uv sync --project "${CLAUDE_PLUGIN_ROOT}"`
   - No NVIDIA GPU: `uv sync --project "${CLAUDE_PLUGIN_ROOT}" --no-group cuda --group cpu`
4. `uv run --no-sync --project "${CLAUDE_PLUGIN_ROOT}" audio-presenter doctor`. The first run downloads the Kokoro weights, about 330 MB. It must end with `ok`.
5. Tell the user they're ready. If they came here from an episode request, carry on with it. Otherwise say `/pagecast:article-to-podcast <url>` makes an episode.

Every command runs with `--no-sync`, so uv doesn't swap the CPU build back to CUDA. Rerun step 3 after a plugin update that adds dependencies.

## When Something Fails

- `uv sync` fails on torch on an Intel Mac: PyTorch dropped Intel Mac wheels. Tell the user it's unsupported.
- Kokoro download fails: it comes from Hugging Face. Check the network or a proxy.
