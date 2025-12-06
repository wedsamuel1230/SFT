# TensorFlow Lite Model Placement

Place your trained stroke classification model here as:
`stroke_classifier.tflite`

## Model Specifications

### Input Shape
- Shape: `[1, 50, 6]`
- Batch size: 1
- Sequence length: 50 samples (~2.5 seconds at 20Hz)
- Features: 6 (accelX, accelY, accelZ, gyroX, gyroY, gyroZ)

### Output Shape
- Shape: `[1, 14]`
- 14 stroke type probabilities

### Stroke Types (in order)
1. FOREHAND_LOOP
2. FOREHAND_DRIVE
3. FOREHAND_FLICK
4. BACKHAND_LOOP
5. BACKHAND_DRIVE
6. BACKHAND_FLICK
7. FOREHAND_BLOCK
8. BACKHAND_BLOCK
9. FOREHAND_CHOP
10. BACKHAND_CHOP
11. FOREHAND_PUSH
12. BACKHAND_PUSH
13. SERVE
14. UNKNOWN

### Normalization
Input data is normalized using:
- Mean: [0, 0, 9.8, 0, 0, 0]
- Std: [5, 5, 5, 2, 2, 2]

Formula: `normalized = (value - mean) / std`

## Creating a Placeholder Model

If you don't have a trained model yet, you can create a placeholder:

```python
import tensorflow as tf
import numpy as np

# Create a simple model
model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(50, 6)),
    tf.keras.layers.LSTM(64, return_sequences=True),
    tf.keras.layers.LSTM(32),
    tf.keras.layers.Dense(14, activation='softmax')
])

model.compile(optimizer='adam', loss='categorical_crossentropy')

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save
with open('stroke_classifier.tflite', 'wb') as f:
    f.write(tflite_model)
```

## Notes
- The model runs locally for privacy and offline support
- GPU acceleration is used when available
- Model is warmed up on first load to reduce inference latency

