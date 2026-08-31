package wseemann.media.romote.event

sealed interface StoreScreenUiEvent {
    data object PageStartedEvent : StoreScreenUiEvent

    data object PageFinishedEvent : StoreScreenUiEvent

    data class HistoryChangedEvent(val canGoBack: Boolean) : StoreScreenUiEvent
}
