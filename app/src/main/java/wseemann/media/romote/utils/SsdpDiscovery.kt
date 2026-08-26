package wseemann.media.romote.utils

import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * SSDP M-SEARCH discovery for Roku devices.
 *
 * This replaces com.wseemann.ecp.api.DeviceRequests.discoverDevices(), whose implementation
 * calls DatagramSocket.receive() without a socket timeout inside a fixed ten iteration loop.
 * UDP replies are dropped routinely over Wi-Fi, and the first drop blocks receive() forever on
 * a thread that Thread.interrupt() cannot wake, so the scan never returns.
 *
 * Here the socket has a read timeout and the whole scan has a deadline, so it always terminates,
 * with an empty list when nothing answered.
 */
object SsdpDiscovery {

    const val DEFAULT_TIMEOUT_MILLIS = 4_000L

    private const val MULTICAST_ADDRESS = "239.255.255.250"
    private const val MULTICAST_PORT = 1900

    /** How long a Roku may wait before replying, in seconds, per the SSDP MX header. */
    private const val MAX_WAIT_SECONDS = 3

    /** M-SEARCH is sent this many times because a single UDP datagram is easily lost. */
    private const val SEARCH_ATTEMPTS = 3

    private const val RECEIVE_TIMEOUT_MILLIS = 500

    private const val RECEIVE_BUFFER_BYTES = 2048

    private const val LOCATION_HEADER = "location:"

    private const val TAG = "SsdpDiscovery"

    private val MSEARCH = buildString {
        append("M-SEARCH * HTTP/1.1\r\n")
        append("HOST: $MULTICAST_ADDRESS:$MULTICAST_PORT\r\n")
        append("MAN: \"ssdp:discover\"\r\n")
        append("ST: roku:ecp\r\n")
        append("MX: $MAX_WAIT_SECONDS\r\n")
        append("\r\n")
    }

    /**
     * Sends an M-SEARCH and collects replies until [timeoutMillis] elapses.
     *
     * @return the base URL of every Roku that answered ("http://192.168.1.9:8060"), deduplicated
     *         and in the order they replied. Empty when nothing answered - never an exception.
     */
    fun discoverHosts(timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS): List<String> {
        val hosts = LinkedHashSet<String>()

        try {
            // An ephemeral port is deliberate: Roku unicasts its reply back to the port the
            // M-SEARCH came from, so we never join the multicast group and need neither a
            // MulticastLock nor CHANGE_WIFI_MULTICAST_STATE.
            DatagramSocket().use { socket ->
                socket.soTimeout = RECEIVE_TIMEOUT_MILLIS

                val group = InetAddress.getByName(MULTICAST_ADDRESS)
                val payload = MSEARCH.toByteArray()

                repeat(SEARCH_ATTEMPTS) {
                    socket.send(DatagramPacket(payload, payload.size, group, MULTICAST_PORT))
                }

                val deadline = System.nanoTime() + timeoutMillis * 1_000_000
                val buffer = ByteArray(RECEIVE_BUFFER_BYTES)

                while (System.nanoTime() < deadline) {
                    val packet = DatagramPacket(buffer, buffer.size)

                    try {
                        socket.receive(packet)
                    } catch (ex: SocketTimeoutException) {
                        // Nothing arrived in this window; keep listening until the deadline.
                        continue
                    }

                    val response = String(packet.data, packet.offset, packet.length)

                    // Anything that isn't a Roku reply is skipped rather than aborting the scan.
                    parseLocation(response)?.let { hosts.add(it) }
                }
            }
        } catch (ex: Exception) {
            Timber.tag(TAG).e(ex, "SSDP discovery failed")
        }

        Timber.tag(TAG).d("Discovered %s device(s)", hosts.size)

        return hosts.toList()
    }

    /**
     * Reads the base URL out of an SSDP reply's LOCATION header, which looks like
     * "LOCATION: http://192.168.1.9:8060/". Returns null when the response carries no usable
     * location, so a stray datagram from some other SSDP responder is simply ignored.
     */
    internal fun parseLocation(response: String): String? {
        val location = response.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(LOCATION_HEADER, ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?: return null

        if (!location.startsWith("http://", ignoreCase = true) &&
            !location.startsWith("https://", ignoreCase = true)
        ) {
            return null
        }

        // Keep scheme, host and port; drop the path ("/" on a Roku) so the result can be used
        // directly as an ECP base URL.
        val schemeEnd = location.indexOf("//") + 2
        val pathStart = location.indexOf('/', schemeEnd)
        val base = if (pathStart == -1) location else location.substring(0, pathStart)

        return base.takeIf { it.length > schemeEnd }
    }
}
