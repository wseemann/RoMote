package wseemann.media.romote.event

sealed interface ManualConnectionScreenUiEvent {
    data class IpAddressChangedEvent(val ipAddress: String) : ManualConnectionScreenUiEvent
    data object ConnectClickedEvent : ManualConnectionScreenUiEvent
}
