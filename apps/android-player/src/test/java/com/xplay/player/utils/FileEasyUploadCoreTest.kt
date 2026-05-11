package com.xplay.player.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileEasyUploadCoreTest {

    @Test
    fun `rejects init when file is larger than 4GB`() {
        val fileSize = FileEasyUploadCore.MAX_FILE_BYTES + 1L

        val error = runCatching {
            FileEasyUploadCore.validateUploadInit(
                fileName = "movie.mp4",
                fileSize = fileSize,
                mimeType = "video/mp4"
            )
        }.exceptionOrNull()

        assertEquals("单文件不能超过 4GB", error?.message)
    }

    @Test
    fun `computes resumable upload state from persisted chunk indexes`() {
        val uploadedChunkIndexes = listOf(3, 1, 1)

        val normalized = FileEasyUploadCore.normalizeUploadedChunkIndexes(uploadedChunkIndexes)
        val missing = FileEasyUploadCore.computeMissingChunkIndexes(
            totalChunks = 5,
            uploadedChunkIndexes = uploadedChunkIndexes
        )

        assertEquals(listOf(1, 3), normalized)
        assertEquals(listOf(0, 2, 4), missing)
    }

    @Test
    fun `rejects unsupported file types before entering upload pipeline`() {
        val error = runCatching {
            FileEasyUploadCore.validateUploadInit(
                fileName = "script.exe",
                fileSize = 1024L,
                mimeType = "application/octet-stream"
            )
        }.exceptionOrNull()

        assertEquals("当前文件类型不在 FileEasy v1 支持范围内", error?.message)
    }

    @Test
    fun `accepts apk files as archive uploads`() {
        val validation = FileEasyUploadCore.validateUploadInit(
            fileName = "release.apk",
            fileSize = 1024L,
            mimeType = "application/vnd.android.package-archive"
        )

        assertEquals("release.apk", validation.normalizedFileName)
        assertEquals("apk", validation.extension)
        assertEquals("archive", validation.category)
    }

    @Test
    fun `auto renames same-name file without overwriting existing file`() {
        val existingNames = mutableSetOf("文件.mp4", "文件(1).mp4")

        val finalName = FileEasyUploadCore.resolveFinalDisplayName("文件.mp4") { candidate ->
            existingNames.contains(candidate)
        }

        assertEquals("文件(2).mp4", finalName)
        assertTrue(existingNames.contains("文件.mp4"))
        assertTrue(existingNames.contains("文件(1).mp4"))
    }
}
