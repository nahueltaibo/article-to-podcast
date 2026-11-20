# /// script
# requires-python = ">=3.11,<3.12"
# dependencies = ["vibevoice[streamingtts]", "soundfile", "numpy", "torch>=2.7"]
# [tool.uv]
# override-dependencies = ["torch>=2.7"]
# [tool.uv.sources]
# vibevoice = { path = "repo" }
# torch = { index = "pytorch-cu128" }
# [[tool.uv.index]]
# name = "pytorch-cu128"
# url = "https://download.pytorch.org/whl/cu128"
# explicit = true
# ///
import copy
import sys
from pathlib import Path

import torch
from vibevoice.modular.modeling_vibevoice_streaming_inference import VibeVoiceStreamingForConditionalGenerationInference
from vibevoice.processor.vibevoice_streaming_processor import VibeVoiceStreamingProcessor

sys.path.insert(0, str(Path(__file__).parent.parent))
from common import run  # noqa: E402

REPO = "microsoft/VibeVoice-Realtime-0.5B"
VOICES = Path(__file__).parent / "repo" / "demo" / "voices" / "streaming_model"

processor = VibeVoiceStreamingProcessor.from_pretrained(REPO)
# sdpa: no flash-attn wheels for Windows. Upstream says only flash_attention_2 is fully tested.
model = VibeVoiceStreamingForConditionalGenerationInference.from_pretrained(
    REPO, torch_dtype=torch.bfloat16, device_map="cuda", attn_implementation="sdpa").eval()
model.set_ddpm_inference_steps(num_steps=5)

for voice in sys.argv[1:] or ["en-Emma_woman", "en-Carter_man"]:
    # torch 2.11's weights_only unpickler rejects these presets; they ship in Microsoft's own repo.
    prompt = torch.load(VOICES / f"{voice}.pt", map_location="cuda", weights_only=False)

    def synth(text, prompt=prompt):
        text = text.replace("’", "'").replace("“", '"').replace("”", '"')
        inputs = processor.process_input_with_cached_prompt(text=text, cached_prompt=prompt, padding=True,
                                                             return_tensors="pt", return_attention_mask=True)
        inputs = {k: v.to("cuda") if torch.is_tensor(v) else v for k, v in inputs.items()}
        out = model.generate(**inputs, max_new_tokens=None, cfg_scale=1.5, tokenizer=processor.tokenizer,
                             generation_config={"do_sample": False}, verbose=False,
                             all_prefilled_outputs=copy.deepcopy(prompt))
        return out.speech_outputs[0].float().cpu().numpy(), 24000

    run("vibevoice", voice, synth)
print(f"peak VRAM {torch.cuda.max_memory_allocated() / 2**30:.1f} GiB")
