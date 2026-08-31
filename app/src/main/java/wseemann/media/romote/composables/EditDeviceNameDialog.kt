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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.RomoteTheme

/**
 * Renaming a paired device, replacing the EditDeviceNameDialog DialogFragment and the single
 * EditText of dialog_fragment_edit_device_name.xml.
 *
 * The dialog it replaced wrote the new name straight to the database from its positive button, on
 * the main thread, and told the list to refresh through a listener that a configuration change
 * dropped. Both now belong to MainScreenViewModel, so this composable only reports the name the
 * user typed.
 */
@Composable
fun EditDeviceNameDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    // The field is view state, not screen state, so it lives here rather than in the UiState - see
    // ManualConnectionScreen for the same note. The caret starts past the existing name so typing
    // extends it instead of landing in front of it. Saveable because the EditText this replaced
    // kept half-typed names across a rotation, and keyed on initialName so opening the dialog for a
    // different device reseeds the field.
    var deviceNameValue by rememberSaveable(initialName, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialName, TextRange(initialName.length)))
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        // "Enter a device name:" was the builder's message, above the field rather than beside it.
        text = {
            Column {
                Text(text = stringResource(R.string.device_name_help))

                OutlinedTextField(
                    value = deviceNameValue,
                    onValueChange = { deviceNameValue = it },
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.device_name)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { onConfirm(deviceNameValue.text) },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .focusRequester(focusRequester),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(deviceNameValue.text) }) {
                Text(text = stringResource(R.string.action_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun EditDeviceNameDialogPreview() {
    RomoteTheme {
        EditDeviceNameDialog(
            initialName = "Living Room",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
