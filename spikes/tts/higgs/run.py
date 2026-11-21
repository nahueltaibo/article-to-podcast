# /// script
# requires-python = ">=3.12,<3.13"
# dependencies = ["transformers>=5.3.0", "accelerate", "librosa", "soundfile", "numpy", "torch==2.8.*", "torchaudio==2.8.*"]
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
# The HF weights moved to the native transformers format, which the boson_multimodal repo code can't load.
# torch pinned to 2.8: torchaudio 2.9+ loads audio through torchcodec, which needs shared FFmpeg DLLs.
import os
import sys
import tempfile
from pathlib import Path

import soundfile as sf
import torch
from transformers import AutoProcessor, HiggsAudioV2ForConditionalGeneration

sys.path.insert(0, str(Path(__file__).parent.parent))
from common import run  # noqa: E402

REPO = "bosonai/higgs-audio-v2-generation-3B-base"
# Voice prompts shipped in github.com/boson-ai/higgs-audio, cloned outside the repo.
PROMPTS = Path(os.environ["LOCALAPPDATA"]) / "tts-spike" / "higgs" / "repo" / "examples" / "voice_prompts"

processor = AutoProcessor.from_pretrained(REPO, device_map="cuda")
model = HiggsAudioV2ForConditionalGeneration.from_pretrained(REPO, device_map="cuda", dtype=torch.bfloat16)
tmp = Path(tempfile.mkdtemp())


def turn(role, **content):
    return {"role": role, "content": [content]}


# Clone a shipped voice prompt: without a reference, Higgs picks a new voice for every segment.
for voice in sys.argv[1:] or ["en_woman", "en_man"]:
    ref_text = (PROMPTS / f"{voice}.txt").read_text(encoding="utf-8").strip()
    prefix = [turn("system", type="text", text="Generate audio following instruction."),
              turn("scene", type="text", text="Audio is recorded from a quiet room."),
              turn("user", type="text", text=ref_text),
              turn("assistant", type="audio", path=str(PROMPTS / f"{voice}.wav"))]

    def synth(text, prefix=prefix):
        torch.manual_seed(7)
        inputs = processor.apply_chat_template(prefix + [turn("user", type="text", text=text)],
                                               add_generation_prompt=True, tokenize=True, return_dict=True,
                                               sampling_rate=24000, return_tensors="pt").to(model.device)
        out = model.generate(**inputs, max_new_tokens=2048, do_sample=False)
        wav = tmp / "seg.wav"
        processor.save_audio(processor.batch_decode(out), str(wav))
        audio, sr = sf.read(wav, dtype="float32")
        return audio, sr

    run("higgs2", voice, synth)
print(f"peak VRAM {torch.cuda.max_memory_allocated() / 2**30:.1f} GiB")
