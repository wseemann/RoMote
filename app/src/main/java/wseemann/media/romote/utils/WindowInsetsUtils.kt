@file:JvmName("WindowInsetsUtils")

package wseemann.media.romote.utils

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * Edge-to-edge helper. The app draws behind the system bars (see [enableRomoteEdgeToEdge]), so the
 * status bar itself is transparent and the purple has to come from the app bar extending underneath
 * it.
 */

/** Scrims androidx uses for the navigation bar on API levels that can't render it transparent. */
private val NAVIGATION_BAR_LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val NAVIGATION_BAR_DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

/**
 * Turns on edge-to-edge with a transparent status bar.
 *
 * The status bar deliberately uses [SystemBarStyle.dark] rather than the `auto` default: `auto`
 * switches the status bar icons to dark in light mode, which would make them invisible against the
 * purple app bar drawn underneath. `dark` keeps them white in both day and night.
 */
fun enableRomoteEdgeToEdge(activity: ComponentActivity) {
    activity.enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.auto(
            NAVIGATION_BAR_LIGHT_SCRIM,
            NAVIGATION_BAR_DARK_SCRIM
        )
    )
}
