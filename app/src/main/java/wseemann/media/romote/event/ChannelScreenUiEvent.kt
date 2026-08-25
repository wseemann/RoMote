package wseemann.media.romote.event

sealed interface ChannelScreenUiEvent {

    data object LoadChannelsEvent : ChannelScreenUiEvent
}