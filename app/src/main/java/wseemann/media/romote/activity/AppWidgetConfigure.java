package wseemann.media.romote.activity;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.fragment.MainFragment;
import wseemann.media.romote.utils.WindowInsetsUtils;

/**
 * Created by wseemann on 6/25/16.
 */
@AndroidEntryPoint
public class AppWidgetConfigure extends AppCompatActivity implements MainFragment.OnDeviceSelectedListener {

    private int mAppWidgetId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This Activity extends AppCompatActivity directly, so it can't inherit the
        // edge-to-edge setup ShakeActivity does for the rest of the app.
        WindowInsetsUtils.enableRomoteEdgeToEdge(this);

        setContentView(R.layout.activity_appwidget_configure);

        setSupportActionBar(findViewById(R.id.toolbar));
        WindowInsetsUtils.applyStatusBarTopPadding(findViewById(R.id.app_bar_layout));

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // MainFragment applies the navigation bar inset to its own root.
        transaction.add(R.id.content, new MainFragment()).commit();

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
