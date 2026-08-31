package wseemann.media.romote.event

sealed interface ConnectivityUiEvent {
    data object DismissedEvent : ConnectivityUiEvent
}
