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
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.activity.MainActivity;
import wseemann.media.romote.activity.ManualConnectionActivity;
import wseemann.media.romote.event.ConfigureDeviceScreenUiEvent;
import wseemann.media.romote.data.Device;
import wseemann.media.romote.utils.DBUtils;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.viewmodels.ConfigureDeviceScreenViewModel;

/**
 * Created by wseemann on 6/20/16.
 */
@AndroidEntryPoint
public class ConfigureDeviceFragment extends Fragment {

    @Inject
    protected SharedPreferences sharedPreferences;

    @Inject
    protected PreferenceUtils preferenceUtils;

    private TextView mWirelessNextworkTextview;
    private TextView mSelectDeviceText;
    private RelativeLayout mProgressLayout;
    private LinearLayout mList;
    private Button mConnectManuallyButton;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private ConfigureDeviceScreenViewModel configureDeviceScreenViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureDeviceScreenViewModel = new ViewModelProvider(this).get(ConfigureDeviceScreenViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configure_device, container, false);

        mWirelessNextworkTextview = view.findViewById(R.id.wireless_nextwork_textview);
        mSelectDeviceText = view.findViewById(R.id.select_device_text);
        mProgressLayout = view.findViewById(R.id.progress_layout);
        mList = view.findViewById(R.id.list);
        mConnectManuallyButton = view.findViewById(R.id.connect_manually_button);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        configureDeviceScreenViewModel.getUiStateLiveData().observe(getViewLifecycleOwner(), state -> {
            setListShown(!state.isLoading());

            if (!state.isLoading()) {
                onLoadFinished(state.getAvailableDevices());
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mConnectManuallyButton.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigureDeviceFragment.this.getActivity(), ManualConnectionActivity.class);
            startActivityForResult(intent, 0);
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        mWirelessNextworkTextview.setText(getWirelessNetworkName(requireContext()));

        scheduleScan(1000);
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
            ConfigureDeviceFragment.this.requireActivity().finish();
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

    private void scheduleScan(long delayMillis) {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(() -> configureDeviceScreenViewModel.onHandleEvent(
                ConfigureDeviceScreenUiEvent.LoadAvailableDevicesEvent.INSTANCE), delayMillis);
    }

    private void onLoadFinished(List<Device> devices) {
        scheduleScan(5000);

        if (devices.isEmpty()) {
            return;
        }

        // Set the new devices in the adapter.
        for (int i = 0; i < devices.size(); i++) {
            if (!containDevice(devices.get(i))) {
                RelativeLayout view = (RelativeLayout) requireActivity().getLayoutInflater().inflate(R.layout.configure_device_list_item, null, false);

                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                String deviceName = devices.get(i).getModelName();
                String friendlyName = devices.get(i).getUserDeviceName();

                if (friendlyName != null && !friendlyName.isEmpty()) {
                    deviceName = friendlyName + " (" + deviceName + ")";
                }

                text1.setText(deviceName);
                text2.setText("SN: " + devices.get(i).getSerialNumber());

                view.setTag(devices.get(i));
                view.setOnClickListener(mClickListener);

                mList.addView(view);
            }
        }

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

    private final View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Device device = (Device) v.getTag();

            DBUtils.insertDevice(ConfigureDeviceFragment.this.getActivity(), device);
            preferenceUtils.setConnectedDevice(device.getSerialNumber());

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("first_use", false);
            editor.commit();

            startActivity(new Intent(ConfigureDeviceFragment.this.getActivity(), MainActivity.class));
            ConfigureDeviceFragment.this.requireActivity().finish();
        }
    };
}
