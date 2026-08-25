package wseemann.media.romote.event

sealed interface ChannelScreenUiEvent {

    data object LoadChannelsEvent : ChannelScreenUiEvent

    data class ChannelClickedEvent(val channelId: String) : ChannelScreenUiEvent
}
