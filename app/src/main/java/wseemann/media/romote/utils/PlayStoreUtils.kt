package wseemann.media.romote.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import timber.log.Timber

private const val PLAY_STORE_PACKAGE = "com.android.vending"
private const val PLAY_STORE_MARKET_URI = "market://details?id="
private const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id="

/**
 * Opens the app's Play Store listing.
 *
 * This is the explicit "rate the app" route, and deliberately not the in-app review API: Play
 * enforces a quota it won't disclose, so a button wired to that API does nothing at all once the
 * quota is spent. The listing always opens.
 *
 * market:// goes straight to the Play Store app; a device without it - or with it disabled - falls
 * back to the web listing in a browser.
 */
fun Context.openPlayStoreListing() {
    val marketIntent = Intent(Intent.ACTION_VIEW, "$PLAY_STORE_MARKET_URI$packageName".toUri())
        .setPackage(PLAY_STORE_PACKAGE)

    try {
        startActivity(marketIntent)
        return
    } catch (ex: ActivityNotFoundException) {
        Timber.d(ex, "Play Store app unavailable, falling back to the web listing")
    }

    try {
        startActivity(Intent(Intent.ACTION_VIEW, "$PLAY_STORE_WEB_URL$packageName".toUri()))
    } catch (ex: ActivityNotFoundException) {
        Timber.e(ex, "No handler for the Play Store listing")
    }
}
