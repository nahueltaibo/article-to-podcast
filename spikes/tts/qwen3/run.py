# /// script
# requires-python = ">=3.12,<3.13"
# dependencies = ["qwen-tts", "soundfile", "numpy", "torch>=2.7", "torchaudio>=2.7"]
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
from qwen_tts import Qwen3TTSModel

sys.path.insert(0, str(Path(__file__).parent.parent))
from common import run  # noqa: E402

# sdpa instead of flash_attention_2: no prebuilt flash-attn wheels for Windows.
model = Qwen3TTSModel.from_pretrained("Qwen/Qwen3-TTS-12Hz-1.7B-CustomVoice", device_map="cuda:0",
                                      dtype=torch.bfloat16, attn_implementation="sdpa")
INSTRUCT = "Calm, clear podcast narration at a steady pace."

for speaker in sys.argv[1:] or ["Ryan", "Aiden"]:
    def synth(text, speaker=speaker):
        torch.manual_seed(7)
        wavs, sr = model.generate_custom_voice(text=text, language="English", speaker=speaker, instruct=INSTRUCT)
        return wavs[0], sr

    run("qwen3", speaker, synth)
