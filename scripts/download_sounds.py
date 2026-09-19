#!/usr/bin/env python3
r"""
Remote Playsounds Synchronization Script
========================================

This script synchronizes sound bite definitions from a remote API and downloads
audio files directly into the project's resource directory.

Features:
---------
1. Incremental Synchronization: Uses server ETag headers and per-sound 'uploadedAt'
   timestamps to download/convert only new or modified sounds.
2. Full Clean Reset: Pass '--clean' to nuke all local sounds and re-fetch everything.
3. Automatic Pruning: Deletes local sound files that no longer exist on the remote API.
4. Concurrent Download & Audio Conversion: Downloads in parallel and normalizes loudness
   using a 2-pass ffmpeg filter.
5. Manifest Rebuilding: Rebuilds and sorts 'manifest.json' dynamically in the target directory.
6. Real-Time Progress: Displays live progress updates during parallel conversion.

Usage:
------
python scripts/download_sounds.py          # Incremental sync (default)
python scripts/download_sounds.py --clean  # Nuke local files & full re-download
python scripts/download_sounds.py --dry-run # Preview changes
"""

import os
import sys
import json
import shutil
import argparse
import subprocess
import urllib.request
import urllib.parse
import tempfile
import re
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed

# Determine script directories relative to repository root
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.dirname(SCRIPT_DIR)
DEFAULT_RESOURCES_DIR = os.path.join(REPO_ROOT, "src", "main", "resources", "sounds")
DEFAULT_FFMPEG_PATH = os.path.join(SCRIPT_DIR, "ffmpeg.exe")
CACHE_FILE_PATH = os.path.join(SCRIPT_DIR, ".sounds_cache.json")

# Remote endpoint URLs and defaults
API_URL = "https://admiralclanker-irc.up.railway.app/api/dashboard/sounds"
DOWNLOAD_BASE_URL = "https://admiralclanker-irc.up.railway.app/playsounds"
VALID_AUDIO_EXTENSIONS = ('.mp3', '.ogg', '.wav')
WORKERS = 10


def load_cache(cache_path):
    """Load local sync cache containing last seen ETag and sound metadata."""
    if os.path.exists(cache_path):
        try:
            with open(cache_path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"Warning: Failed to read cache file ({e}). Starting fresh.")
    return {"api_etag": None, "sounds": {}}


