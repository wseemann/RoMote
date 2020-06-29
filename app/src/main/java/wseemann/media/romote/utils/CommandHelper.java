package wseemann.media.romote.utils;

import android.content.Context;

import wseemann.media.romote.model.Device;

/**
 * Created by wseemann on 6/25/16.
 */
public class CommandHelper {

    private CommandHelper() {

    }

    public static String getDeviceURL(Context context) {
        String url = "";

        try {
            Device device = PreferenceUtils.getConnectedDevice(context);

            url = device.getHost();
        } catch (Exception ex) {
        }

        return url;
    }

    public static String getIconURL(Context context, String channelId) {
        String url = "";

        try {
            Device device = PreferenceUtils.getConnectedDevice(context);

            url = device.getHost() + "/query/icon/" + channelId;
        } catch (Exception ex) {
        }

        return url;
    }

    public static String getDeviceInfoURL(Context context, String host) {
        String url = host;

        return url;
    }

    public static String getConnectedDeviceInfoURL(Context context, String host) {
        String url = "";

        try {
            Device device = PreferenceUtils.getConnectedDevice(context);

            url = device.getHost();
        } catch (Exception ex) {
        }

        return url;
    }
}
