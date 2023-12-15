package wseemann.media.romote.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wseemann.ecp.api.ResponseCallback;
import com.wseemann.ecp.core.KeyPressKeyValues;
import com.wseemann.ecp.request.KeyPressRequest;

import javax.inject.Inject;

import wseemann.media.romote.utils.CommandHelper;

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
            KeyPressKeyValues keypressKeyValues = (KeyPressKeyValues) intent.getSerializableExtra("keypress");

            new CommandServiceAsyncTask(url, keypressKeyValues).execute();
        }
    }

    private static class CommandServiceAsyncTask extends AsyncTask<Void, Void, Void> {

        private final String url;
        private final KeyPressKeyValues keypressKeyValues;

        private CommandServiceAsyncTask(String url, KeyPressKeyValues keypressKeyValues) {
            this.url = url;
            this.keypressKeyValues = keypressKeyValues;
        }

        @Override
        public Void doInBackground(Void... params) {
            performKeypress(keypressKeyValues);
            return null;
        }

        private void performKeypress(KeyPressKeyValues keypressKeyValue) {
            KeyPressRequest keypressRequest = new KeyPressRequest(url, keypressKeyValue.getValue());
            keypressRequest.sendAsync(new ResponseCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void unused) {

                }

                @Override
                public void onError(@NonNull Exception e) {

                }
            });
        }
    }
}