def save_cache(cache_path, cache_data):
    """Save sync cache to disk."""
    try:
        with open(cache_path, "w", encoding="utf-8") as f:
            json.dump(cache_data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    except Exception as e:
        print(f"Warning: Failed to save cache file {cache_path}: {e}")


def fetch_sound_catalog(api_url, cached_etag=None):
    """
    Fetch the list of sounds from the remote API endpoint.
    Supports HTTP 304 Not Modified when cached_etag is provided.
    Returns (status_code, new_etag, sounds_list)
    """
    headers = {"User-Agent": "Mozilla/5.0 (compatible; DiscordRoonsBot/1.0)"}
    if cached_etag:
        headers["If-None-Match"] = cached_etag

    req = urllib.request.Request(api_url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            status = response.status
            new_etag = response.headers.get("ETag")
            if status == 200:
                data = json.loads(response.read().decode("utf-8"))
                if isinstance(data, dict) and "sounds" in data:
                    sounds = data["sounds"]
                elif isinstance(data, list):
                    sounds = data
                else:
                    raise ValueError(f"Unexpected response format from API: {type(data)}")
                return 200, new_etag, sounds
            return status, new_etag, []
    except urllib.error.HTTPError as e:
        if e.code == 304:
            return 304, cached_etag, []
        raise RuntimeError(f"HTTP error {e.code} fetching sounds list: {e.reason}")


def download_file(url, dest_path):
    """Download a single file from url to dest_path."""
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0 (compatible; DiscordRoonsBot/1.0)"}
    )
    with urllib.request.urlopen(req, timeout=30) as response:
        if response.status != 200:
            raise RuntimeError(f"HTTP error {response.status} downloading {url}")
        with open(dest_path, 'wb') as out_file:
            shutil.copyfileobj(response, out_file)


def convert_file(ffmpeg_path, src_path, dest_path):
    """Convert audio file to target MP3 with two-pass loudness normalization using ffmpeg."""
    # Pass 1: Analyze loudness
    cmd1 = [ffmpeg_path, "-y", "-i", src_path, "-af", "loudnorm=I=-16:TP=-1.5:LRA=11:print_format=json", "-f", "null", "-"]
    try:
        res1 = subprocess.run(cmd1, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True)
        output_text = res1.stderr + res1.stdout
        match = re.search(r'\{\s*"input_i"\s*:.*?\}', output_text, re.DOTALL)
        if match:
            stats_json = json.loads(match.group(0))
            # Pass 2: Apply linear normalization
            filter_str = (
                f"loudnorm=I=-16:TP=-1.5:LRA=11"
                f":measured_I={stats_json['input_i']}"
                f":measured_TP={stats_json['input_tp']}"
                f":measured_LRA={stats_json['input_lra']}"
                f":measured_thresh={stats_json['input_thresh']}"
                f":measured_offset={stats_json['target_offset']}"
                f":linear=true"
            )
            cmd2 = [ffmpeg_path, "-loglevel", "error", "-y", "-i", src_path, "-af", filter_str, dest_path]
            subprocess.run(cmd2, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True)
            return True
    except Exception:
        pass

    # Fallback to single-pass if two-pass fails
    cmd_fallback = [ffmpeg_path, "-loglevel", "error", "-y", "-i", src_path, "-af", "loudnorm=I=-16:TP=-1.5:LRA=11", dest_path]
    try:
        subprocess.run(cmd_fallback, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True)
        return True
    except Exception as e:
        print(f"Failed to convert {src_path} to {dest_path}: {e}")
        return False


def is_executable(path):
    """Check if a file path exists and is executable on the current platform."""
    if not path or not os.path.isfile(path):
        return False
    if sys.platform != "win32" and path.lower().endswith(".exe"):
        return False
    return os.access(path, os.X_OK)


def resolve_ffmpeg_path():
    """Find a valid ffmpeg binary from system PATH or local scripts dir."""
    system_ffmpeg = shutil.which("ffmpeg")
    if system_ffmpeg:
        return system_ffmpeg
    if sys.platform == "win32" and is_executable(DEFAULT_FFMPEG_PATH):
        return DEFAULT_FFMPEG_PATH
    return None


def main():
    parser = argparse.ArgumentParser(description="Synchronize sound bites from remote API into project resources.")
    parser.add_argument("--clean", "-c", action="store_true", help="Wipe all local sounds and cache to re-fetch everything fresh from server")
    parser.add_argument("--dry-run", "-n", action="store_true", help="Preview additions, conversions, and prunes without modifying disk")
    args = parser.parse_args()

    os.makedirs(DEFAULT_RESOURCES_DIR, exist_ok=True)
    ffmpeg_executable = resolve_ffmpeg_path()

    if not ffmpeg_executable:
        print("Note: ffmpeg was not found. Downloading raw audio files directly without MP3 conversion.")

    # Load cache unless --clean is requested
    cache = {"api_etag": None, "sounds": {}} if args.clean else load_cache(CACHE_FILE_PATH)
    cached_etag = cache.get("api_etag")

    # Step 1: Query catalog from remote API (with conditional ETag if incremental)
    print(f"Connecting to catalog API: {API_URL}")
    try:
        status, new_etag, raw_sounds = fetch_sound_catalog(API_URL, cached_etag=cached_etag)
    except Exception as e:
        print(f"Error fetching sound catalog: {e}")
        sys.exit(1)

    existing_files = {f for f in os.listdir(DEFAULT_RESOURCES_DIR) if f.lower().endswith(VALID_AUDIO_EXTENSIONS)}

    # Check for instant 304 up-to-date shortcut
    if status == 304 and not args.clean:
        cached_sound_names = set(cache.get("sounds", {}).keys())
        missing_on_disk = cached_sound_names - existing_files
        orphan_files = existing_files - cached_sound_names
        if cached_sound_names and not missing_on_disk and not orphan_files:
            print("Remote catalog unchanged (HTTP 304 Not Modified) and all local sounds present. Everything is up to date!")
            return
        # If disk state diverged from cache, fall back to fetching fresh catalog
        print("Cache ETag matches, but local files differ from cache. Fetching complete catalog...")
        try:
            status, new_etag, raw_sounds = fetch_sound_catalog(API_URL, cached_etag=None)
        except Exception as e:
            print(f"Error fetching full sound catalog: {e}")
            sys.exit(1)

    # Build dictionary of all remote sounds
    remote_catalog = {}
    for item in raw_sounds:
        if isinstance(item, dict) and "filename" in item:
            fn = item["filename"]
            up_at = item.get("uploadedAt")
        elif isinstance(item, str):
            fn = item
            up_at = None
        else:
            continue

        base_name, _ = os.path.splitext(fn)
        dest_filename = f"{base_name.lower()}.mp3" if ffmpeg_executable else fn
        remote_catalog[dest_filename] = {
            "remote_filename": fn,
            "uploadedAt": up_at
        }

    remote_dest_names = set(remote_catalog.keys())
    cached_sounds = cache.get("sounds", {})

    # Determine actions: to download, to prune, and up-to-date
    to_download = []
    for dest_name, info in remote_catalog.items():
        dest_path = os.path.join(DEFAULT_RESOURCES_DIR, dest_name)
        needs_download = False
        if args.clean or not os.path.isfile(dest_path):
            needs_download = True
        else:
            cached_info = cached_sounds.get(dest_name)
            if not cached_info:
                needs_download = True
            elif info["uploadedAt"] is not None and cached_info.get("uploadedAt") != info["uploadedAt"]:
                needs_download = True

        if needs_download:
            to_download.append((dest_name, info["remote_filename"], info["uploadedAt"]))

    to_prune = existing_files - remote_dest_names
    up_to_date_count = len(remote_catalog) - len(to_download)

    print("\n--- Synchronization Summary ---")
    print(f"Total remote sounds:  {len(remote_catalog)}")
    print(f"Already up-to-date:   {up_to_date_count}")
    print(f"To download/convert:  {len(to_download)}")
    print(f"To prune (obsolete):  {len(to_prune)}")
    print("-------------------------------\n")

    if args.dry_run:
        print("[Dry Run] No files modified.")
        return

    # Handle clean nuke
    if args.clean:
        print("Cleaning destination directory of existing sound files...")
        cleaned = 0
        for f in existing_files:
            try:
                os.remove(os.path.join(DEFAULT_RESOURCES_DIR, f))
                cleaned += 1
            except Exception as e:
                print(f"Error removing {f}: {e}")
        print(f"Wiped {cleaned} local files.")
        existing_files = set()
    elif to_prune:
        print(f"Pruning {len(to_prune)} orphaned sound files...")
        for orphan in sorted(to_prune):
            try:
                os.remove(os.path.join(DEFAULT_RESOURCES_DIR, orphan))
                cached_sounds.pop(orphan, None)
                print(f"  Pruned: {orphan}")
            except Exception as e:
                print(f"  Error pruning {orphan}: {e}")

    # Process downloads and conversions
    stats = {'success': 0, 'errors': 0}
    if to_download:
        temp_dir = tempfile.mkdtemp(prefix="roons_sounds_")
        base_download_url = DOWNLOAD_BASE_URL.rstrip("/")
        total_downloads = len(to_download)
        counter_lock = threading.Lock()
        completed_count = 0

        def process_sound(entry):
            dest_name, remote_fn, up_at = entry
            encoded_fn = urllib.parse.quote(remote_fn)
            download_url = f"{base_download_url}/{encoded_fn}"
            temp_file_path = os.path.join(temp_dir, remote_fn)

            try:
                download_file(download_url, temp_file_path)
            except Exception as e:
                return False, dest_name, remote_fn, up_at, f"Download failed ({e})"

            dest_path = os.path.join(DEFAULT_RESOURCES_DIR, dest_name)
            if ffmpeg_executable:
                ok = convert_file(ffmpeg_executable, temp_file_path, dest_path)
                if not ok:
                    return False, dest_name, remote_fn, up_at, "Conversion failed"
            else:
                shutil.copy2(temp_file_path, dest_path)

            return True, dest_name, remote_fn, up_at, None

        print(f"Downloading and processing {total_downloads} sounds with {WORKERS} workers...")
        try:
            with ThreadPoolExecutor(max_workers=WORKERS) as executor:
                futures = [executor.submit(process_sound, entry) for entry in to_download]
                for future in as_completed(futures):
                    success, dest_name, remote_fn, up_at, err = future.result()
                    with counter_lock:
                        completed_count += 1
                        pct = (completed_count / total_downloads) * 100
                        if success:
                            stats['success'] += 1
                            cached_sounds[dest_name] = {
                                "remote_filename": remote_fn,
                                "uploadedAt": up_at
                            }
                            print(f"[{completed_count}/{total_downloads}] ({pct:.1f}%) {dest_name}")
                        else:
                            stats['errors'] += 1
                            print(f"[{completed_count}/{total_downloads}] ({pct:.1f}%) ERROR {dest_name}: {err}")
        finally:
            shutil.rmtree(temp_dir, ignore_errors=True)

    print(f"\nProcessing complete. Newly synced: {stats['success']}, Errors: {stats['errors']}")

    # Step 3: Rebuild manifest.json
    print("Rebuilding manifest.json...")
    all_sounds = [f for f in os.listdir(DEFAULT_RESOURCES_DIR) if f.lower().endswith(VALID_AUDIO_EXTENSIONS)]
    all_sounds.sort(key=str.lower)

    manifest_path = os.path.join(DEFAULT_RESOURCES_DIR, "manifest.json")
    with open(manifest_path, 'w', encoding='utf-8') as f:
        json.dump(all_sounds, f, indent=2, ensure_ascii=False)
        f.write('\n')
    print(f"manifest.json updated successfully with {len(all_sounds)} total sound bites.")

    # Save cache
    cache["api_etag"] = new_etag
    cache["sounds"] = cached_sounds
    save_cache(CACHE_FILE_PATH, cache)
    print(f"Sync cache saved to {os.path.basename(CACHE_FILE_PATH)}.")

    if stats['errors'] > 0:
        sys.exit(1)


if __name__ == "__main__":
    main()
