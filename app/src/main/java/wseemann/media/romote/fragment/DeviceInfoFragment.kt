package wseemann.media.romote.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import wseemann.media.romote.composables.DeviceInfoScreen
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.event.DeviceInfoScreenUiEvent
import wseemann.media.romote.viewmodels.DeviceInfoScreenViewModel

/**
 * Everything the device reports about itself. Reached from the device lists, either for a paired
 * device (by serial number) or one that was just discovered (by host).
 */
@AndroidEntryPoint
class DeviceInfoFragment : Fragment() {

    private lateinit var deviceInfoScreenViewModel: DeviceInfoScreenViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceInfoScreenViewModel = ViewModelProvider(this)[DeviceInfoScreenViewModel::class.java]

        val bundle = arguments ?: return

        // The ViewModel outlives a configuration change, so only query a device that has yet to
        // answer - otherwise a rotation throws away the list and starts over.
        if (deviceInfoScreenViewModel.uiState.value.entries.isEmpty()) {
            deviceInfoScreenViewModel.onHandleEvent(
                DeviceInfoScreenUiEvent.LoadDeviceInfoEvent(
                    serialNumber = bundle.getString(ARG_SERIAL_NUMBER),
                    host = bundle.getString(ARG_HOST)
                )
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by deviceInfoScreenViewModel.uiState.collectAsStateWithLifecycle()

                RomoteTheme {
                    DeviceInfoScreen(uiState = uiState)
                }
            }
        }
    }

    companion object {
        private const val ARG_SERIAL_NUMBER = "serial_number"
        private const val ARG_HOST = "host"

        fun getInstance(serialNumber: String?, host: String?): DeviceInfoFragment {
            return DeviceInfoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SERIAL_NUMBER, serialNumber)
                    putString(ARG_HOST, host)
                }
            }
        }
    }
}
