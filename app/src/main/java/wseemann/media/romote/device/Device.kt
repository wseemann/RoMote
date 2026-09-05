package wseemann.media.romote.device

import com.wseemann.ecp.api.KeyRequests
import com.wseemann.ecp.api.LaunchRequests
import com.wseemann.ecp.api.QueryRequests
import com.wseemann.ecp.api.SearchRequests
import com.wseemann.ecp.core.KeyPressKeyValues
import com.wseemann.ecp.model.Channel
import com.wseemann.ecp.request.KeyPressRequest
import timber.log.Timber
import wseemann.media.romote.data.ChannelItem
import wseemann.media.romote.data.Device

@Suppress("TooGenericExceptionCaught")
class Device(private val device: Device) {

    fun getDeviceInfo(): Device = device

    fun queryDeviceInfo(): com.wseemann.ecp.model.Device? {
        val host = device.host ?: return null

        return try {
            QueryRequests.queryDeviceInfo(host)
        } catch (ex: Exception) {
            Timber.tag(TAG).e(ex, "Failed to query device info")
            null
        }
    }

    fun performKeyPress(keyPressKeyValue: KeyPressKeyValues) {
        val host = device.host ?: return

        return try {
            KeyRequests.keyPressRequest(host, keyPressKeyValue)
        } catch (ex: Exception) {
            Timber.tag(TAG).e(ex, "Failed to perform key press")
        }
    }

    fun performLiteralKeyPress(character: String) {
        val host = device.host ?: return

        try {
            KeyPressRequest(host, KeyPressKeyValues.LIT_.value + character).send()
        } catch (ex: Exception) {
            Timber.tag(TAG).e(ex, "Failed to perform key press")
        }
    }

    fun performQueryApps(): List<ChannelItem>? {
        val host = device.host ?: return null

        return try {
            QueryRequests.queryAppsRequest(host)
                .map { channel ->
                    ChannelItem(
                        id = channel.id.orEmpty(),
                        title = channel.title.orEmpty(),
                        iconUrl = ChannelItem.iconUrl(host, channel.id.orEmpty())
                    )
                }
        } catch (ex: Exception) {
            Timber.tag(TAG).e(ex, "Failed to query apps")
            null
        }
    }

    fun performQueryActiveApp(): List<Channel>? {
        val host = device.host ?: return null

        return try {
            QueryRequests.queryActiveAppRequest(host)
        } catch (ex: Exception) {
            Timber.tag(TAG).e(ex, "Failed to query active app")
            return null
        }
    }

    fun performQueryIcon(appId: String): ByteArray? {
        val host = device.host ?: return null

        return try {
            QueryRequests.queryIconRequest(host, appId)
        } catch (ex: Exception) {
            Timber.tag(TAG).e(ex, "Failed to query icon")
            return null
        }
    }

    fun performLaunchApp(channelId: String) {
        val host = device.host ?: return

        try {
            LaunchRequests.launchAppIdRequest(host, channelId)
        } catch (ex: Exception) {
            Timber.tag(TAG).e(ex, "Failed to launch channel")
        }
    }

    private companion object {
        const val TAG = "Device"
    }
}
