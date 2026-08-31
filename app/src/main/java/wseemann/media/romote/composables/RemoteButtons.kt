package wseemann.media.romote.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import wseemann.media.romote.utils.ViewUtils

/** The 52dip every button row in fragment_remote.xml was fixed at. */
private val RemoteButtonHeight = 52.dp

/** 10dip of margin on each side of adjacent buttons. */
private val RemoteButtonSpacing = 20.dp

/** The duration VibratingImageButton and RepeatingImageButton both vibrated for. */
private const val VibrateDurationMillis = 100

/** The interval the old XML remote passed to RepeatingImageButton.setRepeatListener. */
private const val RepeatIntervalMillis = 400L

/** The 15dp corner radius of every shape in @drawable/remote_button_bg. */
private val ButtonCornerRadius = 15.dp

/** The bottom/right inset both layers of that drawable are drawn with. */
private val ButtonEdgeInset = 2.dp

/** The extra top/left inset on its second layer, which is what leaves the highlight hairline. */
private val ButtonHighlightWidth = 1.dp

private val ButtonGradientTop = Color(0xFF544C5E)
private val ButtonGradientBottom = Color(0xFF0A0A0A)
private val ButtonFill = Color(0xFF151218)

/** The start color of @drawable/background_glow_bg, which fades out to transparent. */
private val GlowColor = Color(0xFFA0A0A0)

/** @drawable/background_glow_bg is drawn at 70% of the size it sits in. */
private const val GlowRadiusFraction = 0.7f

/**
 * A row of remote buttons, matching the LinearLayouts of fragment_remote.xml: a fixed height, 20dp
 * between neighbours, and 20dp of margin at each end.
 */
@Composable
fun RemoteButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(RemoteButtonSpacing),
        modifier = modifier
            .fillMaxWidth()
            .height(RemoteButtonHeight)
            .padding(horizontal = 20.dp),
        content = content
    )
}

/**
 * Replaces VibratingImageButton on @drawable/remote_button_bg. The haptics still run through
 * [ViewUtils.provideHapticFeedback] so the user's "vibrate" preference keeps gating them, and there
 * is no ripple because the ImageButton this replaced never showed one.
 */
@Composable
fun RemoteButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val view = LocalView.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .remoteButtonBackground()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    ViewUtils.provideHapticFeedback(view, VibrateDurationMillis)
                    onClick()
                }
            )
    ) {
        // fillMaxSize plus ContentScale.Fit is the ImageView's fitCenter default, which is what
        // scaled these icons to the row's height; ContentScale.None leaves the power button's icon
        // at its natural size, the way its scaleType="center" did.
        Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Replaces RepeatingImageButton and the VibratingImageButton at the centre of the d-pad. The button
 * itself is transparent - the d-pad art is the image underneath it - and shows the radial glow of
 * @drawable/background_glow_selector while pressed.
 *
 * When [repeating], holding the button starts firing [onClick] once the long press timeout elapses
 * and then every 400ms, which is what RepeatingImageButton's repeater did. The long press is
 * consumed so that letting go afterwards doesn't also count as a tap, the way View suppressed
 * performClick once performLongClick had handled the gesture.
 */
@Composable
fun DPadButton(
    onClick: () -> Unit,
    repeating: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val longPressTimeoutMillis = LocalViewConfiguration.current.longPressTimeoutMillis

    // Kept current so a long hold that outlives a recomposition still calls the latest lambda.
    val currentOnClick by rememberUpdatedState(onClick)

    fun press() {
        ViewUtils.provideHapticFeedback(view, VibrateDurationMillis)
        currentOnClick()
    }

    LaunchedEffect(isPressed) {
        if (!repeating || !isPressed) {
            return@LaunchedEffect
        }

        delay(longPressTimeoutMillis)

        while (true) {
            press()
            delay(RepeatIntervalMillis)
        }
    }

    Box(
        modifier = modifier
            .drawBehind {
                if (isPressed) {
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(GlowColor, Color.Transparent),
                            center = center,
                            radius = GlowRadiusFraction * size.minDimension
                        )
                    )
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = contentDescription,
                // Present only so a hold is treated as handled; the repeat effect above is what
                // actually fires while the button is held.
                onLongClick = if (repeating) ({}) else null,
                onClick = { press() }
            )
    )
}

/**
 * Redraws @drawable/remote_button_bg, a layer-list Compose can't load through painterResource: a
 * rounded rect filled with the vertical gradient, covered from 1dp in on the top and left by the
 * flat fill, both stopping 2dp short of the bottom and right edges.
 */
private fun Modifier.remoteButtonBackground(): Modifier = drawBehind {
    val edgeInset = ButtonEdgeInset.toPx()
    val highlight = ButtonHighlightWidth.toPx()
    val cornerRadius = CornerRadius(ButtonCornerRadius.toPx())

    val outerSize = Size(size.width - edgeInset, size.height - edgeInset)

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(ButtonGradientTop, ButtonGradientBottom),
            startY = 0f,
            endY = outerSize.height
        ),
        size = outerSize,
        cornerRadius = cornerRadius
    )

    drawRoundRect(
        color = ButtonFill,
        topLeft = Offset(highlight, highlight),
        size = Size(outerSize.width - highlight, outerSize.height - highlight),
        cornerRadius = cornerRadius
    )
}
