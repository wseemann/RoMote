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
        String url = "";

        try {
            Device device = PreferenceUtils.getConnectedDevice(context);

            url = device.getHost() + "/query/icon/" + channelId;
        } catch (Exception ex) {
        }

        return url;
    }

    public static String getLaunchURL(Context context, String channelId) {
        String url = "";

        try {
            Device device = PreferenceUtils.getConnectedDevice(context);

            url = device.getHost() + "/launch/" + channelId;
        } catch (Exception ex) {
        }

        return url;
    }

    public static String getAppQueryURL(Context context) {
        String url = "";

        try {
            Device device = PreferenceUtils.getConnectedDevice(context);

            url = device.getHost() + "/query/apps";
        } catch (Exception ex) {
        }

        return url;
    }

    public static String getKeypressURL(Context context, String command) {
        String url = "";

        try {
            Device device = PreferenceUtils.getConnectedDevice(context);

            url = device.getHost() + "/keypress/" + command;
        } catch (Exception ex) {
        }

        return url;
    }

    public static String getActiveAppURL(Context context) {
        String url = "";

        try {
            Device device = PreferenceUtils.getConnectedDevice(context);

            url = device.getHost() + "/query/active-app";
        } catch (Exception ex) {
        }

        return url;
    }

    public static String getDeviceInfoURL(Context context, String host) {
        String url = host + "/query/device-info";

        return url;
    }

    public static String getConnectedDeviceInfoURL(Context context, String host) {
        String url = "";

        try {
            Device device = PreferenceUtils.getConnectedDevice(context);

            url = device.getHost() + "/query/device-info";
        } catch (Exception ex) {
        }

        return url;
    }
}
