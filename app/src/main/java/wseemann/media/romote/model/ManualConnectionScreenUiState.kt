package wseemann.media.romote.model

data class ManualConnectionScreenUiState(
    /** Starts empty; the screen shows a sample address as a placeholder instead. */
    val ipAddress: String = "",
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    /**
     * Set once the device has been stored. ManualConnectionActivity watches this to hand its
     * caller a RESULT_OK and finish.
     */
    val isConnected: Boolean = false
)
