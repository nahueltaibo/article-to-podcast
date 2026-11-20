# TTS Spike

Find a voice you'd happily listen to for 20 minutes a day, from free models that run locally.

**Decision:** Kokoro `af_heart` is the default. Qwen3-TTS `Ryan` comes second, and is the male voice option. It renders at about 2x real time, so a 30-minute article takes about an hour. Choosing the voice per episode is a later feature.

## Test Text

[spikes/tts/test-script.json](../../spikes/tts/test-script.json) holds 14 segments, about 3 minutes, written by hand from [Harness design for long-running application development](https://www.anthropic.com/research/harness-design-long-running-apps). It stands in for the narration step's output and covers:

- Names and acronyms: Claude, Opus 4.5, GANs, MCP, FastAPI, SDK, QA
- Money, durations, and ranges: $124.70, 4.7 minutes, 5 to 15, 20x
- A list, two quotes, two table summaries, and a code summary
- An image pointer line

The same text, split into the same paragraphs, goes through every engine. Each paragraph is rendered **separately**, then the clips are joined, the same way the real pipeline works. That surfaces voice drift between clips.

## Candidates

This space changes fast. Before starting, check for newer models on Hugging Face trending and the TTS Arena leaderboard.

**Local**, on the RTX 5080 Laptop with 16 GB of VRAM:

- Kokoro-82M: small and fast, the baseline to beat
- Fish Audio: the top open-weights model on the Artificial Analysis leaderboard in November 2026. Its license allows personal use.
- Qwen3-TTS
- MOSS-TTS: built for long-form narration
- Chatterbox
- Orpheus
- Higgs Audio v2
- VibeVoice
- IndexTTS2
- F5-TTS
- Piper: quality floor and CPU-only fallback

The RTX 50 series needs a PyTorch build for CUDA 12.8 or newer. Older wheels fail with "no kernel image". Give each engine its own uv environment so their dependencies don't clash.

## How to Judge

| Criterion | How |
| --- | --- |
| Naturalness | Listen on the phone with earbuds. Score 1–5. Shuffle the files and hide the names first. |
| Pronunciation | Count wrong names, terms, and numbers |
| Consistency | Does the voice, pace, or loudness drift between joined paragraphs? |
| Speed | Real-time factor on the 5080. Anything faster than real time is fine. |
| Controls | Speed, pauses, and a custom pronunciation list |
| Setup | Effort to install on Windows, and whether it could later run in a cloud routine |

Shortlist the top 3 by naturalness first. Only measure the rest of the criteria for those.

## Output

- Spike code and samples go in `spikes/tts/<engine>/`, with audio files kept out of git.
- Results go in a table at the end of this doc, with the decision and the chosen voice.
