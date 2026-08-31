package wseemann.media.romote.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import wseemann.media.romote.composables.ConnectivityDialogHost;
import wseemann.media.romote.utils.NetworkMonitor;

public class ConnectivityActivity extends ShakeActivity {

    private ConnectivityDialogHost mDialogHost;

    private NetworkMonitor mNetworkMonitor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDialogHost = new ConnectivityDialogHost(this);
        mNetworkMonitor = new NetworkMonitor(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mNetworkMonitor.isConnectedToiWiFi() &&
                !mNetworkMonitor.isMobileAccessPointOn() &&
                !mDialogHost.isShowing()) {
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

    private synchronized void showDialog() {
        mDialogHost.show();
    }

    private synchronized void dismissDialog() {
        mDialogHost.dismiss();
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
                        !mDialogHost.isShowing()) {
                    showDialog();
                    onWifiDisconnected();
                }
            } else if (mNetworkMonitor.isConnectedToiWiFi() && mDialogHost.isShowing()) {
                dismissDialog();
                onWifiConnected();
            }
        }
    };

    protected void onWifiConnected() {}

    protected void onWifiDisconnected() {}
}
