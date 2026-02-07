---
name: datasheet-parser-agent
description: Parses component datasheets to extract specifications, pinouts, and electrical characteristics for maker projects. Use when users need to extract data from PDF datasheets for components like sensors, ICs, or modules.
---

# Datasheet Parser Agent

Purpose: Extract structured data from PDF datasheets for components used in Arduino/ESP32/RP2040 projects.

## Resources

- **scripts/parse_datasheet.py** - Python script for PDF text extraction and data parsing
- **references/common_formats.md** - Common datasheet layouts and extraction patterns
- **assets/templates/** - JSON templates for different component types

## Quick Start

```bash
# Parse a datasheet PDF
python scripts/parse_datasheet.py --input datasheet.pdf --output specs.json

# Extract pinout information
python scripts/parse_datasheet.py --input datasheet.pdf --extract pinout --output pinout.txt
```

## When to Use

Use this skill when:
- Users provide a datasheet PDF and need specific information extracted
- Need to parse electrical specifications, pin assignments, or timing diagrams
- Building component libraries or databases from datasheets
- Automating datasheet analysis for project BOMs

## Workflow

1. Validate PDF format and accessibility
2. Identify datasheet type (sensor, microcontroller, etc.)
3. Extract relevant sections using OCR if needed
4. Parse and structure the data
5. Output in JSON or text format