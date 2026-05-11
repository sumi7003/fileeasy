package com.xplay.player.utils

object FileEasyUploadCore {
    const val CHUNK_SIZE_BYTES: Long = 8L * 1024 * 1024
    const val MAX_FILE_BYTES: Long = 4L * 1024 * 1024 * 1024

    data class UploadInitValidation(
        val normalizedFileName: String,
        val extension: String,
        val category: String
    )

    fun normalizeClientFileName(fileName: String): String {
        return fileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()
    }

    fun extractExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    fun getUploadCategory(extension: String): String? {
        return when (extension) {
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt" -> "document"
            "jpg", "jpeg", "png", "gif", "webp" -> "image"
            "mp4", "mov" -> "video"
            "mp3", "wav", "m4a" -> "audio"
            "zip", "apk" -> "archive"
            else -> null
        }
    }

    fun isPreviewSupported(extension: String, category: String): Boolean {
        return extension == "pdf" || category == "image" || category == "video" || category == "audio"
    }

    fun resolveMimeCategory(mimeType: String?): String? {
        val normalized = mimeType?.trim()?.lowercase().orEmpty()
        if (normalized.isBlank() || normalized == "application/octet-stream") {
            return null
        }
        return when {
            normalized.startsWith("image/") -> "image"
            normalized.startsWith("video/") -> "video"
            normalized.startsWith("audio/") -> "audio"
            normalized == "application/pdf" -> "document"
            normalized == "text/plain" -> "document"
            normalized == "application/msword" -> "document"
            normalized == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "document"
            normalized == "application/vnd.ms-excel" -> "document"
            normalized == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "document"
            normalized == "application/vnd.ms-powerpoint" -> "document"
            normalized == "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "document"
            normalized == "application/zip" -> "archive"
            normalized == "application/x-zip-compressed" -> "archive"
            normalized == "application/vnd.android.package-archive" -> "archive"
            else -> null
        }
    }

    fun validateUploadInit(fileName: String, fileSize: Long, mimeType: String?): UploadInitValidation {
        val normalizedName = normalizeClientFileName(fileName)
        if (normalizedName.isBlank()) {
            throw IllegalArgumentException("当前文件类型不在 FileEasy v1 支持范围内")
        }
        if (fileSize > MAX_FILE_BYTES) {
            throw IllegalArgumentException("单文件不能超过 4GB")
        }
        if (fileSize < 0L) {
            throw IllegalArgumentException("Invalid file size")
        }

        val extension = extractExtension(normalizedName)
        val category = getUploadCategory(extension)
            ?: throw IllegalArgumentException("当前文件类型不在 FileEasy v1 支持范围内")
        val mimeCategory = resolveMimeCategory(mimeType)
        if (mimeCategory != null && mimeCategory != category) {
            throw IllegalArgumentException("当前文件类型不在 FileEasy v1 支持范围内")
        }

        return UploadInitValidation(
            normalizedFileName = normalizedName,
            extension = extension,
            category = category
        )
    }

    fun calculateTotalChunks(fileSize: Long, chunkSize: Long = CHUNK_SIZE_BYTES): Int {
        return ((fileSize + chunkSize - 1) / chunkSize)
            .coerceAtLeast(1L)
            .toInt()
    }

    fun normalizeUploadedChunkIndexes(indexes: Collection<Int>): List<Int> {
        return indexes.distinct().sorted()
    }

    fun computeMissingChunkIndexes(totalChunks: Int, uploadedChunkIndexes: Collection<Int>): List<Int> {
        val normalizedUploaded = normalizeUploadedChunkIndexes(uploadedChunkIndexes).toSet()
        return (0 until totalChunks).filterNot { normalizedUploaded.contains(it) }
    }

    fun resolveFinalDisplayName(originalName: String, exists: (String) -> Boolean): String {
        val extension = extractExtension(originalName)
        val baseName = if (extension.isBlank()) {
            originalName
        } else {
            originalName.removeSuffix(".$extension")
        }
        var candidate = originalName
        var suffix = 1
        while (exists(candidate)) {
            candidate = if (extension.isBlank()) {
                "$baseName($suffix)"
            } else {
                "$baseName($suffix).$extension"
            }
            suffix += 1
        }
        return candidate
    }
}
