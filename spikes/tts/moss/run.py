# /// script
# requires-python = ">=3.12,<3.13"
# dependencies = ["torch==2.9.1", "torchaudio==2.9.1", "torchcodec==0.8.1", "transformers==5.0.0", "accelerate>=1.10.1",
#   "safetensors==0.6.2", "numpy==2.1.0", "orjson", "tqdm", "PyYAML", "einops==0.8.1", "scipy", "librosa==0.11.0",
#   "tiktoken==0.12.0", "soundfile"]
# [tool.uv.sources]
# torch = { index = "pytorch-cu128" }
# torchaudio = { index = "pytorch-cu128" }
# [[tool.uv.index]]
# name = "pytorch-cu128"
# url = "https://download.pytorch.org/whl/cu128"
# explicit = true
# ///
import os
import sys
from pathlib import Path

# torchcodec needs shared FFmpeg DLLs; the FFmpeg on PATH is a static build.
FFMPEG = next((Path(__file__).parent / "ffmpeg-dl").glob("*/bin"))
os.add_dll_directory(str(FFMPEG))
os.environ["PATH"] = f"{FFMPEG};{os.environ['PATH']}"

import torch  # noqa: E402
from transformers import AutoModel, AutoProcessor  # noqa: E402

sys.path.insert(0, str(Path(__file__).parent.parent))
from common import ROOT, run  # noqa: E402

# The 8B MOSS-TTS-v1.5 needs ~17 GB in bf16, so this runs the 4B Local-Transformer-v1.5.
REPO = "OpenMOSS-Team/MOSS-TTS-Local-Transformer-v1.5"
# Voice cloning: without a reference the voice changes per segment.
REF = ROOT / "out" / "ref-af_heart-s1.wav"
torch.backends.cuda.enable_cudnn_sdp(False)

processor = AutoProcessor.from_pretrained(REPO, trust_remote_code=True)
processor.audio_tokenizer = processor.audio_tokenizer.to("cuda")
model = AutoModel.from_pretrained(REPO, trust_remote_code=True, attn_implementation="sdpa",
                                  torch_dtype=torch.bfloat16).to("cuda").eval()


def synth(text):
    torch.manual_seed(7)
    batch = processor([[processor.build_user_message(text=text, reference=[str(REF)], language="English")]],
                      mode="generation")
    with torch.no_grad():
        out = model.generate(input_ids=batch["input_ids"].to("cuda"), attention_mask=batch["attention_mask"].to("cuda"),
                             max_new_tokens=4096, do_sample=True, audio_temperature=1.7, audio_top_p=0.8,
                             audio_top_k=25, audio_repetition_penalty=1.0)
    audio = processor.decode(out)[0].audio_codes_list[0].float().cpu()
    # Stereo [channels, samples]: downmix, since common.run expects mono.
    return (audio.mean(0) if audio.ndim == 2 else audio).numpy(), processor.model_config.sampling_rate


run("moss", "local-v1.5-clone-kokoro_af_heart", synth)
print(f"peak VRAM {torch.cuda.max_memory_allocated() / 2**30:.1f} GiB")
