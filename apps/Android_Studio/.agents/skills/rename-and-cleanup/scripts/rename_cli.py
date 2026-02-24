#!/usr/bin/env python3
"""
File Rename CLI Tool

A safe command-line tool for renaming files with preview, rollback, and error handling.
Supports single file renames and batch renames with wildcards.

Usage:
    uv run rename_cli.py [options] source dest

Examples:
    # Single file rename
    uv run rename_cli.py old.txt new.txt

    # Batch rename with pattern
    uv run rename_cli.py "file_*.txt" "new_*.txt"

    # Dry run
    uv run rename_cli.py --dry-run "*.txt" ".bak"
"""

import argparse
import sys
from pathlib import Path
from glob import glob
import fnmatch
import os


def parse_args():
    parser = argparse.ArgumentParser(
        description="Safe file rename CLI tool with preview and rollback support",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument(
        'source',
        help='Source file or pattern (use * for wildcards in batch mode)'
    )
    parser.add_argument(
        'dest',
        help='Destination file or pattern (must contain * if source does)'
    )
    parser.add_argument(
        '--dry-run', '-n',
        action='store_true',
        help='Preview changes without applying them'
    )
    parser.add_argument(
        '--force', '-f',
        action='store_true',
        help='Overwrite existing files'
    )
    parser.add_argument(
        '--verbose', '-v',
        action='store_true',
        help='Verbose output'
    )
    return parser.parse_args()


def find_matching_files(pattern):
    """Find files matching the pattern using glob."""
    return [Path(p) for p in glob(pattern, recursive=True)]


def generate_rename_pairs(source_pattern, dest_pattern, files):
    """Generate (old_path, new_path) pairs for renaming."""
    pairs = []
    for file_path in files:
        if '*' in source_pattern:
            # Batch mode: replace pattern
            old_str = str(file_path)
            # Simple replacement: assume * is the wildcard
            # For more complex patterns, this is simplistic
            # Split source_pattern by *
            if source_pattern.count('*') == 1 and dest_pattern.count('*') == 1:
                pre, suf = source_pattern.split('*', 1)
                dest_pre, dest_suf = dest_pattern.split('*', 1)
                if old_str.startswith(pre) and old_str.endswith(suf):
                    middle = old_str[len(pre):-len(suf)] if suf else old_str[len(pre):]
                    new_name = dest_pre + middle + dest_suf
                    new_path = file_path.parent / new_name
                    pairs.append((file_path, new_path))
                else:
                    print(f"Warning: {file_path} does not match pattern {source_pattern}", file=sys.stderr)
            else:
                print(f"Error: Complex patterns not supported. Use single * in both source and dest.", file=sys.stderr)
                return []
        else:
            # Single file mode
            if len(files) == 1:
                new_path = Path(dest_pattern)
                if not new_path.is_absolute():
                    new_path = file_path.parent / new_path
                pairs.append((file_path, new_path))
            else:
                print(f"Error: Multiple files found for single rename: {files}", file=sys.stderr)
                return []
    return pairs


def validate_pairs(pairs, force=False):
    """Validate rename pairs, check for conflicts."""
    valid_pairs = []
    for old_path, new_path in pairs:
        if not old_path.exists():
            print(f"Error: Source {old_path} does not exist", file=sys.stderr)
            continue
        if new_path.exists() and not force:
            print(f"Error: Destination {new_path} exists, use --force to overwrite", file=sys.stderr)
            continue
        if old_path == new_path:
            print(f"Warning: Source and dest are the same: {old_path}", file=sys.stderr)
            continue
        valid_pairs.append((old_path, new_path))
    return valid_pairs


def preview_changes(pairs, verbose=False):
    """Preview the changes."""
    print("Proposed renames:")
    for old_path, new_path in pairs:
        print(f"  {old_path} -> {new_path}")
    print(f"\nTotal: {len(pairs)} files")


def apply_changes(pairs, verbose=False):
    """Apply the renames."""
    applied = []
    for old_path, new_path in pairs:
        try:
            old_path.rename(new_path)
            applied.append((old_path, new_path))
            if verbose:
                print(f"Renamed: {old_path} -> {new_path}")
        except OSError as e:
            print(f"Error renaming {old_path} to {new_path}: {e}", file=sys.stderr)
    return applied


def print_rollback_info(applied_pairs):
    """Print rollback commands."""
    if applied_pairs:
        print("\nTo rollback, run these commands:")
        for old_path, new_path in reversed(applied_pairs):
            print(f"  mv '{new_path}' '{old_path}'")


def main():
    args = parse_args()

    source_pattern = args.source
    dest_pattern = args.dest

    # Determine mode
    is_batch = '*' in source_pattern

    if is_batch and '*' not in dest_pattern:
        print("Error: For batch mode, dest must contain *", file=sys.stderr)
        sys.exit(1)

    if not is_batch and '*' in dest_pattern:
        print("Error: For single file mode, dest should not contain *", file=sys.stderr)
        sys.exit(1)

    # Find files
    if is_batch:
        files = find_matching_files(source_pattern)
        if not files:
            print(f"No files found matching {source_pattern}", file=sys.stderr)
            sys.exit(1)
    else:
        source_path = Path(source_pattern)
        if not source_path.is_absolute():
            source_path = Path.cwd() / source_path
        files = [source_path]

    # Generate pairs
    pairs = generate_rename_pairs(source_pattern, dest_pattern, files)
    if not pairs:
        sys.exit(1)

    # Validate
    valid_pairs = validate_pairs(pairs, args.force)
    if not valid_pairs:
        print("No valid renames to perform", file=sys.stderr)
        sys.exit(1)

    # Preview
    preview_changes(valid_pairs, args.verbose)

    if args.dry_run:
        print("\nDry run: no changes applied")
        return

    # Confirm
    if not args.force:
        response = input("\nProceed with renames? (y/N): ").strip().lower()
        if response not in ('y', 'yes'):
            print("Aborted")
            return

    # Apply
    applied = apply_changes(valid_pairs, args.verbose)

    # Summary
    print(f"\nRenamed {len(applied)} files")

    # Rollback info
    print_rollback_info(applied)


if __name__ == '__main__':
    main()