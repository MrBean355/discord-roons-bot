#!/usr/bin/env python3
r"""
Playsounds Import and Synchronization Script
===========================================

This script automates the process of importing and synchronizing sound bites from an 
external playsounds repository into the project's resource directory.

Features:
---------
1. Automatic Repository Updates: Runs 'git pull' in the source repository at startup 
   to fetch the latest sound files. Aborts immediately if the pull fails.
2. Target Directory Cleanup: Cleans up the target resources folder by deleting all 
   existing .mp3 files at the start of the sync process to guarantee a clean state.
3. File Conversions: Converts source audio files (.ogg, .wav, etc.) to the target .mp3 
   format using ffmpeg, outputting them into the project resources folder.
4. Priority Resolution: Handles duplicate sound names in different subdirectories 
   of the source repository by prioritizing directories (e.g. 'new' overrides 'old').
5. Exclusion List: Supports an ignored list (EXCLUDED_SOUNDS) to manually filter out 
   spelling variants or unwanted duplicate-content sounds.
6. Manifest Rebuilding: Rebuilds and sorts 'manifest.json' dynamically based on the 
   final set of .mp3 sound files.
7. Content Duplicate Detection: Performs a final validation checking for identical 
   audio contents (SHA-256 hash comparison) in the target directory and reports 
   them as errors, aborting if duplicates exist.

Usage:
------
python scripts/import_sounds.py --source I:\Source\playsounds\files
"""

import os
import sys
import json
import hashlib
import argparse
import subprocess
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor, as_completed

# Determine script directories relative to repository root
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.dirname(SCRIPT_DIR)
DEFAULT_RESOURCES_DIR = os.path.join(REPO_ROOT, "src", "main", "resources", "sounds")
DEFAULT_FFMPEG_PATH = os.path.join(SCRIPT_DIR, "ffmpeg.exe")

# Sounds to exclude from importing (e.g. duplicates by content/spelling)
EXCLUDED_SOUNDS = {
    'aaaah',
    'aahh',
    'bulldogoniichan2',
    'bigritar',
    'cd5min',
    'glickitrae',
    'hachama',
    'iamscared',
    'impoweringup',
    'letsgo',
    'parasites',
    'praedator',
    'throw',
    'thunderr',
    'yeabut'
}

def get_file_hash(filepath):
    """Calculate the SHA-256 hash of a file's content."""
    sha256 = hashlib.sha256()
    with open(filepath, 'rb') as f:
        while chunk := f.read(8192):
            sha256.update(chunk)
    return sha256.hexdigest()

# Priority for resolving duplicate base names in playsounds subfolders
PRIORITY = {
    'old': 1,
    'vadikus': 2,
    'raeyei': 3,
    'bulldog': 4,
    'admiralbulldog': 5,
    'new': 6
}

def get_priority(rel_path):
    """Retrieve priority based on directory name (higher number = higher priority)."""
    parts = rel_path.split(os.sep)
    if not parts:
        return 0
    folder = parts[0].lower()
    return PRIORITY.get(folder, 2)  # Default to 2 for other/unknown categories

