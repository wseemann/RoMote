package wseemann.media.romote.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

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
 * Created by wseemann on 6/19/16.
 */
@AndroidEntryPoint
public class CommandService extends IntentService {

    private static final String TAG = CommandService.class.getName();

    @Inject
    protected CommandHelper commandHelper;

    public CommandService() {
        super(CommandService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent called");

        if (intent != null) {
            //if (intent.getAction() != null) {
                //Log.d(TAG, "onHandleIntent: " + intent.getAction());
                performKeypress((KeypressKeyValues) intent.getSerializableExtra("keypress"));
            //}
        }
    }

    private void performKeypress(KeypressKeyValues keypressKeyValue) {
        String url = commandHelper.getDeviceURL();

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
