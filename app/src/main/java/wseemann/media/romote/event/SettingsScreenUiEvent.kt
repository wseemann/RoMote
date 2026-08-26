package wseemann.media.romote.event

sealed interface SettingsScreenUiEvent {
    data object FindRemoteClickedEvent: SettingsScreenUiEvent
}