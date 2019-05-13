package wseemann.media.romote.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by wseemann on 6/19/16.
 */
public class NetworkMonitor {

    private ConnectivityManager mCm;
    private WifiApManager mWifiApManager;

    public NetworkMonitor(Activity activity, int requestCode) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(activity, requestCode);
        } else {
            mCm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            mWifiApManager = new WifiApManager(activity);
        }
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
        if (mWifiApManager == null) {
            return false;
        }

        return mWifiApManager.isWifiApEnabled();
    }

    private void requestPermission(Activity activity, int requestCode) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_NETWORK_STATE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                        requestCode);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }
}
