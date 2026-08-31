package wseemann.media.romote.device

import android.content.ContentValues
import android.content.SharedPreferences
import android.database.Cursor
import wseemann.media.romote.data.Device
import wseemann.media.romote.database.DeviceDatabase

/**
 * Stored-device reads and writes.
 *
 * [deviceDatabase] is a singleton, so every method here shares one cached SQLiteDatabase. Nothing
 * closes it: the helper owns the connection for the life of the process. Closing it per call would
 * both defeat that caching and let one coroutine close the database out from under another - the
 * view models run these off several independent coroutines on the IO dispatcher.
 */
class DeviceRepository(private val deviceDatabase: DeviceDatabase, private val sharedPreferences: SharedPreferences) {

    fun insertDevice(device: Device): Long {
        val serialNumber = device.serialNumber

        if (serialNumber == null || deviceExists(serialNumber)) {
            return -1
        }

        val db = deviceDatabase.writableDatabase

        val values = ContentValues()
        values.put(DeviceDatabase.HOST, device.host)
        values.put(DeviceDatabase.UDN, device.udn)
        values.put(DeviceDatabase.SERIAL_NUMBER, device.serialNumber)
        values.put(DeviceDatabase.DEVICE_ID, device.deviceId)
        values.put(DeviceDatabase.VENDOR_NAME, device.vendorName)
        values.put(DeviceDatabase.MODEL_NUMBER, device.modelNumber)
        values.put(DeviceDatabase.MODEL_NAME, device.modelName)
        values.put(DeviceDatabase.WIFI_MAC, device.wifiMac)
        values.put(DeviceDatabase.ETHERNET_MAC, device.ethernetMac)
        values.put(DeviceDatabase.NETWORK_TYPE, device.networkType)
        values.put(DeviceDatabase.USER_DEVICE_NAME, device.userDeviceName)
        values.put(DeviceDatabase.SOFTWARE_VERSION, device.softwareVersion)
        values.put(DeviceDatabase.SOFTWARE_BUILD, device.softwareBuild)
        values.put(DeviceDatabase.SECURE_DEVICE, device.secureDevice)
        values.put(DeviceDatabase.LANGUAGE, device.language)
        values.put(DeviceDatabase.COUNTY, device.country)
        values.put(DeviceDatabase.LOCALE, device.locale)
        values.put(DeviceDatabase.TIME_ZONE, device.timeZone)
        values.put(DeviceDatabase.TIME_ZONE_OFFSET, device.timeZoneOffset)
        values.put(DeviceDatabase.POWER_MODE, device.powerMode)
        values.put(DeviceDatabase.SUPPORTS_SUSPEND, device.supportsSuspend)
        values.put(DeviceDatabase.SUPPORTS_FIND_REMOTE, device.supportsFindRemote)
        values.put(DeviceDatabase.SUPPORTS_AUDIO_GUIDE, device.supportsAudioGuide)
        values.put(DeviceDatabase.DEVELOPER_ENABLED, device.developerEnabled)
        values.put(DeviceDatabase.KEYED_DEVELOPER_ID, device.keyedDeveloperId)
        values.put(DeviceDatabase.SEARCH_ENABLED, device.searchEnabled)
        values.put(DeviceDatabase.VOICE_SEARCH_ENABLED, device.voiceSearchEnabled)
        values.put(DeviceDatabase.NOTIFICATIONS_ENABLED, device.notificationsEnabled)
        values.put(DeviceDatabase.NOTIFICATIONS_FIRST_USE, device.notificationsFirstUse)
        values.put(DeviceDatabase.SUPPORTS_PRIVATE_LISTENING, device.supportsPrivateListening)
        values.put(DeviceDatabase.HEADPHONES_CONNECTED, device.headphonesConnected)
        values.put(DeviceDatabase.IS_TV, device.tv)
        values.put(DeviceDatabase.IS_STICK, device.stick)
        values.put(DeviceDatabase.CUSTOM_USER_DEVICE_NAME, device.getCustomUserDeviceName())
        values.put(DeviceDatabase.DEVICE_IMAGE_URL, device.deviceImageUrl)

        val id = db.insert(DeviceDatabase.DEVICES_TABLE_NAME, null, values)

        return id
    }

    fun getDevice(serialNumber: String): Device? {
        val db = deviceDatabase.writableDatabase

        val device = db.query(
            DeviceDatabase.DEVICES_TABLE_NAME,
            null,
            DeviceDatabase.SERIAL_NUMBER + " = ?",
            arrayOf(serialNumber),
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor.moveToNext()) parseDevice(cursor) else null
        }

