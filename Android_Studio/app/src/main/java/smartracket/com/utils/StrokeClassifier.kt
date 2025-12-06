package smartracket.com.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import smartracket.com.model.MotionData
import smartracket.com.model.StrokeClassificationResult
import smartracket.com.model.StrokeType
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TensorFlow Lite classifier for stroke type recognition.
 *
 * Uses a pre-trained model to classify table tennis strokes based on
 * IMU sensor data (accelerometer + gyroscope readings).
 *
 * Model Input: [batch_size, sequence_length, features]
 *   - sequence_length: 50 (samples at ~20Hz = 2.5 seconds of data)
 *   - features: 6 (accelX, accelY, accelZ, gyroX, gyroY, gyroZ)
 *
 * Model Output: [batch_size, num_classes]
 *   - num_classes: 14 (different stroke types)
 *
 * Supports GPU acceleration on compatible devices.
 */
@Singleton
class StrokeClassifier @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "StrokeClassifier"
        private const val MODEL_FILE = "stroke_classifier.tflite"

        // Model input dimensions
        private const val SEQUENCE_LENGTH = 50
        private const val NUM_FEATURES = 6
        private const val BATCH_SIZE = 1

        // Number of output classes
        private const val NUM_CLASSES = 14

        // Confidence threshold for valid classification
        private const val MIN_CONFIDENCE = 0.3f

        // Score thresholds (based on motion quality metrics)
        private const val EXCELLENT_THRESHOLD = 0.9f
        private const val GOOD_THRESHOLD = 0.7f
        private const val AVERAGE_THRESHOLD = 0.5f
    }

    private var interpreter: Interpreter? = null
    private var isGpuEnabled = false

    // Normalization parameters (from training)
    private val meanValues = floatArrayOf(0f, 0f, 9.8f, 0f, 0f, 0f)  // Gravity on Z-axis
    private val stdValues = floatArrayOf(5f, 5f, 5f, 2f, 2f, 2f)

    // Stroke type labels in order of model output
    private val strokeLabels = arrayOf(
        StrokeType.FOREHAND_LOOP,
        StrokeType.FOREHAND_DRIVE,
        StrokeType.FOREHAND_FLICK,
        StrokeType.BACKHAND_LOOP,
        StrokeType.BACKHAND_DRIVE,
        StrokeType.BACKHAND_FLICK,
        StrokeType.FOREHAND_BLOCK,
        StrokeType.BACKHAND_BLOCK,
        StrokeType.FOREHAND_CHOP,
        StrokeType.BACKHAND_CHOP,
        StrokeType.FOREHAND_PUSH,
        StrokeType.BACKHAND_PUSH,
        StrokeType.SERVE,
        StrokeType.UNKNOWN
    )

    // Feedback messages for each stroke type and quality level
    private val feedbackMessages = mapOf(
        StrokeType.FOREHAND_LOOP to mapOf(
            "excellent" to "Perfect loop! Great topspin and follow-through.",
            "good" to "Nice loop! Try to brush the ball more for extra spin.",
            "average" to "Good attempt. Relax your wrist and accelerate through contact.",
            "poor" to "Focus on timing. Start the swing earlier and use your legs."
        ),
        StrokeType.FOREHAND_DRIVE to mapOf(
            "excellent" to "Excellent drive! Perfect timing and placement.",
            "good" to "Good drive! Keep your arm relaxed for more power.",
            "average" to "Solid contact. Work on transferring weight forward.",
            "poor" to "Watch the ball longer. Contact should be in front of your body."
        ),
        StrokeType.BACKHAND_LOOP to mapOf(
            "excellent" to "Outstanding backhand loop! Great spin generation.",
            "good" to "Nice technique! Use more wrist for added spin.",
            "average" to "Good form. Try to contact the ball at the top of the bounce.",
            "poor" to "Stay balanced. Rotate your shoulders into the shot."
        ),
        StrokeType.BACKHAND_BLOCK to mapOf(
            "excellent" to "Perfect block! Great angle control.",
            "good" to "Good block! Absorb the speed and redirect.",
            "average" to "Solid. Keep your paddle angle stable.",
            "poor" to "Simplify the motion. Less movement, more control."
        ),
        StrokeType.SERVE to mapOf(
            "excellent" to "Excellent serve! Great spin variation.",
            "good" to "Good serve! Try to hide your contact point.",
            "average" to "Decent serve. Work on ball toss consistency.",
            "poor" to "Slow down. Focus on clean contact first."
        )
    )

    /**
     * Initialize the TensorFlow Lite interpreter.
     * Uses CPU inference for compatibility.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val model = loadModelFile()

            val options = Interpreter.Options()
            options.setNumThreads(4)

            interpreter = Interpreter(model, options)
            Log.d(TAG, "Model loaded successfully (CPU mode)")

            // Warm up the model
            warmUp()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model", e)
            false
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Warm up the model with a dummy inference to reduce first-call latency.
     */
    private fun warmUp() {
        val dummyInput = createInputBuffer(MotionData.empty())
        val dummyOutput = Array(BATCH_SIZE) { FloatArray(NUM_CLASSES) }
        interpreter?.run(dummyInput, dummyOutput)
        Log.d(TAG, "Model warmed up")
    }

    /**
     * Classify a stroke based on motion data.
     *
     * @param motionData The IMU data collected during the stroke
     * @return Classification result with stroke type, confidence, score, and feedback
     */
    suspend fun classify(motionData: MotionData): StrokeClassificationResult = withContext(Dispatchers.Default) {
        val interpreter = interpreter ?: return@withContext createUnknownResult()

        try {
            val inputBuffer = createInputBuffer(motionData)
            val outputArray = Array(BATCH_SIZE) { FloatArray(NUM_CLASSES) }

            interpreter.run(inputBuffer, outputArray)

            val probabilities = outputArray[0]
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val confidence = probabilities[maxIndex]

            val strokeType = if (confidence >= MIN_CONFIDENCE) {
                strokeLabels.getOrElse(maxIndex) { StrokeType.UNKNOWN }
            } else {
                StrokeType.UNKNOWN
            }

            val score = calculateScore(motionData, confidence, strokeType)
            val feedback = generateFeedback(strokeType, score)

            val allProbabilities = strokeLabels.zip(probabilities.toList()).toMap()

            StrokeClassificationResult(
                strokeType = strokeType,
                confidence = confidence,
                score = score,
                feedback = feedback,
                allProbabilities = allProbabilities
            )
        } catch (e: Exception) {
            Log.e(TAG, "Classification failed", e)
            createUnknownResult()
        }
    }

    private fun createInputBuffer(motionData: MotionData): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(BATCH_SIZE * SEQUENCE_LENGTH * NUM_FEATURES * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        // Resample or pad motion data to SEQUENCE_LENGTH
        val resampled = resampleMotionData(motionData)

        for (i in 0 until SEQUENCE_LENGTH) {
            // Normalize and add each feature
            inputBuffer.putFloat(normalize(resampled.accelX.getOrElse(i) { 0f }, 0))
            inputBuffer.putFloat(normalize(resampled.accelY.getOrElse(i) { 0f }, 1))
            inputBuffer.putFloat(normalize(resampled.accelZ.getOrElse(i) { 9.8f }, 2))
            inputBuffer.putFloat(normalize(resampled.gyroX.getOrElse(i) { 0f }, 3))
            inputBuffer.putFloat(normalize(resampled.gyroY.getOrElse(i) { 0f }, 4))
            inputBuffer.putFloat(normalize(resampled.gyroZ.getOrElse(i) { 0f }, 5))
        }

        inputBuffer.rewind()
        return inputBuffer
    }

    private fun resampleMotionData(motionData: MotionData): MotionData {
        val originalLength = motionData.accelX.size
        if (originalLength == 0) return createEmptySequence()
        if (originalLength == SEQUENCE_LENGTH) return motionData

        // Linear interpolation for resampling
        fun resample(data: List<Float>): List<Float> {
            if (data.isEmpty()) return List(SEQUENCE_LENGTH) { 0f }
            if (data.size == SEQUENCE_LENGTH) return data

            return List(SEQUENCE_LENGTH) { i ->
                val srcIndex = i.toFloat() * (data.size - 1) / (SEQUENCE_LENGTH - 1)
                val lowerIndex = srcIndex.toInt().coerceIn(0, data.size - 1)
                val upperIndex = (lowerIndex + 1).coerceIn(0, data.size - 1)
                val fraction = srcIndex - lowerIndex

                data[lowerIndex] * (1 - fraction) + data[upperIndex] * fraction
            }
        }

        return MotionData(
            accelX = resample(motionData.accelX),
            accelY = resample(motionData.accelY),
            accelZ = resample(motionData.accelZ),
            gyroX = resample(motionData.gyroX),
            gyroY = resample(motionData.gyroY),
            gyroZ = resample(motionData.gyroZ)
        )
    }

    private fun createEmptySequence(): MotionData {
        return MotionData(
            accelX = List(SEQUENCE_LENGTH) { 0f },
            accelY = List(SEQUENCE_LENGTH) { 0f },
            accelZ = List(SEQUENCE_LENGTH) { 9.8f },
            gyroX = List(SEQUENCE_LENGTH) { 0f },
            gyroY = List(SEQUENCE_LENGTH) { 0f },
            gyroZ = List(SEQUENCE_LENGTH) { 0f }
        )
    }

    private fun normalize(value: Float, featureIndex: Int): Float {
        return (value - meanValues[featureIndex]) / stdValues[featureIndex]
    }

    /**
     * Calculate performance score based on motion quality metrics.
     */
    private fun calculateScore(motionData: MotionData, confidence: Float, strokeType: StrokeType): Int {
        if (strokeType == StrokeType.UNKNOWN) return 0

        // Calculate motion quality metrics
        val peakAccel = calculatePeakAcceleration(motionData)
        val smoothness = calculateSmoothness(motionData)
        val angularVelocity = calculatePeakAngularVelocity(motionData)

        // Normalize metrics (based on typical stroke characteristics)
        val accelScore = (peakAccel / 50f).coerceIn(0f, 1f)  // Max ~50 m/s²
        val smoothnessScore = smoothness  // Already 0-1
        val angularScore = (angularVelocity / 15f).coerceIn(0f, 1f)  // Max ~15 rad/s

        // Weighted combination with confidence
        val qualityScore = (
            accelScore * 0.3f +
            smoothnessScore * 0.2f +
            angularScore * 0.2f +
            confidence * 0.3f
        )

        // Convert to 1-10 scale
        return (qualityScore * 9 + 1).toInt().coerceIn(1, 10)
    }

    private fun calculatePeakAcceleration(motionData: MotionData): Float {
        if (motionData.accelX.isEmpty()) return 0f

        return motionData.accelX.indices.maxOfOrNull { i ->
            val ax = motionData.accelX.getOrElse(i) { 0f }
            val ay = motionData.accelY.getOrElse(i) { 0f }
            val az = motionData.accelZ.getOrElse(i) { 0f }
            kotlin.math.sqrt(ax * ax + ay * ay + az * az)
        } ?: 0f
    }

    private fun calculatePeakAngularVelocity(motionData: MotionData): Float {
        if (motionData.gyroX.isEmpty()) return 0f

        return motionData.gyroX.indices.maxOfOrNull { i ->
            val gx = motionData.gyroX.getOrElse(i) { 0f }
            val gy = motionData.gyroY.getOrElse(i) { 0f }
            val gz = motionData.gyroZ.getOrElse(i) { 0f }
            kotlin.math.sqrt(gx * gx + gy * gy + gz * gz)
        } ?: 0f
    }

    private fun calculateSmoothness(motionData: MotionData): Float {
        if (motionData.accelX.size < 3) return 0.5f

        // Calculate jerk (rate of change of acceleration)
        var totalJerk = 0f
        for (i in 1 until motionData.accelX.size - 1) {
            val jerkX = motionData.accelX[i + 1] - 2 * motionData.accelX[i] + motionData.accelX[i - 1]
            val jerkY = motionData.accelY[i + 1] - 2 * motionData.accelY[i] + motionData.accelY[i - 1]
            val jerkZ = motionData.accelZ[i + 1] - 2 * motionData.accelZ[i] + motionData.accelZ[i - 1]
            totalJerk += kotlin.math.sqrt(jerkX * jerkX + jerkY * jerkY + jerkZ * jerkZ)
        }

        val avgJerk = totalJerk / (motionData.accelX.size - 2)
        // Lower jerk = smoother motion = higher score
        return (1f - (avgJerk / 20f).coerceIn(0f, 1f))
    }

    /**
     * Generate feedback message based on stroke type and score.
     */
    private fun generateFeedback(strokeType: StrokeType, score: Int): String {
        val qualityLevel = when {
            score >= 9 -> "excellent"
            score >= 7 -> "good"
            score >= 5 -> "average"
            else -> "poor"
        }

        return feedbackMessages[strokeType]?.get(qualityLevel)
            ?: feedbackMessages[StrokeType.FOREHAND_DRIVE]?.get(qualityLevel)
            ?: "Keep practicing! Focus on consistent contact."
    }

    private fun createUnknownResult(): StrokeClassificationResult {
        return StrokeClassificationResult(
            strokeType = StrokeType.UNKNOWN,
            confidence = 0f,
            score = 0,
            feedback = "Stroke not detected. Make sure the paddle is connected."
        )
    }

    /**
     * Check if the classifier is ready for inference.
     */
    fun isReady(): Boolean = interpreter != null

    /**
     * Get whether GPU acceleration is being used.
     */
    fun isUsingGpu(): Boolean = isGpuEnabled

    /**
     * Clean up resources.
     */
    fun cleanup() {
        interpreter?.close()
        interpreter = null
    }
}

