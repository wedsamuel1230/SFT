# Step 3: Perform Conversion

## Overview
Execute the actual file conversion using the selected toolchain and handle all input/output operations according to the specified parameters.

## Input/Output Handling
- **Inputs**:
  - `file_path`: Local file path (string)
  - `base64`: Base64 encoded data (string)
  - `url`: HTTP/HTTPS URL for remote file (string)
- **Outputs**:
  - `file_path`: Save to local file path (string)
  - `base64`: Return base64 encoded result (string)
- **Validation**: Input validation, network error handling, temporary file management
- **Streaming**: Large files (>100MB) are streamed to temporary files to avoid memory issues
- **Windows Compatible**: Uses proper path separators and temporary file handling

## Core Functions
- `read_input_to_bytes(input_type, input_value)`: Read input from various sources to bytes
- `write_output_from_bytes(output_type, output_value, data)`: Write output data to various destinations
- `convert_with_io_handling(...)`: Unified conversion function with IO handling

## Conversion Process
1. Prepare input data (download URLs, decode base64)
2. Apply selected conversion toolchain
3. Handle format-specific parameters (quality, compression, etc.)
4. Generate output in requested format
5. Clean up temporary files and resources

## Progress Monitoring
- Track conversion progress for large files
- Provide status updates during processing
- Handle interruptions gracefully
- Report processing time and resource usage