package wseemann.media.romote.composables.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand colors, and the source of truth for them. res/values/colors.xml keeps only the subset the
 * widget layout and the drawables still reference, so not every constant here has an XML twin.
 */
internal val Purple = Color(0xFF65318F)
internal val PurpleAccent = Color(0xFF843ABC)
internal val PurpleAccentTwo = Color(0xFF8F318A)
internal val OnPurple = Color(0xFFEBEBEB)

/**
 * [Purple] is a fill color, so on a dark surface it is unreadable as a foreground: #65318F measures
 * 1.6:1 against the Material 3 dialog container and 2.1:1 against [DarkBackground], where WCAG AA
 * asks for 4.5:1. These are its Material 3 dark-scheme tones (80 and 20), used as
 * `primary`/`onPrimary` there so that everything M3 draws in `primary` - dialog action buttons,
 * settings category headers - is legible. Chroma is pulled to 80% of what tone 80 allows, which
 * keeps it reading purple rather than pink.
 */
internal val PurpleLight = Color(0xFFDDB9F3)
internal val OnPurpleLight = Color(0xFF490081)

/** The lighter purple the connect/scan buttons are tinted with. Not a role in the color scheme. */
internal val PurpleButton = Color(0xFFA865F3)

/** The dot the device list draws beside a device that is not the connected one. */
internal val SemiTransparentBlack = Color(0x66000000)

/**
 * The disc a device's picture is drawn on in the device list.
 *
 * Roku serves those pictures as dark hardware on a transparent background, so the disc has to stay
 * light in both themes to keep the device visible; surfaceVariant resolves to a dark grey on the
 * dark theme and would swallow it.
 */
internal val DeviceIconBackground = Color(0xFFE6E1E5)

internal val LightBackground = Color(0xFFFFFFFF)
internal val DarkBackground = Color(0xFF121212)
