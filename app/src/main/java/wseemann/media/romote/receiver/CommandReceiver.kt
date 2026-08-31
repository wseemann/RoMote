package wseemann.media.romote.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wseemann.ecp.api.ResponseCallback
import com.wseemann.ecp.core.KeyPressKeyValues
import com.wseemann.ecp.request.KeyPressRequest
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import wseemann.media.romote.utils.CommandHelper
import javax.inject.Inject

@AndroidEntryPoint
class CommandReceiver : BroadcastReceiver() {

    @Inject
    lateinit var commandHelper: CommandHelper

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val keypressKeyValues = intent.getSerializableExtra("keypress") as? KeyPressKeyValues
        if (keypressKeyValues == null) {
            Timber.w("Received intent without a valid 'keypress' extra; ignoring")
            return
        }

        val url = commandHelper.deviceURL
        val keyValue = keypressKeyValues.value

        try {
            Timber.d("Sending keypress '%s' to %s", keyValue, url)
            KeyPressRequest(url, keyValue).sendAsync(object : ResponseCallback<Void> {
                override fun onSuccess(data: Void?) {
                    Timber.d("Keypress '%s' sent successfully to %s", keyValue, url)
                }

                override fun onError(ex: Exception) {
                    Timber.e(ex, "Keypress '%s' to %s failed", keyValue, url)
                }
            })
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to dispatch keypress '%s' to %s", keyValue, url)
        }
    }
}
