package wseemann.media.romote.event

import com.wseemann.ecp.core.KeyPressKeyValues

sealed interface RemoteScreenUiEvent {

    data class KeyPressedEvent(val key: KeyPressKeyValues) : RemoteScreenUiEvent

    data object PowerClickedEvent : RemoteScreenUiEvent

    data object PowerOffConfirmedEvent : RemoteScreenUiEvent

    data object PowerOffDismissedEvent : RemoteScreenUiEvent

    data object KeyboardClickedEvent : RemoteScreenUiEvent

    data object PrivateListeningClickedEvent : RemoteScreenUiEvent

    data class PrivateListeningChangedEvent(val isActive: Boolean) : RemoteScreenUiEvent

    data object InstallPrivateListeningConfirmedEvent : RemoteScreenUiEvent

    data object InstallPrivateListeningDismissedEvent : RemoteScreenUiEvent

    /**
     * The connected device changed, or the screen came back to the foreground. Both re-read the
     * device and re-check whether the private listening app is installed - the user may have gone
     * off and installed it.
     */
    data object DeviceChangedEvent : RemoteScreenUiEvent

    data object MessageShownEvent : RemoteScreenUiEvent
}
