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

import wseemann.media.romote.model.Channel;
import wseemann.media.romote.parser.ChannelParser;
import wseemann.media.romote.utils.CommandHelper;

/**
 * A custom Loader that loads all of the installed applications.
 */
public class ChannelLoader extends AsyncTaskLoader<List<Channel>> {
    // TODO fix this
    //final InterestingConfigChanges mLastConfig = new InterestingConfigChanges();

    private List<Channel> mChannels;

    public ChannelLoader(Context context, Bundle args) {
        super(context);
    }

    /**
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */
    @Override public List<Channel> loadInBackground() {
        // Retrieve all Channels.
        String xml = download(CommandHelper.getAppQueryURL(getContext()));

        ChannelParser parser = new ChannelParser();

        List<Channel> channels = parser.parse(xml);

        // Sort the list.
        //Collections.sort(entries, ALPHA_COMPARATOR);

        // Done!
        return channels;
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override public void deliverResult(List<Channel> channels) {
        if (channels.size() == 0) {
            /*Toast.makeText(getContext(),
                    getContext().getString(R.string.error_message),
                    Toast.LENGTH_SHORT).show();*/
        }

        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (channels != null) {
                onReleaseResources(channels);
            }
        }
        List<Channel> oldChannels = channels;
        mChannels = channels;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(channels);
        }

        // At this point we can release the resources associated with
        // 'oldDevice' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldChannels != null) {
            onReleaseResources(oldChannels);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override protected void onStartLoading() {
        if (mChannels != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mChannels);
        }

        // TODO fix this
        if (takeContentChanged() || mChannels == null) { //|| configChange) {
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
    @Override public void onCanceled(List<Channel> channels) {
        super.onCanceled(channels);

        // At this point we can release the resources associated with 'Device'
        // if needed.
        onReleaseResources(channels);
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
        if (mChannels != null) {
            onReleaseResources(mChannels);
            mChannels = null;
        }
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<Channel> channels) {
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