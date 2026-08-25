package wseemann.media.romote.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import wseemann.media.romote.event.StoreScreenUiEvent
import wseemann.media.romote.model.StoreScreenUiState
import javax.inject.Inject

@HiltViewModel
class StoreScreenViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(StoreScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun onHandleEvent(event: StoreScreenUiEvent) {
        when (event) {
            is StoreScreenUiEvent.PageStartedEvent -> onPageStarted()
            is StoreScreenUiEvent.PageFinishedEvent -> onPageFinished()
            is StoreScreenUiEvent.HistoryChangedEvent -> onHistoryChanged(event.canGoBack)
        }
    }

    private fun onPageStarted() {
        _uiState.update { it.copy(isLoading = true) }
    }

    private fun onPageFinished() {
        _uiState.update { it.copy(isLoading = false) }
    }

    private fun onHistoryChanged(canGoBack: Boolean) {
        _uiState.update { it.copy(canGoBack = canGoBack) }
    }
}
