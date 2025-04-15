package wseemann.media.romote.receiver;

import java.io.UnsupportedEncodingException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wseemann.ecp.api.ResponseCallback;
import com.wseemann.ecp.core.KeyPressKeyValues;
import com.wseemann.ecp.request.KeyPressRequest;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.utils.CommandHelper;

/**
 * Created by wseemann on 4/14/18.
 */
@AndroidEntryPoint
public class CommandReceiver extends BroadcastReceiver {

    @Inject
    protected CommandHelper commandHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String url = commandHelper.getDeviceURL();
            KeyPressKeyValues keypressKeyValues = (KeyPressKeyValues) intent.getSerializableExtra("keypress");

            if (keypressKeyValues == null) {
                return;
            }

            KeyPressRequest keypressRequest;
            try {
                keypressRequest = new KeyPressRequest(url, keypressKeyValues.getValue());
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
                return;
            }

            keypressRequest.sendAsync(new ResponseCallback<>() {
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
