package wseemann.media.romote.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import wseemann.media.romote.R
import wseemann.media.romote.composables.ChannelScreen
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.event.ChannelScreenUiEvent
import wseemann.media.romote.utils.BroadcastUtils
import wseemann.media.romote.utils.Constants
import wseemann.media.romote.viewmodels.ChannelScreenViewModel

/**
 * The channels tab: a grid of the apps installed on the connected device. Tapping a channel
 * launches it on the device.
 */
@AndroidEntryPoint
class ChannelFragment : Fragment() {

    private lateinit var channelScreenViewModel: ChannelScreenViewModel

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            channelScreenViewModel.onHandleEvent(ChannelScreenUiEvent.LoadChannelsEvent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        channelScreenViewModel = ViewModelProvider(this)[ChannelScreenViewModel::class.java]

        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)

        val intentFilter = IntentFilter().apply {
            addAction(Constants.UPDATE_DEVICE_BROADCAST)
        }
        ContextCompat.registerReceiver(
            requireActivity(),
            updateReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by channelScreenViewModel.uiState.collectAsStateWithLifecycle()

                RomoteTheme {
                    ChannelScreen(
                        uiState = uiState,
                        onEvent = ::handleEvent
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        requireActivity().unregisterReceiver(updateReceiver)
    }

    /**
     * The refresh action lives in MainActivity's toolbar rather than in this fragment's own view,
     * so it stays an options menu item instead of moving into the composable.
     */
    @Deprecated("Superseded by MenuProvider, which the rest of the app has yet to adopt")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

    /**
     * Launching a channel changes what the device is doing, so the rest of the app is told about
     * it. The broadcast comes back to [updateReceiver], which is what reloads the grid. Doing this
     * here rather than in the ViewModel keeps a Context out of the ViewModel, and keeps
     * [ChannelScreen] a pure function of its state.
     */
    private fun handleEvent(event: ChannelScreenUiEvent) {
        if (event is ChannelScreenUiEvent.ChannelClickedEvent) {
            BroadcastUtils.sendUpdateDeviceBroadcast(requireContext())
        }

        channelScreenViewModel.onHandleEvent(event)
    }

    /**
     * Called by MainActivity when this page is selected or when wifi reconnects. Nothing else
     * triggers the first load, so the fragment keeps this entry point even though StoreFragment
     * was able to drop its equivalent.
     */
    fun refresh() {
        if (channelScreenViewModel.uiState.value.channels.isEmpty()) {
            channelScreenViewModel.onHandleEvent(ChannelScreenUiEvent.LoadChannelsEvent)
        }
    }
}
