package wseemann.media.romote.model

data class ManualConnectionScreenUiState(
    /** Seeded with the prefix the EditText in fragment_manual_connection.xml started out with. */
    val ipAddress: String = "192.168.1.",
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    /**
     * Set once the device has been stored. ManualConnectionActivity watches this to hand its
     * caller a RESULT_OK and finish.
     */
    val isConnected: Boolean = false
)
