package wseemann.media.romote.utils

import android.content.Context
import android.content.SharedPreferences
import wseemann.media.romote.data.Device
import wseemann.media.romote.preferences.AppPreferences

class PreferenceUtils(
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val appPreferences: AppPreferences
) {
    fun setConnectedDevice(serialNumber: String?) {
        val editor = sharedPreferences.edit()
        editor.putString("serial_number", serialNumber)
        editor.commit()
    }

    @get:Throws(Exception::class)
    val connectedDevice: Device
        get() {
            val device: Device

            val serialNumber = sharedPreferences.getString("serial_number", null)

            device = DBUtils.getDevice(context, serialNumber)

            if (device == null) {
                throw Exception("Device not connected")
            }

            return device
        }

    fun shouldProvideHapticFeedback(): Boolean {
        return appPreferences.isHapticFeedbackEnabled()
    }
}
