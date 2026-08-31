package wseemann.media.romote.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wseemann.ecp.api.ResponseCallback
import com.wseemann.ecp.core.KeyPressKeyValues
import com.wseemann.ecp.request.KeyPressRequest
import com.wseemann.ecp.request.QueryDeviceInfoRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.R
import wseemann.media.romote.event.RemoteScreenUiEvent
import wseemann.media.romote.model.RemoteScreenUiState
import wseemann.media.romote.model.RemoteScreenUiState.PrivateListening
import wseemann.media.romote.inappreview.AppReviewManager
import wseemann.media.romote.keyboard.KeyboardRelay
import wseemann.media.romote.tasks.ResponseCallbackWrapper
import wseemann.media.romote.utils.CommandHelper
import wseemann.media.romote.utils.PreferenceUtils
import wseemann.media.romote.utils.WakeOnLan
import javax.inject.Inject

@HiltViewModel
class RemoteScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val commandHelper: CommandHelper,
    private val preferenceUtils: PreferenceUtils,
    private val appReviewManager: AppReviewManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteScreenUiState())
    val uiState = _uiState.asStateFlow()

    /** Whether the connected device claims to support private listening at all. */
    private var deviceSupportsPrivateListening = false

    /** Whether the bound service last reported that audio is playing through the phone. */
    private var privateListeningActive = false

    private val keyboardRelay = KeyboardRelay(viewModelScope) { key ->
        KeyPressRequest(commandHelper.getDeviceURL(), key).send()
    }

    init {
        viewModelScope.launch {
            keyboardRelay.state.collect { keyboard ->
                _uiState.update {
                    it.copy(keyboardActive = keyboard.isActive, typedText = keyboard.text)
                }
            }
        }
    }

    fun onHandleEvent(event: RemoteScreenUiEvent) {
        when (event) {
            is RemoteScreenUiEvent.KeyPressedEvent -> onKeyPressed(event.key)
            is RemoteScreenUiEvent.PowerClickedEvent -> onPowerClicked()
            is RemoteScreenUiEvent.PowerOffConfirmedEvent -> onPowerOffConfirmed()
            is RemoteScreenUiEvent.PowerOffDismissedEvent -> onPowerOffDismissed()
            is RemoteScreenUiEvent.PrivateListeningClickedEvent -> onPrivateListeningClicked()
            is RemoteScreenUiEvent.PrivateListeningChangedEvent ->
                onPrivateListeningChanged(event.isActive)
            is RemoteScreenUiEvent.InstallPrivateListeningConfirmedEvent,
            is RemoteScreenUiEvent.InstallPrivateListeningDismissedEvent ->
                onInstallPrivateListeningClosed()
            is RemoteScreenUiEvent.DeviceChangedEvent -> onDeviceChanged()
            is RemoteScreenUiEvent.MessageShownEvent -> onMessageShown()
            is RemoteScreenUiEvent.KeyboardEvent -> onKeyboardEvent(event)
        }
    }

    private fun onKeyboardEvent(event: RemoteScreenUiEvent.KeyboardEvent) {
        when (event) {
            is RemoteScreenUiEvent.KeyboardEvent.ClickedEvent -> keyboardRelay.toggle()
            is RemoteScreenUiEvent.KeyboardEvent.TextChangedEvent ->
                keyboardRelay.onTextChanged(event.text)
            is RemoteScreenUiEvent.KeyboardEvent.BackspaceEvent -> keyboardRelay.backspace()
            is RemoteScreenUiEvent.KeyboardEvent.DoneEvent -> keyboardRelay.done()
            is RemoteScreenUiEvent.KeyboardEvent.DismissedEvent -> keyboardRelay.dismiss()
        }
    }

    /** Whether the private listening companion app is installed, for the tab's bind decision. */
    fun isPrivateListeningInstalled(): Boolean {
        val intent = Intent().apply { component = REMOTE_AUDIO_COMPONENT }

        return context.packageManager
            .queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()
    }

    /**
     * The address the private listening service should stream from. Read here rather than in the
     * remote tab so that PreferenceUtils - and the SQLite read behind it - stays out of the UI.
     */
    fun connectedDeviceHost(): String? = try {
        preferenceUtils.connectedDevice.host
    } catch (ex: Exception) {
        Timber.tag(TAG).e(ex, "Error reading the connected device")
        null
    }

    /**
     * Re-reads the connected device. This goes to the IO dispatcher because
     * [PreferenceUtils.connectedDevice] reads SQLite - RemoteFragment used to do it on the main
     * thread on every broadcast.
     */
    private fun onDeviceChanged() {
        viewModelScope.launch(Dispatchers.IO) {
            var deviceName = ""
            var showVolumeControls = true

            try {
                val device = preferenceUtils.connectedDevice

                deviceName = device.getCustomUserDeviceName()
                    ?.takeIf { it.isNotEmpty() }
                    ?: device.userDeviceName.orEmpty()

                showVolumeControls =
                    (device.supportsAudioGuide ?: device.tv)?.toBoolean() ?: showVolumeControls

                deviceSupportsPrivateListening = device.supportsPrivateListening.toBoolean()
            } catch (ex: Exception) {
                Timber.tag(TAG).e(ex, "Error reading the newly connected device")
                deviceSupportsPrivateListening = false
            }

            _uiState.update {
                it.copy(
                    deviceName = deviceName,
                    showVolumeControls = showVolumeControls,
                    privateListening = resolvePrivateListening()
                )
            }
        }
    }

    private fun onKeyPressed(key: KeyPressKeyValues) {
        val url = commandHelper.getDeviceURL()

        try {
            KeyPressRequest(url, key.value).sendAsync(object : ResponseCallback<Void> {
                // A key the device accepted is the one unambiguous sign the app is doing its job,
                // which is what the in-app review prompt counts sessions by.
                override fun onSuccess(data: Void?) = appReviewManager.onDeviceCommandSucceeded()

                override fun onError(ex: Exception) {
                    Timber.tag(TAG).e(ex, "Failed to execute command")
                }
            })
        } catch (ex: Exception) {
            Timber.tag(TAG).e(ex, "Failed to execute command")
        }
    }

    /**
     * Asks the device whether it is on before deciding what the power button does. A device that
     * can't be reached is assumed to be fully powered off, which Wake-on-LAN can fix.
     */
    private fun onPowerClicked() {
        val request = QueryDeviceInfoRequest(commandHelper.getDeviceURL())

        request.sendAsync(ResponseCallbackWrapper(object :
            ResponseCallback<com.wseemann.ecp.model.Device> {
            override fun onSuccess(data: com.wseemann.ecp.model.Device?) {
                if (data == null) {
                    wakeDevice()
                    return
                }

                if (POWER_ON_MODE == data.powerMode) {
                    _uiState.update { it.copy(showPowerOffConfirmation = true) }
                } else {
                    onKeyPressed(KeyPressKeyValues.POWER_ON)
                }
            }

            override fun onError(ex: Exception) {
                Timber.tag(TAG).d(ex, "Power mode query failed, falling back to Wake-on-LAN")
                wakeDevice()
            }
        }))
    }

    private fun onPowerOffConfirmed() {
        _uiState.update { it.copy(showPowerOffConfirmation = false) }
        onKeyPressed(KeyPressKeyValues.POWER_OFF)
    }

    private fun onPowerOffDismissed() {
        _uiState.update { it.copy(showPowerOffConfirmation = false) }
    }

    private fun wakeDevice() {
        WakeOnLan.wakeAsync(context, preferenceUtils) { result ->
            val messageResId = when (result) {
                is WakeOnLan.WakeResult.Sent -> R.string.waking_device
                is WakeOnLan.WakeResult.NoMacAddress -> R.string.wake_no_mac
                is WakeOnLan.WakeResult.Failed -> {
                    Timber.tag(TAG).e(result.exception, "Failed to wake the device")
                    R.string.wake_failed
                }
            }

            _uiState.update { it.copy(messageResId = messageResId) }
        }
    }

    /**
     * Only reaches the ViewModel when the companion app isn't installed - the remote tab owns the
     * binding, so it handles the click itself in every other case.
     */
    private fun onPrivateListeningClicked() {
        _uiState.update { it.copy(showInstallPrivateListening = true) }
    }

    private fun onInstallPrivateListeningClosed() {
        _uiState.update { it.copy(showInstallPrivateListening = false) }
    }

    private fun onPrivateListeningChanged(isActive: Boolean) {
        privateListeningActive = isActive
        _uiState.update { it.copy(privateListening = resolvePrivateListening()) }
    }

    private fun resolvePrivateListening(): PrivateListening {
        if (!deviceSupportsPrivateListening || !isPrivateListeningInstalled()) {
            return PrivateListening.UNAVAILABLE
        }

        return if (privateListeningActive) PrivateListening.ACTIVE else PrivateListening.AVAILABLE
    }

    private fun onMessageShown() {
        _uiState.update { it.copy(messageResId = null) }
    }

    private companion object {
        const val TAG = "RemoteScreenViewModel"

        const val POWER_ON_MODE = "PowerOn"

        val REMOTE_AUDIO_COMPONENT = ComponentName(
            "wseemann.media.romote.audio",
            "wseemann.media.romote.audio.remoteaudio.RemoteAudio"
        )
    }
}
