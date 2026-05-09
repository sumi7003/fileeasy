package com.xplay.player.utils

import android.content.Context
import java.io.File

object WebAdminInitializer {
    private const val PREFS_NAME = "xplay_prefs"
    private const val KEY_WEB_ADMIN_INITIALIZED = "web_admin_initialized"
    private const val ASSETS_ROOT = "web-admin"
    private const val INDEX_FILE = "index.html"

    fun isInitialized(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_WEB_ADMIN_INITIALIZED, false)) {
            return false
        }

        val targetIndex = getWebRootDir(context).resolve(INDEX_FILE)
        if (!targetIndex.exists()) {
            return false
        }

        return try {
            val assetIndexBytes = context.assets.open("$ASSETS_ROOT/$INDEX_FILE").use { it.readBytes() }
            val targetIndexBytes = targetIndex.readBytes()
            assetIndexBytes.contentEquals(targetIndexBytes)
        } catch (_: Exception) {
            false
        }
    }

    fun hasAssets(context: Context): Boolean {
        return try {
            context.assets.open("$ASSETS_ROOT/$INDEX_FILE").close()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun markInitialized(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_WEB_ADMIN_INITIALIZED, true).apply()
    }

    fun getWebRootDir(context: Context): File {
        return File(context.filesDir, ASSETS_ROOT)
    }

    fun copyAssetsToWebRoot(context: Context): Boolean {
        return try {
            val targetRoot = getWebRootDir(context)
            if (targetRoot.exists()) {
                targetRoot.deleteRecursively()
            }
            targetRoot.mkdirs()
            copyAssetsRecursively(context, ASSETS_ROOT, targetRoot)
            markInitialized(context)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun copyAssetsRecursively(context: Context, assetPath: String, targetDir: File) {
        val children = context.assets.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            val targetFile = File(targetDir, assetPath.substringAfterLast('/'))
            context.assets.open(assetPath).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return
        }
        children.forEach { child ->
            val childAssetPath = "$assetPath/$child"
            val childTargetDir = if (isAssetDirectory(context, childAssetPath)) {
                File(targetDir, child).also { it.mkdirs() }
            } else {
                targetDir
            }
            copyAssetsRecursively(context, childAssetPath, childTargetDir)
        }
    }

    private fun isAssetDirectory(context: Context, path: String): Boolean {
        val list = context.assets.list(path)
        return list != null && list.isNotEmpty()
    }
}
