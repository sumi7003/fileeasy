package com.xplay.player.server.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UploadSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UploadSessionEntity)

    @Query("SELECT * FROM upload_sessions WHERE uploadId = :uploadId LIMIT 1")
    suspend fun getSessionById(uploadId: String): UploadSessionEntity?

    @Query("DELETE FROM upload_sessions WHERE uploadId = :uploadId")
    suspend fun deleteSessionById(uploadId: String)

    @Query("SELECT * FROM upload_sessions WHERE expiresAt < :now")
    suspend fun getExpiredSessions(now: Long): List<UploadSessionEntity>

    @Query("SELECT * FROM upload_sessions WHERE expiresAt >= :now AND status != 'completed' ORDER BY updatedAt DESC LIMIT :maxRows")
    suspend fun getActiveSessions(now: Long, maxRows: Int): List<UploadSessionEntity>

    @Query("SELECT * FROM upload_sessions WHERE expiresAt >= :now ORDER BY createdAt ASC, updatedAt ASC LIMIT :maxRows")
    suspend fun getRecentQueueSessions(now: Long, maxRows: Int): List<UploadSessionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChunk(chunk: UploadChunkEntity): Long

    @Query("SELECT chunkIndex FROM upload_session_chunks WHERE uploadId = :uploadId ORDER BY chunkIndex ASC")
    suspend fun getChunkIndexes(uploadId: String): List<Int>

    @Query("SELECT COUNT(*) FROM upload_session_chunks WHERE uploadId = :uploadId")
    suspend fun getChunkCount(uploadId: String): Int

    @Query("DELETE FROM upload_session_chunks WHERE uploadId = :uploadId")
    suspend fun deleteChunksByUploadId(uploadId: String)
}
