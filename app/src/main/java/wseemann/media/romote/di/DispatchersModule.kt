package wseemann.media.romote.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * Provides the dispatchers the app launches work on, so that everything which uses one takes it
 * as a dependency rather than reaching for the global object.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}

/** The dispatcher for blocking IO: network calls, SQLite reads, socket work. */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class IoDispatcher

/** The main thread, for delivering results to the UI. */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class MainDispatcher
