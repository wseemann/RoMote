package wseemann.media.romote.database.recents

import androidx.room.Entity

/**
 * The device's serial number is half of the primary key rather than a foreign key: the devices
 * table lives in a different database file, so there is nothing for Room to reference. Pairing the
 * two columns is also what makes relaunching a channel an update rather than a duplicate row.
 */
@Entity(tableName = "recent_channels", primaryKeys = ["deviceSerialNumber", "channelId"])
data class RecentChannelEntity(
    val deviceSerialNumber: String,
    val channelId: String,
    val title: String,
    val launchedAtMillis: Long
)
