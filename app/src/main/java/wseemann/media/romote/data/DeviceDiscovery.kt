package wseemann.media.romote.data

import android.content.Context
import com.wseemann.ecp.api.QueryRequests
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import wseemann.media.romote.data.Device.Companion.fromDevice
import wseemann.media.romote.discovery.DeviceDescription
import wseemann.media.romote.discovery.SsdpDiscovery
import wseemann.media.romote.utils.WifiApManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds the Roku devices reachable from this phone, either over SSDP or - when the phone is
 * itself the access point, where multicast doesn't reach the clients - by querying every
 * connected client directly.
 *
 * Scans are serialised: a burst of refreshes shouldn't stack network work, and every caller that
 * asks during a scan gets that scan's result.
 */
@Singleton
class DeviceDiscovery @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val mutex = Mutex()

    suspend fun discoverDevices(): List<Device> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val wifiApManager = WifiApManager(context)

            if (wifiApManager.isWifiApEnabled) {
                scanAccessPointForDevices(wifiApManager)
            } else {
                scanNetworkForDevices()
            }
        }
    }

    private fun scanNetworkForDevices(): List<Device> {
        return SsdpDiscovery.discoverDevices().mapNotNull { ssdpDevice ->
            try {
                // The device info response carries no address, so the host we reached it at is
                // the only record of where it lives.
                fromDevice(QueryRequests.queryDeviceInfo(ssdpDevice.host)).apply {
                    this.host = ssdpDevice.host
                    // The description document is where the device names its own image, and the
                    // device told us where that document is when it answered the M-SEARCH.
                    this.deviceImageUrl = DeviceDescription.fetchIconUrl(ssdpDevice.descriptionUrl)
                }
            } catch (ex: Exception) {
                Timber.tag(TAG).e(ex, "Failed to query %s", ssdpDevice.host)
                null
            }
        }
    }

    private fun scanAccessPointForDevices(wifiApManager: WifiApManager): List<Device> {
        val clients = wifiApManager.getClientList(false, CLIENT_SCAN_TIMEOUT_MILLIS)

        Timber.tag(TAG).d("Access point scan completed.")

        if (clients == null) {
            return emptyList()
        }

        Timber.tag(TAG).d("Found %s connected devices.", clients.size)

        return clients.mapNotNull { clientScanResult ->
            Timber.tag(TAG).d(
                "Device: " + clientScanResult.device +
                        " HW Address: " + clientScanResult.hwAddr +
                        " IP Address:  " + clientScanResult.ipAddr
            )

            val host = "http://" + clientScanResult.ipAddr + ":" + ECP_PORT

            try {
                fromDevice(QueryRequests.queryDeviceInfo(host)).apply {
                    this.host = host
                    // No M-SEARCH ran here, so there is no advertised LOCATION to work from. A
                    // Roku answers ST roku:ecp with its ECP base url, which is what this is; the
                    // image's filename still comes out of the document it serves.
                    this.deviceImageUrl = DeviceDescription.fetchIconUrl("$host/")
                }
            } catch (ex: Exception) {
                Timber.tag(TAG).e("Invalid device: %s", ex.message)
                null
            }
        }
    }

    private companion object {
        const val ECP_PORT = 8060
        const val CLIENT_SCAN_TIMEOUT_MILLIS = 3000
        const val TAG = "DeviceDiscovery"
    }
}
