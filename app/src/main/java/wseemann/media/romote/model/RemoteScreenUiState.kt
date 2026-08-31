package wseemann.media.romote.model

import androidx.annotation.StringRes

data class RemoteScreenUiState(
    val deviceName: String = "",
    /**
     * The old updateVolumeControls() only touched the row's visibility when the device reported a
     * `tv` flag, so a device that never reports one - or a read that throws - left the row on
     * screen. Defaulting to true keeps that.
     */
    val showVolumeControls: Boolean = true,
    val privateListening: PrivateListening = PrivateListening.UNAVAILABLE,
    /** Whether the soft keyboard is up and every keystroke is being relayed to the device. */
    val keyboardActive: Boolean = false,
    /**
     * What has been typed since the keyboard was raised. This is the app's picture of the device's
     * text field, not the device's own - the ECP has no way to read that back - so it starts empty
     * every time the keyboard is raised.
     */
    val typedText: String = "",
    val showPowerOffConfirmation: Boolean = false,
    val showInstallPrivateListening: Boolean = false,
    /** One-shot Wake-on-LAN toast, cleared by [wseemann.media.romote.event.RemoteScreenUiEvent.MessageShownEvent]. */
    @field:StringRes val messageResId: Int? = null
) {

    /**
     * Collapses the three-way icon choice the fragment used to make by hand: the device has to
     * support private listening *and* the companion app has to be installed before the button does
     * anything, and it only reads as on while the bound service says audio is playing.
     */
    enum class PrivateListening {
        UNAVAILABLE,
        AVAILABLE,
        ACTIVE
    }
}
