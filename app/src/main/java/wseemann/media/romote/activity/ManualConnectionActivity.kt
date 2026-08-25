package wseemann.media.romote.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import wseemann.media.romote.R
import wseemann.media.romote.utils.applyNavigationBarBottomPadding
import wseemann.media.romote.utils.applyStatusBarTopPadding

class ManualConnectionActivity : ConnectivityActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_connection)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        applyStatusBarTopPadding(findViewById(R.id.app_bar_layout))
        applyNavigationBarBottomPadding(findViewById(R.id.manual_connection_fragment))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }

            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }
}