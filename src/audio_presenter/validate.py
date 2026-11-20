"""Check script.json against the schema and against the media in article.json."""
import json
from importlib.resources import files
from pathlib import Path

import jsonschema


def validate(episode_dir: Path) -> list[str]:
    """Returns warnings. Raises ValueError on errors that would break rendering."""
    script = json.loads((episode_dir / "script.json").read_text(encoding="utf-8"))
    article = json.loads((episode_dir / "article.json").read_text(encoding="utf-8"))
    schema = json.loads(files("audio_presenter.schemas").joinpath("script.schema.json").read_text(encoding="utf-8"))
    try:
        jsonschema.validate(script, schema)
    except jsonschema.ValidationError as err:
        raise ValueError(f"script.json: {err.message} at {list(err.absolute_path)}") from err

    errors, warnings = [], []
    ids = [s["id"] for s in script["segments"]]
    if len(ids) != len(set(ids)):
        errors.append("segment ids are not unique")
    media = {m["id"]: m for m in article["media"]}
    used = set()
    for seg in script["segments"]:
        ref = seg.get("media")
        if seg["kind"] == "media" and not ref:
            errors.append(f"{seg['id']}: media segment without a media reference")
        if ref:
            if ref not in media:
                errors.append(f"{seg['id']}: unknown media {ref}")
            elif not media[ref].get("file"):
                errors.append(f"{seg['id']}: {ref} has no still image to show")
            used.add(ref)
    if script["segments"][0]["kind"] != "title":
        errors.append("first segment must be the title")
    for m in article["media"]:
        if m["id"] not in used and m.get("file"):
            warnings.append(f"{m['id']} ({m['type']}) is never shown")
    if errors:
        raise ValueError("; ".join(errors))
    return warnings
