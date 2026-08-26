package wseemann.media.romote.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import wseemann.media.romote.data.Device

data class MainScreenUiState(
    val availableDevices: ImmutableList<Device> = persistentListOf(),
    val pairedDevices: ImmutableList<Device> = persistentListOf(),
    val isLoading: Boolean = false,
    val renameTarget: RenameTarget? = null
) {
    /**
     * The device the rename dialog is open for, or null when it is closed. It carries the name
     * rather than the Device itself because Device is mutable and compares by identity, which a
     * state this one copies would carry along.
     */
    data class RenameTarget(
        val serialNumber: String,
        val currentName: String
    )
}
