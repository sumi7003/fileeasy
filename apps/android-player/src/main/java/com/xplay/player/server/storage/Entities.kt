package com.xplay.player.server.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val id: String,
    val originalName: String,
    val filename: String,
    val url: String,
    val type: String,
    val size: Long,
    val createdAt: Long
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val startTime: String? = null,
    val endTime: String? = null,
    val daysOfWeek: String? = null,
    val createdAt: Long
)

@Entity(tableName = "playlist_items")
data class PlaylistItemEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val mediaId: String,
    val orderIndex: Int,
    val duration: Int
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val serialNumber: String,
    val name: String,
    val status: String,
    val lastHeartbeat: Long?,
    val ipAddress: String?,
    val version: String?,
    val createdAt: Long
)

@Entity(primaryKeys = ["deviceId", "playlistId"], tableName = "device_playlist")
data class DevicePlaylistCrossRef(
    val deviceId: String,
    val playlistId: String
)

// --- File Transfer Entities ---

@Entity(tableName = "transfer_files")
data class TransferFileEntity(
    @PrimaryKey val id: String,
    val originalName: String,
    val storedFilename: String,
    val size: Long,
    val uploaderIp: String?,
    val uploaderUserAgent: String?,
    val downloadCount: Int = 0,
    val expiresAt: Long, // Timestamp for expiration
    val createdAt: Long
)

@Entity(tableName = "transfer_logs")
data class TransferLogEntity(
    @PrimaryKey val id: String,
    val fileId: String?,
    val action: String, // UPLOAD, DOWNLOAD, DELETE, EXPIRE
    val ip: String?,
    val userAgent: String?,
    val authResult: Boolean, // true if authorized
    val clientRemark: String? = null,
    val result: String?, // "Success" or error message
    val time: Long
)

@Entity(tableName = "transfer_ip_quota", primaryKeys = ["ip", "date"])
data class TransferIpQuotaEntity(
    val ip: String,
    val date: String, // Format: yyyy-MM-dd
    val downloadCount: Int
)
