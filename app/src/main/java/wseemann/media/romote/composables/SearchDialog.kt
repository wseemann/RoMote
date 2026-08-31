package wseemann.media.romote.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.RomoteTheme

/**
 * Searching the paired Roku, replacing the SearchDialog DialogFragment and
 * dialog_fragment_search.xml.
 *
 * The dialog it replaced reached its caller through a listener held in a static field on the
 * companion object, set by casting the Activity in newInstance(). That reference outlived the
 * fragment and was never re-established after the system recreated the dialog, so "Go" could fire
 * against a stale activity. This composable only reports the text the user typed; MainActivity
 * decides what to do with it.
 *
 * Its two MaterialButtons were tinted #a865f3 by hand; as AlertDialog text buttons they take their
 * color from RomoteTheme instead.
 */
@Composable
fun SearchDialog(
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    // The field is view state, not screen state, so it lives here rather than in a UiState - see
    // EditDeviceNameDialog for the same note. Saveable because the EditText this replaced kept a
    // half-typed query across a rotation.
    var searchValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.action_search)) },
        // "Enter your search criteria:" was the builder's message. The old dialog had that line
        // commented out, leaving a bare field; it is restored here, above the field, the way
        // EditDeviceNameDialog shows device_name_help.
        text = {
            Column {
                Text(text = stringResource(R.string.search_help))

                OutlinedTextField(
                    value = searchValue,
                    onValueChange = { searchValue = it },
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.action_search)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { onSearch(searchValue.text) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSearch(searchValue.text) }) {
                Text(text = stringResource(R.string.go))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun SearchDialogPreview() {
    RomoteTheme {
        SearchDialog(
            onSearch = {},
            onDismiss = {}
        )
    }
}
