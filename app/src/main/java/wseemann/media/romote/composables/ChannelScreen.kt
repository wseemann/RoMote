package wseemann.media.romote.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.persistentListOf
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.data.ChannelItem
import wseemann.media.romote.event.ChannelScreenUiEvent
import wseemann.media.romote.model.ChannelScreenUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    uiState: ChannelScreenUiState,
    onEvent: (ChannelScreenUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { onEvent(ChannelScreenUiEvent.LoadChannelsEvent) },
        modifier = modifier.fillMaxSize()
    ) {
        // The empty state has to fill the viewport so the pull gesture still has somewhere to
        // travel when there is nothing to show, and LazyGridItemScope offers no fillParentMaxSize
        // the way LazyItemScope does.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val viewportHeight = maxHeight

            LazyVerticalGrid(
                columns = GridCells.Fixed(integerResource(R.integer.fragment_channel_columns)),
                horizontalArrangement = Arrangement.spacedBy(ThumbnailSpacing),
                verticalArrangement = Arrangement.spacedBy(ThumbnailSpacing),
                contentPadding = PaddingValues(ThumbnailSpacing),
                modifier = Modifier.fillMaxSize().navigationBarsPadding()
            ) {
                if (uiState.channels.isEmpty() && !uiState.isLoading) {
                    // An empty grid means one of two different things, and saying "no channels"
                    // when there is no device to have channels on sends the user looking in the
                    // wrong place - the Roku isn't missing its apps, the app has nothing paired.
                    val emptyMessage = if (uiState.isDeviceConnected) {
                        R.string.empty_channel_list
                    } else {
                        R.string.no_device_connected
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().height(viewportHeight)
                        ) {
                            Text(
                                text = stringResource(emptyMessage),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                items(uiState.channels, key = { channel -> channel.id }) { channel ->
                    ChannelThumbnail(
                        channel = channel,
                        onClick = {
                            onEvent(ChannelScreenUiEvent.ChannelClickedEvent(channel))
                        },
                        modifier = Modifier.aspectRatio(THUMBNAIL_ASPECT_RATIO)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChannelScreenPreview() {
    RomoteTheme {
        ChannelScreen(
            uiState = ChannelScreenUiState(
                channels = persistentListOf(
                    ChannelItem(id = "12", title = "Netflix", iconUrl = ""),
                    ChannelItem(id = "13", title = "Prime Video", iconUrl = ""),
                    ChannelItem(id = "837", title = "YouTube", iconUrl = "")
                )
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChannelScreenEmptyPreview() {
    RomoteTheme {
        ChannelScreen(
            uiState = ChannelScreenUiState(),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChannelScreenNoDevicePreview() {
    RomoteTheme {
        ChannelScreen(
            uiState = ChannelScreenUiState(isDeviceConnected = false),
            onEvent = {}
        )
    }
}
