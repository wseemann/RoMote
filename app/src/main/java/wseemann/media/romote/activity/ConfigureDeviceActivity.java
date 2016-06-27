package wseemann.media.romote.activity;

import android.os.Bundle;

import wseemann.media.romote.R;

/**
 * Created by wseemann on 6/20/16.
 */
public class ConfigureDeviceActivity extends ConnectivityActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_device);
    }
}
