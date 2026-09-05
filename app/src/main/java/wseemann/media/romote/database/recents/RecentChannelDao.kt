package wseemann.media.romote.database.recents

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentChannelDao {

    @Query(
        "SELECT * FROM recent_channels WHERE deviceSerialNumber = :serialNumber " +
            "ORDER BY launchedAtMillis DESC LIMIT :limit"
    )
    fun observeRecents(serialNumber: String, limit: Int): Flow<List<RecentChannelEntity>>

    /**
     * Replace rather than ignore: relaunching a channel has to move it back to the front, and the
     * composite primary key means the existing row is the one being replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recentChannel: RecentChannelEntity)

    /** Drops everything the newest [limit] rows for this device pushed off the end. */
    @Query(
        "DELETE FROM recent_channels WHERE deviceSerialNumber = :serialNumber AND channelId NOT IN (" +
            "SELECT channelId FROM recent_channels WHERE deviceSerialNumber = :serialNumber " +
            "ORDER BY launchedAtMillis DESC LIMIT :limit)"
    )
    suspend fun trim(serialNumber: String, limit: Int)

    @Query("DELETE FROM recent_channels WHERE deviceSerialNumber = :serialNumber")
    suspend fun deleteForDevice(serialNumber: String)
}
