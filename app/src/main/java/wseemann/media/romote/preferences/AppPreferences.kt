package wseemann.media.romote.preferences

import android.content.SharedPreferences
import androidx.core.content.edit

class AppPreferences(private val sharedPreferences: SharedPreferences) {

    fun isHapticFeedbackEnabled(): Boolean = sharedPreferences.getBoolean(APP_PREFERENCE_HAPTIC_FEEDBACK, false)

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(APP_PREFERENCE_HAPTIC_FEEDBACK, enabled)
        }
    }

    fun isNotificationWidgetEnabled(): Boolean = sharedPreferences.getBoolean(APP_PREFERENCE_NOTIFICATION_WIDGET, false)

    fun setNotificationWidgetEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(APP_PREFERENCE_NOTIFICATION_WIDGET, enabled)
        }
    }

    fun isShakeToPauseEnabled(): Boolean = sharedPreferences.getBoolean(APP_PREFERENCE_SHAKE_TO_PAUSE, false)

    fun setShakeToPauseEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(APP_PREFERENCE_SHAKE_TO_PAUSE, enabled)
        }
    }

    fun getFirstLaunchMillis(): Long = sharedPreferences.getLong(APP_PREFERENCE_REVIEW_FIRST_LAUNCH, 0L)

    fun setFirstLaunchMillis(millis: Long) {
        sharedPreferences.edit {
            putLong(APP_PREFERENCE_REVIEW_FIRST_LAUNCH, millis)
        }
    }

    fun getEngagedSessionCount(): Int = sharedPreferences.getInt(APP_PREFERENCE_REVIEW_ENGAGED_SESSIONS, 0)

    fun setEngagedSessionCount(count: Int) {
        sharedPreferences.edit {
            putInt(APP_PREFERENCE_REVIEW_ENGAGED_SESSIONS, count)
        }
    }

    fun hasSeenRemoteAccessHelp(): Boolean = sharedPreferences.getBoolean(APP_PREFERENCE_REMOTE_ACCESS_HELP_SEEN, false)

    fun setRemoteAccessHelpSeen() {
        sharedPreferences.edit {
            putBoolean(APP_PREFERENCE_REMOTE_ACCESS_HELP_SEEN, true)
        }
    }

    /** When the review flow was last launched, whether or not Play went on to show a card. */
    fun getLastReviewPromptMillis(): Long = sharedPreferences.getLong(APP_PREFERENCE_REVIEW_LAST_PROMPT, 0L)

    fun setLastReviewPromptMillis(millis: Long) {
        sharedPreferences.edit {
            putLong(APP_PREFERENCE_REVIEW_LAST_PROMPT, millis)
        }
    }

    companion object {
        private const val APP_PREFERENCE_HAPTIC_FEEDBACK = "haptic_feedback_preference"
        const val APP_PREFERENCE_NOTIFICATION_WIDGET = "notification_checkbox_preference"
        private const val APP_PREFERENCE_SHAKE_TO_PAUSE = "shake_to_pause_checkbox_preference"
        private const val APP_PREFERENCE_REVIEW_FIRST_LAUNCH = "review_first_launch_millis"
        private const val APP_PREFERENCE_REVIEW_ENGAGED_SESSIONS = "review_engaged_session_count"
        private const val APP_PREFERENCE_REVIEW_LAST_PROMPT = "review_last_prompt_millis"
        private const val APP_PREFERENCE_REMOTE_ACCESS_HELP_SEEN = "remote_access_help_seen"
    }
}
