package wseemann.media.romote.event

import wseemann.media.romote.data.Device

sealed interface MainScreenUiEvent {
    data object RefreshEvent : MainScreenUiEvent

    data object TabSelectedEvent : MainScreenUiEvent

    data class DeviceSelectedEvent(val device: Device) : MainScreenUiEvent

    data class ForgetDeviceEvent(val serialNumber: String) : MainScreenUiEvent

    data class DeviceInfoClickedEvent(val serialNumber: String, val host: String) : MainScreenUiEvent

    data object AddDeviceClickedEvent : MainScreenUiEvent

    data class RenameDeviceClickedEvent(val serialNumber: String, val currentName: String) : MainScreenUiEvent

    data class RenameDeviceConfirmedEvent(val name: String) : MainScreenUiEvent

    data object RenameDeviceDismissedEvent : MainScreenUiEvent
}
