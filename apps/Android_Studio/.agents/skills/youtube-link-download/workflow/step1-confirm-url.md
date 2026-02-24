# Step 1: Confirm URL

## Overview
Validate and categorize the provided YouTube URL(s) to determine the download approach.

## URL Types
- **Single Video**: Standard YouTube video URL (e.g., https://www.youtube.com/watch?v=VIDEO_ID)
- **Playlist**: YouTube playlist URL (e.g., https://www.youtube.com/playlist?list=PLAYLIST_ID)
- **Channel**: Channel URL (may require special handling)
- **Shorts**: YouTube Shorts URLs

## Validation Steps
1. Verify URL format matches YouTube patterns
2. Check if URL is accessible (not private/deleted)
3. Determine content type (video, playlist, channel)
4. Assess content availability in user's region

## Decision Points
- Single video → proceed to output selection
- Playlist → consider batch processing requirements
- Invalid/unavailable → report error and exit

## Tools Required
- URL validation regex patterns
- yt-dlp URL info extraction
- Basic connectivity checks