package wseemann.media.romote.event

sealed interface MainScreenUiEvent {
    /**
     * Reloads the paired devices and scans for available ones. A single scan serves both the
     * available device list and the refresh of the connected device's stored details, which used
     * to be two separate events running two concurrent SSDP scans against each other.
     */
    data object RefreshEvent : MainScreenUiEvent

    data object LoadPairedDevicesEvent : MainScreenUiEvent

    data class ForgetDeviceEvent(val serialNumber: String) : MainScreenUiEvent

    data class RenameDeviceClickedEvent(
        val serialNumber: String,
        val currentName: String
    ) : MainScreenUiEvent

    data class RenameDeviceConfirmedEvent(val name: String) : MainScreenUiEvent

    data object RenameDeviceDismissedEvent : MainScreenUiEvent
}
