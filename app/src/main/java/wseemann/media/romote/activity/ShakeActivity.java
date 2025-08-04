package wseemann.media.romote.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.wseemann.ecp.api.ResponseCallback;
import com.wseemann.ecp.core.KeyPressKeyValues;
import com.wseemann.ecp.request.KeyPressRequest;
import java.io.UnsupportedEncodingException;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.ShakeMonitor;

@AndroidEntryPoint
public class ShakeActivity extends AppCompatActivity {

    @Inject
    protected SharedPreferences sharedPreferences;

    @Inject
    protected CommandHelper commandHelper;

    private ShakeMonitor mShakeMonitor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mShakeMonitor = new ShakeMonitor(this);
        mShakeMonitor.setOnShakeListener(mShakeListener);

        if (shakeEnabled()) {
            mShakeMonitor.resume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (shakeEnabled()) {
            mShakeMonitor.pause();
        }
    }

    private final ShakeMonitor.OnShakeListener mShakeListener = new ShakeMonitor.OnShakeListener() {
        @Override
        public void onShake() {
            String url = commandHelper.getDeviceURL();

            try {
                KeyPressRequest keyPressRequest = new KeyPressRequest(url, KeyPressKeyValues.PLAY.getValue());
                keyPressRequest.sendAsync(new ResponseCallback<>() {
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
    };

    private boolean shakeEnabled() {
        return sharedPreferences.getBoolean("shake_to_pause_checkbox_preference", false);
    }
}
