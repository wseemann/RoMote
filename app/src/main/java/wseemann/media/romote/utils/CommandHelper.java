package wseemann.media.romote.utils;

import android.content.Context;
import android.util.Log;

import wseemann.media.romote.model.Device;

/**
 * Created by wseemann on 6/25/16.
 */
public class CommandHelper {

    private CommandHelper() {

    }

    public static String getIconURL(Context context, String channelId) {
        Device device = PreferenceUtils.getConnectedDevice(context);

        String url = device.getHost() + "/query/icon/" + channelId;

        return url;
    }

    public static String getLaunchURL(Context context, String channelId) {
        Device device = PreferenceUtils.getConnectedDevice(context);

        String url = device.getHost() + "/launch/" + channelId;

        return url;
    }

    public static String getAppQueryURL(Context context) {
        Device device = PreferenceUtils.getConnectedDevice(context);

        String url = device.getHost() + "/query/apps";

        return url;
    }

    public static String getKeypressURL(Context context, String command) {
        Device device = PreferenceUtils.getConnectedDevice(context);

        String url = device.getHost() + "/keypress/" + command;

        return url;
    }

    public static String getActiveAppURL(Context context) {
        Device device = PreferenceUtils.getConnectedDevice(context);

        String url = device.getHost() + "/query/active-app";

        return url;
    }

    public static String getDeviceInfoURL(Context context, String host) {
        String url = host + "/query/device-info";

        return url;
    }

    public static String getConnectedDeviceInfoURL(Context context, String host) {
        Device device = PreferenceUtils.getConnectedDevice(context);

        String url = device.getHost() + "/query/device-info";

        return url;
    }
}
