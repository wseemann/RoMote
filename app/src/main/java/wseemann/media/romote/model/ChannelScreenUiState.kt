package wseemann.media.romote.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import wseemann.media.romote.data.ChannelItem

data class ChannelScreenUiState(
    val channels: ImmutableList<ChannelItem> = persistentListOf(),
    val isLoading: Boolean = false,
    /**
     * Whether there is a device to read channels from at all. It starts out true so that the grid
     * never accuses the user of having no device before anything has actually been read - the
     * ViewModel sets it from [wseemann.media.romote.utils.CommandHelper.getDeviceURL], which
     * returns an empty string when nothing is paired.
     */
    val isDeviceConnected: Boolean = true
)
