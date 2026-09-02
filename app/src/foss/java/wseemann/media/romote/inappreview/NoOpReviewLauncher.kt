package wseemann.media.romote.inappreview

import android.app.Activity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The FOSS build ships without the proprietary Play review library, so there is no review flow to
 * launch. Engagement tracking still runs; the prompt simply never appears.
 */
@Singleton
class NoOpReviewLauncher @Inject constructor() : ReviewLauncher {

    override suspend fun launchReviewFlow(activity: Activity): Boolean {
        return false
    }
}
