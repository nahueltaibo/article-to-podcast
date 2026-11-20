# /// script
# requires-python = ">=3.10,<3.13"
# dependencies = [
#   "kokoro>=0.9.4",
#   "soundfile",
#   "numpy",
#   "torch",
#   "pip",
#   "en-core-web-sm @ https://github.com/explosion/spacy-models/releases/download/en_core_web_sm-3.8.0/en_core_web_sm-3.8.0-py3-none-any.whl",
# ]
# [tool.uv.sources]
# torch = { index = "pytorch-cu128" }
# [[tool.uv.index]]
# name = "pytorch-cu128"
# url = "https://download.pytorch.org/whl/cu128"
# explicit = true
# ///
import sys
from pathlib import Path

import numpy as np
import torch
from kokoro import KPipeline

sys.path.insert(0, str(Path(__file__).parent.parent))
from common import run  # noqa: E402

VOICES = sys.argv[1:] or ["af_heart", "am_michael", "bf_emma"]
print("CUDA:", torch.cuda.is_available(), torch.cuda.get_device_name(0) if torch.cuda.is_available() else "")
device = "cuda" if torch.cuda.is_available() else "cpu"
pipelines = {}

for voice in VOICES:
    lang = voice[0]  # a = American, b = British
    pipe = pipelines.setdefault(lang, KPipeline(lang_code=lang, repo_id="hexgrad/Kokoro-82M", device=device))

    def synth(text, pipe=pipe, voice=voice):
        chunks = [a.cpu().numpy() if hasattr(a, "cpu") else a for _, _, a in pipe(text, voice=voice, speed=1.0)]
        return np.concatenate(chunks), 24000

    run("kokoro", voice, synth)
