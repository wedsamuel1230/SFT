import time
import csv
from collections import deque
from datetime import datetime
import serial
from serial import SerialException
import matplotlib
matplotlib.use('QtAgg')

from PyQt6.QtWidgets import (QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, 
                             QPushButton, QLabel, QFrame, QFileDialog, QMessageBox)
from PyQt6.QtCore import QTimer, Qt
from PyQt6.QtGui import QPalette, QColor

from matplotlib.backends.backend_qtagg import FigureCanvasQTAgg, NavigationToolbar2QT
from matplotlib.figure import Figure
import matplotlib.pyplot as plt

# --- Data Handling Helper Classes ---

class CSVLogger:
    """CSV logger with buffering for IMU data."""
    
    def __init__(self, filepath: str | None = None):
        self.filepath = filepath
        self.file_handle = None
        self.csv_writer = None
        self.buffer = []
        self.buffer_size = 100
        self.last_flush = time.time()
        self.flush_interval = 1.0  # seconds
        
        if self.filepath is not None:
            self._open_file()
    
    def _open_file(self):
        """Open CSV file and write header."""
        if self.filepath is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            self.filepath = f"imu_data_{timestamp}.csv"
        
        try:
            self.file_handle = open(self.filepath, 'w', newline='', encoding='utf-8')
            self.csv_writer = csv.writer(self.file_handle)
            self.csv_writer.writerow(['timestamp', 'ax', 'ay', 'az', 'gx', 'gy', 'gz'])
            self.file_handle.flush()
            print(f"Logging data to: {self.filepath}")
        except Exception as e:
            print(f"Error opening log file: {e}")
            self.file_handle = None
            self.csv_writer = None
    
    def log(self, data: dict[str, float]):
        """Log a data row to CSV with timestamp."""
        if self.csv_writer is None:
            return
        
        timestamp = datetime.now().isoformat()
        row = [
            timestamp,
            data.get('ax', 0.0),
            data.get('ay', 0.0),
            data.get('az', 0.0),
            data.get('gx', 0.0),
            data.get('gy', 0.0),
            data.get('gz', 0.0)
        ]
        self.buffer.append(row)
        
        # Flush if buffer is full or time interval elapsed
        current_time = time.time()
        if len(self.buffer) >= self.buffer_size or (current_time - self.last_flush) >= self.flush_interval:
            self._flush()
    
    def _flush(self):
        """Flush buffer to disk."""
        if self.csv_writer and self.buffer:
            try:
                self.csv_writer.writerows(self.buffer)
                self.file_handle.flush()
                self.buffer.clear()
                self.last_flush = time.time()
            except Exception as e:
                print(f"Error flushing data: {e}")
    
    def close(self):
        """Flush remaining data and close file."""
        if self.file_handle:
            self._flush()
            self.file_handle.close()
            print(f"CSV log closed: {self.filepath}")

