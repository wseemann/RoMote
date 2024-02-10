package wseemann.media.romote.tasks;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Callable;

import wseemann.media.romote.model.Device;
import wseemann.media.romote.utils.Constants;
import wseemann.media.romote.utils.DBUtils;
import wseemann.media.romote.utils.PreferenceUtils;

public class UpdatePairedDeviceTask implements Callable {
    private static final String TAG = "UpdatePairedDevicesTask";

    private final Context context;
    private final PreferenceUtils preferenceUtils;

    public UpdatePairedDeviceTask(
            final Context context,
            final PreferenceUtils preferenceUtils
    ) {
        this.context = context;
        this.preferenceUtils = preferenceUtils;
    }

    public class Result {
        Object mResultValue;
        Exception mException;
        public Result(Object resultValue) {
            mResultValue = resultValue;
        }
        public Result(Exception exception) {
            mException = exception;
        }
    }

    public Boolean call() {
        AvailableDevicesTask availableDevicesTask = new AvailableDevicesTask(context, false);
        List<Device> devices = availableDevicesTask.call();

        try {
            Device connectedDevice = preferenceUtils.getConnectedDevice();

            if (connectedDevice == null) {
                return false;
            }

            for (Device device: devices) {
                if (device.getSerialNumber().equals(connectedDevice.getSerialNumber())) {
                    DBUtils.updateDevice(context, device);
                    context.sendBroadcast(new Intent(Constants.UPDATE_DEVICE_BROADCAST));
                    break;
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Device not found");
        }

        return false;
    }
}