def main():
    parser = argparse.ArgumentParser(description="Import and convert playsounds into the project resources.")
    parser.add_argument("--source", "-s", required=True, help="Path to the external playsounds 'files' directory (e.g. I:\\Source\\playsounds\\files)")
    parser.add_argument("--destination", "-d", default=DEFAULT_RESOURCES_DIR, help="Destination directory inside the project (default: src/main/resources/sounds)")
    parser.add_argument("--ffmpeg", "-f", default=DEFAULT_FFMPEG_PATH, help="Path to the ffmpeg executable (default: scripts/ffmpeg.exe)")
    parser.add_argument("--workers", "-w", type=int, default=10, help="Number of parallel worker threads (default: 10)")
    args = parser.parse_args()

    # Validate paths
    if not os.path.isdir(args.source):
        print(f"Error: Source directory does not exist or is not a directory: {args.source}")
        sys.exit(1)
    if not os.path.isdir(args.destination):
        print(f"Error: Destination directory does not exist or is not a directory: {args.destination}")
        sys.exit(1)
    if not os.path.isfile(args.ffmpeg):
        print(f"Error: FFMPEG executable does not exist: {args.ffmpeg}")
        sys.exit(1)

    # Update the source repository
    print("Updating source repository...")
    try:
        subprocess.run(["git", "pull"], cwd=args.source, check=True)
        print("Source repository updated successfully.")
    except subprocess.CalledProcessError as e:
        print(f"Error: 'git pull' failed with exit code {e.returncode} in source directory '{args.source}'.")
        print("Aborting script execution.")
        sys.exit(1)
    except Exception as e:
        print(f"Error: Could not run 'git pull' in source directory '{args.source}': {e}.")
        print("Aborting script execution.")
        sys.exit(1)

    print(f"Scanning source directory: {args.source}")
    sound_files = []
    for root, _, files in os.walk(args.source):
        for f in files:
            if f.lower().endswith(('.ogg', '.mp3', '.wav')):
                full_path = os.path.join(root, f)
                rel_path = os.path.relpath(full_path, args.source)
                sound_files.append((f, full_path, rel_path))

    print(f"Found {len(sound_files)} sound files in source directory.")

    # Group by base name and resolve duplicate names
    base_name_groups = defaultdict(list)
    for f, full_path, rel_path in sound_files:
        name, _ = os.path.splitext(f)
        base_lower = name.lower()
        if base_lower in EXCLUDED_SOUNDS:
            continue
        base_name_groups[base_lower].append((full_path, rel_path))

    selected_files = {}
    for base_lower, items in base_name_groups.items():
        if len(items) == 1:
            selected_files[base_lower] = items[0][0]
        else:
            items_sorted = sorted(items, key=lambda x: get_priority(x[1]), reverse=True)
            selected_files[base_lower] = items_sorted[0][0]
            print(f"Duplicate resolved for '{base_lower}': chose {items_sorted[0][1]} over {[x[1] for x in items_sorted[1:]]}")

    print(f"Total unique sound bites to import: {len(selected_files)}")

    # Clean the destination directory at the start of import to clear deprecated sounds
    print("Cleaning destination directory of existing .mp3 files...")
    cleaned_count = 0
    for f in os.listdir(args.destination):
        if f.lower().endswith('.mp3'):
            path_to_del = os.path.join(args.destination, f)
            try:
                os.remove(path_to_del)
                cleaned_count += 1
            except Exception as e:
                print(f"Error removing old sound file {f}: {e}")
    print(f"Cleaned {cleaned_count} existing files from destination directory.")

    stats = {'new': 0, 'errors': 0}

    def convert_file(base_lower, src_path):
        dest_filename = f"{base_lower}.mp3"
        dest_path = os.path.join(args.destination, dest_filename)

        # Run ffmpeg to convert
        cmd = [args.ffmpeg, "-loglevel", "error", "-y", "-i", src_path, dest_path]
        try:
            subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True)
            return True, base_lower, None
        except Exception as e:
            err_msg = f"Failed to convert {src_path} to {dest_path}: {e}"
            return False, base_lower, err_msg

    print(f"Starting parallel conversions with {args.workers} workers...")
    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = {executor.submit(convert_file, base, path): base for base, path in selected_files.items()}
        for future in as_completed(futures):
            success, base, err = future.result()
            if success:
                stats['new'] += 1
            else:
                stats['errors'] += 1
                print(err)

    print(f"Import complete. Imported: {stats['new']}, Errors: {stats['errors']}")

    # Re-scan the destination directory to rebuild manifest.json
    print("Rebuilding manifest.json...")
    all_mp3s = [f for f in os.listdir(args.destination) if f.lower().endswith('.mp3')]
    all_mp3s.sort(key=str.lower)

    manifest_path = os.path.join(args.destination, "manifest.json")
    with open(manifest_path, 'w', encoding='utf-8') as f:
        json.dump(all_mp3s, f, indent=2, ensure_ascii=False)
        f.write('\n')
    print(f"manifest.json updated successfully with {len(all_mp3s)} total sound bites.")

    # Check for duplicate sound files by content in the destination directory
    print("Checking for duplicate sound files by content in destination...")
    hashes = defaultdict(list)
    for f in all_mp3s:
        filepath = os.path.join(args.destination, f)
        try:
            file_hash = get_file_hash(filepath)
            hashes[file_hash].append(f)
        except Exception as e:
            print(f"Error reading file hash for {filepath}: {e}")
            sys.exit(1)

    content_duplicates = {h: files for h, files in hashes.items() if len(files) > 1}
    if content_duplicates:
        print("\nError: Duplicate sound files by content detected in destination directory!")
        print("The following groups of files have identical audio content but different names:")
        for file_hash, files in content_duplicates.items():
            print(f"  Hash {file_hash[:16]}... -> {', '.join(files)}")
        print("\nTo resolve this, please add the duplicate names you want to ignore to the 'EXCLUDED_SOUNDS' set in this script.")
        print("Aborting script execution.")
        sys.exit(1)

    print("No content duplicates detected in destination.")

if __name__ == "__main__":
    main()
