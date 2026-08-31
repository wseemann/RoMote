package wseemann.media.romote.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.wseemann.ecp.api.ResponseCallback
import com.wseemann.ecp.request.SearchRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.R
import wseemann.media.romote.composables.ChannelsTab
import wseemann.media.romote.composables.DevicesTab
import wseemann.media.romote.composables.RemoteTab
import wseemann.media.romote.composables.RomoteTopAppBar
import wseemann.media.romote.composables.SearchDialog
import wseemann.media.romote.composables.StoreTab
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.event.ChannelScreenUiEvent
import wseemann.media.romote.fragment.InstallChannelDialog
import wseemann.media.romote.inappreview.AppReviewManager
import wseemann.media.romote.service.NotificationService
import wseemann.media.romote.viewmodels.ChannelScreenViewModel
import wseemann.media.romote.viewmodels.MainScreenViewModel
import wseemann.media.romote.viewmodels.RemoteScreenViewModel
import wseemann.media.romote.viewmodels.StoreScreenViewModel
import javax.inject.Inject

/**
 * The app's four tabs. This was an XML shell - an AppBarLayout with a Toolbar and a TabLayout over a
 * v1 ViewPager driven by a FragmentPagerAdapter - around four fragments that were themselves only
 * ComposeViews. The chrome is now a Scaffold with a TopAppBar, a SecondaryTabRow and a HorizontalPager,
 * and the fragments are the tab composables in MainTabs.kt.
 *
 * It stays an AppCompatActivity (through [ConnectivityActivity] and ShakeActivity) because the two
 * remaining DialogFragments - [InstallChannelDialog] here and TextInputDialog on the remote tab -
 * still need a FragmentManager.
 */
@AndroidEntryPoint
class MainActivity : ConnectivityActivity(), InstallChannelDialog.InstallChannelListener {

    @Inject
    lateinit var appReviewManager: AppReviewManager

    private val mainScreenViewModel: MainScreenViewModel by viewModels()
    private val remoteScreenViewModel: RemoteScreenViewModel by viewModels()
    private val channelScreenViewModel: ChannelScreenViewModel by viewModels()
    private val storeScreenViewModel: StoreScreenViewModel by viewModels()

    private var isBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        appReviewManager.onAppSessionStarted()

        intent?.data?.path?.let { path ->
            val channelCode = path.replace("/install/", "")

            InstallChannelDialog.getInstance(this, channelCode)
                .show(supportFragmentManager, InstallChannelDialog::class.java.name)
        }

        setContent {
            RomoteTheme {
                MainContent()
            }
        }

