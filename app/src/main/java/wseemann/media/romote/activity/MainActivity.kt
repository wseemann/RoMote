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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import wseemann.media.romote.R
import wseemann.media.romote.composables.ChannelsTab
import wseemann.media.romote.composables.ConnectivityDialogHost
import wseemann.media.romote.composables.DevicesTab
import wseemann.media.romote.composables.RemoteAccessHelpDialog
import wseemann.media.romote.composables.RemoteTab
import wseemann.media.romote.composables.RomoteTopAppBar
import wseemann.media.romote.composables.SearchDialog
import wseemann.media.romote.composables.StoreTab
import wseemann.media.romote.composables.theme.OnPurple
import wseemann.media.romote.composables.theme.Purple
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.event.ChannelScreenUiEvent
import wseemann.media.romote.inappreview.AppReviewManager
import wseemann.media.romote.service.NotificationService
import wseemann.media.romote.utils.Constants
import wseemann.media.romote.viewmodels.ChannelScreenViewModel
import wseemann.media.romote.viewmodels.ConnectivityViewModel
import wseemann.media.romote.viewmodels.MainScreenViewModel
import wseemann.media.romote.viewmodels.RemoteScreenViewModel
import wseemann.media.romote.viewmodels.StoreScreenViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ShakeActivity() {

    @Inject
    lateinit var appReviewManager: AppReviewManager

    private val mainScreenViewModel: MainScreenViewModel by viewModels()
    private val remoteScreenViewModel: RemoteScreenViewModel by viewModels()
    private val channelScreenViewModel: ChannelScreenViewModel by viewModels()
    private val storeScreenViewModel: StoreScreenViewModel by viewModels()
    private val connectivityViewModel: ConnectivityViewModel by viewModels()

    private var isBound = false

    /** Gates the splash screen; see [onCreate]. */
    private var isContentReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Without this the splash comes down at the window's first frame, which is only
        // AppTheme.NoActionBar's windowBackground - a flat white or #121212 rectangle where the
        // purple app bar and tab strip are about to be. Holding it until the first composition
        // makes the handoff splash -> content, with no bare window in between. The flag is flipped
        // from inside setContent below, so it is always eventually released.
        splashScreen.setKeepOnScreenCondition { !isContentReady }

        appReviewManager.onAppSessionStarted()

        setContent {
            RomoteTheme {
                MainContent()
            }

            // Runs on the main thread once the first composition is done, before the frame it
            // belongs to is drawn - so the splash is released exactly when there is something
            // behind it.
            LaunchedEffect(Unit) { isContentReady = true }
        }

        // Bind to NotificationService
        bindService(
            Intent(this, NotificationService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            appReviewManager.maybeLaunchReviewFlow(this@MainActivity)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    @Composable
    private fun MainContent() {
        val scope = rememberCoroutineScope()

        // Opens on the remote when there is already a device to drive, the way
        // viewPager.setCurrentItem(1) used to.
        val pagerState = rememberPagerState(
            initialPage = if (deviceManager.getConnectedDevice() != null) REMOTE_PAGE else DEVICES_PAGE,
            pageCount = { PAGE_COUNT }
        )

        var isSearchDialogVisible by rememberSaveable { mutableStateOf(false) }

        // Seeded from preferences rather than re-read on every recomposition, and saveable so a
        // rotation doesn't take the dialog down: it is only marked seen once the user closes it.
        var isRemoteAccessHelpVisible by rememberSaveable {
            mutableStateOf(!appPreferences.hasSeenRemoteAccessHelp())
        }

        fun dismissRemoteAccessHelp() {
            isRemoteAccessHelpVisible = false
            appPreferences.setRemoteAccessHelpSeen()
        }

        // Every tab has to stay composed - that is what keeps the store's WebView and the remote's
        // service binding alive across a swipe, the way viewPager.setOffscreenPageLimit(3) did.
        // But building them all during the first composition puts creating the process's first
        // WebView, which loads the WebView APK on the main thread, in front of the first frame.
        // Starting at 0 and widening a frame later keeps the behavior and takes it off the
        // critical path; beyondViewportPageCount only ever adds pages, so nothing is torn down.
        var offscreenPages by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) { offscreenPages = PAGE_COUNT - 1 }

        // What ConnectivityActivity.onWifiConnected() did: the channels grid can't have loaded
        // while the device was unreachable, so reload it once the phone is back on a local
        // network. Only a *return* to the network counts - the grid's first load belongs to
        // ChannelsTab, which does it when the tab is first selected. The ViewModel is held by this
        // activity rather than by a fragment, so this no longer has to reach for a fragment
        // instance the pager may not have created yet.
        LaunchedEffect(Unit) {
            var wasAvailable = connectivityViewModel.uiState.value.isLocalNetworkAvailable

            connectivityViewModel.uiState.collect { state ->
                if (state.isLocalNetworkAvailable && !wasAvailable &&
                    channelScreenViewModel.uiState.value.channels.isEmpty()
                ) {
                    channelScreenViewModel.onHandleEvent(ChannelScreenUiEvent.LoadChannelsEvent)
                }

                wasAvailable = state.isLocalNetworkAvailable
            }
        }

        Scaffold(
            topBar = {
                Column {
                    RomoteTopAppBar(
                        title = stringResource(R.string.app_name),
                        actions = {
                            MainActions { isSearchDialogVisible = true }
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
                beyondViewportPageCount = offscreenPages,
                modifier = Modifier.padding(contentPadding)
            ) { page ->
                when (page) {
                    DEVICES_PAGE -> DevicesTab(viewModel = mainScreenViewModel)

                    REMOTE_PAGE -> RemoteTab(
                        viewModel = remoteScreenViewModel,
                        isCurrentPage = pagerState.currentPage == REMOTE_PAGE
                    )

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

        ConnectivityDialogHost(viewModel = connectivityViewModel)

        if (isRemoteAccessHelpVisible) {
            RemoteAccessHelpDialog(
                onLearnMoreClick = {
                    dismissRemoteAccessHelp()

                    startActivity(
                        Intent(Intent.ACTION_VIEW, Constants.ROKU_MOBILE_APP_SETUP_URL.toUri())
                    )
                },
                onDismiss = { dismissRemoteAccessHelp() }
            )
        }
    }

    /**
     * What menu/main.xml held: search shown as an icon, settings in the overflow.
     */
    @Composable
    private fun MainActions(onSearchClick: () -> Unit) {
        var isOverflowExpanded by remember { mutableStateOf(false) }

        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.action_search)
            )
        }

        IconButton(onClick = { isOverflowExpanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
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

        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            // The brand purple by name, matching RomoteTopAppBar above it; the dark scheme's
            // primary is a light foreground tone and would wash the strip out.
            containerColor = Purple,
            contentColor = OnPurple,
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
        lifecycleScope.launch(ioDispatcher) {
            deviceManager.getConnectedDevice()?.performSearch(searchText)
        }
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            isBound = true
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            isBound = false
        }
    }

    private companion object {
        const val DEVICES_PAGE = 0
        const val REMOTE_PAGE = 1
        const val CHANNELS_PAGE = 2
        const val STORE_PAGE = 3
        const val PAGE_COUNT = 4
    }
}
