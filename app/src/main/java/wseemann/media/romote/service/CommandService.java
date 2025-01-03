package wseemann.media.romote.service;

import java.io.UnsupportedEncodingException;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wseemann.ecp.api.ResponseCallback;
import com.wseemann.ecp.core.KeyPressKeyValues;
import com.wseemann.ecp.request.KeyPressRequest;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.utils.CommandHelper;

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
                performKeypress((KeyPressKeyValues) intent.getSerializableExtra("keypress"));
            //}
        }
    }

    private void performKeypress(KeyPressKeyValues keypressKeyValue) {
        String url = commandHelper.getDeviceURL();

        KeyPressRequest keypressRequest;
        try {
            keypressRequest = new KeyPressRequest(url, keypressKeyValue.getValue());
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return;
        }
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
