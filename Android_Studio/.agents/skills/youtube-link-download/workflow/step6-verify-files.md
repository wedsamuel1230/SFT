# Step 6: Verify Files

## Overview
Validate downloaded files for integrity and basic functionality.

## Verification Steps

### File Presence Check
- Confirm all expected files exist
- Verify file sizes are reasonable (>0 bytes)
- Check file permissions (readable/writable)

### Integrity Validation
- **Video files**: Basic playback test (ffprobe)
- **Audio files**: Format validation (ffprobe)
- **Subtitle files**: Encoding check (UTF-8)
- **Metadata files**: JSON syntax validation

### Content Validation
- **Duration check**: Compare with YouTube duration
- **Format verification**: Confirm selected codec/container
- **Quality assessment**: Basic resolution/bitrate check
- **Completeness**: Ensure no truncated files

### Playback Testing
- **Video**: Quick frame extraction test
- **Audio**: Basic decode test
- **Subtitles**: Sync validation if possible

## Error Reporting
- List any failed verifications
- Provide specific error details
- Suggest remediation steps
- Track verification success rate

## Cleanup Options
- Remove incomplete/corrupted files
- Retry failed downloads automatically
- Archive verification logs

## Success Metrics
- **100% files present**: All downloads completed
- **100% integrity**: No corrupted files
- **Playback ready**: Files ready for use
- **Report generated**: Verification summary available

## Automated vs Manual
- **Automated**: Script-based verification
- **Manual**: User spot-checks for critical content
- **Hybrid**: Automated checks with user confirmation