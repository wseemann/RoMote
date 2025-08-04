package wseemann.media.romote.service;

import android.app.IntentService;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wseemann.ecp.api.ResponseCallback;
import com.wseemann.ecp.core.KeyPressKeyValues;
import com.wseemann.ecp.request.KeyPressRequest;

import java.io.UnsupportedEncodingException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;
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
        Timber.tag(TAG).d("onHandleIntent called");

        if (intent != null) {
            performKeypress((KeyPressKeyValues) intent.getSerializableExtra("keypress"));
        }
    }

    private void performKeypress(KeyPressKeyValues keypressKeyValue) {
        String url = commandHelper.getDeviceURL();

        try {
            KeyPressRequest keypressRequest = new KeyPressRequest(url, keypressKeyValue.getValue());
            keypressRequest.sendAsync(new ResponseCallback<>() {
                @Override
                public void onSuccess(@Nullable Void unused) {

                }

                @Override
                public void onError(@NonNull Exception e) {

                }
            });
        } catch (UnsupportedEncodingException ex) {
            Timber.e(ex, "Failed to execute command");
        }
    }
}
