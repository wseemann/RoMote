package wseemann.media.romote.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import wseemann.media.romote.preferences.AppPreferences
import wseemann.media.romote.utils.PreferenceUtils
import javax.inject.Singleton

/**
 * Provides SharedPreferences when injected
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Singleton
    @Provides
    fun provideAppPreferences(sharedPreferences: SharedPreferences): AppPreferences = AppPreferences(sharedPreferences)

    @Singleton
    @Provides
    fun providePreferenceUtils(appPreferences: AppPreferences): PreferenceUtils {
        PreferenceUtilsSingleton.preferenceUtils = PreferenceUtils(appPreferences)
        return PreferenceUtilsSingleton.preferenceUtils
    }

    object PreferenceUtilsSingleton {
        lateinit var preferenceUtils: PreferenceUtils
    }
}
