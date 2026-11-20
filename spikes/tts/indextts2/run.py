# /// script
# requires-python = ">=3.11,<3.12"
# dependencies = ["indextts", "soundfile", "numpy", "torch==2.8.*", "torchaudio==2.8.*"]
# [tool.uv]
# constraint-dependencies = ["kaldifst==1.7.17"]
# [tool.uv.sources]
# indextts = { path = "repo" }
# torch = { index = "pytorch-cu128" }
# torchaudio = { index = "pytorch-cu128" }
# [[tool.uv.index]]
# name = "pytorch-cu128"
# url = "https://download.pytorch.org/whl/cu128"
# explicit = true
# ///
import json
import sys
import tempfile
from pathlib import Path

import soundfile as sf
import torch
from indextts.infer_v2 import IndexTTS2

sys.path.insert(0, str(Path(__file__).parent.parent))
from common import ROOT, run  # noqa: E402

HERE = Path(__file__).parent
CKPT = HERE / "checkpoints"
# Voice-cloning only: the reference is Kokoro af_heart reading segment s1.
REF = ROOT / "out" / "ref-af_heart-s1.wav"
FP16 = "--fp16" in sys.argv

tts = IndexTTS2(cfg_path=str(CKPT / "config.yaml"), model_dir=str(CKPT), use_fp16=FP16,
                use_cuda_kernel=False, use_deepspeed=False)
tmp = Path(tempfile.mkdtemp())


def synth(text):
    out = tmp / "seg.wav"
    tts.infer(spk_audio_prompt=str(REF), text=text, output_path=str(out), verbose=False)
    audio, sr = sf.read(out, dtype="float32")
    return audio, sr


run("indextts2", f"clone-kokoro_af_heart{'-fp16' if FP16 else ''}", synth)
print(f"peak VRAM {torch.cuda.max_memory_allocated() / 2**30:.1f} GiB")
