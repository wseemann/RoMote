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
    var commandHelper: CommandHelper? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            val url = commandHelper!!.getDeviceURL()
            val keypressKeyValues = intent.getSerializableExtra("keypress") as KeyPressKeyValues?

            if (keypressKeyValues == null) {
                return
            }

            try {
                val keypressRequest = KeyPressRequest(url, keypressKeyValues.getValue())
                keypressRequest.sendAsync(object : ResponseCallback<Void?> {
                    override fun onSuccess(unused: Void?) {
                        Timber.d("------------->")
                    }

                    override fun onError(e: Exception) {
                    }
                })
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to execute command")
            }
        }
    }
}
