# /// script
# requires-python = ">=3.11,<3.12"
# dependencies = ["f5-tts", "soundfile", "numpy", "torch==2.8.*", "torchaudio==2.8.*"]
# [tool.uv]
# override-dependencies = ["torch==2.8.*", "torchaudio==2.8.*"]
# [tool.uv.sources]
# torch = { index = "pytorch-cu128" }
# torchaudio = { index = "pytorch-cu128" }
# [[tool.uv.index]]
# name = "pytorch-cu128"
# url = "https://download.pytorch.org/whl/cu128"
# explicit = true
# ///
# torch pinned to 2.8: torchaudio 2.9+ loads audio through torchcodec, which needs shared FFmpeg DLLs.
import json
import sys
from pathlib import Path

import torch
from f5_tts.api import F5TTS

sys.path.insert(0, str(Path(__file__).parent.parent))
from common import ROOT, run  # noqa: E402

# Voice-cloning only: the reference is Kokoro af_heart reading segment s1.
REF = ROOT / "out" / "ref-af_heart-s1.wav"
REF_TEXT = json.loads((ROOT / "test-script.json").read_text(encoding="utf-8"))["segments"][0]["text"]

model = F5TTS(model="F5TTS_v1_Base", device="cuda")


def synth(text):
    wav, sr, _ = model.infer(ref_file=str(REF), ref_text=REF_TEXT, gen_text=text, seed=7, remove_silence=False)
    return wav, sr


run("f5", "clone-kokoro_af_heart", synth)
print(f"peak VRAM {torch.cuda.max_memory_allocated() / 2**30:.1f} GiB")
