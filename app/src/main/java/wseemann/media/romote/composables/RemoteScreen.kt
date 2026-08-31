package wseemann.media.romote.composables

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wseemann.ecp.core.KeyPressKeyValues
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.event.RemoteScreenUiEvent
import wseemann.media.romote.model.RemoteScreenUiState
import wseemann.media.romote.model.RemoteScreenUiState.PrivateListening

/** @dimen/remote_view_top_title_height, the height the device name TextView was fixed at. */
private val DeviceNameHeight = 35.dp

/** @dimen/font_size_18sp. */
private val DeviceNameFontSize = 18.sp

/** The margin every button row carried between itself and the row above. */
private val RowSpacing = 10.dp

/** The power button was a fixed 52dip square rather than sharing the row's weight. */
private val PowerButtonSize = 52.dp

/** The height of the bar that rides on top of the soft keyboard. */
private val KeyboardBarHeight = 48.dp

/** @drawable/remote_button_bg's flat fill, so the keyboard bar sits on the same black as the buttons. */
private val KeyboardBarBackground = Color(0xFF151218)

/**
 * The remote tab. A pure function of [uiState] - the private listening service binding belongs to
 * RemoteTab, which intercepts those events before they get here.
 */
@Composable
fun RemoteScreen(
    uiState: RemoteScreenUiState,
    onEvent: (RemoteScreenUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val softwareKeyboard = LocalSoftwareKeyboardController.current

    // Removing the bar takes focus off its field, which is normally enough to send the IME away.
    // Asking for it explicitly covers the keyboard button, which puts the keyboard down while the
    // user is still typing into it. Only on the way out of keyboard mode, so that the tab being
    // composed does not swipe away a keyboard something else raised.
    var keyboardWasActive by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.keyboardActive) {
        if (uiState.keyboardActive) {
            keyboardWasActive = true
        } else if (keyboardWasActive) {
            softwareKeyboard?.hide()
        }
    }

    // Wake-on-LAN reports back long after the button was released, so the result arrives as a
    // one-shot message in the state rather than as a return value.
    LaunchedEffect(uiState.messageResId) {
        uiState.messageResId?.let { messageResId ->
            Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show()
            onEvent(RemoteScreenUiEvent.MessageShownEvent)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // A View background bitmap is stretched to the view's bounds, not letterboxed.
            .paint(painterResource(R.drawable.remote_bg), contentScale = ContentScale.FillBounds)
    ) {
        // The app is edge-to-edge, so keep the bottom row clear of the navigation bar.
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            Text(
                text = uiState.deviceName,
                color = Color.White,
                fontSize = DeviceNameFontSize,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DeviceNameHeight)
                    .padding(horizontal = 30.dp)
                    .padding(top = 6.dp)
            )

            RemoteButtonRow(modifier = Modifier.padding(top = RowSpacing)) {
                RemoteButton(
                    icon = R.mipmap.remote_back,
                    contentDescription = stringResource(R.string.remote_back),
                    onClick = { onEvent(keyPressed(KeyPressKeyValues.BACK)) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                RemoteButton(
                    icon = R.mipmap.remote_options,
                    contentDescription = stringResource(R.string.remote_options),
                    onClick = { onEvent(keyPressed(KeyPressKeyValues.INFO)) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                RemoteButton(
                    icon = R.mipmap.remote_home,
                    contentDescription = stringResource(R.string.remote_home),
                    onClick = { onEvent(keyPressed(KeyPressKeyValues.HOME)) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                RemoteButton(
                    icon = R.mipmap.remote_power,
                    contentDescription = stringResource(R.string.remote_power),
                    onClick = { onEvent(RemoteScreenUiEvent.PowerClickedEvent) },
                    // scaleType="center": the icon keeps its natural size in the square button.
                    contentScale = ContentScale.None,
                    modifier = Modifier.size(PowerButtonSize)
                )
            }

            DPad(
                onEvent = onEvent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .padding(top = RowSpacing)
            )

            RemoteButtonRow(modifier = Modifier.padding(top = RowSpacing)) {
                RemoteButton(
                    icon = R.mipmap.remote_rw,
                    contentDescription = stringResource(R.string.remote_rewind),
                    onClick = { onEvent(keyPressed(KeyPressKeyValues.REV)) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                RemoteButton(
                    icon = R.mipmap.remote_playpause,
                    contentDescription = stringResource(R.string.remote_play_pause),
                    onClick = { onEvent(keyPressed(KeyPressKeyValues.PLAY)) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                RemoteButton(
                    icon = R.mipmap.remote_ff,
                    contentDescription = stringResource(R.string.remote_fast_forward),
                    onClick = { onEvent(keyPressed(KeyPressKeyValues.FWD)) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            RemoteButtonRow(
                modifier = Modifier.padding(top = RowSpacing, bottom = RowSpacing)
            ) {
                RemoteButton(
                    icon = R.mipmap.remote_replay,
                    contentDescription = stringResource(R.string.remote_instant_replay),
                    onClick = { onEvent(keyPressed(KeyPressKeyValues.INTANT_REPLAY)) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                RemoteButton(
                    icon = R.mipmap.remote_keyboard_2,
                    contentDescription = stringResource(R.string.keyboard),
                    onClick = { onEvent(RemoteScreenUiEvent.KeyboardEvent.ClickedEvent) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    active = uiState.keyboardActive
                )
                RemoteButton(
                    icon = uiState.privateListening.icon(),
                    contentDescription = stringResource(R.string.remote_private_listening),
                    onClick = { onEvent(RemoteScreenUiEvent.PrivateListeningClickedEvent) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            if (uiState.showVolumeControls) {
                RemoteButtonRow(modifier = Modifier.padding(bottom = RowSpacing)) {
                    RemoteButton(
                        icon = R.mipmap.remote_vol_mute,
                        contentDescription = stringResource(R.string.remote_volume_mute),
                        onClick = { onEvent(keyPressed(KeyPressKeyValues.VOLUME_MUTE)) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    RemoteButton(
                        icon = R.mipmap.remote_vol_down,
                        contentDescription = stringResource(R.string.remote_volume_down),
                        onClick = { onEvent(keyPressed(KeyPressKeyValues.VOLUME_DOWN)) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    RemoteButton(
                        icon = R.mipmap.remote_vol_up,
                        contentDescription = stringResource(R.string.remote_volume_up),
                        onClick = { onEvent(keyPressed(KeyPressKeyValues.VOLUME_UP)) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }

        if (uiState.keyboardActive) {
            KeyboardBar(
                text = uiState.typedText,
                onEvent = onEvent,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (uiState.showPowerOffConfirmation) {
        AlertDialog(
            onDismissRequest = { onEvent(RemoteScreenUiEvent.PowerOffDismissedEvent) },
            title = { Text(text = stringResource(R.string.power_dialog_title)) },
            text = { Text(text = stringResource(R.string.power_dialog_message)) },
            confirmButton = {
                TextButton(onClick = { onEvent(RemoteScreenUiEvent.PowerOffConfirmedEvent) }) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(RemoteScreenUiEvent.PowerOffDismissedEvent) }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (uiState.showInstallPrivateListening) {
        AlertDialog(
            onDismissRequest = {
                onEvent(RemoteScreenUiEvent.InstallPrivateListeningDismissedEvent)
            },
            text = { Text(text = stringResource(R.string.download_private_listening)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(RemoteScreenUiEvent.InstallPrivateListeningConfirmedEvent)
                    }
                ) {
                    Text(text = stringResource(R.string.install_channel_dialog_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onEvent(RemoteScreenUiEvent.InstallPrivateListeningDismissedEvent)
                    }
                ) {
                    Text(text = stringResource(R.string.close))
                }
            }
        )
    }
}

/**
 * The strip that rides on top of the soft keyboard while the remote is relaying what is typed.
 *
 * It exists because there is nothing else to look at: the keys go straight to the device, and the
 * ECP gives no way to read the device's field back, so this is the only record of what was sent.
 *
 * Its text field is what holds the IME up - Android will not raise a keyboard without a focused
 * view that owns an InputConnection - and reporting the field's whole contents on every edit is
 * what [wseemann.media.romote.keyboard.KeyboardRelay] turns into key presses.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyboardBar(
    text: String,
    onEvent: (RemoteScreenUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val imeVisible = WindowInsets.isImeVisible

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // The system back gesture takes the keyboard down without touching focus, so the insets are the
    // only sign of it. The bar is composed a frame before the IME animates in, hence waiting for it
    // to have been up at least once before reading its absence as a dismissal.
    var imeWasVisible by remember { mutableStateOf(false) }

    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            imeWasVisible = true
        } else if (imeWasVisible) {
            onEvent(RemoteScreenUiEvent.KeyboardEvent.DismissedEvent)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .height(KeyboardBarHeight)
            .background(KeyboardBarBackground)
            .padding(start = 16.dp, end = 4.dp)
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = text,
                onValueChange = { onEvent(RemoteScreenUiEvent.KeyboardEvent.TextChangedEvent(it)) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                cursorBrush = SolidColor(Color.White),
                // Suggestions and auto-capitalisation would rewrite words after the fact, and every
                // rewrite costs a run of backspaces on the device. Ascii keeps the IME from
                // composing, which is the other source of text that changes under you.
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onEvent(RemoteScreenUiEvent.KeyboardEvent.DoneEvent) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    // An empty field has nothing to delete, so its backspaces produce no edit to
                    // diff. Forwarding them keeps deleting on the device, which still has text.
                    .onKeyEvent { keyEvent ->
                        val isBackspace = keyEvent.type == KeyEventType.KeyDown &&
                            keyEvent.key == Key.Backspace

                        if (isBackspace && text.isEmpty()) {
                            onEvent(RemoteScreenUiEvent.KeyboardEvent.BackspaceEvent)
                            true
                        } else {
                            false
                        }
                    }
            )

            if (text.isEmpty()) {
                Text(
                    text = stringResource(R.string.keyboard_hint),
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = { onEvent(RemoteScreenUiEvent.KeyboardEvent.BackspaceEvent) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.keyboard_backspace),
                tint = Color.White
            )
        }
    }
}

/**
 * The directional pad: the d-pad artwork with a 3x3 grid of transparent buttons on top of it. The
 * fragment used to arrange that overlap by calling bringToFront() on the button layout.
 */
@Composable
private fun DPad(
    onEvent: (RemoteScreenUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.mipmap.remote_dpad_bg),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
            DPadRow {
                Spacer(modifier = Modifier.weight(1f))
                DPadButton(
                    onClick = { onEvent(keyPressed(KeyPressKeyValues.UP)) },
                    repeating = true,
                    contentDescription = stringResource(R.string.remote_up),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            DPadRow {
                DPadButton(
                    onClick = { onEvent(keyPressed(KeyPressKeyValues.LEFT)) },
                    repeating = true,
                    contentDescription = stringResource(R.string.remote_left),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                DPadButton(
                    onClick = { onEvent(keyPressed(KeyPressKeyValues.SELECT)) },
                    repeating = false,
                    contentDescription = stringResource(R.string.remote_select),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                DPadButton(
                    onClick = { onEvent(keyPressed(KeyPressKeyValues.RIGHT)) },
                    repeating = true,
                    contentDescription = stringResource(R.string.remote_right),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            DPadRow {
                Spacer(modifier = Modifier.weight(1f))
                DPadButton(
                    onClick = { onEvent(keyPressed(KeyPressKeyValues.DOWN)) },
                    repeating = true,
                    contentDescription = stringResource(R.string.remote_down),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ColumnScope.DPadRow(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        content = content
    )
}

private fun keyPressed(key: KeyPressKeyValues) = RemoteScreenUiEvent.KeyPressedEvent(key)

private fun PrivateListening.icon(): Int = when (this) {
    PrivateListening.UNAVAILABLE -> R.mipmap.remote_private_listening_unavailable
    PrivateListening.AVAILABLE -> R.mipmap.remote_private_listening_available
    PrivateListening.ACTIVE -> R.mipmap.remote_private_listening_on
}

@Preview(showBackground = true)
@Composable
private fun RemoteScreenPreview() {
    RomoteTheme {
        RemoteScreen(
            uiState = RemoteScreenUiState(
                deviceName = "Living Room TV",
                privateListening = PrivateListening.AVAILABLE
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RemoteScreenStickPreview() {
    RomoteTheme {
        RemoteScreen(
            uiState = RemoteScreenUiState(
                deviceName = "Bedroom Stick",
                showVolumeControls = false,
                privateListening = PrivateListening.ACTIVE
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RemoteScreenKeyboardPreview() {
    RomoteTheme {
        RemoteScreen(
            uiState = RemoteScreenUiState(
                deviceName = "Living Room TV",
                keyboardActive = true,
                typedText = "breaking bad"
            ),
            onEvent = {}
        )
    }
}
