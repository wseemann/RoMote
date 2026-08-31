package wseemann.media.romote.composables.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Maps the XML AppTheme (styles.xml, parent Theme.MaterialComponents.DayNight) onto Material 3:
 *
 *   colorPrimary        -> primary
 *   colorAccent /
 *   colorSecondary      -> secondary
 *   colorSecondaryVariant -> tertiary
 *   ToolbarStyle titleTextColor -> onPrimary
 *
 * There is no values-night/styles.xml override, so the XML theme resolves the same brand purples in
 * light and dark. The Compose chrome that wants that branded purple bar - the top app bar, the tab
 * strip, the FAB, the connected-device dot - names [Purple]/[OnPurple] directly rather than going
 * through the primary role, the same way PurpleButton and DeviceIconBackground already do. That
 * leaves `primary` free to be what Material 3 uses it for: a foreground. Material 3 draws every
 * dialog action button and every settings category header in it, and #65318F on a dark surface is
 * 1.6:1 - so the dark scheme takes its tone 80 instead. The backgrounds are pinned to the values
 * Theme.MaterialComponents.DayNight resolves to, because the Material 3 defaults are a tinted white
 * and a warm grey that read as a different app next to the XML screens. Typography and shapes stay
 * at the Material 3 defaults because the XML theme never customized them.
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple,
    onPrimary = OnPurple,
    secondary = PurpleAccent,
    tertiary = PurpleAccentTwo,
    background = LightBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = PurpleLight,
    onPrimary = OnPurpleLight,
    secondary = PurpleAccent,
    tertiary = PurpleAccentTwo,
    background = DarkBackground
)

@Composable
fun RomoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
