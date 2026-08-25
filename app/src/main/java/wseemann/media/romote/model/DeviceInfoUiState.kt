package wseemann.media.romote.model

sealed class DeviceInfoUiState {
    object Loading : DeviceInfoUiState()
    data class Success(val entries: MutableList<Entry>) : DeviceInfoUiState()
    data class Error(val exception: Exception) : DeviceInfoUiState()
}