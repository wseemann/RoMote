package wseemann.media.romote.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import wseemann.media.romote.composables.ConnectivityDialogHost
import wseemann.media.romote.composables.ManualConnectionScreen
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.viewmodels.ConnectivityViewModel
import wseemann.media.romote.viewmodels.ManualConnectionScreenViewModel

@AndroidEntryPoint
class ManualConnectionActivity : ShakeActivity() {

    private val manualConnectionScreenViewModel: ManualConnectionScreenViewModel by viewModels()
    private val connectivityViewModel: ConnectivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Backing out without pairing is the default outcome; the collector below upgrades it.
        setResult(RESULT_CANCELED)

        setContent {
            val uiState by manualConnectionScreenViewModel.uiState.collectAsStateWithLifecycle()

            RomoteTheme {
                ManualConnectionScreen(
                    uiState = uiState,
                    onEvent = manualConnectionScreenViewModel::onHandleEvent,
                    onBackClick = { finish() }
                )

                ConnectivityDialogHost(viewModel = connectivityViewModel)
            }
        }

        // Finishing is collected here rather than run from the composition, so
        // ManualConnectionScreen stays a pure function of its state.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                manualConnectionScreenViewModel.uiState.collect { state ->
                    if (state.isConnected) {
                        setResult(RESULT_OK, Intent())
                        finish()
                    }
                }
            }
        }
    }
}
