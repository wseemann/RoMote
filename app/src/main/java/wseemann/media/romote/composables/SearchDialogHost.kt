package wseemann.media.romote.composables

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import wseemann.media.romote.composables.theme.RomoteTheme

/**
 * Puts SearchDialog on screen for MainActivity, which is still Java and whose content view is the
 * CoordinatorLayout of activity_main.xml.
 *
 * show() stands in for the DialogFragment the activity used to build in its options menu, so the
 * menu handler reads the same way. A Compose dialog opens a window of its own, so the ComposeView
 * this attaches measures 0x0 and never disturbs the toolbar, tabs or ViewPager beneath it - which
 * is why activity_main.xml needs no ComposeView of its own.
 */
class SearchDialogHost(
    private val activity: ComponentActivity,
    private val listener: OnSearchListener
) {

    /**
     * Replaces SearchDialog.SearchDialogListener. A Kotlin interface rather than a (String) -> Unit
     * so that the Java caller can pass a method reference without returning Unit.INSTANCE.
     */
    fun interface OnSearchListener {
        fun onSearch(searchText: String)
    }

    private val visible = mutableStateOf(false)

    private var composeView: ComposeView? = null

    fun show() {
        if (composeView == null) {
            attach()
        }

        visible.value = true
    }

    private fun attach() {
        val view = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RomoteTheme {
                    if (visible.value) {
                        SearchDialog(
                            // The old dialog dismissed itself from both buttons; keep that.
                            onSearch = { searchText ->
                                visible.value = false
                                listener.onSearch(searchText)
                            },
                            onDismiss = { visible.value = false }
                        )
                    }
                }
            }
        }

        // Attached lazily, on the first show(), rather than in onCreate - by which point the
        // activity has installed its own content view.
        activity.addContentView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        composeView = view
    }
}
