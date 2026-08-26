package wseemann.media.romote.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import wseemann.media.romote.data.Device

data class ConfigureDeviceScreenUiState(
    val availableDevices: ImmutableList<Device> = persistentListOf(),
    val isLoading: Boolean = true
)
