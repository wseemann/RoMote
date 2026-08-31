package wseemann.media.romote.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wseemann.ecp.core.KeyPressKeyValues
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import wseemann.media.romote.device.DeviceManager
import wseemann.media.romote.di.IoDispatcher
import wseemann.media.romote.preferences.AppPreferences
import wseemann.media.romote.utils.ShakeMonitor
import wseemann.media.romote.utils.enableRomoteEdgeToEdge
import javax.inject.Inject

@AndroidEntryPoint
open class ShakeActivity : AppCompatActivity() {

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var deviceManager: DeviceManager

    private lateinit var shakeMonitor: ShakeMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableRomoteEdgeToEdge(this)

        shakeMonitor = ShakeMonitor(this)
        shakeMonitor.setOnShakeListener { onShake() }

        if (shakeEnabled()) {
            shakeMonitor.resume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (shakeEnabled()) {
            shakeMonitor.pause()
        }
    }

    private fun onShake() {
        lifecycleScope.launch(ioDispatcher) {
            deviceManager.getConnectedDevice()?.performKeyPress(KeyPressKeyValues.PLAY)
        }
    }

    private fun shakeEnabled(): Boolean = appPreferences.isShakeToPauseEnabled()
}