        return device
    }

    fun getAllDevices(): List<Device> {
        val devices = mutableListOf<Device>()

        val db = deviceDatabase.writableDatabase

        db.query(DeviceDatabase.DEVICES_TABLE_NAME, null, null, null, null, null, null)
            .use { cursor ->
                while (cursor.moveToNext()) {
                    devices.add(parseDevice(cursor))
                }
            }

        return devices
    }

    fun setConnectedDevice(serialNumber: String?) {
        val editor = sharedPreferences.edit()
        editor.putString("serial_number", serialNumber)
        editor.commit()
    }

    fun getConnectedDevice(): Device? {
        val serialNumber = sharedPreferences.getString("serial_number", null) ?: return null

        // getDevice returns null when nothing is paired, so the guard below is load bearing.
        val device = getDevice(serialNumber) ?: return null

        return device
    }

    fun updateDevice(device: Device): Long {
        val db = deviceDatabase.writableDatabase

        val values = ContentValues()
        values.put(DeviceDatabase.HOST, device.host)
        values.put(DeviceDatabase.IS_TV, device.tv)
        values.put(DeviceDatabase.IS_STICK, device.stick)
        device.getCustomUserDeviceName()?.let {
            values.put(DeviceDatabase.CUSTOM_USER_DEVICE_NAME, it)
        }
        // A device that is asleep or briefly unreachable reports no image. That is not the same as
        // having none, so it must not overwrite an image url already stored.
        device.deviceImageUrl?.let {
            values.put(DeviceDatabase.DEVICE_IMAGE_URL, it)
        }

        val whereClause = DeviceDatabase.SERIAL_NUMBER + " = ?"
        val whereArgs = arrayOf(device.serialNumber)

        val id = db.update(DeviceDatabase.DEVICES_TABLE_NAME, values, whereClause, whereArgs).toLong()

        return id
    }

    fun removeDevice(serialNumber: String): Int {
        val db = deviceDatabase.writableDatabase

        val rowsAffected = db.delete(
            DeviceDatabase.DEVICES_TABLE_NAME,
            DeviceDatabase.SERIAL_NUMBER + " = ?",
            arrayOf(serialNumber),
        )

        return rowsAffected
    }

    private fun deviceExists(serialNumber: String): Boolean {
        val db = deviceDatabase.writableDatabase

        val exists = db.query(
            DeviceDatabase.DEVICES_TABLE_NAME,
            null,
            DeviceDatabase.SERIAL_NUMBER + " = ?",
            arrayOf(serialNumber),
            null,
            null,
            null,
        ).use { cursor ->
            cursor.moveToNext()
        }

        return exists
    }

    private fun parseDevice(cursor: Cursor): Device {
        val device = Device()
        device.host = cursor.string(DeviceDatabase.HOST)
        device.udn = cursor.string(DeviceDatabase.UDN)
        device.serialNumber = cursor.string(DeviceDatabase.SERIAL_NUMBER)
        device.deviceId = cursor.string(DeviceDatabase.DEVICE_ID)
        device.vendorName = cursor.string(DeviceDatabase.VENDOR_NAME)
        device.modelNumber = cursor.string(DeviceDatabase.MODEL_NUMBER)
        device.modelName = cursor.string(DeviceDatabase.MODEL_NAME)
        device.wifiMac = cursor.string(DeviceDatabase.WIFI_MAC)
        device.ethernetMac = cursor.string(DeviceDatabase.ETHERNET_MAC)
        device.networkType = cursor.string(DeviceDatabase.NETWORK_TYPE)
        device.userDeviceName = cursor.string(DeviceDatabase.USER_DEVICE_NAME)
        device.softwareVersion = cursor.string(DeviceDatabase.SOFTWARE_VERSION)
        device.softwareBuild = cursor.string(DeviceDatabase.SOFTWARE_BUILD)
        device.secureDevice = cursor.string(DeviceDatabase.SECURE_DEVICE)
        device.language = cursor.string(DeviceDatabase.LANGUAGE)
        device.country = cursor.string(DeviceDatabase.COUNTY)
        device.locale = cursor.string(DeviceDatabase.LOCALE)
        device.timeZone = cursor.string(DeviceDatabase.TIME_ZONE)
        device.timeZoneOffset = cursor.string(DeviceDatabase.TIME_ZONE_OFFSET)
        device.powerMode = cursor.string(DeviceDatabase.POWER_MODE)
        device.supportsSuspend = cursor.string(DeviceDatabase.SUPPORTS_SUSPEND)
        device.supportsFindRemote = cursor.string(DeviceDatabase.SUPPORTS_FIND_REMOTE)
        device.supportsAudioGuide = cursor.string(DeviceDatabase.SUPPORTS_AUDIO_GUIDE)
        device.developerEnabled = cursor.string(DeviceDatabase.DEVELOPER_ENABLED)
        device.keyedDeveloperId = cursor.string(DeviceDatabase.KEYED_DEVELOPER_ID)
        device.searchEnabled = cursor.string(DeviceDatabase.SEARCH_ENABLED)
        device.voiceSearchEnabled = cursor.string(DeviceDatabase.VOICE_SEARCH_ENABLED)
        device.notificationsEnabled = cursor.string(DeviceDatabase.NOTIFICATIONS_ENABLED)
        device.notificationsFirstUse = cursor.string(DeviceDatabase.NOTIFICATIONS_FIRST_USE)
        device.supportsPrivateListening = cursor.string(DeviceDatabase.SUPPORTS_PRIVATE_LISTENING)
        device.headphonesConnected = cursor.string(DeviceDatabase.HEADPHONES_CONNECTED)
        device.tv = cursor.string(DeviceDatabase.IS_TV)
        device.stick = cursor.string(DeviceDatabase.IS_STICK)
        device.setCustomUserDeviceName(cursor.string(DeviceDatabase.CUSTOM_USER_DEVICE_NAME))
        device.deviceImageUrl = cursor.string(DeviceDatabase.DEVICE_IMAGE_URL)

        return device
    }

    /**
     * Reads a nullable text column by name.
     *
     * getColumnIndex returns -1 for a column the table does not have, and getString(-1) throws.
     * A database that reaches version five has every column - [DeviceDatabase.onUpgrade] replays
     * the alters to repair the ones that skipped some - but that repair only runs when the version
     * changes, so this reads a missing column as null rather than crashing the device list the way
     * it did before that repair existed.
     */
    private fun Cursor.string(column: String): String? {
        val index = getColumnIndex(column)
        return if (index < 0) null else getString(index)
    }
}
