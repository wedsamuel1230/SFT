# Download Examples

## Basic Usage with uv

### Single Video Download
```bash
uv run --no-project scripts/download.py "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

### Audio-Only Download
```bash
uv run --no-project scripts/download.py --audio-only "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

### Playlist Download
```bash
uv run --no-project scripts/download.py "https://www.youtube.com/playlist?list=PLrAXtmRdnEQy4qtr5GrLOg8ONEwQYjF6"
```

### High Quality Video
```bash
uv run --no-project scripts/download.py --quality best "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

### Custom Output Folder
```bash
uv run --no-project scripts/download.py --output-folder "/path/to/downloads" "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

### Subtitles Only
```bash
uv run --no-project scripts/download.py --subtitles-only "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

## Advanced Examples

### Batch Download from File
```bash
# Create urls.txt with one URL per line
echo "https://www.youtube.com/watch?v=VIDEO1" > urls.txt
echo "https://www.youtube.com/watch?v=VIDEO2" >> urls.txt

uv run --no-project scripts/download.py --batch urls.txt
```

### Custom Naming Template
```bash
uv run --no-project scripts/download.py \
  --template "%(uploader)s - %(title)s [%(id)s].%(ext)s" \
  "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

### Playlist with Organization
```bash
uv run --no-project scripts/download.py \
  --playlist-folder \
  "https://www.youtube.com/playlist?list=PLrAXtmRdnEQy4qtr5GrLOg8ONEwQYjF6"
```

### Metadata Extraction
```bash
uv run --no-project scripts/download.py \
  --metadata-only \
  --output-folder "./metadata" \
  "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

## Configuration Examples

### Quality Presets
- **best**: Highest quality available
- **balanced**: 1080p or best under 1080p
- **small**: 480p or lowest available

### Format Options
- **mp4**: H.264 video in MP4 container
- **webm**: VP9 video in WebM container
- **mp3**: Audio-only MP3
- **m4a**: Audio-only AAC in M4A container

## Error Handling Examples

### Retry Failed Downloads
```bash
# Script automatically retries network errors
uv run --no-project scripts/download.py --retry-failed "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

### Skip Unavailable Content
```bash
# Continue downloading even if some videos are unavailable
uv run --no-project scripts/download.py --skip-unavailable "PLAYLIST_URL"
```

## Integration Examples

### With Other Tools
```bash
# Download and immediately convert
uv run --no-project scripts/download.py --audio-only "URL" && \
ffmpeg -i "downloaded.mp3" -acodec libopus "converted.opus"
```

### Scheduled Downloads
```bash
# Cron job for regular downloads
0 2 * * * cd /path/to/project && uv run --no-project scripts/download.py "PLAYLIST_URL"
```