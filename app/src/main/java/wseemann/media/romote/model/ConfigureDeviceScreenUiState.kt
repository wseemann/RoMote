package wseemann.media.romote.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ConfigureDeviceScreenUiState(
    val availableDevices: ImmutableList<Device> = persistentListOf(),
    // This screen starts out discovering devices, so the progress state is the initial one
    val isLoading: Boolean = true
)
