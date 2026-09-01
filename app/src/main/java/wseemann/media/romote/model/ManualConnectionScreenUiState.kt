package wseemann.media.romote.model

data class ManualConnectionScreenUiState(
    val ipAddress: String = "",
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val hasError: Boolean = false
)
