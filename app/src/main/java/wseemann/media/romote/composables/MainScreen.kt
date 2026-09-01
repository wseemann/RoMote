package wseemann.media.romote.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import timber.log.Timber
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.DeviceIconBackground
import wseemann.media.romote.composables.theme.Purple
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.composables.theme.SemiTransparentBlack
import wseemann.media.romote.data.Device
import wseemann.media.romote.event.MainScreenUiEvent
import wseemann.media.romote.model.MainScreenUiState

private val DeviceNameFontSize = 18.sp
private val DeviceDetailFontSize = 14.sp
private val ConnectedDotSize = 8.dp
private val ConnectedDotGap = 6.dp
private val DeviceIconBoxSize = 100.dp
private val DeviceIconSize = 70.dp

/**
 * The frame a device's own picture is drawn in.
 *
 * Roku composes that art on a wide canvas with a lot of transparent padding - a Streambar SE is a
 * 278x72 device sitting in a 360x219 image - so the frame is the shape of the canvas rather than a
 * square. Fitting a 1.6:1 image into a circle either cut the ends off the device or left it a
 * sliver in the middle of the disc.
 */
private val DeviceImageWidth = 84.dp
private val DeviceImageHeight = 52.dp
private val DeviceImageCornerRadius = 10.dp

private val FabMargin = 16.dp

/**
 * A pure function of [uiState] - starting DeviceInfoActivity and ManualConnectionActivity needs a
 * Context, so DevicesTab intercepts those two events before they get here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(uiState: MainScreenUiState, onEvent: (MainScreenUiEvent) -> Unit, modifier: Modifier = Modifier) {
    // No background of its own: MainActivity's Scaffold already paints colorScheme.background. A
    // Surface here would paint colorScheme.surface over it - the one role RomoteTheme leaves at the
    // Material 3 default - so the devices tab came out a shade off the channels grid beside it.
    Box(modifier = modifier
        .fillMaxSize()
        .navigationBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { onEvent(MainScreenUiEvent.RefreshEvent) },
                // The LinearProgressIndicator below is the screen's only loading affordance; the
                // two of them drew at once on a cold start.
                indicator = {},
                modifier = Modifier.weight(1f)
            ) {
                DeviceList(uiState = uiState, onEvent = onEvent)
            }
        }

        FloatingActionButton(
            onClick = { onEvent(MainScreenUiEvent.AddDeviceClickedEvent) },
            containerColor = Purple,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(FabMargin)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.connect_manually)
            )
        }

        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }
    }

    uiState.renameTarget?.let { target ->
        EditDeviceNameDialog(
            initialName = target.currentName,
            onConfirm = { onEvent(MainScreenUiEvent.RenameDeviceConfirmedEvent(it)) },
            onDismiss = { onEvent(MainScreenUiEvent.RenameDeviceDismissedEvent) }
        )
    }
}

@Composable
private fun DeviceList(
    uiState: MainScreenUiState,
    onEvent: (MainScreenUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEmpty = uiState.pairedDevices.isEmpty() && uiState.availableDevices.isEmpty()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (isEmpty && !uiState.isLoading) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    // Fills the viewport so the pull gesture still has somewhere to travel.
                    modifier = Modifier.fillParentMaxSize()
                ) {
                    Text(
                        text = stringResource(R.string.empty_list),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            return@LazyColumn
        }

        // A "Paired devices" header over an empty space reads as a section that failed to load
        // rather than one the user has not filled yet.
        if (uiState.pairedDevices.isNotEmpty()) {
            deviceSection(
                title = R.string.paired_devices,
                devices = uiState.pairedDevices,
                isPaired = true,
                connectedSerialNumber = uiState.connectedSerialNumber,
                onEvent = onEvent
            )
        }

        deviceSection(
            title = R.string.available_devices,
            devices = uiState.availableDevices,
            isPaired = false,
            connectedSerialNumber = uiState.connectedSerialNumber,
            onEvent = onEvent
        )
    }
}

/** The header is drawn whenever the section is, so a caller with nothing to show skips the call. */
private fun LazyListScope.deviceSection(
    @StringRes title: Int,
    devices: ImmutableList<Device>,
    isPaired: Boolean,
    connectedSerialNumber: String?,
    onEvent: (MainScreenUiEvent) -> Unit
) {
    item(key = "header-$title") {
        SectionHeader(
            title = stringResource(title),
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
        )
    }

    items(devices, key = { device -> "$isPaired-${device.serialNumber}" }) { device ->
        DeviceRow(
            device = device,
            isPaired = isPaired,
            isConnected = device.serialNumber == connectedSerialNumber,
            onEvent = onEvent
        )
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 5.dp, top = 2.dp, bottom = 2.dp)
        )

        HorizontalDivider()
    }
}

