package wseemann.media.romote.composables.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The chrome that wants the branded purple bar - the top app bar, the tab strip, the FAB, the
 * connected-device dot - names [Purple]/[OnPurple] directly rather than going through the primary
 * role. That leaves `primary` free to be what Material 3 uses it for: a foreground. M3 draws every
 * dialog action button and every settings category header in it, and #65318F on a dark surface is
 * 1.6:1, so the dark scheme takes its tone 80 instead. The backgrounds are pinned because the M3
 * defaults are a tinted white and a warm grey that read as a different app.
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
fun RomoteTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
