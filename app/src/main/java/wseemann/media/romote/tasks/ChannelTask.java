package wseemann.media.romote.tasks;

import android.content.Context;

import com.jaku.api.QueryRequests;
import com.jaku.model.Channel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import wseemann.media.romote.utils.CommandHelper;

public class ChannelTask implements Callable {
    private Context context;

    public ChannelTask(final Context context) {
        this.context = context;
    }

    public List<Channel> call() {
        // Retrieve all Channels.
        List<Channel> channels;

        try {
            channels = QueryRequests.queryAppsRequest(CommandHelper.getDeviceURL(context));
        } catch (IOException ex) {
            ex.printStackTrace();
            channels = new ArrayList<>();
        }

        return channels;
    }
}