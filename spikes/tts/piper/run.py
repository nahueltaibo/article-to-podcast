# /// script
# requires-python = ">=3.10,<3.13"
# dependencies = ["piper-tts>=1.3", "soundfile", "numpy"]
# ///
import os
import subprocess
import sys
from pathlib import Path

import numpy as np
from piper import PiperVoice

sys.path.insert(0, str(Path(__file__).parent.parent))
from common import run  # noqa: E402

VOICE_DIR = Path(os.environ["LOCALAPPDATA"]) / "tts-spike" / "piper" / "voices"
VOICE_DIR.mkdir(parents=True, exist_ok=True)

for name in sys.argv[1:] or ["en_US-lessac-high", "en_GB-alba-medium"]:
    model = VOICE_DIR / f"{name}.onnx"
    if not model.exists():
        subprocess.run([sys.executable, "-m", "piper.download_voices", "--data-dir", str(VOICE_DIR), name], check=True)
    voice = PiperVoice.load(str(model))

    def synth(text, voice=voice):
        chunks = list(voice.synthesize(text))
        pcm = np.concatenate([np.frombuffer(c.audio_int16_bytes, dtype=np.int16) for c in chunks])
        return pcm.astype(np.float32) / 32768, chunks[0].sample_rate

    run("piper", name, synth)
