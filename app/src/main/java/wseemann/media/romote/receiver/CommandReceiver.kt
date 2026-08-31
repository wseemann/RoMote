package wseemann.media.romote.receiver

import android.content.BroadcastReceiver
import android.content.Context
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
class CommandReceiver : BroadcastReceiver() {

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var deviceManager: DeviceManager

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val keypressKeyValues = intent.getSerializableExtra("keypress") as? KeyPressKeyValues
        if (keypressKeyValues == null) {
            Timber.w("Received intent without a valid 'keypress' extra, ignoring")
            return
        }

        CoroutineScope(ioDispatcher).launch {
            deviceManager.getConnectedDevice()?.performKeyPress(keypressKeyValues)
        }
    }
}
