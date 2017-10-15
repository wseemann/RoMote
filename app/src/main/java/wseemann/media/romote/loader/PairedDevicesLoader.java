package wseemann.media.romote.loader;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import wseemann.media.romote.utils.DBUtils;

import com.jaku.model.Device;

/**
 * A custom Loader that loads all of the installed applications.
 */
public class PairedDevicesLoader extends AsyncTaskLoader<List<Device>> {
    // TODO fix this
    //final InterestingConfigChanges mLastConfig = new InterestingConfigChanges();

    private List<Device> mDevices;

    public PairedDevicesLoader(Context context, Bundle args) {
        super(context);
    }

    /**
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */
    @Override public List<Device> loadInBackground() {
        // Retrieve all Device.
        List<Device> devices = DBUtils.getAllDevices(getContext());

        // Sort the list.
        //Collections.sort(entries, ALPHA_COMPARATOR);

        // Done!
        return devices;
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override public void deliverResult(List<Device> devices) {
        if (devices.size() == 0) {
            /*Toast.makeText(getContext(),
                    getContext().getString(R.string.error_message),
                    Toast.LENGTH_SHORT).show();*/
        }

        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (devices != null) {
                onReleaseResources(devices);
            }
        }
        List<Device> oldDevice = devices;
        mDevices = devices;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(devices);
        }

        // At this point we can release the resources associated with
        // 'oldDevice' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldDevice != null) {
            onReleaseResources(oldDevice);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override protected void onStartLoading() {
        if (mDevices != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mDevices);
        }

        // TODO fix this
        if (takeContentChanged() || mDevices == null) { //|| configChange) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override public void onCanceled(List<Device> devices) {
        super.onCanceled(devices);

        // At this point we can release the resources associated with 'Device'
        // if needed.
        onReleaseResources(devices);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'Device'
        // if needed.
        if (mDevices != null) {
            onReleaseResources(mDevices);
            mDevices = null;
        }
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<Device> devices) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }

    private String download(String url) {

        HttpURLConnection conn = null;
        StringBuffer html = new StringBuffer();
        String line = null;
        BufferedReader reader = null;

        try {
            URL mURL = new URL(url);

            if (mURL.getProtocol().equalsIgnoreCase("http")) {
                conn = (HttpURLConnection) mURL.openConnection();
            }

            conn.setRequestProperty("User-Agent", "ServeStream");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestMethod("GET");

            // Start the query
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            conn.connect();

            while ((line = reader.readLine()) != null) {
                html = html.append(line);
            }

        } catch (Exception ex) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                }
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        return html.toString();
    }
}