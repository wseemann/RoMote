package wseemann.media.romote.discovery

import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * A Roku that answered an M-SEARCH.
 *
 * @param host the ECP base URL ("http://192.168.1.9:8060"), which every request is built on.
 * @param descriptionUrl the LOCATION the device advertised, verbatim. Roku points it at the UPnP
 *        description document, so this is where [DeviceDescription] looks for the device image
 *        rather than assembling a path we guessed at.
 */
data class SsdpDevice(
    val host: String,
    val descriptionUrl: String
)

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
     * @return every Roku that answered, deduplicated by host and in the order they replied. Empty
     *         when nothing answered - never an exception.
     */
    fun discoverDevices(timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS): List<SsdpDevice> {
        val devices = LinkedHashMap<String, SsdpDevice>()

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
                    val location = locationHeader(response) ?: continue
                    val host = baseUrl(location) ?: continue

                    devices.putIfAbsent(host, SsdpDevice(host = host, descriptionUrl = location))
                }
            }
        } catch (ex: Exception) {
            Timber.tag(TAG).e(ex, "SSDP discovery failed")
        }

        Timber.tag(TAG).d("Discovered %s device(s)", devices.size)

        return devices.values.toList()
    }

    /**
     * Reads the base URL out of an SSDP reply's LOCATION header, which looks like
     * "LOCATION: http://192.168.1.9:8060/". Returns null when the response carries no usable
     * location, so a stray datagram from some other SSDP responder is simply ignored.
     */
    internal fun parseLocation(response: String): String? =
        locationHeader(response)?.let { baseUrl(it) }

    /**
     * The same header, but with its path left on, because that path is the device's description
     * document. Roku answers ST roku:ecp with "http://192.168.1.9:8060/", which serves the
     * document that names the device image.
     */
    internal fun parseDescriptionUrl(response: String): String? {
        val location = locationHeader(response) ?: return null

        // A location with no host is no more usable here than it is in parseLocation.
        return if (baseUrl(location) == null) null else location
    }

    /** The LOCATION header's value, verbatim, once it is known to be an http(s) URL. */
    private fun locationHeader(response: String): String? {
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

        return location
    }

    /**
     * Keeps scheme, host and port and drops the path ("/" on a Roku) so the result can be used
     * directly as an ECP base URL.
     */
    private fun baseUrl(location: String): String? {
        val schemeEnd = location.indexOf("//") + 2
        val pathStart = location.indexOf('/', schemeEnd)
        val base = if (pathStart == -1) location else location.substring(0, pathStart)

        return base.takeIf { it.length > schemeEnd }
    }
}
