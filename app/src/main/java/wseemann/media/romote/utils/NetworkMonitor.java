package wseemann.media.romote.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Answers whether this phone is on a network a Roku could be sitting on.
 *
 * ACCESS_NETWORK_STATE is a normal permission, granted at install and never revocable, so there is
 * nothing to request here and nothing that can fail - the manifest declaration is the whole story.
 */
public class NetworkMonitor {

    private final ConnectivityManager mCm;
    private final WifiApManager mWifiApManager;

    public NetworkMonitor(Context context) {
        mCm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiApManager = new WifiApManager(context);
    }

    public boolean isConnectedToiWiFi() {
        if (mCm == null) {
            return false;
        }

        NetworkInfo activeNetwork = mCm.getActiveNetworkInfo();

        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        }

        return false;
    }

    public boolean isMobileAccessPointOn() {
        return mWifiApManager.isWifiApEnabled();
    }
}
