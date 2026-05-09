package com.xplay.player.server.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_sessions")
data class UploadSessionEntity(
    @PrimaryKey val uploadId: String,
    val fileName: String,
    val fileSize: Long,
    val chunkSize: Long,
    val totalChunks: Int,
    val uploadedChunks: Int,
    val status: String,
    val mimeType: String? = null,
    val expiresAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val finalFileId: String? = null,
    val finalDisplayName: String? = null
)

@Entity(tableName = "upload_session_chunks", primaryKeys = ["uploadId", "chunkIndex"])
data class UploadChunkEntity(
    val uploadId: String,
    val chunkIndex: Int,
    val size: Long,
    val createdAt: Long
)
