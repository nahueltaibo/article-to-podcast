# TTS Spike

Find a voice you'd happily listen to for 20 minutes a day. If a local model is good enough, use it. Otherwise use the best paid one.

## Test Text

One Anthropic research post, about 3 minutes of narration. It must include:

- Technical terms and names: Claude, RLHF, Anthropic, model names
- Numbers, percentages, years, and ranges
- A quote and a list
- An image pointer line: "There's a chart on screen."

The same text, split into the same paragraphs, goes through every engine. Each paragraph is rendered **separately**, then the clips are joined, the same way the real pipeline works. That surfaces voice drift between clips.

## Candidates

This space changes fast. Before starting, check for newer models on Hugging Face trending and the TTS Arena leaderboard.

**Local**, on the RTX 5080 Laptop with 16 GB of VRAM:

- Kokoro-82M: small and fast, the baseline to beat
- Chatterbox
- Orpheus
- Higgs Audio v2
- VibeVoice
- IndexTTS2
- F5-TTS
- Piper: quality floor and CPU-only fallback

**Paid:**

- ElevenLabs
- OpenAI `gpt-4o-mini-tts`
- Google Cloud TTS (Chirp 3 HD voices)
- Azure Neural TTS

The RTX 50 series needs a PyTorch build for CUDA 12.8 or newer. Older wheels fail with "no kernel image". Give each engine its own uv environment so their dependencies don't clash.

## How to Judge

| Criterion | How |
| --- | --- |
| Naturalness | Listen on the phone with earbuds. Score 1–5. Shuffle the files and hide the names first. |
| Pronunciation | Count wrong names, terms, and numbers |
| Consistency | Does the voice, pace, or loudness drift between joined paragraphs? |
| Speed | Real-time factor on the 5080. Anything faster than real time is fine. |
| Cost | Per 10,000 characters, roughly one article |
| Controls | Speed, pauses, and a custom pronunciation list |
| Setup | Effort to install on Windows, and whether it could later run in a cloud routine |

Shortlist the top 3 by naturalness first. Only measure the rest of the criteria for those.

## Output

- Spike code and samples go in `spikes/tts/<engine>/`, with audio files kept out of git.
- Results go in a table at the end of this doc, with the decision and the chosen voice.
