package wseemann.media.romote.composables

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

/**
 * The remote tab. A pure function of [uiState] - the private listening service binding and the
 * keyboard dialog belong to RemoteFragment, which intercepts those events before they get here.
 */
@Composable
fun RemoteScreen(
    uiState: RemoteScreenUiState,
    onEvent: (RemoteScreenUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                    onClick = { onEvent(RemoteScreenUiEvent.KeyboardClickedEvent) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
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