class SerialHandler:
    """Handles serial connection and parsing."""
    def __init__(self, port, baud, window_size):
        self.port = port
        self.baud = baud
        self.ser = None
        self.error_msg = None
        self.buffers = {name: deque(maxlen=window_size) for name in ['ax','ay','az','gx','gy','gz']}
        
        # Fields mapping
        self.acc_fields = ["ax", "ay", "az"]
        self.gyr_fields = ["gx", "gy", "gz"]
        
    def connect(self):
        try:
            self.ser = serial.Serial(self.port, self.baud, timeout=1)
            time.sleep(2)
            self.ser.reset_input_buffer()
            return True, f"Connected to {self.port}"
        except SerialException as e:
            self.error_msg = str(e)
            return False, f"Error: {e}"
        except Exception as e:
            self.error_msg = str(e)
            return False, f"Error: {e}"

    def close(self):
        if self.ser:
            try:
                self.ser.close()
            except:
                pass
            self.ser = None

    def read_data(self, logger=None):
        """Read all available lines from serial."""
        if not self.ser:
            return 
            
        try:
            while self.ser.in_waiting:
                try:
                    raw = self.ser.readline().decode(errors="ignore")
                    values = self._parse_line(raw)
                    if values:
                        data_dict = {}
                        # values structure: [t, ax, ay, az, gx, gy, gz] - t might be dummy
                        idx = 1
                        for key in self.acc_fields + self.gyr_fields:
                            val = values[idx]
                            self.buffers[key].append(val)
                            data_dict[key] = val
                            idx += 1
                            
                        if logger:
                            logger.log(data_dict)
                            
                except (SerialException, OSError):
                    break
        except Exception:
            pass

    def _parse_line(self, line):
        parts = line.strip().split(",")
        if len(parts) < 6: return None
        try:
            values = []
            for part in parts:
                if ':' in part: values.append(float(part.split(':')[1]))
                else: values.append(float(part))
            
            # Ensure 6 values
            if len(values) >= 6:
                return [0] + values[:6] # [t, ax, ay, az, gx, gy, gz]
            return None
        except:
            return None

# --- Main Window ---

