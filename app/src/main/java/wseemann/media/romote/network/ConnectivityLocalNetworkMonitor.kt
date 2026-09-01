package wseemann.media.romote.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place that talks to ConnectivityManager, translating it into the answer
 * [LocalNetworkPolicy] defines. The platform hands over the set of matching networks and every
 * change to it, so there is no state to re-derive on resume.
 */
@Singleton
class ConnectivityLocalNetworkMonitor @Inject constructor(@param:ApplicationContext private val context: Context) :
    LocalNetworkMonitor {

    override val isLocalNetworkAvailable: Flow<Boolean> = callbackFlow {
        val connectivityManager = connectivityManager()

        if (connectivityManager == null) {
            // Fail open. Being wrong here costs a dialog that shouldn't be there, and this app has
            // nothing to offer behind it.
            send(true)
            awaitClose { }
            return@callbackFlow
        }

        // registerNetworkCallback replays onAvailable for networks that are already up, but says
        // nothing at all when there are none - so "no local network" only ever arrives from here.
        val networks = connectivityManager.currentLocalNetworks().toMutableSet()
        send(networks.isNotEmpty())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networks += network
                trySend(networks.isNotEmpty())
            }

            override fun onLost(network: Network) {
                networks -= network
                trySend(networks.isNotEmpty())
            }
        }

        // The handler serialises the callbacks onto the main looper, which is what lets `networks`
        // above be a plain MutableSet.
        connectivityManager.registerNetworkCallback(
            localNetworkRequest(),
            callback,
            Handler(Looper.getMainLooper())
        )

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        // trySend from a callback would drop onto the default rendezvous channel; only the latest
        // answer matters, so conflate rather than buffer.
        .conflate()
        .distinctUntilChanged()

    private fun connectivityManager(): ConnectivityManager? = context.getSystemService(ConnectivityManager::class.java)

    /**
     * Matches only the transports a Roku can be reached over, so the callbacks above never have to
     * look at capabilities themselves.
     */
    private fun localNetworkRequest(): NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        .apply {
            // TRANSPORT_USB only exists from API 31. Below it a USB dongle reports itself as
            // ethernet anyway, so nothing is lost.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                addTransportType(NetworkCapabilities.TRANSPORT_USB)
            }
        }
        .build()

    /**
     * The synchronous seed. `allNetworks` is deprecated at API 31 in favour of network callbacks,
     * which is what this flow moves on to - but a callback cannot report an absence, so there is no
     * supported replacement for this one read.
     */
    @Suppress("DEPRECATION")
    private fun ConnectivityManager.currentLocalNetworks(): Set<Network> = allNetworks
        .filter { network ->
            val transports = getNetworkCapabilities(network)?.toTransports().orEmpty()
            LocalNetworkPolicy.isLocalNetwork(transports)
        }
        .toSet()
}

private fun NetworkCapabilities.toTransports(): Set<NetworkTransport> = TRANSPORTS
    .filter { (platformTransport, _) -> hasTransport(platformTransport) }
    .map { (_, transport) -> transport }
    .toSet()

private val TRANSPORTS: List<Pair<Int, NetworkTransport>> = buildList {
    add(NetworkCapabilities.TRANSPORT_WIFI to NetworkTransport.WIFI)
    add(NetworkCapabilities.TRANSPORT_ETHERNET to NetworkTransport.ETHERNET)
    add(NetworkCapabilities.TRANSPORT_CELLULAR to NetworkTransport.CELLULAR)
    add(NetworkCapabilities.TRANSPORT_VPN to NetworkTransport.VPN)
    add(NetworkCapabilities.TRANSPORT_BLUETOOTH to NetworkTransport.BLUETOOTH)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(NetworkCapabilities.TRANSPORT_USB to NetworkTransport.USB)
    }
}
