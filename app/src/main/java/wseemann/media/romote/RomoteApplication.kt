package wseemann.media.romote

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import wseemann.media.romote.device.DeviceManager
import wseemann.media.romote.utils.PreferenceUtils
import javax.inject.Inject

@HiltAndroidApp
class RomoteApplication : Application() {

    @Inject
    lateinit var preferenceUtils: PreferenceUtils

    @Inject
    lateinit var deviceManager: DeviceManager

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
    }
}
