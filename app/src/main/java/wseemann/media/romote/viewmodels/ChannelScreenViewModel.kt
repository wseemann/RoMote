package wseemann.media.romote.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.data.ChannelItem
import wseemann.media.romote.device.DeviceManager
import wseemann.media.romote.di.IoDispatcher
import wseemann.media.romote.event.ChannelScreenUiEvent
import wseemann.media.romote.model.ChannelScreenUiState
import wseemann.media.romote.recents.RecentChannelsRepository
import javax.inject.Inject

@HiltViewModel
class ChannelScreenViewModel @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val deviceManager: DeviceManager,
    private val recentChannelsRepository: RecentChannelsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelScreenUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * The device the channels in [uiState] were fetched from, so a device change can be told apart
     * from the other things that broadcast one. Null whenever there is nothing loaded to trust.
     */
    private var loadedDeviceUrl: String? = null

    fun onHandleEvent(event: ChannelScreenUiEvent) {
        when (event) {
            is ChannelScreenUiEvent.LoadChannelsEvent -> onLoadChannels()
            is ChannelScreenUiEvent.DeviceChangedEvent -> onDeviceChanged()
            is ChannelScreenUiEvent.ChannelClickedEvent -> onChannelClicked(event.channel)
        }
    }

    private fun onLoadChannels() {
        viewModelScope.launch(ioDispatcher) {
            val deviceUrl = deviceManager.getConnectedDevice()?.getDeviceInfo()?.host

            // Nothing is paired, so there is no request worth making: queryAppsRequest("") only
            // throws its way into the catch below after a round trip that was never going to reach
            // anything. loadedDeviceUrl is cleared so the next broadcast retries rather than
            // trusting the state.
            if (deviceManager.getConnectedDevice() == null) {
                loadedDeviceUrl = null
                _uiState.update {
                    it.copy(
                        channels = persistentListOf(),
                        isLoading = false,
                        isDeviceConnected = false
                    )
                }

                return@launch
            }

            _uiState.update { it.copy(isLoading = true, isDeviceConnected = true) }

            try {
                val channels = deviceManager.getConnectedDevice()?.performQueryApps() ?: emptyList()
                loadedDeviceUrl = deviceUrl
                _uiState.update {
                    it.copy(channels = channels.toPersistentList(), isLoading = false)
                }
            } catch (ex: Exception) {
                Timber.e(ex)
                // Nothing was loaded, so the next broadcast should retry rather than treat the
                // empty grid as what this device has on it.
                loadedDeviceUrl = null
                _uiState.update {
                    it.copy(channels = persistentListOf(), isLoading = false)
                }
            }
        }
    }

    /**
     * UPDATE_DEVICE_BROADCAST means "something about the device changed", and most of what sends
     * it - launching a channel, pressing a remote key, renaming a device - leaves the installed
     * app list exactly as it was. Reloading on all of them cost a request per channel tap and,
     * because the load drives the pull-to-refresh indicator, made the indicator appear on taps.
     */
    private fun onDeviceChanged() {
        viewModelScope.launch(ioDispatcher) {
            if (deviceManager.getConnectedDevice()?.getDeviceInfo()?.host != loadedDeviceUrl ||
                _uiState.value.channels.isEmpty()
            ) {
                onLoadChannels()
            }
        }
    }

    private fun onChannelClicked(channel: ChannelItem) {
        viewModelScope.launch(ioDispatcher) {
            val device = deviceManager.getConnectedDevice() ?: return@launch

            device.performLaunchApp(channel.id)

            device.getDeviceInfo().serialNumber?.let { serialNumber ->
                recentChannelsRepository.recordLaunch(serialNumber, channel.id, channel.title)
            }
        }
    }
}
