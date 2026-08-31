package wseemann.media.romote.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.composables.theme.SemiTransparentBlack
import wseemann.media.romote.data.Device
import wseemann.media.romote.event.MainScreenUiEvent
import wseemann.media.romote.model.MainScreenUiState

/** The 50dip the progress header was fixed at in fragment_main.xml. */
private val ProgressHeaderHeight = 50.dp

/** @android:style/TextAppearance.Medium, the device name's size in device.xml. */
private val DeviceNameFontSize = 18.sp

/** @android:style/TextAppearance.Small, the two lines under the device name. */
private val DeviceDetailFontSize = 14.sp

/** The 100dip square the device icon sat in, which is what gave a row its height. */
private val DeviceIconBoxSize = 100.dp

/** The 70dip circle drawn inside it. */
private val DeviceIconSize = 70.dp

/** @dimen/fab_margin. */
private val FabMargin = 16.dp

/** Keeps the last row clear of the floating action button, which draws on top of the list. */
private val ListBottomInset = 88.dp

/**
 * The devices tab: the paired devices, then whatever else answered the last scan. Tapping a device
 * connects to it, and each row's overflow menu renames, describes or unpairs it.
 *
 * A pure function of [uiState] - starting DeviceInfoActivity and ManualConnectionActivity needs a
 * Context, so MainFragment intercepts those two events before they get here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainScreenUiState,
    onEvent: (MainScreenUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Surface paints colorScheme.background - the same background the activity's window already
    // has - and supplies the matching content color, which MaterialTheme alone would leave black
    // and unreadable in dark mode.
    Surface(modifier = modifier.fillMaxSize()) {
        // The app is edge-to-edge, so keep the list and the button clear of the navigation bar.
        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    DiscoveringDevicesHeader()
                }

                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { onEvent(MainScreenUiEvent.RefreshEvent) },
                    modifier = Modifier.weight(1f)
                ) {
                    DeviceList(uiState = uiState, onEvent = onEvent)
                }
            }

            FloatingActionButton(
                onClick = { onEvent(MainScreenUiEvent.AddDeviceClickedEvent) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(FabMargin)
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_input_add),
                    contentDescription = stringResource(R.string.connect_manually)
                )
            }
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
        if (isEmpty) {
            // The section headers are dropped while there is nothing under either of them, which
            // is what the ListView's empty view was for - it never actually showed, because the
            // adapter counted its two headers and so was never empty.
            if (!uiState.isLoading) {
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
            }

            return@LazyColumn
        }

        deviceSection(
            title = R.string.paired_devices,
            devices = uiState.pairedDevices,
            isPaired = true,
            connectedSerialNumber = uiState.connectedSerialNumber,
            onEvent = onEvent
        )

        deviceSection(
            title = R.string.available_devices,
            devices = uiState.availableDevices,
            isPaired = false,
            connectedSerialNumber = uiState.connectedSerialNumber,
            onEvent = onEvent
        )

        item {
            Spacer(modifier = Modifier.height(ListBottomInset))
        }
    }
}

/**
 * One of the two sections the SeparatedListAdapter used to stitch together. The header stays put
 * when the section is empty, the way that adapter drew it.
 */
private fun LazyListScope.deviceSection(
    @StringRes title: Int,
    devices: ImmutableList<Device>,
    isPaired: Boolean,
    connectedSerialNumber: String?,
    onEvent: (MainScreenUiEvent) -> Unit
) {
    item(key = "header-$title") {
        SectionHeader(title = stringResource(title))
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

/**
 * ?android:attr/listSeparatorTextViewStyle, which list_item_header.xml was styled with: Body2 in
 * ?android:attr/textColorSecondary - a muted near-white in the night theme, dark grey in the day
 * one - above a divider. onSurfaceVariant is the Material 3 role that plays the same part.
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
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
private fun DiscoveringDevicesHeader(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().height(ProgressHeaderHeight)
    ) {
        Text(
            text = stringResource(R.string.discovering_devices),
            fontSize = DeviceNameFontSize
        )

        CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp).size(24.dp))
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
    // The serial number is the device's identity everywhere the app stores or looks one up. It is
    // nullable on the model the discovery library returns, but a device the list can draw has one.
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
            Box(
                modifier = Modifier
                    .size(DeviceIconSize)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            SemiTransparentBlack
                        }
                    )
            )
        }

        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            // No line limit: the TextView this replaced had none either, and the row is as tall as
            // the icon beside it, so a long name wraps rather than being cut off.
            Text(
                text = device.displayName(),
                fontSize = DeviceNameFontSize
            )

            Text(
                text = stringResource(R.string.serial_number, serialNumber),
                fontSize = DeviceDetailFontSize
            )

            Text(
                text = stringResource(
                    if (isConnected) R.string.connected else R.string.not_connected
                ),
                fontSize = DeviceDetailFontSize
            )
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

/**
 * The PopupMenu the row's overflow button used to open through a Handler. Unpairing is only
 * offered for a device that is actually paired, as the popup was doing by removing the item.
 */
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
                painter = painterResource(R.drawable.ic_more_vert),
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

/**
 * What the row calls the device: the name the user gave it, or the model name qualified by the
 * name the device reports for itself. Carried over from DeviceAdapter.
 */
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
