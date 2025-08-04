package wseemann.media.romote.activity

import android.os.Bundle
import android.view.MenuItem
import wseemann.media.romote.R

class ManualConnectionActivity : ConnectivityActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_connection)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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