 ---
name: youtube-link-download
description: Download videos, audio, or subtitles from YouTube links or playlists using yt-dlp. Use for single links, playlists, batch jobs, and metadata/subtitle extraction workflows.
license: Proprietary. LICENSE.txt has complete terms
---

# YouTube Link Download

## Overview
Provide a reliable, repeatable workflow for downloading YouTube content from URLs using yt-dlp, with safe defaults and clear output expectations.

## When to Use
- The user provides one or more YouTube links and wants video or audio files
- The user requests subtitles, metadata, thumbnails, or playlist downloads
- The user needs batch downloads with consistent naming

## Required Inputs
- Source URL(s): single video or playlist
- Desired output: video, audio-only, subtitles, or metadata
- Output folder and naming preference
- Quality preference: best, balanced, or smallest

## Workflow Checklist

### [ ] Step 1: Confirm URL
- [ ] Validate URL format and accessibility
- [ ] Determine content type (single/playlist/channel)
- [ ] Assess regional availability
- 📖 [Read detailed guidance](workflow/step1-confirm-url.md)

### [ ] Step 2: Choose Output Type
- [ ] Select video, audio-only, subtitles, or metadata
- [ ] Choose appropriate format (MP4, MP3, SRT, etc.)
- [ ] Consider storage and compatibility requirements
- 📖 [Read detailed guidance](workflow/step2-choose-output.md)

### [ ] Step 3: Select Quality Profile
- [ ] Choose best/balanced/smallest quality
- [ ] Consider download time vs quality trade-offs
- [ ] Set resolution and codec preferences
- 📖 [Read detailed guidance](workflow/step3-select-quality.md)

### [ ] Step 4: Apply Naming Rules
- [ ] Configure output folder location
- [ ] Set filename template and sanitization
- [ ] Handle playlist organization if applicable
- 📖 [Read detailed guidance](workflow/step4-apply-naming.md)

### [ ] Step 5: Run Download
- [ ] Execute yt-dlp with configured options
- [ ] Monitor progress and handle interruptions
- [ ] Generate download summary report
- 📖 [Read detailed guidance](workflow/step5-run-download.md)

### [ ] Step 6: Verify Files
- [ ] Check file presence and integrity
- [ ] Validate format and basic playback
- [ ] Review verification report
- 📖 [Read detailed guidance](workflow/step6-verify-files.md)

## Rules and Guidelines

### Safety & Compliance
- [ ] Review legal and ethical considerations
- [ ] Ensure compliance with platform policies
- [ ] Respect copyright and access restrictions
- 📖 [Read safety rules](rules/safety-compliance.md)

### Error Handling
- [ ] Understand common error types
- [ ] Review retry and fallback strategies
- [ ] Know how errors are reported
- 📖 [Read error handling rules](rules/error-handling.md)

## Templates and Examples

### Output Report Template
- [ ] Use for consistent download reporting
- 📖 [View template](templates/output-report-template.md)

### Download Examples
- [ ] Review usage examples and command patterns
- 📖 [View examples](examples/download-examples.md)

## References
- Read `references/yt-dlp-options.md` for option guidance and format selection
- Read `references/filename-templates.md` for naming conventions
