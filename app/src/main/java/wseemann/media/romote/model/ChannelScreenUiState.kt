package wseemann.media.romote.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ChannelScreenUiState(
    val channels: ImmutableList<ChannelItem> = persistentListOf(),
    val isLoading: Boolean = false
)
