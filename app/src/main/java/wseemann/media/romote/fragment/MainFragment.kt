package wseemann.media.romote.fragment

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import wseemann.media.romote.R
import wseemann.media.romote.activity.DeviceInfoActivity
import wseemann.media.romote.activity.ManualConnectionActivity
import wseemann.media.romote.composables.MainScreen
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.event.MainScreenUiEvent
import wseemann.media.romote.viewmodels.MainScreenViewModel
import wseemann.media.romote.widget.RokuAppWidgetProvider

/**
 * The devices tab. [MainScreen] draws it and [MainScreenViewModel] holds its state; what stays here
 * is the work that needs a Context - the two activities the screen can start, the toast that
 * confirms a connection, and the widget update that follows it.
 */
@AndroidEntryPoint
class MainFragment : Fragment() {

    private lateinit var mainScreenViewModel: MainScreenViewModel

    /**
     * Pairing by IP address. A device paired that way is not in the results of the scan that is
     * on screen, so the list is scanned again once the activity reports back.
     */
    private val manualConnectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            mainScreenViewModel.onHandleEvent(MainScreenUiEvent.RefreshEvent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainScreenViewModel = ViewModelProvider(this)[MainScreenViewModel::class.java]

        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)

        // Nothing else kicks off the first scan; the fragment used to do this from
        // onActivityCreated, once its adapters were built. The ViewModel outlives a rotation with
        // its results, so a scan only starts when there is nothing to show yet.
        val uiState = mainScreenViewModel.uiState.value

        if (!uiState.isLoading &&
            uiState.pairedDevices.isEmpty() &&
            uiState.availableDevices.isEmpty()
        ) {
            mainScreenViewModel.onHandleEvent(MainScreenUiEvent.RefreshEvent)
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
                val uiState by mainScreenViewModel.uiState.collectAsStateWithLifecycle()

                RomoteTheme {
                    MainScreen(
                        uiState = uiState,
                        onEvent = ::handleEvent
                    )
                }
            }
        }
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
     * Intercepts the events that can't be served from the ViewModel, the way ChannelFragment does,
     * and forwards everything else untouched.
     */
    private fun handleEvent(event: MainScreenUiEvent) {
        when (event) {
            is MainScreenUiEvent.DeviceSelectedEvent -> {
                val serialNumber = event.device.serialNumber

                Toast.makeText(
                    requireContext(),
                    getString(R.string.device_connected, serialNumber),
                    Toast.LENGTH_SHORT
                ).show()

                updateWidgets()
            }

            is MainScreenUiEvent.DeviceInfoClickedEvent -> {
                startActivity(
                    Intent(requireContext(), DeviceInfoActivity::class.java).apply {
                        putExtra("serial_number", event.serialNumber)
                        putExtra("host", event.host)
                    }
                )
                // The ViewModel has nothing to do with this one.
                return
            }

            is MainScreenUiEvent.AddDeviceClickedEvent -> {
                manualConnectionLauncher.launch(
                    Intent(requireContext(), ManualConnectionActivity::class.java)
                )
                return
            }

            else -> Unit
        }

        mainScreenViewModel.onHandleEvent(event)
    }

    /** Tells the home screen widgets to redraw against the device that was just connected. */
    private fun updateWidgets() {
        val widgetManager = AppWidgetManager.getInstance(requireContext())
        val widgetComponent = ComponentName(requireContext(), RokuAppWidgetProvider::class.java)

        val update = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                widgetManager.getAppWidgetIds(widgetComponent)
            )
        }

        requireActivity().sendBroadcast(update)
    }
}
