package wseemann.media.romote.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import wseemann.media.romote.fragment.ConnectivityDialog;
import wseemann.media.romote.utils.NetworkMonitor;

/**
 * Created by wseemann on 6/19/16.
 */
public class ConnectivityActivity extends ShakeActivity {

    private static final int MY_PERMISSIONS_ACCESS_NETWORK_STATE = 100;

    private ConnectivityDialog mDialog;

    private NetworkMonitor mNetworkMonitor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNetworkMonitor = new NetworkMonitor(this, MY_PERMISSIONS_ACCESS_NETWORK_STATE);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mNetworkMonitor.isConnectedToiWiFi() &&
                !mNetworkMonitor.isMobileAccessPointOn() &&
                mDialog == null) {
            showDialog();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        registerReceiver(mConnectivityReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(mConnectivityReceiver);
        dismissDialog();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_NETWORK_STATE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    mNetworkMonitor = new NetworkMonitor(this, MY_PERMISSIONS_ACCESS_NETWORK_STATE);
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private synchronized void showDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }

        mDialog = new ConnectivityDialog();
        mDialog.show(getFragmentManager(), ConnectivityDialog.class.getName());
    }

    private synchronized void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || (!intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)
                    && !intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE"))) {
                return;
            }

            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);

                boolean isConnected = wifiState == WifiManager.WIFI_STATE_ENABLED;

                if (!isConnected &&
                        !mNetworkMonitor.isMobileAccessPointOn() &&
                        mDialog == null) { //!mNetworkMonitor.isConnectedToiWiFi() && mDialog == null) {
                    showDialog();
                    onWifiDisconnected();
                }
            } else if (mNetworkMonitor.isConnectedToiWiFi() && mDialog != null) {
                dismissDialog();
                onWifiConnected();
            }
        }
    };

    protected void onWifiConnected() {}

    protected void onWifiDisconnected() {}
}
