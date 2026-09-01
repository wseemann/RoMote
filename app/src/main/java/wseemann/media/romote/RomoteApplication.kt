package wseemann.media.romote

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import wseemann.media.romote.device.DeviceManager
import javax.inject.Inject

@HiltAndroidApp
class RomoteApplication : Application() {

    @Inject
    lateinit var deviceManager: DeviceManager

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
    }
}
