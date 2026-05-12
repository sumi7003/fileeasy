package com.xplay.player.update

import okhttp3.Request
import java.io.File

object OtaDownloader {
    fun download(
        url: String,
        targetFile: File,
        onProgress: (Int) -> Unit
    ): Boolean {
        if (targetFile.exists()) targetFile.delete()
        targetFile.parentFile?.mkdirs()

        val request = Request.Builder().url(url).get().build()
        OtaHttpClient.newDownloadClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) return false
            val body = response.body ?: return false
            val totalBytes = body.contentLength().takeIf { it > 0L } ?: -1L
            var readBytes = 0L

            body.byteStream().use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        output.write(buffer, 0, count)
                        readBytes += count

                        if (totalBytes > 0L) {
                            onProgress(((readBytes * 100L) / totalBytes).toInt().coerceIn(0, 100))
                        }
                    }
                }
            }
        }

        onProgress(100)
        return targetFile.exists() && targetFile.length() > 0L
    }
}

