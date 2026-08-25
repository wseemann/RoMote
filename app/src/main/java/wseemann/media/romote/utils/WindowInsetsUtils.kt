@file:JvmName("WindowInsetsUtils")

package wseemann.media.romote.utils

import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Edge-to-edge helpers. The app draws behind the system bars (see [enableRomoteEdgeToEdge]), so
 * the status bar itself is transparent and the purple has to come from the app bar extending
 * underneath it.
 *
 * The padding helpers capture the view's padding up front - the listener fires again on every
 * inset change - and return the insets unconsumed so children still see them.
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

/** Pads [view] down by the status bar height so its background fills the status bar area. */
fun applyStatusBarTopPadding(view: View) {
    val initialTopPadding = view.paddingTop

    ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(v.paddingLeft, initialTopPadding + insets.top, v.paddingRight, v.paddingBottom)
        windowInsets
    }

    ViewCompat.requestApplyInsets(view)
}

/** Pads [view] up by the navigation bar height so content isn't hidden behind the gesture bar. */
fun applyNavigationBarBottomPadding(view: View) {
    val initialBottomPadding = view.paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, initialBottomPadding + insets.bottom)
        windowInsets
    }

    ViewCompat.requestApplyInsets(view)
}
