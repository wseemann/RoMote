package wseemann.media.romote.tasks;

import android.content.Context;

import com.jaku.model.Device;

import java.util.List;
import java.util.concurrent.Callable;

import wseemann.media.romote.utils.DBUtils;

public class PairedDevicesTask implements Callable {
    private Context context;

    public PairedDevicesTask(final Context context) {
        this.context = context;
    }

    public List<Device> call() {
        // Retrieve all Device.
        List<Device> devices = DBUtils.getAllDevices(context);

        // Sort the list.
        //Collections.sort(entries, ALPHA_COMPARATOR);

        // Done!
        return devices;
    }
}