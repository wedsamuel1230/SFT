# SmartRacket Overload Test (局部負荷過大) Documentation

## Overview

The `test_overload.ino` file provides comprehensive testing of overload (過負荷) and extreme condition scenarios for the SmartRacket firmware. It tests how the device handles:

- **Accelerometer peaks exceeding 5G** (normal: ~2.2G)
- **Gyroscope peaks exceeding 500 DPS** (normal: ~250 DPS)
- **Rapid-fire motion detection spam** (minimal inter-event delay)
- **Maximum BLE notification throughput**
- **Memory allocation/deallocation stress**
- **Sustained overload across all conditions** (sequential phases)

## Test Modes

### Mode: Accel Overload (`a` / `A`)
**Duration:** 10 seconds (customizable)

Simulates extreme acceleration events by amplifying real IMU acceleration data by 2.5x. Tests:
- Peak detection at 5G+ levels
- Motion filtering under extreme accel conditions
- Notification payload with overload_accel stroke type

**Output:**
```
{
  "ts": 1234567890,
  "stroke": "overload_accel",
  "conf": 0.99,
  "peak": 49.05  // m/s^2 (5G = ~49 m/s^2)
}
```

### Mode: Gyro Overload (`g` / `G`)
**Duration:** 10 seconds (customizable)

Amplifies gyroscope data by 2.2x to simulate 500+ DPS rotations. Tests:
- Spin detection at extreme angular velocities
- Gyro thresholding under peak conditions
- JSON payload with overload_gyro stroke type

**Output:**
```
{
  "ts": 1234567890,
  "stroke": "overload_gyro",
  "conf": 0.99,
  "peak": 550.0  // DPS
}
```

### Mode: Rapid Fire (`r`)
**Duration:** 10 seconds

Sends motion notifications as fast as the motion cooldown allows (100ms minimum between events). Tests:
- Stability under rapid event generation
- BLE queue behavior with sustained high throughput
- Event de-duplication and filtering

**Output:** Rotating stroke types (forehand, backhand, volley, serve) with ~100ms spacing
```
[0ms] {forehand, conf=0.92, peak=18.5}
[100ms] {backhand, conf=0.88, peak=17.3}
[200ms] {volley, conf=0.99, peak=19.1}
...
```

### Mode: BLE Flood (`b`)
**Duration:** 10 seconds

Sends notifications at maximum possible rate (~1ms minimum between packets). Tests:
- BLE characteristic buffer handling
- Notification loss/failure modes
- Memory pressure under sustained flooding
- Free RAM tracking during stress

**Output:**
```
[FLOOD] 50 sent | 2 failed | RAM: 3256 bytes
[FLOOD] 100 sent | 5 failed | RAM: 3140 bytes
[FLOOD] 150 sent | 8 failed | RAM: 3080 bytes
```

### Mode: Memory Stress (`m`)
**Duration:** 10 seconds

Repeatedly allocates and deallocates 256-byte buffers in a tight loop. Tests:
- Heap fragmentation tolerance
- Memory allocator stability under churn
- Free memory tracking

**Output:**
```
[MEM] Free RAM: 3456 bytes
[MEM] Free RAM: 3450 bytes
[MEM] Free RAM: 3445 bytes
```

### Mode: Sustained Load (`s`)
**Duration:** 40 seconds (4 × 10-second phases)

Runs all overload tests sequentially:
1. **Phase 0 (0–10s):** Accel Overload
2. **Phase 1 (10–20s):** Gyro Overload  
3. **Phase 2 (20–30s):** Rapid Fire
4. **Phase 3 (30–40s):** BLE Flood

Tests cumulative stress and fatigue behavior. Useful for long-duration reliability testing.

### Mode: Normal (`c` / `C`)
Stops any active test and resumes normal operation.

## Serial Command Reference

| Command | Action |
|---------|--------|
| `a` or `A` | Start Accel Overload test |
| `g` or `G` | Start Gyro Overload test |
| `r` | Start Rapid Fire test |
| `b` | Start BLE Flood test |
| `m` | Start Memory Stress test |
| `s` | Start Sustained Load (4 phases) |
| `c` or `C` | Return to normal operation |
| `R` | Software reset |

## Expected Behavior

