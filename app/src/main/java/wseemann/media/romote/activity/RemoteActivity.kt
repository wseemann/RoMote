package wseemann.media.romote.activity

import android.os.Bundle
import wseemann.media.romote.R

/**
 * Created by wseemann on 6/19/16.
 */
class RemoteActivity : ConnectivityActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote)
    }
}