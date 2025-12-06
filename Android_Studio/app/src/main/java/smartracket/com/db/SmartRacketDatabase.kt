package smartracket.com.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import smartracket.com.model.DevicePairing
import smartracket.com.model.HighlightClip
import smartracket.com.model.Stroke
import smartracket.com.model.TrainingSession

/**
 * Main Room database for SmartRacket Coach.
 *
 * Contains all entities for training data persistence:
 * - TrainingSession: Complete training sessions
 * - Stroke: Individual stroke data with classification results
 * - HighlightClip: Saved highlight moments
 * - DevicePairing: Paired Bluetooth devices
 *
 * Database version should be incremented when schema changes.
 */
@Database(
    entities = [
        TrainingSession::class,
        Stroke::class,
        HighlightClip::class,
        DevicePairing::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SmartRacketDatabase : RoomDatabase() {

    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun strokeDao(): StrokeDao
    abstract fun highlightClipDao(): HighlightClipDao
    abstract fun devicePairingDao(): DevicePairingDao

    companion object {
        private const val DATABASE_NAME = "smartracket_database"

        @Volatile
        private var INSTANCE: SmartRacketDatabase? = null

        /**
         * Get singleton instance of the database.
         * Uses double-checked locking for thread safety.
         */
        fun getInstance(context: Context): SmartRacketDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): SmartRacketDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SmartRacketDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration() // For development; use proper migrations in production
                .build()
        }
    }
}

