package wseemann.media.romote.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import wseemann.media.romote.data.Device
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.util.Locale
import androidx.core.net.toUri

/**
 * Sends a Wake-on-LAN magic packet to the connected device. Used as a fallback when the device
 * can't be reached over ECP, which is the case when it's fully powered off.
 */
class WakeOnLan {

    sealed interface WakeResult {
        data object Sent : WakeResult
        data object NoMacAddress : WakeResult
        data class Failed(val exception: Exception) : WakeResult
    }

    fun interface WakeCallback {
        fun onResult(result: WakeResult)
    }

    companion object {

        private const val BROADCAST_FALLBACK = "255.255.255.255"
        private val PORTS = intArrayOf(9, 7)
        private const val REPEAT_COUNT = 3
        private const val REPEAT_DELAY_MILLIS = 100L

        /** A MAC address is six bytes, written as twelve hex digits. */
        private const val MAC_BYTES = 6
        private const val HEX_DIGITS_PER_BYTE = 2
        private const val HEX_RADIX = 16
        private const val MAC_HEX_DIGITS = MAC_BYTES * HEX_DIGITS_PER_BYTE

        /** The magic packet repeats the target MAC sixteen times after the 0xFF header. */
        private const val MAGIC_PACKET_MAC_REPEATS = 16

        private const val IPV4_OCTETS = 4
        private const val BITS_PER_OCTET = 8
        private const val IPV4_BITS = IPV4_OCTETS * BITS_PER_OCTET
        private const val MAX_OCTET_VALUE = 255

        /** /0 covers every address and /32 is a single host, so neither has a useful broadcast. */
        private val USABLE_PREFIX_LENGTHS = 1..<IPV4_BITS

        /**
         * Resolves the connected device, builds a magic packet for it and broadcasts it on the
         * device's subnet. The result is delivered on the main thread.
         */
        @JvmStatic
        fun wakeAsync(
            context: Context,
            preferenceUtils: PreferenceUtils,
            callback: WakeCallback
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val result = try {
                    // Reads SQLite and throws when nothing is paired, so it has to stay off the
                    // main thread.
                    wake(context, preferenceUtils.connectedDevice)
                } catch (ex: Exception) {
                    WakeResult.Failed(ex)
                }

                withContext(Dispatchers.Main) {
                    callback.onResult(result)
                }
            }
        }

