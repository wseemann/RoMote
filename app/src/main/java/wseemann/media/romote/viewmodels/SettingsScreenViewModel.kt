package wseemann.media.romote.viewmodels

import androidx.lifecycle.ViewModel
import com.wseemann.ecp.api.ResponseCallback
import com.wseemann.ecp.core.KeyPressKeyValues
import com.wseemann.ecp.request.KeyPressRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import wseemann.media.romote.event.SettingsScreenUiEvent
import wseemann.media.romote.utils.CommandHelper
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val commandHelper: CommandHelper
) : ViewModel() {

    fun onHandleEvent(event: SettingsScreenUiEvent) {
        when (event) {
            is SettingsScreenUiEvent.FindRemoteClickedEvent -> onFindRemoteClicked()
        }
    }

    private fun onFindRemoteClicked() {
        val url = commandHelper.getDeviceURL()

        try {
            val keypressRequest = KeyPressRequest(url, KeyPressKeyValues.FIND_REMOTE.value)
            keypressRequest.sendAsync(object : ResponseCallback<Void> {
                override fun onSuccess(data: Void?) = Unit

                override fun onError(ex: Exception) = Unit
            })
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to execute command")
        }
    }
}