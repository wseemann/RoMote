package wseemann.media.romote.inappreview

import android.app.Activity
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayReviewLauncher @Inject constructor(
    private val reviewManager: ReviewManager
) : ReviewLauncher {

    override suspend fun launchReviewFlow(activity: Activity): Boolean {
        val reviewInfo = reviewManager.requestReview()
        reviewManager.launchReview(activity, reviewInfo)

        return true
    }
}