@Composable
private fun DeviceRow(
    device: Device,
    isPaired: Boolean,
    isConnected: Boolean,
    onEvent: (MainScreenUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Nullable on the model the discovery library returns, but a device the list can draw has one.
    val serialNumber = device.serialNumber.orEmpty()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEvent(MainScreenUiEvent.DeviceSelectedEvent(device)) }
            .padding(1.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(DeviceIconBoxSize)
        ) {
            val deviceImageUrl = device.deviceImageUrl

            if (!deviceImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = deviceImageUrl,
                    // Decorative: displayName() is read out immediately to its right.
                    contentDescription = null,
                    // Fit, never Crop: the art is mostly transparent padding, so cropping it to
                    // fill a square scales past the device and takes its ends off.
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(width = DeviceImageWidth, height = DeviceImageHeight)
                        .clip(RoundedCornerShape(DeviceImageCornerRadius))
                        .background(DeviceIconBackground)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(DeviceIconSize)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) {
                                Purple
                            } else {
                                SemiTransparentBlack
                            }
                        )
                )
            }
        }

        Column(modifier = Modifier
            .weight(1f)
            .padding(start = 10.dp)) {
            Text(
                text = device.displayName(),
                fontSize = DeviceNameFontSize
            )

            Text(
                text = stringResource(R.string.serial_number, serialNumber),
                fontSize = DeviceDetailFontSize
            )

            // The disc beside the row only marks the connected device when that device publishes
            // no picture of itself, so the dot is the marker that survives either branch.
            // Decorative - the word next to it is what a screen reader reads out.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isConnected) {
                    Box(
                        modifier = Modifier
                            .size(ConnectedDotSize)
                            .clip(CircleShape)
                            .background(Purple)
                    )

                    Spacer(modifier = Modifier.width(ConnectedDotGap))
                }

                Text(
                    text = stringResource(
                        if (isConnected) R.string.connected else R.string.not_connected
                    ),
                    fontSize = DeviceDetailFontSize
                )
            }
        }

        DeviceOverflowMenu(
            device = device,
            serialNumber = serialNumber,
            isPaired = isPaired,
            onEvent = onEvent,
            modifier = Modifier.padding(end = 12.dp)
        )
    }
}

@Composable
private fun DeviceOverflowMenu(
    device: Device,
    serialNumber: String,
    isPaired: Boolean,
    onEvent: (MainScreenUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { isExpanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.device_options_content_description)
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_rename)) },
                onClick = {
                    isExpanded = false
                    onEvent(
                        MainScreenUiEvent.RenameDeviceClickedEvent(
                            serialNumber = serialNumber,
                            currentName = device.getCustomUserDeviceName().orEmpty()
                        )
                    )
                }
            )

            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_info)) },
                onClick = {
                    isExpanded = false
                    onEvent(
                        MainScreenUiEvent.DeviceInfoClickedEvent(
                            serialNumber = serialNumber,
                            host = device.host.orEmpty()
                        )
                    )
                }
            )

            if (isPaired) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.action_unpair)) },
                    onClick = {
                        isExpanded = false
                        onEvent(MainScreenUiEvent.ForgetDeviceEvent(serialNumber))
                    }
                )
            }
        }
    }
}

private fun Device.displayName(): String {
    val customName = getCustomUserDeviceName()

    if (!customName.isNullOrEmpty()) {
        return customName
    }

    val friendlyName = userDeviceName

    return if (!friendlyName.isNullOrEmpty()) {
        "$friendlyName ($modelName)"
    } else {
        modelName.orEmpty()
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    RomoteTheme {
        MainScreen(
            uiState = MainScreenUiState(
                pairedDevices = persistentListOf(
                    previewDevice(serialNumber = "X0055FR4M4NG", userDeviceName = "Living room")
                ),
                availableDevices = persistentListOf(
                    previewDevice(serialNumber = "X0055FR4M4NH", userDeviceName = "Bedroom")
                ),
                connectedSerialNumber = "X0055FR4M4NG"
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenLoadingPreview() {
    RomoteTheme {
        MainScreen(
            uiState = MainScreenUiState(isLoading = true),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenEmptyPreview() {
    RomoteTheme {
        MainScreen(
            uiState = MainScreenUiState(),
            onEvent = {}
        )
    }
}

private fun previewDevice(serialNumber: String, userDeviceName: String) = Device().apply {
    this.serialNumber = serialNumber
    this.userDeviceName = userDeviceName
    this.modelName = "Roku Ultra"
    this.host = "192.168.1.100"
}
