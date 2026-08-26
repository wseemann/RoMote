package wseemann.media.romote.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import wseemann.media.romote.data.ChannelItem

data class ChannelScreenUiState(
    val channels: ImmutableList<ChannelItem> = persistentListOf(),
    val isLoading: Boolean = false
)
