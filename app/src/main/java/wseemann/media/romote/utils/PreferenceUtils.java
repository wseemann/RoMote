package wseemann.media.romote.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import wseemann.media.romote.model.Device;

/**
 * Created by wseemann on 6/21/16.
 */
public class PreferenceUtils {

    private PreferenceUtils() {

    }

    public static void setConnectedDevice(Context context, String serialNumber) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("serial_number", serialNumber);
        editor.commit();
    }

    public static Device getConnectedDevice(Context context) {
        Device device = null;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String serialNumber = prefs.getString("serial_number", null);

        device = DBUtils.getDevice(context, serialNumber);

        return device;
    }
}
