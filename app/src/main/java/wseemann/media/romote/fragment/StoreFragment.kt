package wseemann.media.romote.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import wseemann.media.romote.composables.StoreScreen
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.viewmodels.StoreScreenViewModel

/**
 * Created by wseemann on 8/6/16.
 */
@AndroidEntryPoint
class StoreFragment : Fragment() {

    private lateinit var storeScreenViewModel: StoreScreenViewModel

    /**
     * Whether this fragment is the ViewPager's current page. MainActivity's SectionsPagerAdapter
     * uses the default FragmentPagerAdapter behavior (BEHAVIOR_SET_USER_VISIBLE_HINT) with an
     * offscreen page limit of 3, so this fragment is RESUMED even while another tab is on screen.
     * setUserVisibleHint is the signal that behavior mode drives, which is why the deprecated hook
     * is still the right one here - it replaces the old
     * `mViewPager.getCurrentItem() != 3` guard MainActivity used to apply before forwarding
     * back-key presses.
     */
    private var isCurrentPage by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storeScreenViewModel = ViewModelProvider(this)[StoreScreenViewModel::class.java]
    }

    @Deprecated("Required by FragmentPagerAdapter's BEHAVIOR_SET_USER_VISIBLE_HINT mode")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        @Suppress("DEPRECATION")
        super.setUserVisibleHint(isVisibleToUser)
        isCurrentPage = isVisibleToUser
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by storeScreenViewModel.uiState.collectAsStateWithLifecycle()

                RomoteTheme {
                    StoreScreen(
                        uiState = uiState,
                        isCurrentPage = isCurrentPage,
                        onEvent = storeScreenViewModel::onHandleEvent
                    )
                }
            }
        }
    }
}
