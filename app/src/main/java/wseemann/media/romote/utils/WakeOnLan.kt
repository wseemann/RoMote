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
import androidx.core.net.toUri

/**
 * Sends a Wake-on-LAN magic packet to the connected device. Used as a fallback when the device
 * can't be reached over ECP, which is the case when it's fully powered off.
 */
class WakeOnLan {

    sealed class WakeResult {
        data object Sent : WakeResult()
        data object NoMacAddress : WakeResult()
        data class Failed(val exception: Exception) : WakeResult()
    }

    fun interface WakeCallback {
        fun onResult(result: WakeResult)
    }

    companion object {

        private const val BROADCAST_FALLBACK = "255.255.255.255"
        private val PORTS = intArrayOf(9, 7)
        private const val REPEAT_COUNT = 3
        private const val REPEAT_DELAY_MILLIS = 100L

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

            if (digits.length != 12) {
                return null
            }

            return try {
                ByteArray(6) { i ->
                    digits.substring(i * 2, i * 2 + 2).toInt(16).toByte()
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
            val packet = ByteArray(6 + 16 * mac.size)

            for (i in 0 until 6) {
                packet[i] = 0xFF.toByte()
            }

            for (i in 0 until 16) {
                mac.copyInto(packet, 6 + i * mac.size)
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
            if (prefixLength !in 1..31) {
                return null
            }

            val octets = address.split(".")

            if (octets.size != 4) {
                return null
            }

            return try {
                var ip = 0

                for (octet in octets) {
                    val value = octet.toInt()

                    if (value !in 0..255) {
                        return null
                    }

                    ip = (ip shl 8) or value
                }

                val netmask = -1 shl (32 - prefixLength)
                val broadcast = ip or netmask.inv()

                InetAddress.getByAddress(
                    byteArrayOf(
                        (broadcast ushr 24).toByte(),
                        (broadcast ushr 16).toByte(),
                        (broadcast ushr 8).toByte(),
                        broadcast.toByte()
                    )
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
            return mac.joinToString(":") { String.format("%02x", it) }
        }
    }
}
