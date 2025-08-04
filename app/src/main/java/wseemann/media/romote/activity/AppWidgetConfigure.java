package wseemann.media.romote.activity;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import wseemann.media.romote.R;
import wseemann.media.romote.fragment.MainFragment;

/**
 * Created by wseemann on 6/25/16.
 */
public class AppWidgetConfigure extends AppCompatActivity implements MainFragment.OnDeviceSelectedListener {

    private int mAppWidgetId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appwidget_configure);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(android.R.id.content, new MainFragment()).commit();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        setResult(RESULT_CANCELED);
    }

    @Override
    public void onDeviceSelected() {
        setResult(RESULT_OK, new Intent());
        finish();
    }
}