        // Bind to NotificationService
        bindService(
            Intent(this, NotificationService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    /**
     * Offers the Play in-app review card, if this user has driven a Roku often enough and long
     * enough for it to be worth asking. AppReviewManager decides that, and only tries once per
     * process, so coming back from settings doesn't ask again.
     */
    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            appReviewManager.maybeLaunchReviewFlow(this@MainActivity)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unbind from the service
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    /**
     * Reloads the channels grid, which can't have loaded while the device was unreachable. The
     * ViewModel is held here rather than by a fragment, so this no longer has to reach for a
     * fragment instance the pager may not have created yet.
     */
    override fun onWifiConnected() {
        if (channelScreenViewModel.uiState.value.channels.isEmpty()) {
            channelScreenViewModel.onHandleEvent(ChannelScreenUiEvent.LoadChannelsEvent)
        }
    }

    @Composable
    private fun MainContent() {
        val scope = rememberCoroutineScope()

        // Opens on the remote when there is already a device to drive, the way
        // viewPager.setCurrentItem(1) used to.
        val pagerState = rememberPagerState(
            initialPage = if (commandHelper.deviceURL.isNotEmpty()) REMOTE_PAGE else DEVICES_PAGE,
            pageCount = { PAGE_COUNT }
        )

        var isSearchDialogVisible by rememberSaveable { mutableStateOf(false) }

        Scaffold(
            topBar = {
                Column {
                    RomoteTopAppBar(
                        title = stringResource(R.string.app_name),
                        actions = {
                            MainActions(onSearchClick = { isSearchDialogVisible = true })
                        }
                    )

                    MainTabRow(pagerState = pagerState, scope = scope)
                }
            },
            // The screens apply navigationBarsPadding() themselves - they had to, hanging off a
            // ViewPager - so letting the Scaffold inset the content too would pad the bottom twice.
            contentWindowInsets = WindowInsets(0)
        ) { contentPadding ->
            HorizontalPager(
                state = pagerState,
                // Was viewPager.setOffscreenPageLimit(3): every tab stays alive, which is what
                // keeps the store's WebView and the remote's service binding around.
                beyondViewportPageCount = PAGE_COUNT - 1,
                modifier = Modifier.padding(contentPadding)
            ) { page ->
                when (page) {
                    DEVICES_PAGE -> DevicesTab(viewModel = mainScreenViewModel)

                    REMOTE_PAGE -> RemoteTab(viewModel = remoteScreenViewModel)

                    CHANNELS_PAGE -> ChannelsTab(
                        viewModel = channelScreenViewModel,
                        isCurrentPage = pagerState.currentPage == CHANNELS_PAGE
                    )

                    else -> StoreTab(
                        viewModel = storeScreenViewModel,
                        isCurrentPage = pagerState.currentPage == STORE_PAGE
                    )
                }
            }
        }

        if (isSearchDialogVisible) {
            SearchDialog(
                // The old dialog dismissed itself from both buttons; keep that.
                onSearch = { searchText ->
                    isSearchDialogVisible = false
                    performSearch(searchText)
                },
                onDismiss = { isSearchDialogVisible = false }
            )
        }
    }

    /**
     * What menu/main.xml held: search shown as an icon, settings in the overflow.
     */
    @Composable
    private fun RowScope.MainActions(onSearchClick: () -> Unit) {
        var isOverflowExpanded by remember { mutableStateOf(false) }

        IconButton(onClick = onSearchClick) {
            Icon(
                painter = painterResource(R.drawable.ic_action_search),
                contentDescription = stringResource(R.string.action_search)
            )
        }

        IconButton(onClick = { isOverflowExpanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.more_options_content_description)
            )
        }

        DropdownMenu(
            expanded = isOverflowExpanded,
            onDismissRequest = { isOverflowExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_settings)) },
                onClick = {
                    isOverflowExpanded = false
                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                }
            )
        }
    }

    /** The purple tab strip, matching styles.xml -> AppTheme.TabLayout. */
    @Composable
    private fun MainTabRow(pagerState: PagerState, scope: CoroutineScope) {
        val titles = listOf(
            stringResource(R.string.title_devices),
            stringResource(R.string.title_remote),
            stringResource(R.string.title_channels),
            stringResource(R.string.title_store)
        )

        // Secondary rather than Primary: the Material 3 primary indicator is a short pill under the
        // label, where AppTheme.TabLayout's ran the full width of the tab.
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                    color = MaterialTheme.colorScheme.tertiary
                )
            },
            divider = {}
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(text = title) }
                )
            }
        }
    }

    private fun performSearch(searchText: String) {
        val request = SearchRequest(
            commandHelper.deviceURL, searchText,
            null, null, null, null, null, null, null, null, null
        )

        request.sendAsync(object : ResponseCallback<Void> {
            override fun onSuccess(data: Void?) = Unit

            override fun onError(ex: Exception) {
                Timber.tag(TAG).e(ex, "Search failed")
            }
        })
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            isBound = true
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            isBound = false
        }
    }

    override fun onDialogCancelled(dialog: DialogFragment) {
        dialog.dismiss()
    }

    override fun onInstallSelected(dialog: DialogFragment) {
        dialog.dismiss()
    }

    private companion object {
        const val TAG = "MainActivity"

        const val DEVICES_PAGE = 0
        const val REMOTE_PAGE = 1
        const val CHANNELS_PAGE = 2
        const val STORE_PAGE = 3
        const val PAGE_COUNT = 4
    }
}
