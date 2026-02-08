package smartracket.com

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for SmartRacket Coach.
 *
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 * throughout the application.
 *
 * Implements [Configuration.Provider] to enable Hilt-injected WorkManager workers
 * (e.g., [smartracket.com.sync.SyncWorker]).
 */
@HiltAndroidApp
class SmartRacketApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Application-level initialization can be done here
    }
}

