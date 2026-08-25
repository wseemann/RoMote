package wseemann.media.romote.model

data class StoreScreenUiState(
    val url: String = "https://channelstore.roku.com/browse",
    val isLoading: Boolean = true,
    val canGoBack: Boolean = false
)
