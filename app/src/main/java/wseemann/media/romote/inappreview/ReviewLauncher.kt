package wseemann.media.romote.inappreview

import android.app.Activity

/**
 * Launches the platform's in-app review flow, where one exists.
 * Implementations are supplied per product flavor.
 */
interface ReviewLauncher {

    /**
     * Returns true if a review flow was actually shown.
     */
    suspend fun launchReviewFlow(activity: Activity): Boolean
}
