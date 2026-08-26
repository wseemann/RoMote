package wseemann.media.romote.event

sealed interface DeviceInfoScreenUiEvent {

    data class LoadDeviceInfoEvent(
        val serialNumber: String?,
        val host: String?
    ) : DeviceInfoScreenUiEvent
}
