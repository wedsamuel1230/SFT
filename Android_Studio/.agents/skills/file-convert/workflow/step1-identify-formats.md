# Step 1: Identify Formats

## Overview
Analyze the source file(s) and determine the target format(s) for conversion. This step involves format detection, validation of conversion feasibility, and preparation of conversion parameters.

## Required Inputs
- Source file(s) and target format(s)
- Desired quality or fidelity requirements
- Output folder and naming preference
- Input type: file_path, base64, or url
- Output type: file_path or base64
- Input format (required for base64/url inputs)

## Format Detection
- For file_path inputs: Use file extension and MIME type detection
- For base64 inputs: Require explicit format specification
- For URL inputs: Download headers and content analysis

## Supported Format Categories
- Documents: PDF, DOCX, PPTX
- Tables: CSV, XLSX
- Images: PNG, JPG, WEBP, TIFF
- Animations: GIF
- Video: MP4, MOV, WEBM, AVI
- Audio: MP3, WAV, FLAC
- OCR sources: Images, PDFs
- CAD: DXF
- Subtitles: SRT, VTT

## Validation
- Verify source format is supported
- Confirm target format is achievable
- Check for format-specific requirements (e.g., OCR needs Tesseract)
- Assess file size and complexity for processing feasibility