### Healthy Device
- **Accel/Gyro Overload:** Detects overload strokes at `conf=0.99` without crashes
- **Rapid Fire:** Sends 100+ events over 10s without notification loss
- **BLE Flood:** Sends 1000+ notifications with <1% failure rate, free RAM remains >2KB
- **Memory Stress:** Completes without heap corruption or hard fault

### Under Stress (Expected Degradation)
- **Notification loss:** Some packets may fail to queue under BLE Flood
- **Free memory drops:** Can drop 50–200 bytes during sustained load
- **Latency jitter:** Inter-event timing may vary under memory pressure
- **Cooldown drift:** Large clock drift under extreme CPU load (expected until rtos integration)

## How to Use

### Steps

1. **Upload `test_overload.ino` to your SmartRacket device:**
   ```bash
   # In Arduino IDE:
   # 1. Open test_overload.ino
   # 2. Select Board: Adafruit Feather nRF52840 Express (or equivalent)
   # 3. Select Port: [Your serial port]
   # 4. Click Upload
   ```

2. **Open Serial Monitor (115200 baud):**
   ```
   Tools > Serial Monitor
   ```
   You should see:
   ```
   === SmartRacket Overload Test (局部負荷過大) ===
   Commands: a/A=AccelOL | g/G=GyroOL | r=RapidFire | b=BLE_Flood | m=MemStress | s=Sustained | c=Clear | R=Reset
   
   BLE advertising...
   IMU ready
   Free RAM at startup: 3548 bytes
   ```

3. **Trigger a test:**
   - Type `a` and press Enter to start Accel Overload test
   - Observe output for 10 seconds
   - Type `c` to return to normal

4. **Monitor Android app:**
   - Connect SmartRacket app to the device
   - Open BLE data log or debug view
   - Watch for overload stroke types and high `conf`/`peak` values

## Performance Targets

| Metric | Target | Acceptable |
|--------|--------|------------|
| Accel/Gyro OL detection rate | 100% correct | >95% |
| Rapid Fire success rate | >95% events sent | >85% |
| BLE Flood throughput | 1000+ notifications/10s | >500/10s |
| Flood failure rate | <1% | <5% |
| Memory leak (10min sustained) | No change | <100 bytes drift |
| Hard faults during stress | 0 | 0 |

## Customization

Edit these constants in `test_overload.ino`:

```cpp
const uint32_t testDuration = 10000;         // Test phase length (ms)
const float ACCEL_OVERLOAD_G = 5.0f;         // Accel overload threshold
const float GYRO_OVERLOAD_DPS = 500.0f;      // Gyro overload threshold
const uint32_t RAPID_FIRE_INTERVAL = 100;    // Min time between rapid-fire events (ms)
const size_t MEMORY_TEST_ALLOC_SIZE = 256;   // Allocation size for memory stress test
```

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| IMU init failed | I2C issue | Check LSM6DS3 wiring, pull-ups |
| BLE not advertising | Bluetooth chip issue | Power cycle device, check Bluefruit library |
| Rapid notification failures | Low MTU / queue full | Reduce throughput, check Android app connection quality |
| Memory warning (RAM <1KB) | Memory leak | Check recent changes to heap allocation, look for buffer overruns |
| Hard fault during stress | Stack overflow | Reduce recursion depth, increase SRAM if available |

## Integration with Android App

The SmartRacket Android app will interpret overload events as:
- **overload_accel** stroke: Very high acceleration, likely a powerful swing
- **overload_gyro** stroke: Very high rotation, likely a fast spin move

These can be used for:
- **Advanced coaching:** Detect when player is using excessive force
- **Fatigue analysis:** Track overload events over time to identify form degradation
- **Equipment validation:** Verify racket sensor sensitivity under known overload conditions

## Related Files

- [test.ino](test.ino) — Normal operation baseline
- [SmartRacket_firmware.uf2](SmartRacket_firmware.uf2) — Official firmware
- [BleDeviceProfile.kt](../app/src/main/java/smartracket/com/model/BleDeviceProfile.kt) — BLE UUID definitions
- [BluetoothManager.kt](../app/src/main/java/smartracket/com/service/BluetoothManager.kt) — Android BLE parsing

---
**Last Updated:** 2026-03-25  
**Test Version:** 1.0  
**Firmware Target:** nRF52840 with LSM6DS3 IMU
