package smartracket.com.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import smartracket.com.db.SmartRacketDatabase
import smartracket.com.repository.BluetoothRepository
import smartracket.com.repository.HealthRepository
import smartracket.com.repository.HighlightRepository
import smartracket.com.repository.TrainingRepository
import smartracket.com.utils.BluetoothManager
import smartracket.com.utils.StrokeClassifier
import javax.inject.Singleton

/**
 * Hilt module providing application-level dependencies.
 *
 * All dependencies provided here are singletons and survive
 * across the entire application lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the Room database instance.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartRacketDatabase {
        return SmartRacketDatabase.getInstance(context)
    }

    /**
     * Provides the BluetoothManager instance.
     */
    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothManager {
        return BluetoothManager(context)
    }

    /**
     * Provides the StrokeClassifier instance (TensorFlow Lite).
     */
    @Provides
    @Singleton
    fun provideStrokeClassifier(@ApplicationContext context: Context): StrokeClassifier {
        return StrokeClassifier(context)
    }

    /**
     * Provides the BluetoothRepository instance.
     */
    @Provides
    @Singleton
    fun provideBluetoothRepository(bluetoothManager: BluetoothManager): BluetoothRepository {
        return BluetoothRepository(bluetoothManager)
    }

    /**
     * Provides the TrainingRepository instance.
     */
    @Provides
    @Singleton
    fun provideTrainingRepository(
        database: SmartRacketDatabase,
        strokeClassifier: StrokeClassifier
    ): TrainingRepository {
        return TrainingRepository(database, strokeClassifier)
    }

    /**
     * Provides the HighlightRepository instance.
     */
    @Provides
    @Singleton
    fun provideHighlightRepository(database: SmartRacketDatabase): HighlightRepository {
        return HighlightRepository(database)
    }

    /**
     * Provides the HealthRepository instance.
     */
    @Provides
    @Singleton
    fun provideHealthRepository(@ApplicationContext context: Context): HealthRepository {
        return HealthRepository(context)
    }
}

