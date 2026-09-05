package wseemann.media.romote.recents

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import wseemann.media.romote.data.ChannelItem
import wseemann.media.romote.data.RecentChannel

/** The mapping the recents sheet renders from, kept pure so it can be tested without a device. */
object RecentChannels {

    /** How many channels a device keeps. Older entries are dropped as new ones are launched. */
    const val MAX_RECENTS = 10

    /**
     * Rebuilds the icon urls against [host], which is why they are not stored: the host is rewritten
     * every time the device is rediscovered. With no device there is no host to build them from, so
     * there is nothing worth showing.
     */
    fun toChannelItems(recentChannels: List<RecentChannel>, host: String?): ImmutableList<ChannelItem> {
        if (host.isNullOrEmpty()) {
            return persistentListOf()
        }

        return recentChannels
            .take(MAX_RECENTS)
            .map { recentChannel ->
                ChannelItem(
                    id = recentChannel.id,
                    title = recentChannel.title,
                    iconUrl = ChannelItem.iconUrl(host, recentChannel.id)
                )
            }
            .toPersistentList()
    }
}
