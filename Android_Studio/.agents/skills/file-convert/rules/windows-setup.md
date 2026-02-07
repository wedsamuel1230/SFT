# Windows Setup Requirements

## Essential Tools

### FFmpeg (Video/Audio/OCR)
FFmpeg is required for video transcoding, audio conversion, and OCR preprocessing.

```powershell
# Install via Winget (recommended)
winget install --id=Gyan.FFmpeg -e

# Add to PATH if not automatic
# Check: ffmpeg -version
```

### Tesseract (OCR)
Tesseract OCR engine for text extraction from images and PDFs.

```powershell
# Download MSI installer from:
# https://github.com/UB-Mannheim/tesseract/wiki

# Install and ensure added to PATH
# Check: tesseract --version
```

## Optional Tools

### gifsicle (GIF Optimization)
For advanced GIF animation optimization and manipulation.

```powershell
# Install via Chocolatey
choco install gifsicle

# Or download from: https://www.lcdf.org/gifsicle/
# Add to PATH manually
```

## Python Dependencies
Install required Python packages:

```bash
pip install pikepdf PyMuPDF openpyxl Pillow ffmpeg-python pytesseract ezdxf pysrt
```

## System Requirements
- Windows 10 or later
- Sufficient disk space for temporary files
- Internet access for URL-based conversions
- Administrator privileges for tool installation

## Verification
Run the following to verify all tools are properly installed:

```powershell
ffmpeg -version
tesseract --version
gifsicle --version  # Optional
python -c "import PIL, pytesseract, ffmpeg; print('Python deps OK')"
```