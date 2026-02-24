# Step 4: Apply Naming Rules

## Overview
Configure output file naming conventions and folder organization.

## Naming Templates

### Basic Template
```
%(title)s.%(ext)s
```
- Uses video title as filename
- Extension based on format

### Enhanced Template
```
%(uploader)s - %(title)s [%(id)s].%(ext)s
```
- Includes uploader/channel name
- Video ID for uniqueness
- Prevents filename conflicts

### Playlist Template
```
%(playlist_title)s/%(playlist_index)02d - %(title)s.%(ext)s
```
- Organizes in playlist subfolders
- Zero-padded playlist index
- Maintains playlist order

## Sanitization Rules
- Remove/replace invalid filename characters: `< > : " | ? * \ /`
- Truncate overly long filenames (255 char limit)
- Handle Unicode characters properly
- Avoid duplicate names with incremental numbering

## Folder Organization
- **Single video**: Direct to output folder
- **Playlist**: Subfolder per playlist
- **Channel**: Subfolder per channel
- **Batch download**: Organized by date or category

## Custom Patterns
- Date prefixes: `%(upload_date)s - %(title)s`
- Resolution suffixes: `%(title)s (%(height)sp).%(ext)s`
- Quality indicators: `%(title)s [HQ].%(ext)s`

## Configuration
- Default: Clean, readable filenames
- Option: Preserve original YouTube titles
- Option: Custom template strings