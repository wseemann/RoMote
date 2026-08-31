package wseemann.media.romote.composables

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wseemann.ecp.core.KeyPressKeyValues
import wseemann.media.romote.R
import wseemann.media.romote.activity.DeviceInfoActivity
import wseemann.media.romote.activity.ManualConnectionActivity
import wseemann.media.romote.event.ChannelScreenUiEvent
import wseemann.media.romote.event.MainScreenUiEvent
import wseemann.media.romote.event.RemoteScreenUiEvent
import wseemann.media.romote.service.RemoteAudioConnection
import wseemann.media.romote.utils.BroadcastUtils
import wseemann.media.romote.utils.Constants
import wseemann.media.romote.viewmodels.ChannelScreenViewModel
import wseemann.media.romote.viewmodels.MainScreenViewModel
import wseemann.media.romote.viewmodels.RemoteScreenViewModel
import wseemann.media.romote.viewmodels.StoreScreenViewModel

@Composable
fun DevicesTab(viewModel: MainScreenViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val manualConnectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onHandleEvent(MainScreenUiEvent.RefreshEvent)
        }
    }

    MainScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                is MainScreenUiEvent.DeviceSelectedEvent -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.device_connected, event.device.serialNumber),
                        Toast.LENGTH_SHORT,
                    ).show()

                    BroadcastUtils.sendWidgetUpdateBroadcast(context)

                    viewModel.onHandleEvent(event)
                }

                is MainScreenUiEvent.DeviceInfoClickedEvent -> {
                    context.startActivity(
                        Intent(context, DeviceInfoActivity::class.java).apply {
                            putExtra("serial_number", event.serialNumber)
                            putExtra("host", event.host)
                        },
                    )
                }

                is MainScreenUiEvent.AddDeviceClickedEvent -> {
                    manualConnectionLauncher.launch(
                        Intent(context, ManualConnectionActivity::class.java),
                    )
                }

                else -> viewModel.onHandleEvent(event)
            }
        },
        modifier = modifier,
    )
}

/**
 * The remote tab. Owns the private listening binding and the broadcast that tells the other tabs
 * the device moved on.
 *
 * [isCurrentPage] is what takes the soft keyboard down when the user swipes away. The pager keeps
 * this page composed either side of the one on screen, so without it the keyboard would stay up and
 * keep relaying what was typed on another tab.
 */
@Composable
fun RemoteTab(viewModel: RemoteScreenViewModel, isCurrentPage: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            viewModel.onHandleEvent(RemoteScreenUiEvent.KeyboardEvent.DismissedEvent)
        }
    }

    val remoteAudio = remember(context) {
        RemoteAudioConnection(context) { isActive ->
            viewModel.onHandleEvent(RemoteScreenUiEvent.PrivateListeningChangedEvent(isActive))
        }
    }

    DisposableEffect(remoteAudio) {
        onDispose { remoteAudio.release() }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                viewModel.onHandleEvent(RemoteScreenUiEvent.DeviceChangedEvent)
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Constants.UPDATE_DEVICE_BROADCAST),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        onDispose { context.unregisterReceiver(receiver) }
    }

    /**
     * Re-reads the device on the way back to the foreground. RemoteFragment refreshed the private
     * listening button here for the same reason: the user may have left to install the companion
     * app, and the screen has no other way to find out.
     */
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onHandleEvent(RemoteScreenUiEvent.DeviceChangedEvent)
    }

    // Leaving the app takes the keyboard with it, so keyboard mode has to end with it.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.onHandleEvent(RemoteScreenUiEvent.KeyboardEvent.DismissedEvent)
    }

    RemoteScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                is RemoteScreenUiEvent.KeyPressedEvent -> {
                    if (event.key in DEVICE_CHANGING_KEYS) {
                        BroadcastUtils.sendUpdateDeviceBroadcast(context)
                    }

                    viewModel.onHandleEvent(event)
                }

                is RemoteScreenUiEvent.PrivateListeningClickedEvent -> {
                    if (viewModel.isPrivateListeningInstalled()) {
                        // Handled here; only an uninstalled companion app reaches the ViewModel,
                        // which is what puts the install dialog up.
                        remoteAudio.toggle(viewModel.connectedDeviceHost())
                    } else {
                        viewModel.onHandleEvent(event)
                    }
                }

                is RemoteScreenUiEvent.InstallPrivateListeningConfirmedEvent -> {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Constants.PRIVATE_LISTENING_URL.toUri()),
                    )

                    viewModel.onHandleEvent(event)
                }

                else -> viewModel.onHandleEvent(event)
            }
        },
        modifier = modifier,
    )
}

/**
 * The channels tab: a grid of the apps installed on the connected device.
 *
 * The grid loads the first time the tab is selected, which is what MainActivity's
 * OnPageChangeListener used to drive through ChannelFragment.refresh(). The other caller of that
 * method, a reconnect, is handled by MainActivity, which reloads the grid when
 * ConnectivityViewModel reports the phone is back on a local network.
 */
@Composable
fun ChannelsTab(viewModel: ChannelScreenViewModel, isCurrentPage: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                viewModel.onHandleEvent(ChannelScreenUiEvent.DeviceChangedEvent)
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Constants.UPDATE_DEVICE_BROADCAST),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage && uiState.channels.isEmpty()) {
            viewModel.onHandleEvent(ChannelScreenUiEvent.LoadChannelsEvent)
        }
    }

    ChannelScreen(
        uiState = uiState,
        onEvent = { event ->
            /**
             * Launching a channel changes what the device is playing, and NotificationService
             * listens for this broadcast to re-query the active app and redraw the now-playing
             * notification - nothing else refreshes it while the app runs. The receiver above
             * hears it too, and ignores it: the installed app list can't have changed. Sending it
             * here rather than from the ViewModel keeps a Context out of the ViewModel, and keeps
             * [ChannelScreen] a pure function of its state.
             */
            if (event is ChannelScreenUiEvent.ChannelClickedEvent) {
                BroadcastUtils.sendUpdateDeviceBroadcast(context)
            }

            viewModel.onHandleEvent(event)
        },
        modifier = modifier,
    )
}

/**
 * The store tab. [isCurrentPage] gates the WebView's back navigation; StoreFragment had to derive
 * it from setUserVisibleHint, and the pager reports it directly.
 */
@Composable
fun StoreTab(viewModel: StoreScreenViewModel, isCurrentPage: Boolean, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StoreScreen(
        uiState = uiState,
        isCurrentPage = isCurrentPage,
        onEvent = viewModel::onHandleEvent,
        modifier = modifier,
    )
}

/** The keys that change what the device is showing, so the other tabs are told to refresh. */
private val DEVICE_CHANGING_KEYS = setOf(
    KeyPressKeyValues.BACK,
    KeyPressKeyValues.HOME,
    KeyPressKeyValues.SELECT,
)
