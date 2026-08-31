package wseemann.media.romote.service

import android.app.IntentService
import android.content.Intent
import com.wseemann.ecp.core.KeyPressKeyValues
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.device.DeviceManager
import wseemann.media.romote.di.IoDispatcher
import javax.inject.Inject

@AndroidEntryPoint
class CommandService : IntentService(TAG) {

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var deviceManager: DeviceManager

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        Timber.tag(TAG).d("onHandleIntent called")

        if (intent != null) {
            performKeypress((intent.getSerializableExtra("keypress") as KeyPressKeyValues?)!!)
        }
    }

    private fun performKeypress(keypressKeyValue: KeyPressKeyValues) {
        CoroutineScope(ioDispatcher).launch {
            deviceManager.getConnectedDevice()?.performKeyPress(keypressKeyValue)
        }
    }

    private companion object {
        const val TAG: String = "CommandService"
    }
}
