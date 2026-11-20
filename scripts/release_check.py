"""Decide whether main should release: only when major.minor differs from the latest v* tag.

Prints `release=true|false` and `version=X.Y.Z` lines, ready for $GITHUB_OUTPUT.
"""
import re
import subprocess
import sys
from pathlib import Path

VERSION_FILE = Path(__file__).resolve().parent.parent / "android" / "version.properties"


def parse(version: str) -> tuple[int, int, int]:
    match = re.fullmatch(r"v?(\d+)\.(\d+)\.(\d+)", version.strip())
    if not match:
        raise ValueError(f"not a major.minor.patch version: {version!r}")
    return tuple(int(part) for part in match.groups())


def should_release(current: str, last_tag: str | None) -> bool:
    if last_tag is None:
        return True
    now, last = parse(current), parse(last_tag)
    if now < last:
        raise ValueError(f"version {current} is older than the last release {last_tag}")
    return now[:2] != last[:2]


def current_version() -> str:
    for line in VERSION_FILE.read_text(encoding="utf-8").splitlines():
        if line.startswith("versionName="):
            return line.split("=", 1)[1].strip()
    raise ValueError(f"no versionName in {VERSION_FILE}")


def last_release_tag() -> str | None:
    tags = subprocess.run(["git", "tag", "--list", "v*.*.*", "--sort=-v:refname"],
                          capture_output=True, text=True, check=True).stdout.split()
    return tags[0] if tags else None


if __name__ == "__main__":
    version, last = current_version(), last_release_tag()
    release = should_release(version, last)
    print(f"version={version}")
    print(f"release={str(release).lower()}")
    print(f"{version} vs last release {last or 'none'}: {'release' if release else 'no release'}", file=sys.stderr)
