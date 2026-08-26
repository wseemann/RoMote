package wseemann.media.romote.event

sealed interface ManualConnectionScreenUiEvent {
    data class ConnectClickedEvent(val host: String) : ManualConnectionScreenUiEvent
}