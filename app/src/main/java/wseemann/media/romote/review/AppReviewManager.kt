package wseemann.media.romote.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import timber.log.Timber
import wseemann.media.romote.preferences.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the Play in-app review card.
 *
 * Google's guidance shapes most of this: the card can't hang off a button, because Play's own
 * quota may silently swallow the request and leave the button looking broken; the app can't ask a
 * qualifying question first; and the API never reports what the user did, or whether a card even
 * appeared. So the trigger is a usage milestone, and every failure is logged and dropped.
 *
 * The milestone is an *engaged session*: an app session in which a command actually reached the
 * connected Roku. The check itself runs from MainActivity.onResume, before this session can be
 * counted, so the count it reads always comes from earlier sessions - the card never lands on top
 * of the button press that earned it.
 *
 * A @Singleton, so "once per session" here means once per process.
 */
@Singleton
class AppReviewManager @Inject constructor(
    private val appPreferences: AppPreferences,
    private val reviewManager: ReviewManager
) {

    @Volatile
    private var sessionCounted = false

    @Volatile
    private var promptAttempted = false

    /**
     * Records when the app was first opened. SharedPreferences has no install date of its own, and
     * an existing install that predates this reads as first-launched now - which only delays the
     * first prompt, never brings it forward.
     */
    fun onAppSessionStarted() {
        if (appPreferences.getFirstLaunchMillis() <= 0L) {
            appPreferences.setFirstLaunchMillis(System.currentTimeMillis())
        }
    }

    /**
     * Counts this session as engaged. Called from every successful key press, so it has to be
     * idempotent for the life of the process.
     */
    fun onDeviceCommandSucceeded() {
        if (sessionCounted) {
            return
        }

        sessionCounted = true
        appPreferences.setEngagedSessionCount(appPreferences.getEngagedSessionCount() + 1)
    }

    /**
     * Asks Play for the card if the milestone has been met. Returning to MainActivity from settings
     * or the device info screen resumes it again, hence the once-per-process guard.
     */
    suspend fun maybeLaunchReviewFlow(activity: Activity) {
        if (promptAttempted) {
            return
        }

        val state = ReviewState(
            engagedSessionCount = appPreferences.getEngagedSessionCount(),
            firstLaunchMillis = appPreferences.getFirstLaunchMillis(),
            lastPromptMillis = appPreferences.getLastReviewPromptMillis()
        )

        if (!ReviewEligibility.isEligible(state, System.currentTimeMillis())) {
            return
        }

        promptAttempted = true

        try {
            // ReviewInfo is short lived, so it is requested here rather than pre-cached at startup.
            val reviewInfo = reviewManager.requestReview()
            reviewManager.launchReview(activity, reviewInfo)

            // Recorded whether or not a card was shown: Play won't say, and asking again straight
            // away would only burn its quota.
            appPreferences.setLastReviewPromptMillis(System.currentTimeMillis())

            Timber.tag(TAG).d("In-app review flow finished")
        } catch (ex: Exception) {
            // Deliberately silent. Google's guidance is to leave the user's flow untouched.
            Timber.tag(TAG).e(ex, "In-app review flow failed")
        }
    }

    private companion object {
        const val TAG = "AppReviewManager"
    }
}
