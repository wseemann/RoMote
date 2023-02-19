package wseemann.media.romote.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import wseemann.media.romote.utils.CommandHelper
import wseemann.media.romote.utils.PreferenceUtils
import javax.inject.Singleton

/**
 * Provides SharedPreferences when injected
 */
@Module
@InstallIn(SingletonComponent::class)
object CommonModule {
    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Singleton
    @Provides
    fun providePreferenceUtils(
        @ApplicationContext context: Context,
        sharedPreferences: SharedPreferences
    ): PreferenceUtils {
        PreferenceUtilsSingleton.preferenceUtils = PreferenceUtils(context, sharedPreferences)
        return PreferenceUtilsSingleton.preferenceUtils
    }

    @Singleton
    @Provides
    fun provideCommandHelper(
        preferenceUtils: PreferenceUtils
    ): CommandHelper {
        return CommandHelper(preferenceUtils)
    }

    object PreferenceUtilsSingleton {
        lateinit var preferenceUtils: PreferenceUtils
    }
}
