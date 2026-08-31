package wseemann.media.romote.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import wseemann.media.romote.data.Entry

data class DeviceInfoScreenUiState(
    val entries: ImmutableList<Entry> = persistentListOf(),
    val isLoading: Boolean = true,
)
