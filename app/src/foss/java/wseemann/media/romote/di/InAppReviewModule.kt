package wseemann.media.romote.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import wseemann.media.romote.inappreview.NoOpReviewLauncher
import wseemann.media.romote.inappreview.ReviewLauncher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InAppReviewModule {

    @Singleton
    @Provides
    fun provideReviewLauncher(noOpReviewLauncher: NoOpReviewLauncher): ReviewLauncher {
        return noOpReviewLauncher
    }
}
