@file:JvmName("WindowInsetsUtils")

package wseemann.media.romote.utils

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/** Scrims androidx uses for the navigation bar on API levels that can't render it transparent. */
private val NAVIGATION_BAR_LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val NAVIGATION_BAR_DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

/**
 * The status bar deliberately uses [SystemBarStyle.dark] rather than the `auto` default: `auto`
 * switches the status bar icons to dark in light mode, which would make them invisible against the
 * purple app bar drawn underneath. `dark` keeps them white in both day and night.
 *
 * [darkNavigationBar] asks for the same thing at the bottom, for a screen that paints its own dark
 * surface behind the navigation bar - the remote tab. All three of what [SystemBarStyle.dark] does
 * there matter: the bar's color becomes transparent on every API level, so API 26-28 stops painting
 * [NAVIGATION_BAR_LIGHT_SCRIM] as a near-white band; its night mode turns off
 * `isNavigationBarContrastEnforced` on API 29+, so the system stops adding a contrast scrim of its
 * own in three-button mode; and the icons and the gesture handle stay white rather than going dark
 * against the artwork. Only asked for in the day theme.
 *
 * Safe to call again on a started activity - that is how the style is meant to be changed.
 */
fun enableRomoteEdgeToEdge(activity: ComponentActivity, darkNavigationBar: Boolean = false) {
    activity.enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = if (darkNavigationBar) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.auto(
                NAVIGATION_BAR_LIGHT_SCRIM,
                NAVIGATION_BAR_DARK_SCRIM
            )
        }
    )
}
