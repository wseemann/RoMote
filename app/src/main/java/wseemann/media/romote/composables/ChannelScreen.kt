package wseemann.media.romote.composables

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.collections.immutable.persistentListOf
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.data.ChannelItem
import wseemann.media.romote.event.ChannelScreenUiEvent
import wseemann.media.romote.model.ChannelScreenUiState

/** Spacing carried over from the GridView this screen replaced. */
private val ThumbnailSpacing = 2.dp

/**
 * Roku serves channel art at 4:3, and that is effectively what the GridView showed: it sized each
 * thumbnail's height from floor(width / 102dp) columns while the grid itself laid out the three
 * columns @integer/fragment_channel_columns asks for, leaving cells a third of the width wide and
 * about a quarter tall. Forcing 1:1 here instead would crop the top and bottom off every icon.
 */
private const val ThumbnailAspectRatio = 4f / 3f

/** Matches the 5dp corner size of the ShapeableImageView the grid items used to be. */
private val ThumbnailCornerRadius = 5.dp

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
                // The app is edge-to-edge, so keep the last row clear of the navigation bar.
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
                    ChannelGridItem(
                        channel = channel,
                        onClick = {
                            onEvent(ChannelScreenUiEvent.ChannelClickedEvent(channel.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelGridItem(channel: ChannelItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AsyncImage(
        model = channel.iconUrl,
        contentDescription = channel.title,
        // Fit rather than Crop, so art that is not exactly 4:3 is letterboxed instead of clipped.
        contentScale = ContentScale.Fit,
        modifier = modifier
            .aspectRatio(ThumbnailAspectRatio)
            .clip(RoundedCornerShape(ThumbnailCornerRadius))
            .clickable(onClick = onClick)
    )
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
