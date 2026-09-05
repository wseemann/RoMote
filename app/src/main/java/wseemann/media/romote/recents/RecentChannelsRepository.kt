package wseemann.media.romote.recents

import kotlinx.coroutines.flow.Flow
import wseemann.media.romote.data.RecentChannel

/**
 * The channels launched from RoMote, newest first, scoped to one device.
 *
 * An interface rather than a concrete class so that what depends on it can be tested with a fake -
 * the project has no mocking framework, so a seam is the only way in. Compare DeviceRepository,
 * which is concrete and consequently has nothing testing the code that uses it.
 */
interface RecentChannelsRepository {

    fun observeRecents(serialNumber: String): Flow<List<RecentChannel>>

    suspend fun recordLaunch(serialNumber: String, channelId: String, title: String)

    suspend fun clearForDevice(serialNumber: String)
}
