package wseemann.media.romote.recents

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import wseemann.media.romote.data.RecentChannel
import wseemann.media.romote.database.recents.RecentChannelDao
import wseemann.media.romote.database.recents.RecentChannelEntity

/**
 * No withContext around the dao calls: Room's suspend functions and Flow queries dispatch to its
 * own executor, so they are already safe to start from any thread.
 */
class RecentChannelsRepositoryImpl(private val recentChannelDao: RecentChannelDao) : RecentChannelsRepository {

    override fun observeRecents(serialNumber: String): Flow<List<RecentChannel>> {
        return recentChannelDao.observeRecents(serialNumber, RecentChannels.MAX_RECENTS)
            .map { entities ->
                entities.map { entity -> RecentChannel(id = entity.channelId, title = entity.title) }
            }
    }

    override suspend fun recordLaunch(serialNumber: String, channelId: String, title: String) {
        recentChannelDao.upsert(
            RecentChannelEntity(
                deviceSerialNumber = serialNumber,
                channelId = channelId,
                title = title,
                launchedAtMillis = System.currentTimeMillis()
            )
        )

        recentChannelDao.trim(serialNumber, RecentChannels.MAX_RECENTS)
    }

    override suspend fun clearForDevice(serialNumber: String) {
        recentChannelDao.deleteForDevice(serialNumber)
    }
}
