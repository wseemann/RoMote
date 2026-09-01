package wseemann.media.romote.network

/**
 * Whether this phone sits on a network a Roku could answer on.
 *
 * What matters is only that some network with a local-area transport is up - the phone can hold
 * several at once, and which is the default is the platform's business. Asking the *active*
 * network instead told a hardwired phone it had no connection at all.
 *
 * Transports are matched across a network's whole set rather than a single value because a VPN
 * reports itself alongside whatever it runs over: {VPN, WIFI} is reachable, {VPN, CELLULAR} is not.
 */
object LocalNetworkPolicy {

    /** Cellular is the one to rule out - a Roku is never on the far side of a carrier network. */
    private val LOCAL_TRANSPORTS = setOf(
        NetworkTransport.WIFI,
        NetworkTransport.ETHERNET,
        NetworkTransport.USB
    )

    fun isLocalNetwork(transports: Set<NetworkTransport>): Boolean = transports.any { it in LOCAL_TRANSPORTS }

    fun hasLocalNetwork(networks: Collection<Set<NetworkTransport>>): Boolean = networks.any { isLocalNetwork(it) }
}