class IMUPlotterWindow(QMainWindow):
    def __init__(self, port, baud, window_size, log_file, theme='auto', mode='dual'):
        super().__init__()
        self.mode = mode
        self.theme = theme
        self.window_size = window_size
        
        # Data Handler
        self.serial_handler = SerialHandler(port, baud, window_size)
        connected, msg = self.serial_handler.connect()
        if not connected:
            print(msg) # Still open window for "No Connection" state
        
        self.logger = CSVLogger(log_file) if log_file else None
        
        # Styles
        self.dark_qss = """
            QMainWindow { background-color: #2b2b2b; color: #ffffff; }
            QLabel { color: #ffffff; font-size: 14px; }
            QPushButton { 
                background-color: #0d47a1; color: white; border: none; padding: 8px; border-radius: 4px; font-weight: bold;
            }
            QPushButton:hover { background-color: #1976d2; }
            QPushButton:pressed { background-color: #0d47a1; }
            QFrame { background-color: #333333; border-radius: 8px; }
        """
        self.light_qss = """
            QMainWindow { background-color: #f0f0f0; color: #000000; }
            QLabel { color: #000000; font-size: 14px; }
            QPushButton { 
                background-color: #2196f3; color: white; border: none; padding: 8px; border-radius: 4px; font-weight: bold;
            }
            QPushButton:hover { background-color: #42a5f5; }
        """
        
        self.init_ui()
        self.apply_theme(self.theme)
        
        # Timer for loop
        self.timer = QTimer()
        self.timer.timeout.connect(self.update_loop)
        self.timer.start(30) # ~33fps
        
    def init_ui(self):
        self.setWindowTitle("IMU Serial Plotter Lab")
        self.resize(1200, 800)
        
        # Central Widget & Layout
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        main_layout = QHBoxLayout(central_widget)
        main_layout.setContentsMargins(10, 10, 10, 10)
        main_layout.setSpacing(10)
        
        # --- Sidebar ---
        sidebar = QFrame()
        sidebar.setFixedWidth(250)
        sidebar_layout = QVBoxLayout(sidebar)
        
        # Title
        title_lbl = QLabel("CONTROLS")
        title_lbl.setStyleSheet("font-size: 18px; font-weight: bold; color: #2196f3;")
        sidebar_layout.addWidget(title_lbl)
        sidebar_layout.addSpacing(20)
        
        # Info
        valid_port = self.serial_handler.ser is not None
        status_color = "#4caf50" if valid_port else "#f44336"
        status_text = f"Connected: {self.serial_handler.port}" if valid_port else "Disconnected"
        self.status_lbl = QLabel(status_text)
        self.status_lbl.setStyleSheet(f"color: {status_color}; font-weight: bold;")
        sidebar_layout.addWidget(QLabel("Status:"))
        sidebar_layout.addWidget(self.status_lbl)
        
        baud_lbl = QLabel(f"Baud: {self.serial_handler.baud}")
        sidebar_layout.addWidget(baud_lbl)
        
        sidebar_layout.addSpacing(30)
        
        # Actions
        btn_export = QPushButton("📸 Export Snapshot (CSV)")
        btn_export.clicked.connect(self.export_snapshot)
        sidebar_layout.addWidget(btn_export)
        
        btn_theme = QPushButton("🌓 Toggle Theme")
        btn_theme.clicked.connect(self.toggle_theme)
        sidebar_layout.addWidget(btn_theme)
        
        sidebar_layout.addStretch()
        
        # Version info
        sidebar_layout.addWidget(QLabel("v2.3.0 GUI"))
        
        main_layout.addWidget(sidebar)
        
        # --- Plot Area ---
        plot_container = QWidget()
        plot_layout = QVBoxLayout(plot_container)
        
        # Matplotlib Figure
        self.fig = Figure(figsize=(10, 8))
        self.setup_plot_axes()
        
        self.canvas = FigureCanvasQTAgg(self.fig)
        self.toolbar = NavigationToolbar2QT(self.canvas, self)
        
        plot_layout.addWidget(self.toolbar)
        plot_layout.addWidget(self.canvas)
        
        main_layout.addWidget(plot_container)

    def setup_plot_axes(self):
        """Setup figure based on mode."""
        self.lines = {}
        
        if self.mode == 'dual':
            self.ax1 = self.fig.add_subplot(211)
            self.ax2 = self.fig.add_subplot(212, sharex=self.ax1)
            
            # Acceleration
            self.ax1.set_title("Acceleration (m/s²)")
            self.ax1.set_ylabel("m/s²")
            for name in ['ax','ay','az']:
                line, = self.ax1.plot([], [], lw=1, label=name)
                self.lines[name] = line
            self.ax1.legend(loc="upper right")
            
            # Gyro
            self.ax2.set_title("Gyroscope (rad/s)")
            self.ax2.set_ylabel("rad/s")
            self.ax2.set_xlabel("Sample")
            for name in ['gx','gy','gz']:
                line, = self.ax2.plot([], [], lw=1, label=name)
                self.lines[name] = line
            self.ax2.legend(loc="upper right")
            
        else: # mixed
            self.ax1 = self.fig.add_subplot(111)
            self.ax2 = self.ax1.twinx()
            
            self.ax1.set_title("IMU Data - Mixed Plot")
            self.ax1.set_ylabel("Acceleration (m/s²)", color='#2196f3') # Blue-ish
            self.ax1.tick_params(axis='y', labelcolor='#2196f3')
            self.ax1.set_xlabel("Sample")
            
            self.ax2.set_ylabel("Gyroscope (rad/s)", color='#f44336') # Red-ish
            self.ax2.tick_params(axis='y', labelcolor='#f44336')
            
            # Acc lines (Blue shades)
            colors_acc = ['#1e88e5', '#64b5f6', '#bbdefb']
            styles = ['-', '--', ':']
            for name, col, sty in zip(['ax','ay','az'], colors_acc, styles):
                line, = self.ax1.plot([], [], color=col, linestyle=sty, lw=1.5, label=name)
                self.lines[name] = line
                
            # Gyro lines (Red shades)
            colors_gyr = ['#e53935', '#e57373', '#ffcdd2']
            for name, col, sty in zip(['gx','gy','gz'], colors_gyr, styles):
                line, = self.ax2.plot([], [], color=col, linestyle=sty, lw=1.5, label=name)
                self.lines[name] = line
            
            # Combined Legend
            lines1, labels1 = self.ax1.get_legend_handles_labels()
            lines2, labels2 = self.ax2.get_legend_handles_labels()
            self.ax1.legend(lines1 + lines2, labels1 + labels2, loc='upper right')

    def apply_theme(self, theme_name):
        """Apply theme to App and Matplotlib."""
        if theme_name == 'auto':
            # Simple detection fallback
            theme_name = 'dark' # Default preference in GUI
        
        self.current_theme = theme_name
        
        if theme_name == 'dark':
            self.setStyleSheet(self.dark_qss)
            plt.style.use('dark_background')
            self.fig.patch.set_facecolor('#2b2b2b')
            # Text colors adjust automatically in plt.style('dark_background') usually
        else:
            self.setStyleSheet(self.light_qss)
            plt.style.use('default')
            self.fig.patch.set_facecolor('#f0f0f0')
            
        # Re-apply styles to existing axes
        if hasattr(self, 'ax1'):
            self.ax1.set_facecolor('#1e1e1e' if theme_name == 'dark' else '#ffffff')
            if self.mode == 'dual':
                self.ax2.set_facecolor('#1e1e1e' if theme_name == 'dark' else '#ffffff')
            
            # Mixed mode specific axis label colors need to persist
            if self.mode == 'mixed':
                self.ax1.yaxis.label.set_color('#2196f3')
                self.ax2.yaxis.label.set_color('#f44336')
                self.ax1.tick_params(axis='y', labelcolor='#2196f3') 
                self.ax2.tick_params(axis='y', labelcolor='#f44336')
                
        self.canvas.draw_idle()

    def toggle_theme(self):
        new_theme = 'light' if self.current_theme == 'dark' else 'dark'
        self.apply_theme(new_theme)
        
    def export_snapshot(self):
        filename, _ = QFileDialog.getSaveFileName(self, "Save Snapshot", "", "CSV Files (*.csv)")
        if filename:
            try:
                # Gather data
                header = ['timestamp_idx', 'ax', 'ay', 'az', 'gx', 'gy', 'gz']
                
                # Get max length available
                if not self.serial_handler.buffers['ax']:
                    return
                
                length = len(self.serial_handler.buffers['ax'])
                
                with open(filename, 'w', newline='') as f:
                    writer = csv.writer(f)
                    writer.writerow(header)
                    
                    for i in range(length):
                        row = [i]
                        for key in ['ax','ay','az','gx','gy','gz']:
                            # Handle case where buffers might briefly be unequal length
                            if i < len(self.serial_handler.buffers[key]):
                                row.append(self.serial_handler.buffers[key][i])
                            else:
                                row.append(0)
                        writer.writerow(row)
                
                QMessageBox.information(self, "Export Successful", f"Data saved to {filename}")
                
            except Exception as e:
                QMessageBox.critical(self, "Export Error", str(e))

    def update_loop(self):
        # 1. Read Data
        self.serial_handler.read_data(self.logger)
        
        # 2. Update Plot
        # Determine shared x-axis length
        min_len = 0
        if self.serial_handler.buffers['ax']:
            min_len = len(self.serial_handler.buffers['ax'])
        
        if min_len < 2: return
        
        x = list(range(min_len))
        
        # Update line data
        for name, line in self.lines.items():
            if name in self.serial_handler.buffers:
                data = list(self.serial_handler.buffers[name])
                # Ensure x and y match length
                start_idx = len(data) - min_len
                y = data[start_idx:]
                line.set_data(x, y)
        
        # Rescale
        if self.mode == 'dual':
            self.ax1.set_xlim(0, max(10, min_len))
            self.ax1.relim()
            self.ax1.autoscale_view()
            
            self.ax2.relim()
            self.ax2.autoscale_view()
            
        else:
            self.ax1.set_xlim(0, max(10, min_len))
            self.ax1.relim()
            self.ax1.autoscale_view()
            self.ax2.relim()
            self.ax2.autoscale_view()
            
        self.canvas.draw_idle()

    def closeEvent(self, event):
        self.serial_handler.close()
        if self.logger:
            self.logger.close()
        event.accept()
