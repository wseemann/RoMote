package wseemann.media.romote.model

/**
 * @param isLocalNetworkAvailable defaults to true so nothing flashes on screen in the moment
 * before [wseemann.media.romote.network.LocalNetworkMonitor] reports for the first time.
 * @param isDismissed whether the user has waved the dialog away for the current outage.
 */
data class ConnectivityUiState(
    val isLocalNetworkAvailable: Boolean = true,
    val isDialogVisible: Boolean = false,
    val isDismissed: Boolean = false
)
