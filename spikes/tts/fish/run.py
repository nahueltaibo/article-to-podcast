# /// script
# requires-python = ">=3.12,<3.13"
# dependencies = ["fish-speech", "soundfile", "numpy", "torch==2.8.0", "torchaudio==2.8.0"]
# [tool.uv]
# override-dependencies = ["protobuf>=3.20.0,<6.0.0"]
# [tool.uv.sources]
# fish-speech = { path = "repo" }
# torch = { index = "pytorch-cu128" }
# torchaudio = { index = "pytorch-cu128" }
# [[tool.uv.index]]
# name = "pytorch-cu128"
# url = "https://download.pytorch.org/whl/cu128"
# explicit = true
# ///
import json
import sys
from pathlib import Path

import torch

HERE = Path(__file__).parent
sys.path.insert(0, str(HERE / "repo"))  # for tools.server, which isn't packaged
sys.path.insert(0, str(HERE.parent))
from common import ROOT, run  # noqa: E402
from fish_speech.utils.schema import ServeReferenceAudio, ServeTTSRequest  # noqa: E402
from tools.server.model_manager import ModelManager  # noqa: E402

# Drama 3 needs 48 GB, so this runs S2-Pro (upstream lists 24 GB).
CKPT = HERE / "checkpoints" / "s2-pro"
# Voice-cloning: the reference is Kokoro af_heart reading segment s1.
REF = ROOT / "out" / "ref-af_heart-s1.wav"
REF_TEXT = json.loads((ROOT / "test-script.json").read_text(encoding="utf-8"))["segments"][0]["text"]

manager = ModelManager(mode="tts", device="cuda", half=False, compile=False, llama_checkpoint_path=str(CKPT),
                       decoder_checkpoint_path=str(CKPT / "codec.pth"), decoder_config_name="modded_dac_vq")
reference = ServeReferenceAudio(audio=REF.read_bytes(), text=REF_TEXT)


def synth(text):
    req = ServeTTSRequest(text=text, references=[reference], seed=7, format="wav")
    for result in manager.tts_inference_engine.inference(req):
        if result.code == "error":
            raise result.error
        if result.code == "final":
            sr, audio = result.audio
            return audio, sr


run("fish", "s2-pro-clone-kokoro_af_heart", synth)
print(f"peak VRAM {torch.cuda.max_memory_allocated() / 2**30:.1f} GiB")
