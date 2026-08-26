package wseemann.media.romote.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import dagger.hilt.android.AndroidEntryPoint
import wseemann.media.romote.R
import wseemann.media.romote.fragment.SettingsFragment
import wseemann.media.romote.utils.applyNavigationBarBottomPadding
import wseemann.media.romote.utils.applyStatusBarTopPadding
import wseemann.media.romote.utils.enableRomoteEdgeToEdge

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This Activity extends AppCompatActivity directly, so it can't inherit the
        // edge-to-edge setup ShakeActivity does for the rest of the app.
        enableRomoteEdgeToEdge(this)

        setContentView(R.layout.activity_settings)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        applyStatusBarTopPadding(findViewById(R.id.app_bar_layout))
        applyNavigationBarBottomPadding(findViewById(R.id.content))

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.content, SettingsFragment()).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}