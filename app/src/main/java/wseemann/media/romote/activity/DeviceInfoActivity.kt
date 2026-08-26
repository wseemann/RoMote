package wseemann.media.romote.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import wseemann.media.romote.composables.DeviceInfoScreen
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.viewmodels.DeviceInfoScreenViewModel

/**
 * Everything the device reports about itself. Reached from the device lists, either for a paired
 * device (by serial number) or one that was just discovered (by host); both arrive as Intent
 * extras, which DeviceInfoScreenViewModel reads from its SavedStateHandle.
 */
@AndroidEntryPoint
class DeviceInfoActivity : ConnectivityActivity() {

    private val deviceInfoScreenViewModel: DeviceInfoScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by deviceInfoScreenViewModel.uiState.collectAsStateWithLifecycle()

            RomoteTheme {
                DeviceInfoScreen(uiState = uiState, onBackClick = { finish() })
            }
        }
    }
}
