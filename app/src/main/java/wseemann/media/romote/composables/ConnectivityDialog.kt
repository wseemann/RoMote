package wseemann.media.romote.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.RomoteTheme

/**
 * The "no local network" dialog, shown whenever this phone has no transport a Roku could answer on.
 *
 * The dialog this grew out of was setCancelable(false), on the reasoning that there is nothing to
 * do in the app without a network. It is dismissible now: the check behind it used to insist on
 * wifi specifically and trapped anyone who was hardwired, and while the rule has been fixed, a
 * wrong answer should cost a dialog rather than the app.
 */
@Composable
fun ConnectivityDialog(onOpenSettingsClick: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.connectivity_dialog_title)) },
        text = { Text(text = stringResource(R.string.connectivity_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onOpenSettingsClick) {
                Text(text = stringResource(R.string.connectivity_dialog_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.connectivity_dialog_dismiss_button))
            }
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun ConnectivityDialogPreview() {
    RomoteTheme {
        ConnectivityDialog(onOpenSettingsClick = {}, onDismiss = {})
    }
}
