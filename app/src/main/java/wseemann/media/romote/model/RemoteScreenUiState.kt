package wseemann.media.romote.model

import androidx.annotation.StringRes

data class RemoteScreenUiState(
    val deviceName: String = "",
    val isDeviceConnected: Boolean = true,
    val showVolumeControls: Boolean = true,
    val privateListening: PrivateListening = PrivateListening.UNAVAILABLE,
    val keyboardActive: Boolean = false,
    val typedText: String = "",
    val showPowerOffConfirmation: Boolean = false,
    val showInstallPrivateListening: Boolean = false,
    @field:StringRes val messageResId: Int? = null,
) {

    enum class PrivateListening {
        UNAVAILABLE,
        AVAILABLE,
        ACTIVE,
    }
}
