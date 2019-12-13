package wseemann.media.romote.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.fragment.app.FragmentTransaction;

import wseemann.media.romote.R;
import wseemann.media.romote.fragment.DeviceInfoFragment;

/**
 * Created by wseemann on 6/19/16.
 */
public class DeviceInfoActivity extends ConnectivityActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deviceinfo);

        String serialNumber = getIntent().getStringExtra("serial_number");
        String host = getIntent().getStringExtra("host");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(android.R.id.content, DeviceInfoFragment.getInstance(serialNumber, host)).commit();
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
