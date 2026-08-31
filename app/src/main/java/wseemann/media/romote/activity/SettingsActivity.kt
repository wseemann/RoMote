package wseemann.media.romote.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import wseemann.media.romote.composables.SettingsScreen
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.utils.Constants
import wseemann.media.romote.utils.openPlayStoreListing
import wseemann.media.romote.utils.enableRomoteEdgeToEdge
import wseemann.media.romote.viewmodels.SettingsScreenViewModel
import androidx.core.net.toUri

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    private val settingsScreenViewModel: SettingsScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableRomoteEdgeToEdge(this)
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by settingsScreenViewModel.uiState.collectAsStateWithLifecycle()

            RomoteTheme {
                SettingsScreen(
                    uiState = uiState,
                    onEvent = settingsScreenViewModel::onHandleEvent,
                    onLicensesClick = {
                        startActivity(Intent(this, LicensesActivity::class.java))
                    },
                    onRateClick = { openPlayStoreListing() },
                    onDonateClick = {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, Constants.PAYPAL_DONATION_LINK.toUri())
                        )
                    },
                    onBackClick = { finish() }
                )
            }
        }
    }
}
