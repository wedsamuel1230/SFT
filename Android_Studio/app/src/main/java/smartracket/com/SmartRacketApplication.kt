package smartracket.com

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for SmartRacket Coach.
 *
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 * throughout the application.
 */
@HiltAndroidApp
class SmartRacketApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Application-level initialization can be done here
    }
}

