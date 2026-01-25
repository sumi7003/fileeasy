package com.xplay.player.utils

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@OptIn(UnstableApi::class)
object VideoCache {
    private var simpleCache: SimpleCache? = null
    private const val MAX_CACHE_SIZE = 6L * 1024 * 1024 * 1024 // 6GB 缓存上限

    fun getInstance(context: Context): SimpleCache {
        if (simpleCache == null) {
            Log.d("VideoCache", "Initializing SimpleCache...")
            val cacheDir = File(context.cacheDir, "media_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
            val databaseProvider = StandaloneDatabaseProvider(context)
            
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
            Log.d("VideoCache", "SimpleCache initialized.")
        }
        return simpleCache!!
    }

    fun getDataSourceFactory(context: Context): CacheDataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        
        return CacheDataSource.Factory()
            .setCache(getInstance(context))
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR) // 如果缓存读取出错，忽略缓存直接联网
    }

    // 新增：生成缓存文件路径的方法
    fun getCacheFile(context: Context, url: String): File {
        val cache = getInstance(context)
        // 使用 Cache 的 Key 生成逻辑（通常是 URL）
        val key = url 
        // 注意：SimpleCache 的文件结构比较复杂，通常是分片的。
        // 为了实现“下载完成后本地播放”，我们最好使用独立的下载目录，而不是复用 ExoPlayer 的分片缓存目录。
        // 所以这里我们返回一个位于 filesDir/downloads 下的文件路径。
        val fileName = url.hashCode().toString() + ".mp4" // 简单哈希命名
        val downloadDir = File(context.filesDir, "downloads")
        if (!downloadDir.exists()) downloadDir.mkdirs()
        return File(downloadDir, fileName)
    }
}
