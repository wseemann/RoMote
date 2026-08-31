package wseemann.media.romote.preferences

import android.content.SharedPreferences
import androidx.core.content.edit

class AppPreferences(
    private val sharedPreferences: SharedPreferences
) {

    fun isFirstUse(): Boolean {
        return sharedPreferences.getBoolean(APP_PREFERENCE_FIRST_USE, true)
    }

    fun setFirstUse(isFirstUse: Boolean) {
        sharedPreferences.edit {
            putBoolean(APP_PREFERENCE_FIRST_USE, isFirstUse)
        }
    }

    fun isHapticFeedbackEnabled(): Boolean {
        return sharedPreferences.getBoolean(APP_PREFERENCE_HAPTIC_FEEDBACK, false)
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(APP_PREFERENCE_HAPTIC_FEEDBACK, enabled)
        }
    }

    fun isNotificationWidgetEnabled(): Boolean {
        return sharedPreferences.getBoolean(APP_PREFERENCE_NOTIFICATION_WIDGET, false)
    }

    fun setNotificationWidgetEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(APP_PREFERENCE_NOTIFICATION_WIDGET, enabled)
        }
    }

    fun isShakeToPauseEnabled(): Boolean {
        return sharedPreferences.getBoolean(APP_PREFERENCE_SHAKE_TO_PAUSE, false)
    }

    fun setShakeToPauseEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(APP_PREFERENCE_SHAKE_TO_PAUSE, enabled)
        }
    }

    companion object {
        private const val APP_PREFERENCE_FIRST_USE = "first_use"
        private const val APP_PREFERENCE_HAPTIC_FEEDBACK = "haptic_feedback_preference"
        const val APP_PREFERENCE_NOTIFICATION_WIDGET = "notification_checkbox_preference"
        private const val APP_PREFERENCE_SHAKE_TO_PAUSE = "shake_to_pause_checkbox_preference"
    }
}
