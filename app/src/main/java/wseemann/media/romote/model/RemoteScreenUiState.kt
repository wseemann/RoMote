package wseemann.media.romote.model

import androidx.annotation.StringRes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import wseemann.media.romote.data.ChannelItem

data class RemoteScreenUiState(
    val deviceName: String = "",
    val isDeviceConnected: Boolean = true,
    val showVolumeControls: Boolean = true,
    val privateListening: PrivateListening = PrivateListening.UNAVAILABLE,
    val keyboardActive: Boolean = false,
    val typedText: String = "",
    val showPowerOffConfirmation: Boolean = false,
    val showInstallPrivateListening: Boolean = false,
    /** Empty until something has been launched from RoMote, which is what hides the peek. */
    val recentChannels: ImmutableList<ChannelItem> = persistentListOf(),
    val showRecentsSheet: Boolean = false,
    @field:StringRes val messageResId: Int? = null
) {

    enum class PrivateListening {
        UNAVAILABLE,
        AVAILABLE,
        ACTIVE
    }
}
