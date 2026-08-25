package wseemann.media.romote.model

import com.wseemann.ecp.model.Channel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ChannelScreenUiState(
    val channels: ImmutableList<Channel> = persistentListOf(),
    val isLoading: Boolean = false
)