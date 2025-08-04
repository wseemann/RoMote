package wseemann.media.romote.activity

import android.os.Bundle
import android.view.MenuItem
import wseemann.media.romote.R
import wseemann.media.romote.fragment.DeviceInfoFragment

class DeviceInfoActivity : ConnectivityActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deviceinfo)

        val serialNumber = intent.getStringExtra("serial_number")
        val host = intent.getStringExtra("host")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.content, DeviceInfoFragment.getInstance(serialNumber, host))
            .commit()
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