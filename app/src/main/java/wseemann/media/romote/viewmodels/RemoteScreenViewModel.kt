package wseemann.media.romote.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wseemann.ecp.core.KeyPressKeyValues
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.R
import wseemann.media.romote.data.ChannelItem
import wseemann.media.romote.device.DeviceManager
import wseemann.media.romote.di.IoDispatcher
import wseemann.media.romote.di.MainDispatcher
import wseemann.media.romote.event.RemoteScreenUiEvent
import wseemann.media.romote.inappreview.AppReviewManager
import wseemann.media.romote.keyboard.KeyboardRelay
import wseemann.media.romote.model.RemoteScreenUiState
import wseemann.media.romote.model.RemoteScreenUiState.PrivateListening
import wseemann.media.romote.recents.RecentChannels
import wseemann.media.romote.recents.RecentChannelsRepository
import wseemann.media.romote.utils.WakeOnLan
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RemoteScreenViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val deviceManager: DeviceManager,
    private val appReviewManager: AppReviewManager,
    private val recentChannelsRepository: RecentChannelsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteScreenUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * The connected device, as a flow, because the recents query is keyed by it. There is no
     * observable source for it - DeviceManager is a blocking prefs read plus a SQLite query,
     * announced by UPDATE_DEVICE_BROADCAST - so [onDeviceChanged] pushes into this instead.
     *
     * The host rides along because the icon urls are rebuilt from it.
     */
    private val connectedDevice = MutableStateFlow<ConnectedDevice?>(null)

    private var deviceSupportsPrivateListening = false

    /** Whether the bound service last reported that audio is playing through the phone. */
    private var privateListeningActive = false

    private val keyboardRelay = KeyboardRelay(viewModelScope, ioDispatcher) { key ->
        val device = deviceManager.getConnectedDevice()

        when (key) {
            is KeyboardRelay.Key.Named -> device?.performKeyPress(key.value)
            is KeyboardRelay.Key.Literal -> device?.performLiteralKeyPress(key.text)
        }
    }

    init {
        viewModelScope.launch {
            keyboardRelay.state.collect { keyboard ->
                _uiState.update {
                    it.copy(keyboardActive = keyboard.isActive, typedText = keyboard.text)
                }
            }
        }

        viewModelScope.launch {
            connectedDevice
                .flatMapLatest { connected ->
                    if (connected == null) {
                        flowOf<ImmutableList<ChannelItem>>(persistentListOf())
                    } else {
                        recentChannelsRepository.observeRecents(connected.serialNumber)
                            .map { recentChannels ->
                                RecentChannels.toChannelItems(recentChannels, connected.host)
                            }
                    }
                }
                .collect { recentChannels ->
                    _uiState.update { it.copy(recentChannels = recentChannels) }
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

            is RemoteScreenUiEvent.PrivateListeningChangedEvent -> onPrivateListeningChanged(event.isActive)

            is RemoteScreenUiEvent.InstallPrivateListeningConfirmedEvent,
            is RemoteScreenUiEvent.InstallPrivateListeningDismissedEvent -> onInstallPrivateListeningClosed()

            is RemoteScreenUiEvent.DeviceChangedEvent -> onDeviceChanged()

            is RemoteScreenUiEvent.MessageShownEvent -> onMessageShown()

            is RemoteScreenUiEvent.KeyboardEvent -> onKeyboardEvent(event)

            is RemoteScreenUiEvent.RecentChannelClickedEvent -> onRecentChannelClicked(event.channel)
        }
    }

    private fun onKeyboardEvent(event: RemoteScreenUiEvent.KeyboardEvent) {
        when (event) {
            is RemoteScreenUiEvent.KeyboardEvent.ClickedEvent -> keyboardRelay.toggle()
            is RemoteScreenUiEvent.KeyboardEvent.TextChangedEvent -> keyboardRelay.onTextChanged(event.text)
            is RemoteScreenUiEvent.KeyboardEvent.BackspaceEvent -> keyboardRelay.backspace()
            is RemoteScreenUiEvent.KeyboardEvent.DoneEvent -> keyboardRelay.done()
            is RemoteScreenUiEvent.KeyboardEvent.DismissedEvent -> keyboardRelay.dismiss()
        }
    }

    fun isPrivateListeningInstalled(): Boolean {
        val intent = Intent().apply { component = REMOTE_AUDIO_COMPONENT }

        return context.packageManager
            .queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()
    }

    /**
     * Read here rather than in the remote tab so that DeviceManager - and the SQLite read behind
     * it - stays out of the UI.
     */
    fun connectedDeviceHost(): String? = try {
        deviceManager.getConnectedDevice()?.getDeviceInfo()?.host
    } catch (ex: Exception) {
        Timber.tag(TAG).e(ex, "Error reading the connected device")
        null
    }

    private fun onDeviceChanged() {
        viewModelScope.launch(ioDispatcher) {
            val device = deviceManager.getConnectedDevice()
            val isDeviceConnected = device != null
            val deviceName = device?.getDeviceInfo()?.getCustomUserDeviceName()
                ?.takeIf { it.isNotEmpty() }
                ?: device?.getDeviceInfo()?.userDeviceName.orEmpty()

            val showVolumeControls =
                (device?.getDeviceInfo()?.supportsAudioGuide
                    ?: device?.getDeviceInfo()?.tv)?.toBoolean()
                    ?: false

            deviceSupportsPrivateListening = device?.getDeviceInfo()?.supportsPrivateListening.toBoolean()

            // Distinct by value, so the repeated broadcasts that a key press sends do not restart
            // the recents query for a device that has not actually changed.
            connectedDevice.value = device?.getDeviceInfo()?.let { deviceInfo ->
                val serialNumber = deviceInfo.serialNumber
                val host = deviceInfo.host

                if (serialNumber == null || host == null) {
                    null
                } else {
                    ConnectedDevice(serialNumber = serialNumber, host = host)
                }
            }

            _uiState.update {
                it.copy(
                    deviceName = deviceName,
                    isDeviceConnected = isDeviceConnected,
                    showVolumeControls = showVolumeControls,
                    privateListening = resolvePrivateListening()
                )
            }
        }
    }

    private fun onKeyPressed(keyPressKeyValue: KeyPressKeyValues) {
        viewModelScope.launch(ioDispatcher) {
            deviceManager.getConnectedDevice()?.performKeyPress(keyPressKeyValue)
            appReviewManager.onDeviceCommandSucceeded()
        }
    }

    /**
     * A device that can't be reached is assumed to be fully powered off, which Wake-on-LAN can fix.
     */
    private fun onPowerClicked() {
        viewModelScope.launch(ioDispatcher) {
            val deviceInfo = deviceManager.getConnectedDevice()?.queryDeviceInfo()

            if (deviceInfo == null) {
                wakeDevice()
                return@launch
            }

            if (POWER_ON_MODE == deviceInfo.powerMode) {
                _uiState.update { it.copy(showPowerOffConfirmation = true) }
            } else {
                onKeyPressed(KeyPressKeyValues.POWER_ON)
            }
        }
    }

    private fun onPowerOffConfirmed() {
        _uiState.update { it.copy(showPowerOffConfirmation = false) }
        onKeyPressed(KeyPressKeyValues.POWER_OFF)
    }

    private fun onPowerOffDismissed() {
        _uiState.update { it.copy(showPowerOffConfirmation = false) }
    }

    private fun wakeDevice() {
        WakeOnLan.wakeAsync(context, deviceManager, ioDispatcher, mainDispatcher) { result ->
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

    /**
     * Recorded again on the way out, which is what moves the channel back to the front of the row.
     */
    private fun onRecentChannelClicked(channel: ChannelItem) {
        viewModelScope.launch(ioDispatcher) {
            val device = deviceManager.getConnectedDevice() ?: return@launch

            device.performLaunchApp(channel.id)
            appReviewManager.onDeviceCommandSucceeded()

            device.getDeviceInfo().serialNumber?.let { serialNumber ->
                recentChannelsRepository.recordLaunch(serialNumber, channel.id, channel.title)
            }
        }
    }

    private fun onMessageShown() {
        _uiState.update { it.copy(messageResId = null) }
    }

    private data class ConnectedDevice(val serialNumber: String, val host: String)

    private companion object {
        const val TAG = "RemoteScreenViewModel"

        const val POWER_ON_MODE = "PowerOn"

        val REMOTE_AUDIO_COMPONENT = ComponentName(
            "wseemann.media.romote.audio",
            "wseemann.media.romote.audio.remoteaudio.RemoteAudio"
        )
    }
}
