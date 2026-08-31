package wseemann.media.romote.network

import kotlinx.coroutines.flow.Flow

/**
 * Reports whether this phone is on a network a Roku could be reached over.
 *
 * An interface so the screens can be driven from a plain flow in tests; the platform lives behind
 * [ConnectivityLocalNetworkMonitor].
 */
interface LocalNetworkMonitor {

    /** Emits the current answer on collection, and again on every change. */
    val isLocalNetworkAvailable: Flow<Boolean>
}
