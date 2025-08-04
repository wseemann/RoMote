package wseemann.media.romote.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
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

            try {
                KeyPressRequest keypressRequest = new KeyPressRequest(url, keypressKeyValues.getValue());
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
}
