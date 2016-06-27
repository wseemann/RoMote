package wseemann.media.romote.activity;

import android.content.pm.PackageManager;
import android.os.Bundle;

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

        if (!mNetworkMonitor.isConnectedToiWiFi()) {
            if (mDialog != null) {
                mDialog.dismiss();
                mDialog = null;
            }

            mDialog = new ConnectivityDialog();
            mDialog.show(getFragmentManager(), ConnectivityDialog.class.getName());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
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
}
