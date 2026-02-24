#!/usr/bin/env python3
"""
Preview or apply bulk renames with safe defaults.

Usage examples:
  python rename_cleanup.py --path . --lowercase --spaces-to-hyphens
  python rename_cleanup.py --path . --pattern "\s+" --replace "_" --apply
  python rename_cleanup.py --path . --find-duplicates --report duplicates.json
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
from dataclasses import dataclass
from typing import Dict, Iterable, List, Optional, Tuple


@dataclass
class RenameOptions:
    regex_pattern: Optional[str]
    regex_replacement: str
    lowercase: bool
    spaces_to_hyphens: bool
    keep_extension: bool


def normalize_name(name: str, options: RenameOptions) -> str:
    directory, basename = os.path.split(name)
    stem, ext = os.path.splitext(basename)
    base = stem if options.keep_extension else basename

    if options.regex_pattern:
        base = re.sub(options.regex_pattern, options.regex_replacement, base)

    if options.lowercase:
        base = base.lower()

    if options.spaces_to_hyphens:
        base = re.sub(r"\s+", "-", base).strip("-")

    if options.keep_extension:
        renamed = f"{base}{ext}"
    else:
        renamed = base

    return os.path.join(directory, renamed) if directory else renamed


def build_rename_plan(files: Iterable[str], options: RenameOptions) -> Dict[str, str]:
    plan: Dict[str, str] = {}
    used = set(files)

    for original in files:
        target = normalize_name(original, options)
        if target == original:
            plan[original] = target
            continue

        candidate = target
        counter = 2
        while candidate in used:
            stem, ext = os.path.splitext(target)
            candidate = f"{stem}-{counter}{ext}"
            counter += 1

        plan[original] = candidate
        used.add(candidate)

    return plan


def iter_files(path: str, recursive: bool) -> List[str]:
    files: List[str] = []
    if recursive:
        for root, _, filenames in os.walk(path):
            for name in filenames:
                files.append(os.path.relpath(os.path.join(root, name), path))
    else:
        for name in os.listdir(path):
            full = os.path.join(path, name)
            if os.path.isfile(full):
                files.append(name)
    return files


def file_hash(path: str) -> str:
    hasher = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


def find_duplicates(path: str, files: Iterable[str]) -> List[List[str]]:
    seen: Dict[Tuple[int, str], List[str]] = {}
    for name in files:
        full_path = os.path.join(path, name)
        size = os.path.getsize(full_path)
        digest = file_hash(full_path)
        seen.setdefault((size, digest), []).append(name)
    return [group for group in seen.values() if len(group) > 1]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Bulk rename and duplicate scan")
    parser.add_argument("--path", default=".", help="Target directory")
    parser.add_argument("--recursive", action="store_true", help="Recurse into subfolders")
    parser.add_argument("--pattern", help="Regex pattern to replace")
    parser.add_argument("--replace", default="", help="Replacement for pattern")
    parser.add_argument("--lowercase", action="store_true", help="Lowercase names")
    parser.add_argument("--spaces-to-hyphens", action="store_true", help="Replace spaces with hyphens")
    parser.add_argument("--apply", action="store_true", help="Apply renames")
    parser.add_argument("--report", help="Write JSON report")
    parser.add_argument("--find-duplicates", action="store_true", help="Generate duplicate report")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if not os.path.isdir(args.path):
        print(f"Path not found: {args.path}", file=sys.stderr)
        return 2

    options = RenameOptions(
        regex_pattern=args.pattern,
        regex_replacement=args.replace,
        lowercase=args.lowercase,
        spaces_to_hyphens=args.spaces_to_hyphens,
        keep_extension=True,
    )

    files = iter_files(args.path, args.recursive)
    plan = build_rename_plan(files, options)

    renames = [
        {"from": src, "to": dst}
        for src, dst in plan.items()
        if src != dst
    ]

    if args.apply:
        temp_map: Dict[str, str] = {}
        for item in renames:
            src = os.path.join(args.path, item["from"])
            dst = os.path.join(args.path, item["to"])
            os.makedirs(os.path.dirname(dst) or args.path, exist_ok=True)

            temp_dst = f"{dst}.rename_tmp"
            suffix = 1
            while os.path.exists(temp_dst):
                temp_dst = f"{dst}.rename_tmp-{suffix}"
                suffix += 1

            os.rename(src, temp_dst)
            temp_map[temp_dst] = dst

        for temp_src, final_dst in temp_map.items():
            os.rename(temp_src, final_dst)

    duplicates: List[List[str]] = []
    if args.find_duplicates:
        current_files = iter_files(args.path, args.recursive)
        duplicates = find_duplicates(args.path, current_files)

    report = {
        "path": os.path.abspath(args.path),
        "renames": renames,
        "applied": bool(args.apply),
        "duplicates": duplicates,
    }

    if args.report:
        with open(args.report, "w", encoding="utf-8") as handle:
            json.dump(report, handle, indent=2)

    print(json.dumps(report, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
