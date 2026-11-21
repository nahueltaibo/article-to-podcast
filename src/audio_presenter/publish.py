"""Upload the feed, cover, and episodes to an S3-compatible bucket (Cloudflare R2) with rclone."""
import os
import shutil
import subprocess
from pathlib import Path

DEFAULT_BUCKET = "pagecast"
# Only what the feed and the app read. Keeps git metadata and local notes out of the bucket.
INCLUDE = ["podcast.xml", "cover.jpg", "episodes/**"]


def remote_env() -> dict:
    missing = [k for k in ("R2_ACCESS_KEY_ID", "R2_SECRET_ACCESS_KEY", "R2_ENDPOINT") if not os.environ.get(k)]
    if missing:
        raise SystemExit(f"Set {', '.join(missing)} first.")
    # rclone reads a remote named "r2" from these variables, so no rclone.conf holds the secret.
    return {**os.environ,
            "RCLONE_CONFIG_R2_TYPE": "s3",
            "RCLONE_CONFIG_R2_PROVIDER": "Cloudflare",
            "RCLONE_CONFIG_R2_ACCESS_KEY_ID": os.environ["R2_ACCESS_KEY_ID"],
            "RCLONE_CONFIG_R2_SECRET_ACCESS_KEY": os.environ["R2_SECRET_ACCESS_KEY"],
            "RCLONE_CONFIG_R2_ENDPOINT": os.environ["R2_ENDPOINT"],
            "RCLONE_CONFIG_R2_NO_CHECK_BUCKET": "true"}


def run(output: Path, bucket: str = DEFAULT_BUCKET, delete: bool = False, dry_run: bool = False) -> None:
    rclone = shutil.which("rclone")
    if not rclone:
        raise SystemExit("rclone is missing. Install it and make sure it's on PATH.")
    filters = [arg for pattern in INCLUDE for arg in ("--include", pattern)]
    # copy never removes remote files. sync also deletes episodes that no longer exist locally.
    cmd = [rclone, "sync" if delete else "copy", str(output), f"r2:{bucket}", *filters,
           "--checksum", "--transfers", "8", "--stats-one-line", "-v"]
    if dry_run:
        cmd.append("--dry-run")
    subprocess.run(cmd, env=remote_env(), check=True)
