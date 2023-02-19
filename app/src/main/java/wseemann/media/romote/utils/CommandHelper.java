package wseemann.media.romote.utils;

import wseemann.media.romote.model.Device;

/**
 * Created by wseemann on 6/25/16.
 */
public class CommandHelper {

    private PreferenceUtils preferenceUtils;

    public CommandHelper(PreferenceUtils preferenceUtils) {
        this.preferenceUtils = preferenceUtils;
    }

    public String getDeviceURL() {
        String url = "";

        try {
            Device device = preferenceUtils.getConnectedDevice();

            url = device.getHost();
        } catch (Exception ex) {
        }

        return url;
    }

    public String getIconURL(String channelId) {
        String url = "";

        try {
            Device device = preferenceUtils.getConnectedDevice();

            url = device.getHost() + "/query/icon/" + channelId;
        } catch (Exception ex) {
        }

        return url;
    }

    public String getDeviceInfoURL(String host) {
        String url = host;

        return url;
    }

    public String getConnectedDeviceInfoURL(String host) {
        String url = "";

        try {
            Device device = preferenceUtils.getConnectedDevice();

            url = device.getHost();
        } catch (Exception ex) {
        }

        return url;
    }
}
