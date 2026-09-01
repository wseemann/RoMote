package wseemann.media.romote.composables

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import wseemann.media.romote.event.ConnectivityUiEvent
import wseemann.media.romote.viewmodels.ConnectivityViewModel

@Composable
fun ConnectivityDialogHost(viewModel: ConnectivityViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (uiState.isDialogVisible) {
        ConnectivityDialog(
            onOpenSettingsClick = {
                // The dialog dismissed itself when its button was tapped; keep that.
                viewModel.onHandleEvent(ConnectivityUiEvent.DismissedEvent)
                context.openWifiSettings()
            },
            onDismiss = { viewModel.onHandleEvent(ConnectivityUiEvent.DismissedEvent) },
            modifier = modifier
        )
    }
}

private fun Context.openWifiSettings() {
    try {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    } catch (ignored: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
