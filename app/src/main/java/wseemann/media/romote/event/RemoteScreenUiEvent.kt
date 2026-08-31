package wseemann.media.romote.event

import com.wseemann.ecp.core.KeyPressKeyValues

sealed interface RemoteScreenUiEvent {

    data class KeyPressedEvent(val key: KeyPressKeyValues) : RemoteScreenUiEvent

    data object PowerClickedEvent : RemoteScreenUiEvent

    data object PowerOffConfirmedEvent : RemoteScreenUiEvent

    data object PowerOffDismissedEvent : RemoteScreenUiEvent

    /**
     * Everything the soft keyboard does while it is relaying what is typed to the device. Grouped
     * so that the screen's event handler dispatches the whole keyboard once rather than five times.
     */
    sealed interface KeyboardEvent : RemoteScreenUiEvent {

        /** The keyboard remote button: raises the keyboard if it is down, puts it away if it is up. */
        data object ClickedEvent : KeyboardEvent

        /**
         * The whole contents of the keyboard bar's field after an edit, rather than an individual
         * keystroke, because that is all an IME gives the screen: printable characters are
         * committed through the InputConnection, not delivered as key events, and a paste, a
         * swipe-typed word or an autocorrect replacement each arrive as one change.
         */
        data class TextChangedEvent(val text: String) : KeyboardEvent

        /**
         * The keyboard bar's own backspace. Separate from [TextChangedEvent] because the field on
         * the phone runs empty long before the one on the device does, and an empty field produces
         * no edit to diff.
         */
        data object BackspaceEvent : KeyboardEvent

        /** The IME's Done action: sends Enter to the device and puts the keyboard away. */
        data object DoneEvent : KeyboardEvent

        /** The keyboard went away - by gesture, by leaving the tab, or by the app pausing. */
        data object DismissedEvent : KeyboardEvent
    }

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
