package wseemann.media.romote.tasks;

import com.jaku.api.QueryRequests;
import com.jaku.model.Channel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import wseemann.media.romote.utils.CommandHelper;

public class ChannelTask implements Callable {

    private CommandHelper commandHelper;

    public ChannelTask(final CommandHelper commandHelper) {
        this.commandHelper = commandHelper;
    }

    public List<Channel> call() {
        // Retrieve all Channels.
        List<Channel> channels;

        try {
            channels = QueryRequests.queryAppsRequest(commandHelper.getDeviceURL());
        } catch (IOException ex) {
            ex.printStackTrace();
            channels = new ArrayList<>();
        }

        return channels;
    }
}