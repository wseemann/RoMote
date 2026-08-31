package wseemann.media.romote.event

sealed interface ChannelScreenUiEvent {

    data object LoadChannelsEvent : ChannelScreenUiEvent

    /** The device the app is pointed at may have changed; reload only if it actually did. */
    data object DeviceChangedEvent : ChannelScreenUiEvent

    data class ChannelClickedEvent(val channelId: String) : ChannelScreenUiEvent
}
