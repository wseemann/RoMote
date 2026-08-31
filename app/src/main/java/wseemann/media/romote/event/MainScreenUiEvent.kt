package wseemann.media.romote.event

import wseemann.media.romote.data.Device

sealed interface MainScreenUiEvent {
    /**
     * Reloads the paired devices and scans for available ones. A single scan serves both the
     * available device list and the refresh of the connected device's stored details, which used
     * to be two separate events running two concurrent SSDP scans against each other.
     */
    data object RefreshEvent : MainScreenUiEvent

    /** A row was tapped: the device becomes the paired, connected one. */
    data class DeviceSelectedEvent(val device: Device) : MainScreenUiEvent

    data class ForgetDeviceEvent(val serialNumber: String) : MainScreenUiEvent

    /**
     * Handled by DevicesTab, which starts DeviceInfoActivity. It carries the host as well as the
     * serial number because that is what the activity is started with.
     */
    data class DeviceInfoClickedEvent(
        val serialNumber: String,
        val host: String
    ) : MainScreenUiEvent

    /** The floating action button. Handled by DevicesTab, which starts ManualConnectionActivity. */
    data object AddDeviceClickedEvent : MainScreenUiEvent

    data class RenameDeviceClickedEvent(
        val serialNumber: String,
        val currentName: String
    ) : MainScreenUiEvent

    data class RenameDeviceConfirmedEvent(val name: String) : MainScreenUiEvent

    data object RenameDeviceDismissedEvent : MainScreenUiEvent
}
