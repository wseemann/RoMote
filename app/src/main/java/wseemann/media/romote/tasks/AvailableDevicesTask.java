package wseemann.media.romote.tasks;

import android.content.Context;
import android.util.Log;

import com.jaku.api.DeviceRequests;
import com.jaku.api.QueryRequests;
import com.jaku.model.Device;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import wseemann.media.romote.model.ClientScanResult;
import wseemann.media.romote.utils.WifiApManager;
import wseemann.media.romote.utils.DBUtils;

public class AvailableDevicesTask implements Callable {

    private static final String TAG = AvailableDevicesTask.class.getName();

    private Context context;
    private boolean filterPairedDevices;

    public AvailableDevicesTask(final Context context) {
        this(context, true);
    }

    public AvailableDevicesTask(final Context context, boolean filterPairedDevices) {
        this.context = context;
        this.filterPairedDevices = filterPairedDevices;
    }

    public List<Device> call() {
        // Retrieve all Devices.
        List<Device> devices = new ArrayList();

        if (filterPairedDevices) {
            devices = DBUtils.getAllDevices(context);
        }

        List<Device> availableDevices = new ArrayList<Device>();

        try {
            List<Device> rokuDevices = new ArrayList<>();

            final WifiApManager wifiApManager = new WifiApManager(context);

            if (wifiApManager.isWifiApEnabled()) {
                // Scan the mobile access point for devices
                rokuDevices.addAll(scanAccessPointForDevices());
            } else {
                rokuDevices.addAll(DeviceRequests.discoverDevices());
            }

            for (Device device: rokuDevices) {
                boolean exists = false;

                for (int j = 0; j < devices.size(); j++) {
                    if (devices.get(j).getSerialNumber().equals(device.getSerialNumber())) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    availableDevices.add(device);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Done!
        return availableDevices;
    }

    private ArrayList<Device> scanAccessPointForDevices() {
        ArrayList<Device> availableDevices = new ArrayList<Device>();

        final WifiApManager wifiApManager = new WifiApManager(context);

        if (wifiApManager.isWifiApEnabled()) {
            ArrayList<ClientScanResult> clients = wifiApManager.getClientList(false, 3000);

            Log.d(TAG, "Access point scan completed.");

            if (clients != null) {
                Log.d(TAG, "Found " + clients.size() + " connected devices.");

                for (ClientScanResult clientScanResult: clients) {
                    Log.d(TAG, "Device: " + clientScanResult.getDevice() +
                            " HW Address: " + clientScanResult.getHWAddr() +
                            " IP Address:  " + clientScanResult.getIpAddr());

                    try {
                        Device device = QueryRequests.queryDeviceInfo("http://" + clientScanResult.getIpAddr() + ":8060");
                        device.setHost("http://" + clientScanResult.getIpAddr() + ":8060");
                        availableDevices.add(device);
                    } catch (IOException ex) {
                        Log.e(TAG, "Invalid device: " + ex.getMessage());
                    }
                }
            }
        }

        return availableDevices;
    }
}