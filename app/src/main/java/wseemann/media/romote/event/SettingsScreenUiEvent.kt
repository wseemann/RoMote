package wseemann.media.romote.event

sealed interface SettingsScreenUiEvent {
    data object FindRemoteClickedEvent: SettingsScreenUiEvent
    data class ShakeToPauseToggledEvent(val enabled: Boolean): SettingsScreenUiEvent
    data class NotificationWidgetToggledEvent(val enabled: Boolean): SettingsScreenUiEvent
    data class HapticFeedbackToggledEvent(val enabled: Boolean): SettingsScreenUiEvent
}
