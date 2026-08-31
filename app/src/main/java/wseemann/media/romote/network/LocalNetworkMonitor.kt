package wseemann.media.romote.network

import kotlinx.coroutines.flow.Flow

interface LocalNetworkMonitor {
    val isLocalNetworkAvailable: Flow<Boolean>
}
