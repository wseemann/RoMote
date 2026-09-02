package wseemann.media.romote.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import wseemann.media.romote.composables.theme.OnPurple
import wseemann.media.romote.composables.theme.Purple
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.event.ChannelScreenUiEvent
import wseemann.media.romote.inappreview.AppReviewManager
import wseemann.media.romote.service.NotificationService
import wseemann.media.romote.utils.Constants
import wseemann.media.romote.utils.enableRomoteEdgeToEdge
import wseemann.media.romote.viewmodels.ChannelScreenViewModel
import wseemann.media.romote.viewmodels.ConnectivityViewModel
import wseemann.media.romote.viewmodels.MainScreenViewModel
import wseemann.media.romote.viewmodels.RemoteScreenViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ShakeActivity() {

    @Inject
    lateinit var appReviewManager: AppReviewManager

    private val mainScreenViewModel: MainScreenViewModel by viewModels()
    private val remoteScreenViewModel: RemoteScreenViewModel by viewModels()
    private val channelScreenViewModel: ChannelScreenViewModel by viewModels()
    private val connectivityViewModel: ConnectivityViewModel by viewModels()

    private var isBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        appReviewManager.onAppSessionStarted()

        setContent {
            RomoteTheme {
                MainContent()
            }
        }

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

        val initialPage = if (deviceManager.getConnectedDevice() != null) {
            REMOTE_PAGE
        } else {
            DEVICES_PAGE
        }

        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { PAGE_COUNT }
        )

        // Seeded from preferences rather than re-read on every recomposition, and saveable so a
        // rotation doesn't take the dialog down: it is only marked seen once the user closes it.
        var isRemoteAccessHelpVisible by rememberSaveable {
            mutableStateOf(!appPreferences.hasSeenRemoteAccessHelp())
        }

        fun dismissRemoteAccessHelp() {
            isRemoteAccessHelpVisible = false
            appPreferences.setRemoteAccessHelpSeen()
        }

        // Only in the day theme, where the adaptive bar would put a near-white band under the
        // remote's near-black artwork. The night theme is left alone deliberately - the navigation
        // bar keeps whatever Android gives it, the same as every other screen.
        //
        // Keyed on currentPage rather than settledPage so the bar turns over at the halfway point of
        // a swipe, when the dark artwork already covers most of the screen, instead of lagging until
        // it lands.
        val isDarkTheme = isSystemInDarkTheme()

        LaunchedEffect(pagerState.currentPage, isDarkTheme) {
            enableRomoteEdgeToEdge(
                activity = this@MainActivity,
                darkNavigationBar = pagerState.currentPage == REMOTE_PAGE && !isDarkTheme
            )
        }

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
                            MainActions()
                        }
                    )

                    MainTabRow(pagerState = pagerState, scope = scope)
                }
            },
            // The screens apply navigationBarsPadding() themselves, so letting the Scaffold inset
            // the content too would pad the bottom twice.
            contentWindowInsets = WindowInsets(0)
        ) { contentPadding ->
            HorizontalPager(
                state = pagerState,
                // Every tab stays composed - that is what keeps the remote's service binding
                // alive across a swipe.
                beyondViewportPageCount = PAGE_COUNT - 1,
                modifier = Modifier.padding(contentPadding)
            ) { page ->
                when (page) {
                    DEVICES_PAGE -> DevicesTab(
                        viewModel = mainScreenViewModel,
                        isCurrentPage = pagerState.currentPage == DEVICES_PAGE
                    )

                    REMOTE_PAGE -> RemoteTab(
                        viewModel = remoteScreenViewModel,
                        isCurrentPage = pagerState.currentPage == REMOTE_PAGE
                    )

                    else -> ChannelsTab(
                        viewModel = channelScreenViewModel,
                        isCurrentPage = pagerState.currentPage == CHANNELS_PAGE
                    )
                }
            }
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

    @Composable
    private fun MainActions() {
        var isOverflowExpanded by remember { mutableStateOf(false) }

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

    @Composable
    private fun MainTabRow(pagerState: PagerState, scope: CoroutineScope) {
        val titles = listOf(
            stringResource(R.string.title_devices),
            stringResource(R.string.title_remote),
            stringResource(R.string.title_channels)
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
        const val PAGE_COUNT = 3
    }
}
