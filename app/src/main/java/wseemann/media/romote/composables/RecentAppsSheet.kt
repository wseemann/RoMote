package wseemann.media.romote.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import wseemann.media.romote.R
import wseemann.media.romote.data.ChannelItem

private val TileWidth = 104.dp
private val TileSpacing = 8.dp
private val SheetPadding = 16.dp
private val SheetCornerRadius = 16.dp
private val HandleWidth = 32.dp
private val HandleHeight = 4.dp

/** What is left on screen when the sheet is collapsed: the handle and its label, nothing else. */
internal val RecentsPeekHeight = 52.dp

private enum class RecentsSheetValue {
    Collapsed,
    Expanded
}

/**
 * The recently launched channels, as a sheet dragged up from the bottom of the remote.
 *
 * The handle is not a separate control that opens a sheet - it is the sheet, collapsed, so a drag
 * carries the channels with the finger instead of tripping a velocity threshold and animating in
 * from off screen. Releasing settles at whichever end is closer.
 *
 * Not modal, and a single scrolling row rather than a grid: the remote behind it stays visible and
 * usable at every point in the drag.
 */
@Composable
fun RecentAppsSheet(
    recentChannels: ImmutableList<ChannelItem>,
    onChannelClick: (ChannelItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Outlives the composable the way the ViewModel's flag used to, so a rotation with the sheet
    // open comes back open.
    var savedValue by rememberSaveable { mutableStateOf(RecentsSheetValue.Collapsed) }
    val state = remember { AnchoredDraggableState(savedValue) }
    // Read back below to place the channels, which trail the sheet on its way down.
    var collapsedOffsetPx by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val peekHeightPx = with(density) { RecentsPeekHeight.roundToPx() }
    val navigationBarHeightPx = WindowInsets.navigationBars.getBottom(density)

    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.collect { settledValue -> savedValue = settledValue }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .anchoredDraggable(state, Orientation.Vertical)
            .onSizeChanged { size ->
                collapsedOffsetPx =
                    (size.height - peekHeightPx - navigationBarHeightPx).toFloat()

                state.updateAnchors(
                    DraggableAnchors {
                        RecentsSheetValue.Expanded at 0f
                        RecentsSheetValue.Collapsed at collapsedOffsetPx
                    }
                )
            }
            .offset {
                val offset = state.offset

                IntOffset(x = 0, y = if (offset.isNaN()) 0 else offset.roundToInt())
            }
            .clip(
                RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius)
            )
            .background(KeyboardBarBackground)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(RecentsPeekHeight)
                // A tap is the whole gesture for anyone who does not want to drag.
                .clickable {
                    scope.launch { state.animateTo(state.settledValue.opposite()) }
                }
        ) {
            Box(
                modifier = Modifier
                    .width(HandleWidth)
                    .height(HandleHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White.copy(alpha = 0.4f))
            )

            Text(
                text = stringResource(R.string.recent_apps),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(TileSpacing),
            contentPadding = PaddingValues(SheetPadding),
            modifier = Modifier.offset {
                val sheetOffset = state.offset
                val closed = if (sheetOffset.isNaN() || collapsedOffsetPx <= 0f) {
                    if (state.settledValue == RecentsSheetValue.Collapsed) 1f else 0f
                } else {
                    (sheetOffset / collapsedOffsetPx).coerceIn(0f, 1f)
                }

                IntOffset(x = 0, y = (closed * navigationBarHeightPx).roundToInt())
            }
        ) {
            items(recentChannels, key = { channel -> channel.id }) { channel ->
                ChannelThumbnail(
                    channel = channel,
                    onClick = {
                        scope.launch { state.animateTo(RecentsSheetValue.Collapsed) }
                        onChannelClick(channel)
                    },
                    modifier = Modifier.width(TileWidth).aspectRatio(THUMBNAIL_ASPECT_RATIO)
                )
            }
        }

        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

private fun RecentsSheetValue.opposite(): RecentsSheetValue {
    return if (this == RecentsSheetValue.Collapsed) {
        RecentsSheetValue.Expanded
    } else {
        RecentsSheetValue.Collapsed
    }
}
