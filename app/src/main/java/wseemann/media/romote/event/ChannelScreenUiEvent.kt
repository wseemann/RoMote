package wseemann.media.romote.event

import wseemann.media.romote.data.ChannelItem

sealed interface ChannelScreenUiEvent {
    data object LoadChannelsEvent : ChannelScreenUiEvent

    data object DeviceChangedEvent : ChannelScreenUiEvent

    data class ChannelClickedEvent(val channel: ChannelItem) : ChannelScreenUiEvent
}
