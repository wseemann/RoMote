package wseemann.media.romote.tasks;

import android.content.Context;
import android.util.Log;

import com.jaku.core.JakuRequest;
import com.jaku.core.JakuResponse;
import com.jaku.model.Channel;
import com.jaku.model.Device;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Callable;

import wseemann.media.romote.utils.DBUtils;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.utils.RokuRequestTypes;

public class RxRequestTask implements Callable {
    private static final String TAG = "RxRequestTask";

    private Context context;
    private JakuRequest request;
    private RokuRequestTypes rokuRequestType;

    public RxRequestTask(final Context context, final JakuRequest request, final RokuRequestTypes rokuRequestType) {
        this.context = context;
        this.request = request;
        this.rokuRequestType = rokuRequestType;
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

    public RxRequestTask.Result call() {
        RxRequestTask.Result result;

        try {
            if (rokuRequestType.equals(RokuRequestTypes.query_active_app)) {
                JakuResponse response = request.send();
                List<Channel> channels = (List<Channel>) response.getResponseData();
                result = new Result(channels);
            } else if (rokuRequestType.equals(RokuRequestTypes.query_device_info)) {
                JakuResponse response = request.send();
                Device device = (Device) response.getResponseData();
                result = new Result(device);
            } else if (rokuRequestType.equals(RokuRequestTypes.query_icon)) {
                JakuResponse response = request.send();
                byte[] data = ((ByteArrayOutputStream) response.getResponseData()).toByteArray();
                result = new Result(data);
            } else {
                JakuResponse response = request.send();
                result = new Result(response.getResponseData());
            }
        } catch (IOException ex) {
            if (ex instanceof UnknownHostException) {
                handleUnknownHostException();
            }

            ex.printStackTrace();
            result = new Result(ex);
        }

        return result;
    }

    private void handleUnknownHostException() {
        AvailableDevicesTask availableDevicesTask = new AvailableDevicesTask(context, false);
        List<Device> devices = availableDevicesTask.call();

        try {
            Device connectedDevice = PreferenceUtils.getConnectedDevice(context);

            for (Device device: devices) {
                if (device.getSerialNumber().equals(connectedDevice.getSerialNumber())) {
                    DBUtils.updateDevice(context, device);
                    break;
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Device not found");
        }
    }
}