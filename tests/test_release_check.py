import importlib.util
from pathlib import Path

import pytest

spec = importlib.util.spec_from_file_location(
    "release_check", Path(__file__).resolve().parent.parent / "scripts" / "release_check.py")
release_check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release_check)


@pytest.mark.parametrize("current, last, expected", [
    ("0.1.0", None, True),
    ("0.1.1", "v0.1.0", False),
    ("0.2.0", "v0.1.3", True),
    ("1.0.0", "v0.9.9", True),
    ("0.1.0", "v0.1.0", False),
])
def test_only_major_or_minor_bumps_release(current, last, expected):
    assert release_check.should_release(current, last) is expected


def test_going_backwards_is_an_error():
    with pytest.raises(ValueError):
        release_check.should_release("0.1.0", "v0.2.0")


def test_version_file_is_valid():
    release_check.parse(release_check.current_version())
