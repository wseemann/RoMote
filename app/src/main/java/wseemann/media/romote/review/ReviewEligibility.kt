package wseemann.media.romote.review

import java.util.concurrent.TimeUnit

/**
 * What the eligibility rules need to know, read out of AppPreferences by [AppReviewManager].
 */
data class ReviewState(
    val engagedSessionCount: Int,
    val firstLaunchMillis: Long,
    val lastPromptMillis: Long
)

/**
 * Decides whether the Play in-app review card is worth asking for.
 *
 * Play won't say whether a card was actually shown, and it enforces a quota of its own that it
 * won't disclose, so the app has to be conservative on its own account: only users who have driven
 * a Roku across several sessions, only after the app has been around for a few days, and only once
 * every few months.
 *
 * Kept free of Android types so the rules can be unit tested directly - see ReviewEligibilityTest.
 */
object ReviewEligibility {

    /** Sessions in which a command actually reached the device. */
    const val MIN_ENGAGED_SESSIONS = 5

    val MIN_APP_AGE_MILLIS: Long = TimeUnit.DAYS.toMillis(3)

    val PROMPT_COOLDOWN_MILLIS: Long = TimeUnit.DAYS.toMillis(90)

    fun isEligible(state: ReviewState, nowMillis: Long): Boolean {
        if (state.engagedSessionCount < MIN_ENGAGED_SESSIONS) {
            return false
        }

        // 0 means the first session hasn't been recorded yet, so nothing can be said about age.
        if (state.firstLaunchMillis <= 0L) {
            return false
        }

        // A stored timestamp in the future means the clock moved backwards. Treating the elapsed
        // time as negative holds the prompt back rather than letting the skew satisfy the rule.
        if (nowMillis - state.firstLaunchMillis < MIN_APP_AGE_MILLIS) {
            return false
        }

        if (state.lastPromptMillis > 0L &&
            nowMillis - state.lastPromptMillis < PROMPT_COOLDOWN_MILLIS
        ) {
            return false
        }

        return true
    }
}
