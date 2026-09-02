package wseemann.media.romote.inappreview

import android.app.Activity
import timber.log.Timber
import wseemann.media.romote.preferences.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppReviewManager @Inject constructor(
    private val appPreferences: AppPreferences,
    private val reviewLauncher: ReviewLauncher
) {

    @Volatile
    private var sessionCounted = false

    @Volatile
    private var promptAttempted = false

    fun onAppSessionStarted() {
        if (appPreferences.getFirstLaunchMillis() <= 0L) {
            appPreferences.setFirstLaunchMillis(System.currentTimeMillis())
        }
    }

    fun onDeviceCommandSucceeded() {
        if (sessionCounted) {
            return
        }

        sessionCounted = true
        appPreferences.setEngagedSessionCount(appPreferences.getEngagedSessionCount() + 1)
    }

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
            if (reviewLauncher.launchReviewFlow(activity)) {
                appPreferences.setLastReviewPromptMillis(System.currentTimeMillis())

                Timber.tag(TAG).d("In-app review flow finished")
            }
        } catch (ex: Exception) {
            Timber.tag(TAG).e(ex, "In-app review flow failed")
        }
    }

    private companion object {
        const val TAG = "AppReviewManager"
    }
}
