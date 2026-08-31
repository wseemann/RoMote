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
            val serialNumber = sharedPreferences.getString("serial_number", null)

            // DBUtils is Java and unannotated, so this is a platform type. It really does return
            // null when nothing is paired -- typing it non-null made the guard below dead code
            // and turned "not connected" into an NPE at the assignment instead.
            val device: Device? = DBUtils.getDevice(context, serialNumber)

            return device ?: error("Device not connected")
        }

    fun shouldProvideHapticFeedback(): Boolean {
        return appPreferences.isHapticFeedbackEnabled()
    }
}
