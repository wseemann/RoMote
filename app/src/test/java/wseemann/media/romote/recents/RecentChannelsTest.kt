package wseemann.media.romote.recents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import wseemann.media.romote.data.RecentChannel

class RecentChannelsTest {

    @Test
    fun `builds icon urls against the host it is given`() {
        val channelItems = RecentChannels.toChannelItems(
            recentChannels = listOf(RecentChannel(id = "12", title = "Netflix")),
            host = "http://192.168.1.20:8060"
        )

        assertEquals("http://192.168.1.20:8060/query/icon/12", channelItems.single().iconUrl)
    }

    /** The icon urls are the whole reason the host is a parameter, so a moved device rebuilds them. */
    @Test
    fun `rebuilds icon urls when the host changes`() {
        val recentChannels = listOf(RecentChannel(id = "12", title = "Netflix"))

        val before = RecentChannels.toChannelItems(recentChannels, "http://192.168.1.20:8060")
        val after = RecentChannels.toChannelItems(recentChannels, "http://192.168.1.31:8060")

        assertEquals("http://192.168.1.20:8060/query/icon/12", before.single().iconUrl)
        assertEquals("http://192.168.1.31:8060/query/icon/12", after.single().iconUrl)
    }

    @Test
    fun `keeps the order it is given`() {
        val channelItems = RecentChannels.toChannelItems(
            recentChannels = listOf(
                RecentChannel(id = "13", title = "Prime Video"),
                RecentChannel(id = "12", title = "Netflix"),
                RecentChannel(id = "837", title = "YouTube")
            ),
            host = "http://192.168.1.20:8060"
        )

        assertEquals(listOf("13", "12", "837"), channelItems.map { it.id })
    }

    @Test
    fun `carries the title through for the tile fallback`() {
        val channelItems = RecentChannels.toChannelItems(
            recentChannels = listOf(RecentChannel(id = "12", title = "Netflix")),
            host = "http://192.168.1.20:8060"
        )

        assertEquals("Netflix", channelItems.single().title)
    }

    @Test
    fun `caps the list at the maximum`() {
        val recentChannels = (1..MORE_THAN_MAX).map { index ->
            RecentChannel(id = index.toString(), title = "Channel $index")
        }

        val channelItems = RecentChannels.toChannelItems(recentChannels, "http://192.168.1.20:8060")

        assertEquals(RecentChannels.MAX_RECENTS, channelItems.size)
    }

    /**
     * With no device there is no host, and an icon url built against a null host would point
     * nowhere - so there is nothing worth showing rather than a row of broken tiles.
     */
    @Test
    fun `returns nothing when there is no host`() {
        val recentChannels = listOf(RecentChannel(id = "12", title = "Netflix"))

        assertTrue(RecentChannels.toChannelItems(recentChannels, host = null).isEmpty())
        assertTrue(RecentChannels.toChannelItems(recentChannels, host = "").isEmpty())
    }

    private companion object {
        const val MORE_THAN_MAX = 25
    }
}
