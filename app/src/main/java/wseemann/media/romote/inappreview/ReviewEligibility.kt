package wseemann.media.romote.inappreview

import java.util.concurrent.TimeUnit

data class ReviewState(val engagedSessionCount: Int, val firstLaunchMillis: Long, val lastPromptMillis: Long)

object ReviewEligibility {

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
