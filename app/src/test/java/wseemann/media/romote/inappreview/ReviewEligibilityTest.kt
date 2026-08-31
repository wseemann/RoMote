package wseemann.media.romote.inappreview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ReviewEligibilityTest {

    @Test
    fun `is eligible once every condition is met`() {
        assertTrue(ReviewEligibility.isEligible(state(), NOW))
    }

    @Test
    fun `is not eligible below the engaged session threshold`() {
        val state = state(
            engagedSessionCount = ReviewEligibility.MIN_ENGAGED_SESSIONS - 1
        )

        assertFalse(ReviewEligibility.isEligible(state, NOW))
    }

    @Test
    fun `is eligible at exactly the engaged session threshold`() {
        val state = state(engagedSessionCount = ReviewEligibility.MIN_ENGAGED_SESSIONS)

        assertTrue(ReviewEligibility.isEligible(state, NOW))
    }

    @Test
    fun `is not eligible before the first session has been recorded`() {
        assertFalse(ReviewEligibility.isEligible(state(firstLaunchMillis = 0L), NOW))
    }

    @Test
    fun `is not eligible while the app is younger than the minimum age`() {
        // One millisecond short of the minimum.
        val state = state(
            firstLaunchMillis = NOW - ReviewEligibility.MIN_APP_AGE_MILLIS + 1
        )

        assertFalse(ReviewEligibility.isEligible(state, NOW))
    }

    @Test
    fun `is eligible at exactly the minimum app age`() {
        val state = state(firstLaunchMillis = NOW - ReviewEligibility.MIN_APP_AGE_MILLIS)

        assertTrue(ReviewEligibility.isEligible(state, NOW))
    }

    @Test
    fun `is not eligible inside the cooldown after a previous prompt`() {
        val state = state(
            lastPromptMillis = NOW - ReviewEligibility.PROMPT_COOLDOWN_MILLIS + 1
        )

        assertFalse(ReviewEligibility.isEligible(state, NOW))
    }

    @Test
    fun `is eligible once the cooldown has passed`() {
        val state = state(lastPromptMillis = NOW - ReviewEligibility.PROMPT_COOLDOWN_MILLIS)

        assertTrue(ReviewEligibility.isEligible(state, NOW))
    }

    /**
     * A clock moved backwards leaves a stored timestamp in the future. That has to hold the prompt
     * back rather than satisfy the age rule by way of a negative elapsed time.
     */
    @Test
    fun `is not eligible when the first launch timestamp is in the future`() {
        val state = state(firstLaunchMillis = NOW + TimeUnit.DAYS.toMillis(1))

        assertFalse(ReviewEligibility.isEligible(state, NOW))
    }

    @Test
    fun `is not eligible when the last prompt timestamp is in the future`() {
        val state = state(lastPromptMillis = NOW + TimeUnit.DAYS.toMillis(1))

        assertFalse(ReviewEligibility.isEligible(state, NOW))
    }

    private fun state(
        engagedSessionCount: Int = ReviewEligibility.MIN_ENGAGED_SESSIONS + 1,
        firstLaunchMillis: Long = NOW - ReviewEligibility.MIN_APP_AGE_MILLIS - 1,
        lastPromptMillis: Long = 0L
    ) = ReviewState(
        engagedSessionCount = engagedSessionCount,
        firstLaunchMillis = firstLaunchMillis,
        lastPromptMillis = lastPromptMillis
    )

    private companion object {
        /** A fixed "now" well past every threshold, so the arithmetic never goes negative. */
        val NOW: Long = TimeUnit.DAYS.toMillis(10_000)
    }
}
