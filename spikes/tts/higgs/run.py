# /// script
# requires-python = ">=3.10,<3.11"
# dependencies = ["boson-multimodal", "soundfile", "numpy", "torch>=2.7", "torchaudio>=2.7", "torchvision"]
# [tool.uv]
# override-dependencies = ["torch>=2.7", "torchaudio>=2.7"]
# [tool.uv.sources]
# boson-multimodal = { path = "repo" }
# torch = { index = "pytorch-cu128" }
# torchaudio = { index = "pytorch-cu128" }
# torchvision = { index = "pytorch-cu128" }
# [[tool.uv.index]]
# name = "pytorch-cu128"
# url = "https://download.pytorch.org/whl/cu128"
# explicit = true
# ///
import base64
import sys
from pathlib import Path

import torch
from boson_multimodal.data_types import AudioContent, ChatMLSample, Message
from boson_multimodal.serve.serve_engine import HiggsAudioServeEngine

sys.path.insert(0, str(Path(__file__).parent.parent))
from common import run  # noqa: E402

PROMPTS = Path(__file__).parent / "repo" / "examples" / "voice_prompts"
SYSTEM = "Generate audio following instruction.\n\n<|scene_desc_start|>\nAudio is recorded from a quiet room.\n<|scene_desc_end|>"

engine = HiggsAudioServeEngine("bosonai/higgs-audio-v2-generation-3B-base", "bosonai/higgs-audio-v2-tokenizer",
                               device="cuda")

# Clone a shipped voice prompt: without a reference, Higgs picks a new voice for every segment.
for voice in sys.argv[1:] or ["en_woman", "en_man"]:
    ref_text = (PROMPTS / f"{voice}.txt").read_text(encoding="utf-8").strip()
    ref_audio = base64.b64encode((PROMPTS / f"{voice}.wav").read_bytes()).decode()

    def synth(text, ref_text=ref_text, ref_audio=ref_audio):
        messages = [Message(role="system", content=SYSTEM), Message(role="user", content=ref_text),
                    Message(role="assistant", content=AudioContent(raw_audio=ref_audio, audio_url="placeholder")),
                    Message(role="user", content=text)]
        out = engine.generate(chat_ml_sample=ChatMLSample(messages=messages), max_new_tokens=2048, temperature=0.3,
                              top_p=0.95, top_k=50, stop_strings=["<|end_of_text|>", "<|eot_id|>"], seed=7)
        return out.audio, out.sampling_rate

    run("higgs2", voice, synth)
print(f"peak VRAM {torch.cuda.max_memory_allocated() / 2**30:.1f} GiB")
