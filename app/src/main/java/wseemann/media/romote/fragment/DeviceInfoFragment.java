package wseemann.media.romote.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import com.wseemann.ecp.api.ResponseCallback;
import com.wseemann.ecp.request.QueryDeviceInfoRequest;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.adapter.DeviceInfoAdapter;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.model.Entry;
import wseemann.media.romote.tasks.ResponseCallbackWrapper;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.DBUtils;

import javax.inject.Inject;

/**
 * Created by wseemann on 6/19/16.
 */
@AndroidEntryPoint
public class DeviceInfoFragment extends ListFragment {

    @Inject
    protected CommandHelper commandHelper;

    private DeviceInfoAdapter mAdapter;

    public static DeviceInfoFragment getInstance(String serialNumber, String host) {
        DeviceInfoFragment fragment = new DeviceInfoFragment();

        Bundle bundle = new Bundle();
        bundle.putString("serial_number", serialNumber);
        bundle.putString("host", host);

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_device_info));

        Bundle bundle = getArguments();
        String serialNumber = bundle.getString("serial_number");
        String host = bundle.getString("host");

        Device device = DBUtils.getDevice(getActivity(), serialNumber);

        List<Entry> entries = new ArrayList<Entry>();

        if (device != null) {
            entries = parseDevice(device);
        }

        mAdapter = new DeviceInfoAdapter(DeviceInfoFragment.this.getActivity(), entries);
        setListAdapter(mAdapter);

        setListShown(false);

        if (host == null) {
            sendCommand(commandHelper.getConnectedDeviceInfoURL());
        } else {
            sendCommand(commandHelper.getDeviceInfoURL(host));
        }
    }

    private void sendCommand(String command) {
        String url = command;

        QueryDeviceInfoRequest queryActiveAppRequest = new QueryDeviceInfoRequest(url);
        queryActiveAppRequest.sendAsync(new ResponseCallbackWrapper<>(new ResponseCallback<com.wseemann.ecp.model.Device>() {
            @Override
            public void onSuccess(@Nullable com.wseemann.ecp.model.Device device) {
                List<Entry> entries = parseDevice(Device.Companion.fromDevice(device));

                mAdapter.addAll(entries);
                mAdapter.notifyDataSetChanged();
                DeviceInfoFragment.this.setListShown(true);
            }

            @Override
            public void onError(@NonNull Exception e) {
                DeviceInfoFragment.this.setListShown(true);
            }
        }));
    }

    private List<Entry> parseDevice(Device device) {
        List<Entry> entries = new ArrayList<Entry>();

        entries.add(new Entry("udn", device.getUdn()));
        entries.add(new Entry("serial-number", device.getSerialNumber()));
        entries.add(new Entry("device-id", device.getDeviceId()));
        entries.add(new Entry("vendor-name", device.getVendorName()));
        entries.add(new Entry("model-number", device.getModelNumber()));
        entries.add(new Entry("model-name", device.getModelName()));
        entries.add(new Entry("wifi-mac", device.getWifiMac()));
        entries.add(new Entry("ethernet-mac", device.getEthernetMac()));
        entries.add(new Entry("network-type", device.getNetworkType()));
        entries.add(new Entry("user-device-name", device.getUserDeviceName()));
        entries.add(new Entry("software-version", device.getSoftwareVersion()));
        entries.add(new Entry("software-build", device.getSoftwareBuild()));
        entries.add(new Entry("secure-device", device.getSecureDevice()));
        entries.add(new Entry("language", device.getLanguage()));
        entries.add(new Entry("country", device.getCountry()));
        entries.add(new Entry("locale", device.getLocale()));
        entries.add(new Entry("time-zone", device.getTimeZone()));
        entries.add(new Entry("time-zone-offset", device.getTimeZoneOffset()));
        entries.add(new Entry("power-mode", device.getPowerMode()));
        entries.add(new Entry("supports-suspend", device.getSupportsSuspend()));
        entries.add(new Entry("supports-find-remote", device.getSupportsFindRemote()));
        entries.add(new Entry("supports-audio-guide", device.getSupportsAudioGuide()));
        entries.add(new Entry("developer-enabled", device.getDeveloperEnabled()));
        entries.add(new Entry("keyed-developer-id", device.getKeyedDeveloperId()));
        entries.add(new Entry("search-enabled", device.getSearchEnabled()));
        entries.add(new Entry("voice-search-enabled", device.getVoiceSearchEnabled()));
        entries.add(new Entry("notifications-enabled", device.getNotificationsEnabled()));
        entries.add(new Entry("notifications-first-use", device.getNotificationsFirstUse()));
        entries.add(new Entry("supports-private-listening", device.getSupportsPrivateListening()));
        entries.add(new Entry("headphones-connected", device.getHeadphonesConnected()));

        return entries;
    }
}
