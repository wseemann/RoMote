package wseemann.media.romote.database.recents

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Its own database file, separate from the devices one.
 *
 * DeviceDatabase is a hand-written SQLiteOpenHelper whose onUpgrade replays every ALTER TABLE from
 * version 2 up and swallows the duplicate-column failures, because an older build compared
 * newVersion instead of oldVersion. Its schema at version 5 is therefore not identical across
 * installs, and Room validates the schema when it opens a database - it would refuse to open on
 * some phones. Nothing here needs to join against that table, so there is no reason to share a file
 * with it.
 */
@Database(entities = [RecentChannelEntity::class], version = 1, exportSchema = false)
abstract class RecentsDatabase : RoomDatabase() {

    abstract fun recentChannelDao(): RecentChannelDao

    companion object {
        const val DATABASE_NAME = "romote-recents.db"
    }
}
