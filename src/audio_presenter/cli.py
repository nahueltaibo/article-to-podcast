"""Command line entry point: extract, validate, build, feed, serve, doctor."""
import argparse
import functools
import http.server
import os
import socket
import webbrowser
from pathlib import Path

from . import extract, feed, package, publish, recompress, render, subscribe, tools, validate

OUTPUT = Path(os.environ.get("AUDIO_PRESENTER_OUTPUT", "output"))
PORT = 8000


def lan_ip() -> str:
    # A UDP connect sends no packets. It only asks the OS which interface routes outward.
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
        s.connect(("10.255.255.255", 1))
        return s.getsockname()[0]


def default_base() -> str:
    return f"http://{lan_ip()}:{PORT}"


def serving(port: int) -> bool:
    with socket.socket() as s:
        return s.connect_ex(("127.0.0.1", port)) == 0


def doctor() -> None:
    print("ffmpeg:", tools.ffmpeg())
    import torch

    print("torch:", torch.__version__, "| device:", "cuda" if torch.cuda.is_available() else "cpu")
    # The first call downloads the Kokoro weights from Hugging Face, about 330 MB.
    audio = render.KokoroVoice()("Your podcast setup works.")
    print(f"kokoro: rendered {len(audio) / render.SAMPLE_RATE:.1f} s of speech")
    print("ok")


def main() -> None:
    parser = argparse.ArgumentParser(prog="audio-presenter")
    sub = parser.add_subparsers(dest="command", required=True)
    p = sub.add_parser("extract", help="fetch an article into output/episodes/<id>/article.json")
    p.add_argument("url")
    p.add_argument("--source", help="who published it, for example Anthropic. Default: the site name")
    p.add_argument("--category", action="append", default=[], help="area it came from, for example Science. Repeatable")
    for name, text in (("validate", "check script.json"), ("build", "validate, render audio, embed chapters")):
        p = sub.add_parser(name, help=text)
        p.add_argument("episode", type=Path)
        if name == "build":
            p.add_argument("--voice", default=render.DEFAULT_VOICE)
    p = sub.add_parser("feed", help="write podcast.xml and chapters.json files")
    p.add_argument("--base-url", default=None, help=f"default: http://<LAN IP>:{PORT}")
    p.add_argument("--title", default=feed.FEED_TITLE)
    p = sub.add_parser("publish", help="upload podcast.xml, cover.jpg, and episodes/ to the R2 bucket")
    p.add_argument("--bucket", default=os.environ.get("R2_BUCKET", publish.DEFAULT_BUCKET))
    p.add_argument("--delete", action="store_true", help="also remove remote files that no longer exist locally")
    p.add_argument("--dry-run", action="store_true")
    sub.add_parser("recompress", help="re-encode every episode's MP3s at the current bitrate, then run feed")
    p = sub.add_parser("serve", help=f"rebuild the feed and serve output/ on port {PORT}")
    p.add_argument("--open", action="store_true", help="open the subscribe page in this computer's browser")
    sub.add_parser("doctor", help="check ffmpeg and the TTS model, then render a test sentence")
    args = parser.parse_args()

    if args.command == "extract":
        print(extract.run(args.url, OUTPUT, args.source, args.category))
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
        base = args.base_url or default_base()
        print(feed.run(OUTPUT, base, args.title))
        subscribe.write(OUTPUT, base)
    elif args.command == "publish":
        publish.run(OUTPUT, args.bucket, args.delete, args.dry_run)
    elif args.command == "recompress":
        recompress.run(OUTPUT)
    elif args.command == "doctor":
        doctor()
    elif args.command == "serve":
        base = default_base()
        feed.run(OUTPUT, base)
        subscribe.write(OUTPUT, base)
        print(f"Subscribe page: {base}/")
        print(f"Feed: {base}/podcast.xml")
        if args.open:
            webbrowser.open(f"http://localhost:{PORT}/")
        if serving(PORT):
            print(f"Already serving on port {PORT}.")
            return
        print("Serving. Ctrl+C to stop.", flush=True)
        handler = functools.partial(http.server.SimpleHTTPRequestHandler, directory=str(OUTPUT))
        http.server.ThreadingHTTPServer(("0.0.0.0", PORT), handler).serve_forever()
