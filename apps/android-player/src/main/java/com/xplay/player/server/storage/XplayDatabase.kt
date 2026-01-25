package com.xplay.player.server.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MediaEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        DeviceEntity::class,
        DevicePlaylistCrossRef::class,
        TransferFileEntity::class,
        TransferLogEntity::class,
        TransferIpQuotaEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class XplayDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun deviceDao(): DeviceDao
    abstract fun transferDao(): TransferDao
}

object LocalDatabaseProvider {
    @Volatile
    private var instance: XplayDatabase? = null

    fun init(context: Context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.applicationContext,
                XplayDatabase::class.java,
                "xplay-local.db"
            ).fallbackToDestructiveMigration().build()
        }
    }

    fun get(): XplayDatabase {
        return requireNotNull(instance) { "LocalDatabaseProvider not initialized" }
    }
}
