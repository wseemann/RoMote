package wseemann.media.romote.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import wseemann.media.romote.database.recents.RecentChannelDao
import wseemann.media.romote.database.recents.RecentsDatabase
import wseemann.media.romote.recents.RecentChannelsRepository
import wseemann.media.romote.recents.RecentChannelsRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecentsModule {

    @Singleton
    @Provides
    fun provideRecentsDatabase(@ApplicationContext context: Context): RecentsDatabase {
        return Room.databaseBuilder(
            context,
            RecentsDatabase::class.java,
            RecentsDatabase.DATABASE_NAME
        ).build()
    }

    @Singleton
    @Provides
    fun provideRecentChannelDao(recentsDatabase: RecentsDatabase): RecentChannelDao {
        return recentsDatabase.recentChannelDao()
    }

    @Singleton
    @Provides
    fun provideRecentChannelsRepository(recentChannelDao: RecentChannelDao): RecentChannelsRepository {
        return RecentChannelsRepositoryImpl(recentChannelDao)
    }
}
