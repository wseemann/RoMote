package wseemann.media.romote.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wseemann.ecp.api.ResponseCallback
import com.wseemann.ecp.core.KeyPressKeyValues
import com.wseemann.ecp.request.KeyPressRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.di.IoDispatcher
import wseemann.media.romote.event.SettingsScreenUiEvent
import wseemann.media.romote.model.SettingsScreenUiState
import wseemann.media.romote.utils.CommandHelper
import wseemann.media.romote.utils.PreferenceUtils
import javax.inject.Inject
import wseemann.media.romote.preferences.AppPreferences

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val commandHelper: CommandHelper,
    private val preferenceUtils: PreferenceUtils,
    private val appPreferences: AppPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsScreenUiState(
            shakeToPauseEnabled = appPreferences.isShakeToPauseEnabled(),
            notificationWidgetEnabled = appPreferences.isNotificationWidgetEnabled(),
            hapticFeedbackEnabled = appPreferences.isHapticFeedbackEnabled()
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            val supported = deviceSupportsFindRemote()
            _uiState.update { it.copy(findRemoteSupported = supported) }
        }
    }

    fun onHandleEvent(event: SettingsScreenUiEvent) {
        when (event) {
            is SettingsScreenUiEvent.FindRemoteClickedEvent -> onFindRemoteClicked()
            is SettingsScreenUiEvent.ShakeToPauseToggledEvent -> {
                _uiState.update { it.copy(shakeToPauseEnabled = event.enabled) }
                appPreferences.setShakeToPauseEnabled(event.enabled)
            }
            is SettingsScreenUiEvent.NotificationWidgetToggledEvent -> {
                _uiState.update { it.copy(notificationWidgetEnabled = event.enabled) }
                appPreferences.setNotificationWidgetEnabled(event.enabled)
            }
            is SettingsScreenUiEvent.HapticFeedbackToggledEvent -> {
                _uiState.update { it.copy(hapticFeedbackEnabled = event.enabled) }
                appPreferences.setHapticFeedbackEnabled(event.enabled)
            }
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

    private fun deviceSupportsFindRemote(): Boolean {
        try {
            // Throws when no device is paired.
            val device = preferenceUtils.connectedDevice

            device.supportsFindRemote?.let { return it.toBoolean() }
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to read the connected device")
        }

        return false
    }
}
