package wseemann.media.romote.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import wseemann.media.romote.composables.LicensesScreen
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.utils.enableRomoteEdgeToEdge

class LicensesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableRomoteEdgeToEdge(this)
        super.onCreate(savedInstanceState)

        setContent {
            RomoteTheme {
                LicensesScreen(onBackClick = { finish() })
            }
        }
    }
}
