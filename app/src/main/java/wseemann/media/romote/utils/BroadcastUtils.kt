package wseemann.media.romote.utils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import wseemann.media.romote.widget.RokuAppWidgetProvider

class BroadcastUtils {
    companion object {
        fun sendUpdateDeviceBroadcast(context: Context) {
            val intent = Intent(Constants.UPDATE_DEVICE_BROADCAST)
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        }

        /** Tells the home screen widgets to redraw against the device that was just connected. */
        fun sendWidgetUpdateBroadcast(context: Context) {
            val widgetManager = AppWidgetManager.getInstance(context)
            val widgetComponent = ComponentName(context, RokuAppWidgetProvider::class.java)

            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    widgetManager.getAppWidgetIds(widgetComponent)
                )
            }

            context.sendBroadcast(intent)
        }
    }
}