        private suspend fun wake(context: Context, device: Device): WakeResult {
            val mac = resolveMac(device) ?: return WakeResult.NoMacAddress

            return try {
                val packet = buildMagicPacket(mac)
                val broadcastAddress = resolveBroadcastAddress(context, device)

                Timber.d("Sending magic packet for %s to %s", formatMac(mac), broadcastAddress)

                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    bindToWifi(context, socket)

                    repeat(REPEAT_COUNT) { attempt ->
                        for (port in PORTS) {
                            socket.send(DatagramPacket(packet, packet.size, broadcastAddress, port))
                        }

                        if (attempt < REPEAT_COUNT - 1) {
                            delay(REPEAT_DELAY_MILLIS)
                        }
                    }
                }

                WakeResult.Sent
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to send magic packet")
                WakeResult.Failed(ex)
            }
        }

        /**
         * Prefers the ethernet MAC when the device reports an ethernet connection, the wifi MAC
         * otherwise, falling back to whichever one is present. Returns null when the device has
         * neither, which means it can't be woken.
         */
        internal fun resolveMac(device: Device): ByteArray? {
            val ethernetMac = parseMac(device.ethernetMac)
            val wifiMac = parseMac(device.wifiMac)

            return if ("ethernet".equals(device.networkType, ignoreCase = true)) {
                ethernetMac ?: wifiMac
            } else {
                wifiMac ?: ethernetMac
            }
        }

        /**
         * Accepts the colon separated form Roku reports as well as dash separated and bare hex.
         */
        internal fun parseMac(mac: String?): ByteArray? {
            val digits = mac?.replace(":", "")?.replace("-", "")?.trim() ?: return null

            if (digits.length != MAC_HEX_DIGITS) {
                return null
            }

            return try {
                ByteArray(MAC_BYTES) { i ->
                    val start = i * HEX_DIGITS_PER_BYTE
                    digits.substring(start, start + HEX_DIGITS_PER_BYTE).toInt(HEX_RADIX).toByte()
                }
            } catch (ex: NumberFormatException) {
                Timber.e(ex, "Malformed MAC address")
                null
            }
        }

        /**
         * A magic packet is six 0xFF bytes followed by the target MAC repeated sixteen times.
         */
        internal fun buildMagicPacket(mac: ByteArray): ByteArray {
            val packet = ByteArray(MAC_BYTES + MAGIC_PACKET_MAC_REPEATS * mac.size)

            for (i in 0 until MAC_BYTES) {
                packet[i] = 0xFF.toByte()
            }

            for (i in 0 until MAGIC_PACKET_MAC_REPEATS) {
                mac.copyInto(packet, MAC_BYTES + i * mac.size)
            }

            return packet
        }

        private fun resolveBroadcastAddress(context: Context, device: Device): InetAddress {
            val deviceIp = parseHostAddress(device.host)
            val prefixLength = activeIpv4PrefixLength(context)

            if (deviceIp != null && prefixLength != null) {
                broadcastAddressFor(deviceIp, prefixLength)?.let { return it }
            }

            return InetAddress.getByName(BROADCAST_FALLBACK)
        }

        /** The host is stored as `http://192.168.1.42:8060`. */
        internal fun parseHostAddress(host: String?): String? {
            if (host.isNullOrBlank()) {
                return null
            }

            return try {
                host.toUri().host
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to parse host: %s", host)
                null
            }
        }

        /**
         * Turns an address and prefix length into the subnet broadcast address, e.g.
         * 192.168.1.42/24 becomes 192.168.1.255. Returns null for anything that isn't IPv4.
         */
        internal fun broadcastAddressFor(address: String, prefixLength: Int): InetAddress? {
            if (prefixLength !in USABLE_PREFIX_LENGTHS) {
                return null
            }

            val octets = address.split(".")

            if (octets.size != IPV4_OCTETS) {
                return null
            }

            return try {
                var ip = 0

                for (octet in octets) {
                    val value = octet.toInt()

                    if (value !in 0..MAX_OCTET_VALUE) {
                        return null
                    }

                    ip = (ip shl BITS_PER_OCTET) or value
                }

                val netmask = -1 shl (IPV4_BITS - prefixLength)
                val broadcast = ip or netmask.inv()

                InetAddress.getByAddress(
                    ByteArray(IPV4_OCTETS) { index ->
                        val shift = BITS_PER_OCTET * (IPV4_OCTETS - 1 - index)
                        (broadcast ushr shift).toByte()
                    }
                )
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to derive broadcast address for %s/%s", address, prefixLength)
                null
            }
        }

        private fun activeIpv4PrefixLength(context: Context): Int? {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return null

            val network = connectivityManager.activeNetwork ?: return null
            val linkProperties = connectivityManager.getLinkProperties(network) ?: return null

            return linkProperties.linkAddresses
                .firstOrNull { it.address is Inet4Address }
                ?.prefixLength
        }

        /**
         * Routes the broadcast over Wi-Fi rather than whichever network happens to be the default,
         * which would otherwise be cellular when both are up.
         */
        private fun bindToWifi(context: Context, socket: DatagramSocket) {
            try {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                        ?: return

                val wifiNetwork = connectivityManager.allNetworks.firstOrNull { network ->
                    connectivityManager.getNetworkCapabilities(network)
                        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                } ?: return

                wifiNetwork.bindSocket(socket)
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to bind socket to the wifi network")
            }
        }

        private fun formatMac(mac: ByteArray): String {
            return mac.joinToString(":") { String.format(Locale.ROOT, "%02x", it) }
        }
    }
}
