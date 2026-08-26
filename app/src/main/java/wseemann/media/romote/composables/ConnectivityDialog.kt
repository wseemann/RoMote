package wseemann.media.romote.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.RomoteTheme

/**
 * The "no Wifi" dialog ConnectivityActivity puts up whenever the device drops off the network,
 * replacing the ConnectivityDialog DialogFragment.
 *
 * The dialog it replaced was setCancelable(false) - there is nothing to do in this app without a
 * network, so the only way out is to fix the connection - which is what the DialogProperties below
 * say. The old dialog offered "Go to settings" as a neutral button; Material 3's AlertDialog has no
 * neutral slot, so it becomes the confirm button and sits on the right of the button row instead of
 * the left.
 */
@Composable
fun ConnectivityDialog(
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        // Dismissal is the network's to decide, not the user's: ConnectivityActivity takes the
        // dialog down when wifi comes back.
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = { Text(text = stringResource(R.string.connectivity_dialog_title)) },
        text = { Text(text = stringResource(R.string.connectivity_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onOpenSettingsClick) {
                Text(text = stringResource(R.string.connectivity_dialog_button))
            }
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun ConnectivityDialogPreview() {
    RomoteTheme {
        ConnectivityDialog(onOpenSettingsClick = {})
    }
}
