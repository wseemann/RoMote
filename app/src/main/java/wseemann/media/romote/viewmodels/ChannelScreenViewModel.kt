package wseemann.media.romote.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wseemann.ecp.api.QueryRequests
import com.wseemann.ecp.api.ResponseCallback
import com.wseemann.ecp.request.LaunchAppRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.event.ChannelScreenUiEvent
import wseemann.media.romote.data.ChannelItem
import wseemann.media.romote.di.IoDispatcher
import wseemann.media.romote.model.ChannelScreenUiState
import wseemann.media.romote.utils.CommandHelper
import javax.inject.Inject

@HiltViewModel
class ChannelScreenViewModel @Inject constructor(
    private val commandHelper: CommandHelper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
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
            is ChannelScreenUiEvent.ChannelClickedEvent -> onChannelClicked(event.channelId)
        }
    }

    private fun onLoadChannels() {
        viewModelScope.launch(ioDispatcher) {
            val deviceUrl = commandHelper.getDeviceURL()

            // Nothing is paired, so there is no request worth making: queryAppsRequest("") only
            // throws its way into the catch below after a round trip that was never going to
            // reach anything. loadedDeviceUrl is cleared for the same reason the catch clears it -
            // nothing was loaded, so the next broadcast should retry rather than trust the state.
            if (deviceUrl.isEmpty()) {
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
                val channels = QueryRequests.queryAppsRequest(deviceUrl)
                    .map { channel ->
                        ChannelItem(
                            id = channel.id.orEmpty(),
                            title = channel.title.orEmpty(),
                            iconUrl = commandHelper.getIconURL(channel.id)
                        )
                    }
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
     * Reloads the grid only when it is showing another device's channels, or nothing at all.
     *
     * UPDATE_DEVICE_BROADCAST means "something about the device changed", and most of what sends
     * it - launching a channel, pressing a remote key, renaming a device - leaves the installed
     * app list exactly as it was. Reloading on all of them cost a request per channel tap and,
     * because the load drives the pull-to-refresh indicator, made the indicator appear on taps.
     */
    private fun onDeviceChanged() {
        viewModelScope.launch(ioDispatcher) {
            // getDeviceURL() reads SQLite behind PreferenceUtils, hence the IO dispatcher.
            if (commandHelper.getDeviceURL() != loadedDeviceUrl ||
                _uiState.value.channels.isEmpty()
            ) {
                onLoadChannels()
            }
        }
    }

    private fun onChannelClicked(channelId: String) {
        val request = LaunchAppRequest(commandHelper.getDeviceURL(), channelId)
        request.sendAsync(object : ResponseCallback<Void> {
            override fun onSuccess(data: Void?) = Unit

            override fun onError(ex: Exception) {
                Timber.e(ex)
            }
        })
    }
}
