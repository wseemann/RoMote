package wseemann.media.romote.di

import android.content.Context
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import wseemann.media.romote.BuildConfig
import wseemann.media.romote.inappreview.PlayReviewLauncher
import wseemann.media.romote.inappreview.ReviewLauncher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InAppReviewModule {

    @Singleton
    @Provides
    fun provideReviewManager(@ApplicationContext context: Context): ReviewManager {
        return if (BuildConfig.DEBUG) {
            FakeReviewManager(context)
        } else {
            ReviewManagerFactory.create(context)
        }
    }

    @Singleton
    @Provides
    fun provideReviewLauncher(playReviewLauncher: PlayReviewLauncher): ReviewLauncher {
        return playReviewLauncher
    }
}
