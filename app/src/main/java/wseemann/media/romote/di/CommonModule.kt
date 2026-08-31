package wseemann.media.romote.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideCommandHelper(
        preferenceUtils: PreferenceUtils
    ): CommandHelper {
        return CommandHelper(preferenceUtils)
    }
}
