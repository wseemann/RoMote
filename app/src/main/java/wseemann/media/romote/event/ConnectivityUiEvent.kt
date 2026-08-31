package wseemann.media.romote.event

sealed interface ConnectivityUiEvent {

    /** The user closed the dialog, by its button, by back, or by tapping outside it. */
    data object DismissedEvent : ConnectivityUiEvent
}
