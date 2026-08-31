package wseemann.media.romote.composables

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import wseemann.media.romote.composables.theme.RomoteTheme

/**
 * Puts ConnectivityDialog on screen for ConnectivityActivity, which is still Java and has no
 * content view of its own - it is the base class of every screen in the app (MainActivity,
 * DeviceInfoActivity, ManualConnectionActivity), all of which now set their content with Compose.
 *
 * show()/dismiss()/isShowing() stand in for the DialogFragment the activity used to hold, so its
 * network monitoring is untouched. A Compose dialog opens a window of its own, so the ComposeView
 * this attaches measures 0x0 and never disturbs whatever the subclass set as its content.
 */
class ConnectivityDialogHost(private val activity: ComponentActivity) {

    private val visible = mutableStateOf(false)

    private var composeView: ComposeView? = null

    fun show() {
        if (composeView == null) {
            attach()
        }

        visible.value = true
    }

    fun dismiss() {
        visible.value = false
    }

    fun isShowing(): Boolean = visible.value

    private fun attach() {
        val view = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RomoteTheme {
                    if (visible.value) {
                        ConnectivityDialog(
                            onOpenSettingsClick = {
                                // The platform dialog dismissed itself when its button was
                                // tapped; keep that.
                                visible.value = false
                                openWifiSettings()
                            }
                        )
                    }
                }
            }
        }

        // Attached lazily rather than in onCreate, because the first show() comes from onResume -
        // by which point the subclass has installed its own content view.
        activity.addContentView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        composeView = view
    }

    private fun openWifiSettings() {
        try {
            // In some cases, a matching Activity may not exist, so ensure you safeguard against this.
            activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (ignored: ActivityNotFoundException) {
            activity.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
