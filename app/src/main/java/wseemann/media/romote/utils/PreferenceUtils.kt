package wseemann.media.romote.utils

import wseemann.media.romote.preferences.AppPreferences

class PreferenceUtils(private val appPreferences: AppPreferences) {
    fun shouldProvideHapticFeedback(): Boolean = appPreferences.isHapticFeedbackEnabled()
}
