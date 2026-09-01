package wseemann.media.romote.network

/**
 * The transports [LocalNetworkPolicy] reasons about, named without any android.net type so the
 * rule that decides whether a Roku is reachable can be exercised on the JVM.
 *
 * [OTHER] stands in for every transport the platform may grow that this app has no opinion on;
 * it deliberately does not count as a local network.
 */
enum class NetworkTransport {
    WIFI,
    ETHERNET,
    USB,
    CELLULAR,
    VPN,
    BLUETOOTH,
    OTHER
}
