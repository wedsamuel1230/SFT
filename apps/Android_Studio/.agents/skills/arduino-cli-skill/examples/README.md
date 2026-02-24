# Examples — Arduino CLI Workflows

This folder contains example command sequences demonstrating common workflows.

1) Detect board and upload (Windows PowerShell)

```powershell
# 1. Detect COM ports
Get-PnpDevice -Class Ports | Where-Object { $_.FriendlyName -match 'COM' }

# 2. Compile
arduino-cli compile --fqbn arduino:avr:uno C:\path\to\sketch

# 3. Upload (replace COM3 with detected port)
arduino-cli upload -p COM3 --fqbn arduino:avr:uno C:\path\to\sketch
```

2) Linux: use stable serial by-id

```bash
# List devices
ls -l /dev/serial/by-id

# Compile
arduino-cli compile --fqbn arduino:avr:uno /home/user/sketch

# Upload (use /dev/serial/by-id/…)
arduino-cli upload -p /dev/serial/by-id/usb-... --fqbn arduino:avr:uno /home/user/sketch
```
