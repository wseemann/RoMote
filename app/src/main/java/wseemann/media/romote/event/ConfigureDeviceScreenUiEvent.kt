package wseemann.media.romote.event

sealed interface ConfigureDeviceScreenUiEvent {

    data object LoadAvailableDevicesEvent : ConfigureDeviceScreenUiEvent
}