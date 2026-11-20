# /// script
# requires-python = ">=3.11,<3.12"
# dependencies = ["chatterbox-tts", "soundfile", "numpy", "torch>=2.7", "torchaudio>=2.7"]
# [tool.uv]
# override-dependencies = ["torch>=2.7", "torchaudio>=2.7"]
# [tool.uv.sources]
# torch = { index = "pytorch-cu128" }
# torchaudio = { index = "pytorch-cu128" }
# [[tool.uv.index]]
# name = "pytorch-cu128"
# url = "https://download.pytorch.org/whl/cu128"
# explicit = true
# ///
import sys
from pathlib import Path

import torch
from chatterbox.tts import ChatterboxTTS

sys.path.insert(0, str(Path(__file__).parent.parent))
from common import run  # noqa: E402

model = ChatterboxTTS.from_pretrained(device="cuda")
# Fixed seed so every segment starts from the same voice state.
EXAGGERATION = float(sys.argv[1]) if len(sys.argv) > 1 else 0.5


def synth(text):
    torch.manual_seed(7)
    wav = model.generate(text, exaggeration=EXAGGERATION, cfg_weight=0.5)
    return wav.squeeze(0).cpu().numpy(), model.sr


run("chatterbox", f"default-ex{EXAGGERATION}", synth)
