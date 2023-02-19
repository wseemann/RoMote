package wseemann.media.romote.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.jaku.core.JakuRequest;
import com.jaku.core.KeypressKeyValues;
import com.jaku.request.KeypressRequest;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.RokuRequestTypes;

/**
 * Created by wseemann on 4/14/18.
 */
public class CommandReceiver extends BroadcastReceiver {

    @Inject
    protected CommandHelper commandHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String url = commandHelper.getDeviceURL();
            KeypressKeyValues keypressKeyValues = (KeypressKeyValues) intent.getSerializableExtra("keypress");

            new CommandServiceAsyncTask(url, keypressKeyValues).execute();
        }
    }

    private static class CommandServiceAsyncTask extends AsyncTask<Void, Void, Void> {

        private String url;
        private KeypressKeyValues keypressKeyValues;

        private CommandServiceAsyncTask(String url, KeypressKeyValues keypressKeyValues) {
            this.url = url;
            this.keypressKeyValues = keypressKeyValues;
        }

        @Override
        public Void doInBackground(Void... params) {
            performKeypress(keypressKeyValues);
            return null;
        }

        private void performKeypress(KeypressKeyValues keypressKeyValue) {
            KeypressRequest keypressRequest = new KeypressRequest(url, keypressKeyValue.getValue());
            JakuRequest request = new JakuRequest(keypressRequest, null);

            new RequestTask(request, new RequestCallback() {
                @Override
                public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {

                }

                @Override
                public void onErrorResponse(RequestTask.Result result) {

                }
            }).execute(RokuRequestTypes.keypress);
        }
    }
}
