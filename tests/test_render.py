import numpy as np

from audio_presenter.render import SAMPLE_RATE, mix


def test_narration_keeps_the_timeline_but_silences_the_chime():
    bell = np.ones(100, dtype=np.float32)
    speech = lambda text: np.full(SAMPLE_RATE, 0.5, dtype=np.float32)
    segments = [{"id": "s1", "kind": "title", "text": "a"}, {"id": "s2", "kind": "media", "media": "m1", "text": "b"}]

    full, narration, timed = mix(segments, speech, bell)

    assert len(full) == len(narration)
    start = timed[1]["startMs"] * SAMPLE_RATE // 1000
    assert full[start:start + 100].tolist() == [1.0] * 100
    assert not narration[start:start + 100].any()
    assert np.array_equal(full[:start], narration[:start])


def test_duration_reads_ffmpeg_summary(tmp_path):
    import subprocess

    from audio_presenter import tools

    clip = tmp_path / "clip.mp4"
    subprocess.run([tools.ffmpeg(), "-y", "-loglevel", "error", "-f", "lavfi", "-i", "testsrc=d=2.5:s=64x64", str(clip)],
                   check=True)
    assert abs(tools.duration(str(clip)) - 2.5) < 0.1
