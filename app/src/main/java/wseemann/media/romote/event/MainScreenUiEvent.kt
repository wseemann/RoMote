package wseemann.media.romote.event

sealed interface MainScreenUiEvent {
    data object LoadAvailableDevicesEvent : MainScreenUiEvent

    data object LoadPairedDevicesEvent : MainScreenUiEvent

    data object UpdatePairedDeviceEvent : MainScreenUiEvent

    data class RenameDeviceClickedEvent(
        val serialNumber: String,
        val currentName: String
    ) : MainScreenUiEvent

    data class RenameDeviceConfirmedEvent(val name: String) : MainScreenUiEvent

    data object RenameDeviceDismissedEvent : MainScreenUiEvent
}
