package wseemann.media.romote.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import wseemann.media.romote.R
import wseemann.media.romote.data.ChannelItem

private val TileWidth = 104.dp
private val TileSpacing = 8.dp
private val SheetPadding = 16.dp

/**
 * The recently launched channels, as a single scrolling row.
 *
 * A row rather than a grid: the list is capped short enough that it never needs to wrap, and
 * keeping the sheet shallow leaves the remote visible behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentAppsSheet(
    recentChannels: ImmutableList<ChannelItem>,
    onChannelClick: (ChannelItem) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        Column {
            Text(
                text = stringResource(R.string.recent_apps),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = SheetPadding)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(TileSpacing),
                contentPadding = PaddingValues(SheetPadding)
            ) {
                items(recentChannels, key = { channel -> channel.id }) { channel ->
                    ChannelThumbnail(
                        channel = channel,
                        onClick = { onChannelClick(channel) },
                        modifier = Modifier.width(TileWidth).aspectRatio(THUMBNAIL_ASPECT_RATIO)
                    )
                }
            }
        }
    }
}
