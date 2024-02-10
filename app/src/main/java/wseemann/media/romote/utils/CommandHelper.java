package wseemann.media.romote.utils;

import timber.log.Timber;
import wseemann.media.romote.model.Device;

/**
 * Created by wseemann on 6/25/16.
 */
public class CommandHelper {

    private final PreferenceUtils preferenceUtils;

    public CommandHelper(PreferenceUtils preferenceUtils) {
        this.preferenceUtils = preferenceUtils;
    }

    public String getDeviceURL() {
        String url = "";

        try {
            Device device = preferenceUtils.getConnectedDevice();

            url = device.getHost();
        } catch (Exception ex) {
            Timber.e(ex, "Failed to retrieve device URL");
        }

        return url;
    }

    public String getIconURL(String channelId) {
        String url = "";

        try {
            Device device = preferenceUtils.getConnectedDevice();

            url = device.getHost() + "/query/icon/" + channelId;
        } catch (Exception ex) {
            Timber.e(ex, "Failed to retrieve icon URL for channelId: %s", channelId);
        }

        return url;
    }

    public String getDeviceInfoURL(String host) {
        return host;
    }

    public String getConnectedDeviceInfoURL() {
        String url = "";

        try {
            Device device = preferenceUtils.getConnectedDevice();

            url = device.getHost();
        } catch (Exception ex) {
            Timber.e(ex, "Failed to retrieve device info");
        }

        return url;
    }
}
