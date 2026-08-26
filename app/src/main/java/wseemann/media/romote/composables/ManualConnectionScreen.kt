package wseemann.media.romote.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.PurpleButton
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.event.ManualConnectionScreenUiEvent
import wseemann.media.romote.model.ManualConnectionScreenUiState

/** The 64dp the connect MaterialButton was fixed at in fragment_manual_connection.xml. */
private val ConnectButtonHeight = 64.dp

/** Matches the 14dp corner radius of @drawable/buttonshape, the button's old background. */
private val ConnectButtonCornerRadius = 14.dp

/**
 * Pairing by IP address, for a device SSDP discovery cannot see. The user types an address, the
 * ViewModel asks whatever is there to identify itself, and ManualConnectionActivity closes itself
 * once a device answers.
 */
@Composable
fun ManualConnectionScreen(
    uiState: ManualConnectionScreenUiState,
    onEvent: (ManualConnectionScreenUiEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    // The caret is view state, not screen state, so it lives here rather than in the UiState. It
    // is seeded past the "192.168.1." prefix the field starts with, so typing continues the
    // address instead of landing in front of it.
    var ipAddressValue by remember {
        mutableStateOf(TextFieldValue(uiState.ipAddress, TextRange(uiState.ipAddress.length)))
    }

    // The fragment this replaced force-showed the keyboard on entry, since typing an address is
    // the only thing there is to do here.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // The Scaffold paints colorScheme.background - the same background the activity's window
    // already has - and supplies the matching content color, which MaterialTheme alone would leave
    // black and unreadable in dark mode. Its content padding carries the navigation bar inset the
    // activity used to apply to the fragment container.
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RomoteTopAppBar(
                title = stringResource(R.string.title_connect_manually),
                onBackClick = onBackClick
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .imePadding()
        ) {
            Text(
                text = stringResource(R.string.connect_help),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )

            OutlinedTextField(
                value = ipAddressValue,
                onValueChange = {
                    ipAddressValue = it
                    onEvent(ManualConnectionScreenUiEvent.IpAddressChangedEvent(it.text))
                },
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.hasError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onEvent(ManualConnectionScreenUiEvent.ConnectClickedEvent) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .focusRequester(focusRequester)
            )

            Button(
                onClick = { onEvent(ManualConnectionScreenUiEvent.ConnectClickedEvent) },
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(ConnectButtonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleButton,
                    contentColor = Color.White
                ),
                // The padding comes before the height so the button is a full 64dp tall, the way
                // the MaterialButton's layout_height plus layout_margin used to be.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 10.dp)
                    .height(ConnectButtonHeight)
            ) {
                Text(
                    text = stringResource(R.string.connect),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (uiState.hasError) {
                Text(
                    text = stringResource(R.string.no_roku_device),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 10.dp)
                )
            }

            if (uiState.isLoading) {
                Column(modifier = Modifier.padding(top = 15.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                    Text(
                        text = stringResource(R.string.checking_ip_address),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ManualConnectionScreenPreview() {
    RomoteTheme {
        ManualConnectionScreen(
            uiState = ManualConnectionScreenUiState(),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ManualConnectionScreenLoadingPreview() {
    RomoteTheme {
        ManualConnectionScreen(
            uiState = ManualConnectionScreenUiState(ipAddress = "192.168.1.42", isLoading = true),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ManualConnectionScreenErrorPreview() {
    RomoteTheme {
        ManualConnectionScreen(
            uiState = ManualConnectionScreenUiState(ipAddress = "192.168.1.254", hasError = true),
            onEvent = {},
            onBackClick = {}
        )
    }
}
