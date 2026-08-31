package wseemann.media.romote.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import wseemann.media.romote.network.ConnectivityLocalNetworkMonitor
import wseemann.media.romote.network.LocalNetworkMonitor
import javax.inject.Singleton

/**
 * Provides the local network monitor when injected
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideLocalNetworkMonitor(@ApplicationContext context: Context): LocalNetworkMonitor {
        return ConnectivityLocalNetworkMonitor(context)
    }
}
