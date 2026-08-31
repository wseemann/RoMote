package wseemann.media.romote.composables.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand colors, kept in sync with res/values/colors.xml so the Compose theme and the
 * remaining XML screens (styles.xml -> AppTheme) render the same purples.
 */
internal val Purple = Color(0xFF65318F)
internal val PurpleAccent = Color(0xFF843ABC)
internal val PurpleAccentTwo = Color(0xFF8F318A)
internal val OnPurple = Color(0xFFEBEBEB)

/**
 * The lighter purple the connect/scan buttons are tinted with in the XML layouts
 * (fragment_configure_device.xml, dialog_fragment_volume.xml). It is not a role in the color
 * scheme - it never was in the XML theme either - so Compose buttons that used to be those
 * MaterialButtons name it directly.
 */
internal val PurpleButton = Color(0xFFA865F3)

/**
 * @color/semi_transparent, the dot the device list draws beside a device that is not the connected
 * one. The connected device's dot is [Purple].
 */
internal val SemiTransparentBlack = Color(0x66000000)

/**
 * Window backgrounds. The XML screens take these from Theme.MaterialComponents.DayNight rather than
 * from colors.xml, so they are spelled out here to keep the Compose screens on the same surface.
 */
internal val LightBackground = Color(0xFFFFFFFF)
internal val DarkBackground = Color(0xFF121212)
