#!/usr/bin/env python3
"""
Auto-download YouTube content with sensible defaults.

Usage examples:
  uv run --no-project scripts/download.py --url "https://www.youtube.com/watch?v=abc"
  uv run --no-project scripts/download.py --url "https://www.youtube.com/watch?v=abc&list=PL..." --max-downloads 5
  uv run --no-project scripts/download.py --url-file urls.txt --mode audio --audio-format mp3
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from dataclasses import dataclass
from typing import Iterable, List, Optional


@dataclass
class DownloadOptions:
    mode: str
    quality: str
    output_dir: str
    template: str
    yes_playlist: bool
    max_downloads: Optional[int]
    sub_lang: str
    audio_format: str


def infer_max_downloads(urls: Iterable[str], max_downloads: Optional[int]) -> Optional[int]:
    if max_downloads is not None:
        return max_downloads
    for url in urls:
        if "list=" in url:
            return 5
    return None


def build_command(options: DownloadOptions, urls: List[str]) -> List[str]:
    cmd = ["yt-dlp"]
    cmd.append("--yes-playlist" if options.yes_playlist else "--no-playlist")
    cmd.extend([
        "--ignore-errors",
        "--continue",
        "--no-overwrites",
        "--retries",
        "3",
        "--fragment-retries",
        "3",
        "--newline",
    ])

    max_downloads = infer_max_downloads(urls, options.max_downloads)
    if max_downloads is not None:
        cmd.extend(["--max-downloads", str(max_downloads)])

    cmd.extend(["--paths", options.output_dir])
    cmd.extend(["-o", options.template])

    if options.mode == "audio":
        cmd.extend(["-x", "--audio-format", options.audio_format, "--audio-quality", "0"])
    elif options.mode == "subs":
        cmd.extend([
            "--write-subs",
            "--write-auto-subs",
            "--sub-lang",
            options.sub_lang,
            "--skip-download",
        ])
    elif options.mode == "meta":
        cmd.extend(["--write-info-json", "--write-thumbnail", "--skip-download"])
    else:
        cmd.extend(["-f", quality_to_format(options.quality)])

    cmd.extend(urls)
    return cmd


def quality_to_format(quality: str) -> str:
    if quality == "smallest":
        return "bv*[height<=480]+ba/b[height<=480]"
    if quality == "balanced":
        return "bv*[height<=1080]+ba/b[height<=1080]"
    return "bv*+ba/b"


def read_urls(url_file: Optional[str], urls: List[str]) -> List[str]:
    collected = list(urls)
    if url_file:
        with open(url_file, "r", encoding="utf-8") as handle:
            for line in handle:
                line = line.strip()
                if line and not line.startswith("#"):
                    collected.append(line)
    return collected


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Download YouTube videos with yt-dlp")
    parser.add_argument("--url", action="append", default=[], help="YouTube URL (repeatable)")
    parser.add_argument("--url-file", help="Text file of URLs (one per line)")
    parser.add_argument("--mode", choices=["video", "audio", "subs", "meta"], default="video")
    parser.add_argument("--quality", choices=["best", "balanced", "smallest"], default="balanced")
    parser.add_argument("--output-dir", default="downloads", help="Output directory")
    parser.add_argument(
        "--template",
        default="%(playlist_index)s-%(title)s.%(ext)s",
        help="Filename template",
    )
    parser.add_argument("--no-playlist", action="store_true", help="Force single video")
    parser.add_argument("--max-downloads", type=int, help="Limit playlist downloads")
    parser.add_argument("--sub-lang", default="en", help="Subtitle language")
    parser.add_argument("--audio-format", default="mp3", help="Audio format")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    urls = read_urls(args.url_file, args.url)
    if not urls:
        print("No URLs provided. Use --url or --url-file.", file=sys.stderr)
        return 2

    if shutil.which("yt-dlp") is None:
        print("yt-dlp is not installed or not on PATH.", file=sys.stderr)
        print("Install with: uv pip install yt-dlp", file=sys.stderr)
        return 1

    os.makedirs(args.output_dir, exist_ok=True)

    options = DownloadOptions(
        mode=args.mode,
        quality=args.quality,
        output_dir=args.output_dir,
        template=args.template,
        yes_playlist=not args.no_playlist,
        max_downloads=args.max_downloads,
        sub_lang=args.sub_lang,
        audio_format=args.audio_format,
    )

    command = build_command(options, urls)
    print("Running:", " ".join(command))
    completed = subprocess.run(command, check=False)
    return completed.returncode


if __name__ == "__main__":
    raise SystemExit(main())
