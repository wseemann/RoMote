@file:JvmName("MainScreenDialogHost")

package wseemann.media.romote.composables

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.event.MainScreenUiEvent
import wseemann.media.romote.viewmodels.MainScreenViewModel

/**
 * Hands MainFragment - still a Java ListFragment built from fragment_main.xml - somewhere to put
 * EditDeviceNameDialog. The ComposeView it binds holds no layout of its own; the dialog opens its
 * own window when uiState says a rename is in flight.
 *
 * This collects the StateFlow directly rather than going through the uiStateLiveData bridge the
 * fragment uses for the device lists, so opening the dialog doesn't run through the list observer.
 */
fun bindRenameDialog(composeView: ComposeView, viewModel: MainScreenViewModel) {
    composeView.setViewCompositionStrategy(
        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
    )

    composeView.setContent {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        RomoteTheme {
            uiState.renameTarget?.let { target ->
                EditDeviceNameDialog(
                    initialName = target.currentName,
                    onConfirm = {
                        viewModel.onHandleEvent(MainScreenUiEvent.RenameDeviceConfirmedEvent(it))
                    },
                    onDismiss = {
                        viewModel.onHandleEvent(MainScreenUiEvent.RenameDeviceDismissedEvent)
                    }
                )
            }
        }
    }
}
