package wseemann.media.romote.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import wseemann.media.romote.event.ConnectivityUiEvent
import wseemann.media.romote.model.ConnectivityUiState
import wseemann.media.romote.network.LocalNetworkMonitor
import javax.inject.Inject

/**
 * Holds the "you are not on a local network" dialog for every screen that shows it. Keeping the
 * state here means the dialog survives configuration changes and the activities hold no network
 * code at all.
 */
@HiltViewModel
class ConnectivityViewModel @Inject constructor(localNetworkMonitor: LocalNetworkMonitor) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectivityUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            localNetworkMonitor.isLocalNetworkAvailable.collect { isAvailable ->
                _uiState.update { state ->
                    if (isAvailable) {
                        // The network coming back re-arms the dialog: a dismissal answers for the
                        // outage the user dismissed it during, not for the rest of the session.
                        state.copy(
                            isLocalNetworkAvailable = true,
                            isDialogVisible = false,
                            isDismissed = false
                        )
                    } else {
                        state.copy(
                            isLocalNetworkAvailable = false,
                            isDialogVisible = !state.isDismissed
                        )
                    }
                }
            }
        }
    }

    fun onHandleEvent(event: ConnectivityUiEvent) {
        when (event) {
            is ConnectivityUiEvent.DismissedEvent -> onDismissed()
        }
    }

    private fun onDismissed() {
        _uiState.update { it.copy(isDialogVisible = false, isDismissed = true) }
    }
}
