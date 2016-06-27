package wseemann.media.romote.activity;

import android.os.Bundle;
import android.view.MenuItem;

import wseemann.media.romote.R;

/**
 * Created by wseemann on 6/19/16.
 */
public class ManualConnectionActivity extends ConnectivityActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_connection);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
