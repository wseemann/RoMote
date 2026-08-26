package wseemann.media.romote.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wseemann.ecp.api.QueryRequests
import com.wseemann.ecp.api.ResponseCallback
import com.wseemann.ecp.request.LaunchAppRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.event.ChannelScreenUiEvent
import wseemann.media.romote.data.ChannelItem
import wseemann.media.romote.model.ChannelScreenUiState
import wseemann.media.romote.utils.CommandHelper
import javax.inject.Inject

@HiltViewModel
class ChannelScreenViewModel @Inject constructor(private val commandHelper: CommandHelper) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun onHandleEvent(event: ChannelScreenUiEvent) {
        when (event) {
            is ChannelScreenUiEvent.LoadChannelsEvent -> onLoadChannels()
            is ChannelScreenUiEvent.ChannelClickedEvent -> onChannelClicked(event.channelId)
        }
    }

    private fun onLoadChannels() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val channels = QueryRequests.queryAppsRequest(commandHelper.getDeviceURL())
                    .map { channel ->
                        ChannelItem(
                            id = channel.id.orEmpty(),
                            title = channel.title.orEmpty(),
                            iconUrl = commandHelper.getIconURL(channel.id)
                        )
                    }
                _uiState.update {
                    it.copy(channels = channels.toPersistentList(), isLoading = false)
                }
            } catch (ex: Exception) {
                Timber.e(ex)
                _uiState.update {
                    it.copy(channels = persistentListOf(), isLoading = false)
                }
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
