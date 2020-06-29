package wseemann.media.romote.fragment;

import android.os.Bundle;

import androidx.fragment.app.ListFragment;

import java.util.ArrayList;
import java.util.List;

import wseemann.media.romote.R;
import wseemann.media.romote.adapter.DeviceInfoAdapter;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.model.Entry;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.DBUtils;
import wseemann.media.romote.utils.RokuRequestTypes;

import com.jaku.core.JakuRequest;
import com.jaku.parser.DeviceParser;
import com.jaku.request.QueryDeviceInfoRequest;

/**
 * Created by wseemann on 6/19/16.
 */
public class DeviceInfoFragment extends ListFragment {

    private static final String TAG = DeviceInfoFragment.class.getName();

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
            sendCommand(CommandHelper.getConnectedDeviceInfoURL(getActivity(), device.getHost()));
        } else {
            sendCommand(CommandHelper.getDeviceInfoURL(getActivity(), host));
        }
    }

    private void sendCommand(String command) {
        String url = command;

        QueryDeviceInfoRequest queryActiveAppRequest = new QueryDeviceInfoRequest(url);
        JakuRequest request = new JakuRequest(queryActiveAppRequest, new DeviceParser());

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {
                Device device = (Device) result.mResultValue;

                List<Entry> entries = parseDevice(device);

                mAdapter.addAll(entries);
                mAdapter.notifyDataSetChanged();
                DeviceInfoFragment.this.setListShown(true);
            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {
                DeviceInfoFragment.this.setListShown(true);
            }
        }).execute(RokuRequestTypes.query_device_info);
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
