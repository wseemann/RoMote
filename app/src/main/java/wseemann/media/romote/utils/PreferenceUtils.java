package wseemann.media.romote.utils;

import android.content.Context;
import android.content.SharedPreferences;

import wseemann.media.romote.data.Device;

public class PreferenceUtils {

    private final Context context;
    private final SharedPreferences sharedPreferences;

    public PreferenceUtils(
            Context context,
            SharedPreferences sharedPreferences
    ) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
    }

    public void setConnectedDevice(String serialNumber) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("serial_number", serialNumber);
        editor.commit();
    }

    public Device getConnectedDevice() throws Exception {
        Device device;

        String serialNumber = sharedPreferences.getString("serial_number", null);

        device = DBUtils.getDevice(context, serialNumber);

        if (device == null) {
            throw new Exception("Device not connected");
        }

        return device;
    }

    public boolean shouldProvideHapticFeedback() {
        return sharedPreferences.getBoolean("haptic_feedback_preference", false);
    }
}
