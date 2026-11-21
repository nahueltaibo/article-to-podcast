"""Command line entry point: extract, validate, build, feed, serve, doctor."""
import argparse
import functools
import http.server
import os
import shutil
import socket
from pathlib import Path

from . import extract, feed, package, render, validate

OUTPUT = Path(os.environ.get("AUDIO_PRESENTER_OUTPUT", "output"))
PORT = 8000


def lan_ip() -> str:
    # A UDP connect sends no packets. It only asks the OS which interface routes outward.
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
        s.connect(("10.255.255.255", 1))
        return s.getsockname()[0]


def default_base() -> str:
    return f"http://{lan_ip()}:{PORT}"


def doctor() -> None:
    ffmpeg = shutil.which("ffmpeg")
    print("ffmpeg:", ffmpeg or "MISSING")
    import torch

    print("torch:", torch.__version__, "| device:", "cuda" if torch.cuda.is_available() else "cpu")
    # The first call downloads the Kokoro weights from Hugging Face, about 330 MB.
    audio = render.KokoroVoice()("Your podcast setup works.")
    print(f"kokoro: rendered {len(audio) / render.SAMPLE_RATE:.1f} s of speech")
    if not ffmpeg:
        raise SystemExit("ffmpeg is missing. Install it and make sure it's on PATH.")
    print("ok")


def main() -> None:
    parser = argparse.ArgumentParser(prog="audio-presenter")
    sub = parser.add_subparsers(dest="command", required=True)
    p = sub.add_parser("extract", help="fetch an article into output/episodes/<id>/article.json")
    p.add_argument("url")
    for name, text in (("validate", "check script.json"), ("build", "validate, render audio, embed chapters")):
        p = sub.add_parser(name, help=text)
        p.add_argument("episode", type=Path)
        if name == "build":
            p.add_argument("--voice", default=render.DEFAULT_VOICE)
    p = sub.add_parser("feed", help="write podcast.xml and chapters.json files")
    p.add_argument("--base-url", default=None, help=f"default: http://<LAN IP>:{PORT}")
    sub.add_parser("serve", help=f"rebuild the feed and serve output/ on port {PORT}")
    sub.add_parser("doctor", help="check ffmpeg and the TTS model, then render a test sentence")
    args = parser.parse_args()

    if args.command == "extract":
        print(extract.run(args.url, OUTPUT))
    elif args.command == "validate":
        for warning in validate.validate(args.episode):
            print("warning:", warning)
        print("ok")
    elif args.command == "build":
        for warning in validate.validate(args.episode):
            print("warning:", warning)
        presentation = render.run(args.episode, args.voice)
        chapters = package.run(args.episode)
        print(f"{presentation['audio']['durationMs'] / 60000:.1f} min, {len(chapters)} chapters -> {args.episode / 'audio.mp3'}")
    elif args.command == "feed":
        print(feed.run(OUTPUT, args.base_url or default_base()))
    elif args.command == "doctor":
        doctor()
    elif args.command == "serve":
        base = default_base()
        feed.run(OUTPUT, base)
        print(f"Subscribe in AntennaPod to: {base}/podcast.xml  (Ctrl+C to stop)")
        handler = functools.partial(http.server.SimpleHTTPRequestHandler, directory=str(OUTPUT))
        http.server.ThreadingHTTPServer(("0.0.0.0", PORT), handler).serve_forever()
