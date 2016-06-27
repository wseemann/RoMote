package wseemann.media.romote.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import wseemann.media.romote.R;
import wseemann.media.romote.activity.MainActivity;
import wseemann.media.romote.activity.ManualConnectionActivity;
import wseemann.media.romote.loader.DeviceDiscoveryLoader;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.utils.DBUtils;
import wseemann.media.romote.utils.PreferenceUtils;

/**
 * Created by wseemann on 6/20/16.
 */
public class ConfigureDeviceFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Device>> {

    private TextView mWirelessNextworkTextview;
    private TextView mSelectDeviceText;
    private RelativeLayout mProgressLayout;
    private LinearLayout mList;
    private Button mConnectManuallyButton;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configure_device, container, false);

        mWirelessNextworkTextview = (TextView) view.findViewById(R.id.wireless_nextwork_textview);
        mSelectDeviceText = (TextView) view.findViewById(R.id.select_device_text);
        mProgressLayout = (RelativeLayout) view.findViewById(R.id.progress_layout);
        mList = (LinearLayout) view.findViewById(R.id.list);
        mConnectManuallyButton = (Button) view.findViewById(R.id.connect_manually_button);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mConnectManuallyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfigureDeviceFragment.this.getActivity(), ManualConnectionActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        mHandler = new Handler();

        setListShown(false);

        getLoaderManager().initLoader(0, new Bundle(), this);
    }

    @Override
    public void onResume() {
        super.onResume();

        mWirelessNextworkTextview.setText(getWirelessNetworkName(getActivity()));
    }

    @Override
    public void onPause() {
        super.onPause();

        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            startActivity(new Intent(ConfigureDeviceFragment.this.getActivity(), MainActivity.class));
            ConfigureDeviceFragment.this.getActivity().finish();
        }
    }

    private String getWirelessNetworkName(Context context) {
        String networkName = "";

        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return networkName;
        }

        final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;

        if (isConnected && isWiFi) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();

            networkName = connectionInfo.getSSID();
        }

        return networkName;
    }

    @Override
    public Loader<List<Device>> onCreateLoader(int arg0, Bundle args) {
        return new DeviceDiscoveryLoader(getActivity(), args);
    }

    @Override
    public void onLoadFinished(Loader<List<Device>> loader, List<Device> devices) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setListShown(false);
                getLoaderManager().restartLoader(0, new Bundle(), ConfigureDeviceFragment.this);
            }
        }, 5000);

        if (devices.size() == 0) {
            setListShown(true);
            return;
        }

        // Set the new devices in the adapter.
        for (int i = 0; i < devices.size(); i++) {
            if (!containDevice(devices.get(i))) {
                RelativeLayout view = (RelativeLayout) getActivity().getLayoutInflater().inflate(R.layout.configure_device_list_item, null, false);

                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(devices.get(i).getModelName());
                text2.setText("SN: " + devices.get(i).getSerialNumber());

                view.setTag(devices.get(i));
                view.setOnClickListener(mClickListener);

                mList.addView(view);
            }
        }

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShown(true);
            //setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Device>> devices) {
        // Clear the devices in the adapter.
        // mAdapter.clear();
    }

    public void setListShown(boolean shown) {
        if (shown) {
            mSelectDeviceText.setVisibility(View.VISIBLE);
            mProgressLayout.setVisibility(View.GONE);
        } else {
            mSelectDeviceText.setVisibility(View.GONE);
            mProgressLayout.setVisibility(View.VISIBLE);
        }
    }

    private boolean containDevice(Device device) {
        boolean found = false;

        for (int i = 0; i < mList.getChildCount(); i++) {
            Device existingDevice = (Device) mList.getChildAt(i).getTag();

            if (device.getSerialNumber().equals(existingDevice.getSerialNumber())) {
                found = true;
                break;
            }
        }

        return found;
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Device device = (Device) v.getTag();

            DBUtils.insertDevice(ConfigureDeviceFragment.this.getActivity(), device);
            PreferenceUtils.setConnectedDevice(ConfigureDeviceFragment.this.getActivity(), device.getSerialNumber());

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ConfigureDeviceFragment.this.getActivity());

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("first_use", false);
            editor.commit();

            startActivity(new Intent(ConfigureDeviceFragment.this.getActivity(), MainActivity.class));
            ConfigureDeviceFragment.this.getActivity().finish();
        }
    };
}
