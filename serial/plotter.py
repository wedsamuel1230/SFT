import sys
import argparse
from PyQt6.QtWidgets import QApplication
from plotter_gui import IMUPlotterWindow

def parse_args():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(
        description='Real-time serial plotter for MPU6050 IMU data',
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )
    parser.add_argument(
        '--port',
        type=str,
        default='COM5',
        help='Serial port name (e.g., COM5 on Windows, /dev/ttyUSB0 on Linux)'
    )
    parser.add_argument(
        '--baud',
        type=int,
        default=9600,
        help='Baud rate for serial communication'
    )
    parser.add_argument(
        '--window',
        type=int,
        default=300,
        help='Number of samples to display in rolling window'
    )
    parser.add_argument(
        '--log-file',
        type=str,
        default=None,
        help='CSV file path for logging data (default: auto-generated timestamp)'
    )
    parser.add_argument(
        '--theme',
        type=str,
        choices=['light', 'dark', 'auto'],
        default='auto',
        help='Qt window theme (light, dark, or auto-detect)'
    )
    return parser.parse_args()

def main():
    args = parse_args()
    
    app = QApplication(sys.argv)
    app.setStyle("Fusion") # Cleaner look across platforms
    
    window = IMUPlotterWindow(
        port=args.port,
        baud=args.baud,
        window_size=args.window,
        log_file=args.log_file,
        theme=args.theme,
        mode='dual'
    )
    window.show()
    
    sys.exit(app.exec())

if __name__ == '__main__':
    main()
