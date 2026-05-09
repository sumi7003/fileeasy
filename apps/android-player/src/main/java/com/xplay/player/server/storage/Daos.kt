package com.xplay.player.server.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MediaEntity)

    @Query("SELECT * FROM media ORDER BY createdAt DESC")
    suspend fun getAll(): List<MediaEntity>

    @Query("SELECT * FROM media WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MediaEntity?

    @Query("SELECT COUNT(*) FROM media WHERE COALESCE(NULLIF(displayName, ''), originalName) = :displayName")
    suspend fun countByDisplayName(displayName: String): Int

    @Query("SELECT COUNT(*) FROM media WHERE COALESCE(NULLIF(displayName, ''), originalName) = :displayName AND id != :excludedId")
    suspend fun countByDisplayNameExcludingId(displayName: String, excludedId: String): Int

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PlaylistItemEntity>)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteItemsForPlaylist(playlistId: String)

    @Query("DELETE FROM playlist_items WHERE mediaId = :mediaId")
    suspend fun deleteItemsByMediaId(mediaId: String)

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllWithItems(): List<PlaylistWithItems>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getByIdWithItems(id: String): PlaylistWithItems?

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PlaylistEntity?

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)
}

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)

    @Query("SELECT * FROM devices ORDER BY lastHeartbeat DESC")
    suspend fun getAll(): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE serialNumber = :serial LIMIT 1")
    suspend fun findBySerial(serial: String): DeviceEntity?

    @Transaction
    @Query("SELECT * FROM devices ORDER BY lastHeartbeat DESC")
    suspend fun getAllWithPlaylists(): List<DeviceWithPlaylists>

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DeviceEntity?

    @Query("DELETE FROM device_playlist WHERE deviceId = :deviceId")
    suspend fun clearDevicePlaylists(deviceId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDevicePlaylists(refs: List<DevicePlaylistCrossRef>)

    @Query("SELECT playlistId FROM device_playlist WHERE deviceId = :deviceId")
    suspend fun getPlaylistIdsForDevice(deviceId: String): List<String>

    @Query("DELETE FROM device_playlist WHERE playlistId = :playlistId")
    suspend fun deletePlaylistRefs(playlistId: String)
}

@Dao
interface TransferDao {
    // --- Files ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: TransferFileEntity)

    @Query("SELECT * FROM transfer_files ORDER BY createdAt DESC")
    suspend fun getAllFiles(): List<TransferFileEntity>

    @Query("SELECT * FROM transfer_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: String): TransferFileEntity?

    @Query("DELETE FROM transfer_files WHERE id = :id")
    suspend fun deleteFileById(id: String)

    @Query("SELECT * FROM transfer_files WHERE expiresAt < :now")
    suspend fun getExpiredFiles(now: Long): List<TransferFileEntity>

    @Query("UPDATE transfer_files SET downloadCount = downloadCount + 1 WHERE id = :id")
    suspend fun incrementDownloadCount(id: String)

    // --- Logs ---
    @Insert
    suspend fun insertLog(log: TransferLogEntity)

    @Query("SELECT * FROM transfer_logs WHERE fileId = :fileId ORDER BY time DESC")
    suspend fun getLogsByFileId(fileId: String): List<TransferLogEntity>

    @Query("SELECT * FROM transfer_logs ORDER BY time DESC LIMIT 1000")
    suspend fun getAllLogs(): List<TransferLogEntity>

    // --- Quota ---
    @Query("SELECT * FROM transfer_ip_quota WHERE ip = :ip AND date = :date LIMIT 1")
    suspend fun getQuota(ip: String, date: String): TransferIpQuotaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateQuota(quota: TransferIpQuotaEntity)

    @Query("DELETE FROM transfer_ip_quota WHERE date < :date")
    suspend fun deleteOldQuotas(date: String)
}
