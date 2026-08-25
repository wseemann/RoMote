package wseemann.media.romote.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.wseemann.ecp.api.QueryRequests
import com.wseemann.ecp.model.Channel
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
import wseemann.media.romote.model.ChannelScreenUiState
import wseemann.media.romote.utils.CommandHelper
import javax.inject.Inject

@HiltViewModel
class ChannelScreenViewModel @Inject constructor(private val commandHelper: CommandHelper) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelScreenUiState())
    val uiState = _uiState.asStateFlow()
    val uiStateLiveData = uiState.asLiveData()

    fun onHandleEvent(event: ChannelScreenUiEvent) {
        when (event) {
            is ChannelScreenUiEvent.LoadChannelsEvent -> onLoadChannels()
        }
    }

    private fun onLoadChannels() {
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("--------------->")

            _uiState.update { it.copy(isLoading = true) }

            try {
                val channels = QueryRequests.queryAppsRequest(commandHelper.getDeviceURL())
                _uiState.update {
                    it.copy(channels = channels.toPersistentList(), isLoading = false)
                }
                Timber.d("---------------> done")
            } catch (ex: Exception) {
                ex.printStackTrace()
                _uiState.update {
                    it.copy(channels = persistentListOf(), isLoading = false)
                }
            }
        }
    }
}