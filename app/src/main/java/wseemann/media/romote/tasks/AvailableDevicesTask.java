package wseemann.media.romote.tasks;

import android.content.Context;

import com.jaku.api.DeviceRequests;
import com.jaku.model.Device;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import wseemann.media.romote.utils.DBUtils;

public class AvailableDevicesTask implements Callable {
    private Context context;

    public AvailableDevicesTask(final Context context) {
        this.context = context;
    }

    public List<Device> call() {
        // Retrieve all Devices.
        List<Device> devices = DBUtils.getAllDevices(context);
        List<Device> availableDevices = new ArrayList<Device>();

        try {
            List<Device> rokuDevices = DeviceRequests.discoverDevices();

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
}