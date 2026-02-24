# Step 5: Run Download

## Overview
Execute the download process using yt-dlp with configured options.

## Download Process

### Single Video Download
```bash
yt-dlp [OPTIONS] "VIDEO_URL"
```

### Playlist Download
```bash
yt-dlp [OPTIONS] "PLAYLIST_URL"
```

### Batch Download
```bash
yt-dlp [OPTIONS] -a urls.txt
```

## Key Options
- **Format selection**: `-f "best[height<=1080]"`
- **Output template**: `-o "%(title)s.%(ext)s"`
- **Download archive**: `--download-archive archive.txt`
- **Parallel downloads**: `-N 4` (max 4 concurrent)
- **Rate limiting**: `--limit-rate 1M`

## Progress Monitoring
- Real-time download progress
- ETA calculations
- Speed indicators
- Error reporting

## Resume Capability
- Automatic resume of interrupted downloads
- Partial file recovery
- Skip already downloaded content

## Logging and Reporting
- Capture stdout/stderr for summary
- Track successful vs failed downloads
- Generate completion report
- Log errors with context

## Performance Considerations
- Adjust parallelism based on network
- Respect YouTube's rate limits
- Handle large playlists efficiently
- Monitor system resources

## Success Criteria
- All requested content downloaded
- Files saved to correct locations
- No critical errors encountered
- Summary report generated