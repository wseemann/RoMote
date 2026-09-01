package wseemann.media.romote.network

/**
 * Whether this phone sits on a network a Roku could answer on.
 *
 * The rule this replaced asked whether the *active* network's type was exactly TYPE_WIFI, which
 * told a hardwired phone it had no connection at all. What matters is only that some network with
 * a local-area transport is up - the phone can hold several at once, and which one happens to be
 * the default is the platform's business, not ours.
 *
 * Transports are matched across a network's whole set rather than a single value because a VPN
 * reports itself alongside whatever it runs over: {VPN, WIFI} is reachable, {VPN, CELLULAR} is not.
 */
object LocalNetworkPolicy {

    /**
     * Transports that can put this phone on the same LAN as a Roku. Cellular is the one this app
     * has to rule out - a Roku is never on the far side of a carrier network.
     */
    private val LOCAL_TRANSPORTS = setOf(
        NetworkTransport.WIFI,
        NetworkTransport.ETHERNET,
        NetworkTransport.USB
    )

    fun isLocalNetwork(transports: Set<NetworkTransport>): Boolean = transports.any { it in LOCAL_TRANSPORTS }

    /** True when at least one of [networks] - each given as its own transport set - is local. */
    fun hasLocalNetwork(networks: Collection<Set<NetworkTransport>>): Boolean = networks.any { isLocalNetwork(it) }
}
