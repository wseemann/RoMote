package wseemann.media.romote.model

data class ConnectivityUiState(
    val isLocalNetworkAvailable: Boolean = true,
    val isDialogVisible: Boolean = false,
    val isDismissed: Boolean = false,
)
