package wseemann.media.romote.activity;

import android.os.Bundle;

import wseemann.media.romote.R;

/**
 * Created by wseemann on 6/19/16.
 */
public class RemoteActivity extends ConnectivityActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
    }
}
