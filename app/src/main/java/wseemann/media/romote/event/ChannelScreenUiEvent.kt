package wseemann.media.romote.event

sealed interface ChannelScreenUiEvent {
    data object LoadChannelsEvent : ChannelScreenUiEvent

    data object DeviceChangedEvent : ChannelScreenUiEvent

    data class ChannelClickedEvent(val channelId: String) : ChannelScreenUiEvent
}
