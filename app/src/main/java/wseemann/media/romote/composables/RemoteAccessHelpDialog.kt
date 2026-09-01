package wseemann.media.romote.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.RomoteTheme

/**
 * The first-run explainer for Roku's "Control by mobile apps" setting.
 *
 * Current Roku OS ships that setting on Limited, which only lets a third-party app send text and
 * launch channels - so without this the first thing a new user sees is a remote whose buttons do
 * nothing, with nothing on screen saying why.
 *
 * Cancellable on purpose: backing out or tapping outside counts as having seen it, the same as
 * "Got it". Nothing here is a decision the app needs an answer to.
 */
@Composable
fun RemoteAccessHelpDialog(onLearnMoreClick: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.remote_access_dialog_title)) },
        // The steps are taller than the dialog on a short or landscape screen, and AlertDialog's
        // text slot does not scroll on its own - without this the buttons are pushed off the bottom.
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(text = stringResource(R.string.remote_access_dialog_message))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(text = stringResource(R.string.remote_access_dialog_step_1))
                    Text(text = stringResource(R.string.remote_access_dialog_step_2))
                    Text(text = stringResource(R.string.remote_access_dialog_step_3))
                    Text(text = stringResource(R.string.remote_access_dialog_step_4))
                    Text(text = stringResource(R.string.remote_access_dialog_step_5))
                }

                Text(
                    text = stringResource(R.string.remote_access_dialog_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.remote_access_dialog_confirm))
            }
        },
        // Reading the article is the alternative to acknowledging the steps here, so it takes the
        // dismiss slot; MainActivity closes the dialog before opening the browser.
        dismissButton = {
            TextButton(onClick = onLearnMoreClick) {
                Text(text = stringResource(R.string.remote_access_dialog_learn_more))
            }
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun RemoteAccessHelpDialogPreview() {
    RomoteTheme {
        RemoteAccessHelpDialog(
            onLearnMoreClick = {},
            onDismiss = {}
        )
    }
}
