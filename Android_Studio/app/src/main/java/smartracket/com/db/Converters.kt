package smartracket.com.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import smartracket.com.model.HeartRateReading
import smartracket.com.model.HighlightMetadata
import smartracket.com.model.MotionData

/**
 * Room type converters for complex data types.
 *
 * Handles serialization/deserialization of nested objects
 * to JSON strings for database storage.
 */
class Converters {
    private val gson = Gson()

    // ============= HeartRateReading List =============

    @TypeConverter
    fun fromHeartRateReadingList(value: List<HeartRateReading>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toHeartRateReadingList(value: String): List<HeartRateReading> {
        val type = object : TypeToken<List<HeartRateReading>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ============= MotionData =============

    @TypeConverter
    fun fromMotionData(value: MotionData): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMotionData(value: String): MotionData {
        return try {
            gson.fromJson(value, MotionData::class.java) ?: MotionData.empty()
        } catch (e: Exception) {
            MotionData.empty()
        }
    }

    // ============= HighlightMetadata =============

    @TypeConverter
    fun fromHighlightMetadata(value: HighlightMetadata): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toHighlightMetadata(value: String): HighlightMetadata {
        return try {
            gson.fromJson(value, HighlightMetadata::class.java)
        } catch (e: Exception) {
            HighlightMetadata(score = 0, strokeType = "unknown", confidence = 0f)
        }
    }

    // ============= String Map (for stroke distribution) =============

    @TypeConverter
    fun fromStringIntMap(value: Map<String, Int>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringIntMap(value: String): Map<String, Int> {
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ============= Float List =============

    @TypeConverter
    fun fromFloatList(value: List<Float>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toFloatList(value: String): List<Float> {
        val type = object : TypeToken<List<Float>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ============= Long List =============

    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        val type = object : TypeToken<List<Long>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

