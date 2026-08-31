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
import javax.inject.Singleton

/**
 * Provides the Play ReviewManager when injected.
 */
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
}
