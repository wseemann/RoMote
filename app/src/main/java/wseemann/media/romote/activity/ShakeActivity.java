package wseemann.media.romote.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import wseemann.media.romote.service.CommandService;
import wseemann.media.romote.utils.CommandConstants;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.ShakeMonitor;

/**
 * Created by wseemann on 6/25/16.
 */
public class ShakeActivity extends AppCompatActivity {

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
            Intent intent = new Intent(ShakeActivity.this, CommandService.class);
            intent.setAction(CommandHelper.getKeypressURL(ShakeActivity.this, CommandConstants.PLAY_COMMAND));
            ShakeActivity.this.startService(intent);
        }
    };

    private boolean shakeEnabled() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean("shake_to_pause_checkbox_preference", false);
    }
}
