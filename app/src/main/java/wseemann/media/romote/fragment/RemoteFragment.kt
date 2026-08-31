package wseemann.media.romote.fragment

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wseemann.ecp.core.KeyPressKeyValues
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import wseemann.media.romote.audio.IRemoteAudioInterface
import wseemann.media.romote.composables.RemoteScreen
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.event.RemoteScreenUiEvent
import wseemann.media.romote.utils.BroadcastUtils
import wseemann.media.romote.utils.Constants
import wseemann.media.romote.utils.PreferenceUtils
import wseemann.media.romote.viewmodels.RemoteScreenViewModel
import javax.inject.Inject

/**
 * The remote tab. [RemoteScreen] draws it and [RemoteScreenViewModel] holds its state; what stays
 * here is the work that needs a Context or a FragmentManager - the private listening service
 * binding, the keyboard dialog, and the broadcast that tells the other tabs the device moved on.
 */
@AndroidEntryPoint
class RemoteFragment : Fragment() {

    @Inject
    lateinit var preferenceUtils: PreferenceUtils

    private lateinit var remoteScreenViewModel: RemoteScreenViewModel

    /** The primary interface we will be calling on the service. */
    private var remoteAudioService: IRemoteAudioInterface? = null

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            remoteScreenViewModel.onHandleEvent(RemoteScreenUiEvent.DeviceChangedEvent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        remoteScreenViewModel = ViewModelProvider(this)[RemoteScreenViewModel::class.java]

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
                val uiState by remoteScreenViewModel.uiState.collectAsStateWithLifecycle()

                RomoteTheme {
                    RemoteScreen(
                        uiState = uiState,
                        onEvent = ::handleEvent
                    )
                }
            }
        }
    }

    /**
     * Re-reads the device on the way back to the foreground. The old fragment refreshed the private
     * listening button here for the same reason: the user may have left to install the companion
     * app, and the screen has no other way to find out.
     */
    override fun onResume() {
        super.onResume()
        remoteScreenViewModel.onHandleEvent(RemoteScreenUiEvent.DeviceChangedEvent)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(updateReceiver)
    }

    /**
     * Intercepts the events that can't be served from the ViewModel, the way ChannelFragment does,
     * and forwards everything else untouched.
     */
    private fun handleEvent(event: RemoteScreenUiEvent) {
        when (event) {
            is RemoteScreenUiEvent.KeyPressedEvent -> {
                if (event.key in DEVICE_CHANGING_KEYS) {
                    BroadcastUtils.sendUpdateDeviceBroadcast(requireContext())
                }
            }

            is RemoteScreenUiEvent.KeyboardClickedEvent -> {
                TextInputDialog().show(parentFragmentManager, TextInputDialog::class.java.name)
                // The ViewModel has nothing to do with this one.
                return
            }

            is RemoteScreenUiEvent.PrivateListeningClickedEvent -> {
                if (onPrivateListeningClicked()) {
                    // Handled here; only an uninstalled companion app reaches the ViewModel, which
                    // is what puts the install dialog up.
                    return
                }
            }

            is RemoteScreenUiEvent.InstallPrivateListeningConfirmedEvent -> {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Constants.PRIVATE_LISTENING_URL.toUri())
                )
            }

            else -> Unit
        }

        remoteScreenViewModel.onHandleEvent(event)
    }

    /**
     * Toggles private listening, binding the service first if it isn't already. Returns false when
     * the companion app isn't installed, which is the one case the ViewModel handles.
     */
    private fun onPrivateListeningClicked(): Boolean {
        if (!remoteScreenViewModel.isPrivateListeningInstalled()) {
            return false
        }

        val service = remoteAudioService

        if (service == null) {
            bindToRemoteAudio()
            return true
        }

        try {
            service.toggleRemoteAudio()
            reportPrivateListeningState()
        } catch (ex: RemoteException) {
            Timber.tag(TAG).e(ex, "Failed to toggle private listening")
        }

        return true
    }

    private fun bindToRemoteAudio() {
        val intent = Intent().apply { component = REMOTE_AUDIO_COMPONENT }

        try {
            requireContext().bindService(intent, remoteAudioConnection, Context.BIND_AUTO_CREATE)
        } catch (ex: SecurityException) {
            Timber.tag(TAG).e(ex, "Failed to start private listening service")
        }
    }

    /** Tells the ViewModel what the service is doing, which is what picks the button's icon. */
    private fun reportPrivateListeningState() {
        val isActive = try {
            remoteAudioService?.isRemoteAudioActive == true
        } catch (ex: RemoteException) {
            Timber.tag(TAG).e(ex, "Failed to read the private listening state")
            false
        }

        remoteScreenViewModel.onHandleEvent(
            RemoteScreenUiEvent.PrivateListeningChangedEvent(isActive)
        )
    }

    /** Class for interacting with the main interface of the service. */
    private val remoteAudioConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            Timber.tag(TAG).d("onServiceConnected")
            remoteAudioService = IRemoteAudioInterface.Stub.asInterface(binder)

            try {
                remoteAudioService?.setDevice(preferenceUtils.connectedDevice.host)
                remoteAudioService?.toggleRemoteAudio()
            } catch (ex: Exception) {
                Timber.tag(TAG).e(ex, "Failed to start private listening")
            }

            reportPrivateListeningState()
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            Timber.tag(TAG).d("onServiceDisconnected")
            remoteAudioService = null
            reportPrivateListeningState()
        }

        override fun onBindingDied(name: ComponentName?) {
            Timber.tag(TAG).d("onBindingDied")
            remoteAudioService = null
            reportPrivateListeningState()
        }

        override fun onNullBinding(name: ComponentName?) {
            Timber.tag(TAG).d("onNullBinding")
            remoteAudioService = null
            reportPrivateListeningState()
        }
    }

    private companion object {
        const val TAG = "RemoteFragment"

        /**
         * The keys that change what the device is showing, so the other tabs are told to refresh.
         */
        val DEVICE_CHANGING_KEYS = setOf(
            KeyPressKeyValues.BACK,
            KeyPressKeyValues.HOME,
            KeyPressKeyValues.SELECT
        )

        val REMOTE_AUDIO_COMPONENT = ComponentName(
            "wseemann.media.romote.audio",
            "wseemann.media.romote.audio.remoteaudio.RemoteAudio"
        )
    }
}
