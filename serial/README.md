# Serial MPU6050 Real-Time Plotter

Real-time visualization of MPU6050 accelerometer and gyroscope data from Arduino via serial port.

## Features
- ✅ **Modern PyQt6 GUI**: Clean interface with sidebar controls (v2.3.0)
- ✅ **Two plot modes**: Dual subplot (plotter.py) or mixed single plot (plotter_mixed.py)
- ✅ **Instant CSV Export**: Capture current buffer snapshot to CSV via GUI button
- ✅ **Theme Switcher**: Toggle Light/Dark modes instantly via GUI
- ✅ **CSV data logging** with auto-generated timestamps and buffering
- ✅ **CLI arguments** for port, baud rate, window size, and log file
- ✅ Real-time animation with configurable rolling window
- ✅ Graceful degradation and robust error handling

## Hardware Requirements
- Arduino board (Uno, Nano, ESP32, etc.)
- Adafruit MPU6050 sensor
- USB/Bluetooth serial connection

## Software Requirements
- Python 3.11+
- uv (package manager)
- PyQt6 (auto-installed via uv for Qt GUI backend)

## Installation

```bash
# Install dependencies with uv (includes PyQt6 for GUI)
uv sync
```

## Usage

### Basic Usage (Default Settings)

```bash
# Dual subplot plot (acceleration + gyroscope in separate subplots)
uv run python plotter.py

# Mixed plot (all 6 signals on single plot with dual y-axes)
uv run python plotter_mixed.py
```

### Advanced Usage with CLI Arguments

```bash
# Custom serial port and baud rate
uv run python plotter.py --port COM7 --baud 9600

# Custom window size (number of samples displayed)
uv run python plotter.py --window 500

# Enable CSV logging with auto-generated filename
uv run python plotter.py --log-file ""

# Enable CSV logging with custom filename
uv run python plotter.py --log-file my_data.csv

# Combine multiple options
uv run python plotter_mixed.py --port /dev/ttyUSB0 --baud 115200 --window 1000 --log-file imu_test.csv
```

### CLI Arguments

| Argument | Default | Description |
|----------|---------|-------------|
| `--port` | `COM5` | Serial port name (e.g., COM5 on Windows, /dev/ttyUSB0 on Linux) |
| `--baud` | `115200` | Baud rate for serial communication |
| `--window` | `300` | Number of samples to display in rolling window |
| `--log-file` | `None` | CSV file path for logging data (omit for no logging, use empty string `""` for auto-generated timestamp) |
| `--theme` | `auto` | Qt window theme: `light`, `dark`, or `auto` (auto-detects system theme) |

### Theme Examples

```bash
# Dark mode for night viewing
uv run python plotter.py --theme dark

# Light mode (high contrast)
uv run python plotter_mixed.py --theme light

# Auto-detect system theme (default)
uv run python plotter.py --theme auto
```

### Help

```bash
uv run python plotter.py --help
uv run python plotter_mixed.py --help
```

## Plot Modes

### Dual Subplot Mode (plotter.py)
- **Figure size**: 14x10 inches
- **Layout**: Two vertically stacked subplots
  - Top: Acceleration (ax, ay, az) in m/s²
  - Bottom: Gyroscope (gx, gy, gz) in rad/s
- **Best for**: Separate analysis of acceleration and rotation

### Mixed Plot Mode (plotter_mixed.py)
- **Figure size**: 14x7 inches
- **Layout**: Single plot with dual y-axes
  - Left y-axis (blue): Acceleration (ax, ay, az) in m/s²
  - Right y-axis (red): Gyroscope (gx, gy, gz) in rad/s
- **Distinct line styles**: Solid, dashed, and dotted for each axis
- **Best for**: Compact view and correlation analysis

## CSV Data Logging

When `--log-file` is provided, the plotter logs all received data to a CSV file:

**Features:**
- Buffered writing (100 rows or 1 second interval)
- ISO 8601 timestamps for each sample
- Columns: `timestamp, ax, ay, az, gx, gy, gz`
- Automatic file creation and cleanup on exit

**Example output:**
```csv
timestamp,ax,ay,az,gx,gy,gz
2026-01-14T10:30:45.123456,0.12,0.45,9.81,0.01,0.02,0.03
2026-01-14T10:30:45.156789,0.15,0.43,9.79,0.02,0.01,0.04
```

**Auto-generated filenames:**
```bash
# Use empty string for auto-generated timestamp filename
uv run python plotter.py --log-file ""
# Creates: imu_data_20260114_103045.csv
```

## Arduino Data Format

The Arduino sends data in labeled CSV format:
```
ax:1.23,ay:4.56,az:9.81,gx:0.01,gy:0.02,gz:0.03
```

Where:
- `ax, ay, az`: Acceleration (m/s²)
- `gx, gy, gz`: Gyroscope (rad/s)

## Troubleshooting

### Window closes immediately
✅ **Fixed** - Window now displays even if serial port unavailable

### No data showing
- Check COM port number (Device Manager → Ports)
- Verify baud rate matches (default 115200)
- Check Arduino is sending data (open Serial Monitor)
- Ensure MPU6050 is properly wired
- Try different `--port` and `--baud` values

### Permission denied
- Close other programs using the serial port (Arduino IDE Serial Monitor, PuTTY, etc.)
- On Linux: Add user to `dialout` group

### CSV logging not working
- Ensure you have write permissions in the current directory
- Check disk space
- Use absolute path for `--log-file` if relative path fails

## Project Structure

```
serial/
├── plotter.py              # Dual subplot plotter
├── plotter_mixed.py        # Mixed plot with dual y-axes
├── plotter_twoGraph.py     # Original dual subplot (same as plotter.py)
├── plotter/
│   └── plotter.ino         # Arduino MPU6050 sketch
├── pyproject.toml          # Python dependencies
├── .gitignore              # Git ignore patterns
├── memory-bank/            # AI session context
└── README.md               # This file
```

## Error Handling

The plotter handles these scenarios gracefully:
- ❌ Serial port not found → Shows warning, displays empty window
- ❌ Permission denied → Clear error message
- ❌ Runtime disconnect → Continues animation, logs error
- ❌ Malformed data → Skips invalid lines silently
- ❌ CSV write errors → Logs error, continues plotting

## Examples

```bash
# Monitor default COM5 with logging
uv run python plotter.py --log-file sensor_data.csv

# Monitor COM7 with larger window
uv run python plotter.py --port COM7 --window 1000

# Mixed plot on Linux with logging
uv run python plotter_mixed.py --port /dev/ttyUSB0 --log-file ""

# Low baud rate device with small window
uv run python plotter.py --baud 9600 --window 100
```

## License

MIT
