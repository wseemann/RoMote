package wseemann.media.romote.activity;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.jaku.core.JakuRequest;
import com.jaku.core.KeypressKeyValues;
import com.jaku.request.KeypressRequest;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.RokuRequestTypes;
import wseemann.media.romote.utils.ShakeMonitor;

/**
 * Created by wseemann on 6/25/16.
 */
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

    private ShakeMonitor.OnShakeListener mShakeListener = new ShakeMonitor.OnShakeListener() {
        @Override
        public void onShake() {
            String url = commandHelper.getDeviceURL();

            KeypressRequest keypressRequest = new KeypressRequest(url, KeypressKeyValues.PLAY.getValue());
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
    };

    private boolean shakeEnabled() {
        return sharedPreferences.getBoolean("shake_to_pause_checkbox_preference", false);
    }
}
