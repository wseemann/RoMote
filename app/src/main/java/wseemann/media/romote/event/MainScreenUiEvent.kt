package wseemann.media.romote.event

sealed interface MainScreenUiEvent {
    data object LoadAvailableDevicesEvent : MainScreenUiEvent

    data object LoadPairedDevicesEvent : MainScreenUiEvent
}