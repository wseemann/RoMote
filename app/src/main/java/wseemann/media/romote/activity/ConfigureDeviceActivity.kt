package wseemann.media.romote.activity

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import wseemann.media.romote.R
import wseemann.media.romote.utils.applyNavigationBarBottomPadding
import wseemann.media.romote.utils.applyStatusBarTopPadding

class ConfigureDeviceActivity : ConnectivityActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure_device)

        setSupportActionBar(findViewById(R.id.toolbar))
        applyStatusBarTopPadding(findViewById(R.id.app_bar_layout))
        applyNavigationBarBottomPadding(findViewById(R.id.configure_device_fragment))
    }
}