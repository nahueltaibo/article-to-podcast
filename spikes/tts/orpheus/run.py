# /// script
# requires-python = ">=3.11,<3.12"
# dependencies = ["transformers>=4.50,<5", "accelerate", "snac", "soundfile", "numpy", "torch>=2.7"]
# [tool.uv]
# override-dependencies = ["torch>=2.7"]
# [tool.uv.sources]
# torch = { index = "pytorch-cu128" }
# [[tool.uv.index]]
# name = "pytorch-cu128"
# url = "https://download.pytorch.org/whl/cu128"
# explicit = true
# ///
"""Orpheus via plain transformers + SNAC: the official orpheus-speech package needs vLLM, which has no Windows build."""
import sys
from pathlib import Path

import torch
from snac import SNAC
from transformers import AutoModelForCausalLM, AutoTokenizer

sys.path.insert(0, str(Path(__file__).parent.parent))
from common import run  # noqa: E402

# Ungated mirror of canopylabs/orpheus-3b-0.1-ft.
REPO = "unsloth/orpheus-3b-0.1-ft"
SOH, EOT, EOH = 128259, 128009, 128260
START_AUDIO, END_AUDIO, CODE_BASE = 128257, 128258, 128266

tok = AutoTokenizer.from_pretrained(REPO)
model = AutoModelForCausalLM.from_pretrained(REPO, torch_dtype=torch.bfloat16).cuda().eval()
snac = SNAC.from_pretrained("hubertsiuzdak/snac_24khz").cuda().eval()


def decode(codes):
    codes = codes[: len(codes) // 7 * 7]
    l1, l2, l3 = [], [], []
    for i in range(0, len(codes), 7):
        c = codes[i:i + 7]
        l1.append(c[0])
        l2 += [c[1] - 4096, c[4] - 4 * 4096]
        l3 += [c[2] - 2 * 4096, c[3] - 3 * 4096, c[5] - 5 * 4096, c[6] - 6 * 4096]
    layers = [torch.tensor(x, device="cuda").clamp(0, 4095).unsqueeze(0) for x in (l1, l2, l3)]
    with torch.inference_mode():
        return snac.decode(layers).squeeze().float().cpu().numpy()


for voice in sys.argv[1:] or ["tara", "leo"]:
    def synth(text, voice=voice):
        torch.manual_seed(7)
        ids = tok(f"{voice}: {text}", return_tensors="pt").input_ids
        ids = torch.cat([torch.tensor([[SOH]]), ids, torch.tensor([[EOT, EOH]])], dim=1).cuda()
        with torch.inference_mode():
            out = model.generate(ids, attention_mask=torch.ones_like(ids), max_new_tokens=4800, do_sample=True,
                                 temperature=0.6, top_p=0.95, repetition_penalty=1.1, eos_token_id=END_AUDIO)
        gen = out[0, ids.shape[1]:].tolist()
        if START_AUDIO in gen:
            gen = gen[len(gen) - gen[::-1].index(START_AUDIO):]
        codes = [t - CODE_BASE for t in gen if t >= CODE_BASE]
        return decode(codes), 24000

    run("orpheus", voice, synth)
print(f"peak VRAM {torch.cuda.max_memory_allocated() / 2**30:.1f} GiB")
