package wseemann.media.romote.utils

import android.content.Context
import android.content.Intent

class BroadcastUtils {
    companion object {
        fun sendUpdateDeviceBroadcast(context: Context) {
            val intent = Intent(Constants.UPDATE_DEVICE_BROADCAST)
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        }
    }
}