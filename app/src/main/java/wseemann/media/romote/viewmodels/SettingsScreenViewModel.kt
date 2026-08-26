package wseemann.media.romote.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wseemann.ecp.api.ResponseCallback
import com.wseemann.ecp.core.KeyPressKeyValues
import com.wseemann.ecp.request.KeyPressRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.event.SettingsScreenUiEvent
import wseemann.media.romote.model.SettingsScreenUiState
import wseemann.media.romote.utils.CommandHelper
import wseemann.media.romote.utils.PreferenceUtils
import javax.inject.Inject
import androidx.core.content.edit

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val commandHelper: CommandHelper,
    private val preferenceUtils: PreferenceUtils,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsScreenUiState(
            shakeToPauseEnabled = sharedPreferences.getBoolean(SHAKE_TO_PAUSE_KEY, false),
            notificationWidgetEnabled = sharedPreferences.getBoolean(NOTIFICATION_WIDGET_KEY, false),
            hapticFeedbackEnabled = sharedPreferences.getBoolean(HAPTIC_FEEDBACK_KEY, false)
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val supported = deviceSupportsFindRemote()
            _uiState.update { it.copy(findRemoteSupported = supported) }
        }
    }

    fun onHandleEvent(event: SettingsScreenUiEvent) {
        when (event) {
            is SettingsScreenUiEvent.FindRemoteClickedEvent -> onFindRemoteClicked()
            is SettingsScreenUiEvent.ShakeToPauseToggledEvent -> {
                _uiState.update { it.copy(shakeToPauseEnabled = event.enabled) }
                persist(SHAKE_TO_PAUSE_KEY, event.enabled)
            }
            is SettingsScreenUiEvent.NotificationWidgetToggledEvent -> {
                _uiState.update { it.copy(notificationWidgetEnabled = event.enabled) }
                persist(NOTIFICATION_WIDGET_KEY, event.enabled)
            }
            is SettingsScreenUiEvent.HapticFeedbackToggledEvent -> {
                _uiState.update { it.copy(hapticFeedbackEnabled = event.enabled) }
                persist(HAPTIC_FEEDBACK_KEY, event.enabled)
            }
        }
    }

    /**
     * CheckBoxPreference used to do this write itself. NotificationService listens for changes to
     * the notification key on this same SharedPreferences, so committing here is what keeps the
     * notification widget appearing and disappearing as the switch is flipped.
     */
    private fun persist(key: String, enabled: Boolean) {
        sharedPreferences.edit {putBoolean(key, enabled)}
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

    private companion object {
        /**
         * The keys the settings screen owns. They are read outside this screen - ShakeActivity,
         * NotificationService and PreferenceUtils all pull them straight out of the same default
         * SharedPreferences - so the writes here have to keep landing under these exact names.
         */
        private const val SHAKE_TO_PAUSE_KEY = "shake_to_pause_checkbox_preference"
        private const val NOTIFICATION_WIDGET_KEY = "notification_checkbox_preference"
        private const val HAPTIC_FEEDBACK_KEY = "haptic_feedback_preference"

    }
}
