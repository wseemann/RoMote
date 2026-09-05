package wseemann.media.romote.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.data.ChannelItem

internal val ThumbnailSpacing = 2.dp

/** Roku serves channel art at 4:3; forcing 1:1 would crop the top and bottom off every icon. */
internal const val THUMBNAIL_ASPECT_RATIO = 4f / 3f

internal val ThumbnailCornerRadius = 5.dp

/**
 * One piece of channel art, shared by the channel grid and the recent apps sheet so the two cannot
 * drift apart.
 *
 * Subcompose rather than a plain AsyncImage for the sake of the error slot: a recents entry for a
 * channel that has since been uninstalled serves no icon, and without a fallback the tile would be
 * an invisible tap target.
 */
@Composable
fun ChannelThumbnail(channel: ChannelItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = channel.iconUrl,
        contentDescription = channel.title,
        // Fit rather than Crop, so art that is not exactly 4:3 is letterboxed instead of clipped.
        contentScale = ContentScale.Fit,
        error = { ChannelThumbnailFallback(title = channel.title) },
        modifier = modifier
            .clip(RoundedCornerShape(ThumbnailCornerRadius))
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ChannelThumbnailFallback(title: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize().clip(RoundedCornerShape(ThumbnailCornerRadius))
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChannelThumbnailFallbackPreview() {
    RomoteTheme {
        ChannelThumbnailFallback(title = "A Channel That Is No Longer Installed")
    }
}
