package wseemann.media.romote.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DeviceInfoScreenUiState(
    val entries: ImmutableList<Entry> = persistentListOf(),
    /**
     * Starts true because the screen queries the device as soon as it is created, the way the
     * ListFragment this replaced started out with its list hidden behind a spinner.
     */
    val isLoading: Boolean = true
)
