package wseemann.media.romote.event

import com.wseemann.ecp.core.KeyPressKeyValues
import wseemann.media.romote.data.ChannelItem

sealed interface RemoteScreenUiEvent {
    data class KeyPressedEvent(val key: KeyPressKeyValues) : RemoteScreenUiEvent

    data object PowerClickedEvent : RemoteScreenUiEvent

    data object PowerOffConfirmedEvent : RemoteScreenUiEvent

    data object PowerOffDismissedEvent : RemoteScreenUiEvent

    sealed interface KeyboardEvent : RemoteScreenUiEvent {

        data object ClickedEvent : KeyboardEvent

        data class TextChangedEvent(val text: String) : KeyboardEvent

        data object BackspaceEvent : KeyboardEvent

        data object DoneEvent : KeyboardEvent

        data object DismissedEvent : KeyboardEvent
    }

    data object PrivateListeningClickedEvent : RemoteScreenUiEvent

    data class PrivateListeningChangedEvent(val isActive: Boolean) : RemoteScreenUiEvent

    data object InstallPrivateListeningConfirmedEvent : RemoteScreenUiEvent

    data object InstallPrivateListeningDismissedEvent : RemoteScreenUiEvent

    data object DeviceChangedEvent : RemoteScreenUiEvent

    data object MessageShownEvent : RemoteScreenUiEvent

    data class RecentChannelClickedEvent(val channel: ChannelItem) : RemoteScreenUiEvent
}
