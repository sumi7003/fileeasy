package com.xplay.player

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.compose.material3.CircularProgressIndicator
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.xplay.player.data.model.Playlist
import com.xplay.player.utils.VideoCache
import kotlinx.coroutines.delay

import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(playlist: Playlist, serverHost: String) {
    Log.d("PlayerScreen", "Rendering PlayerScreen with ${playlist.items.size} items")
    var currentIndex by remember { mutableStateOf(0) }
    var playbackKey by remember { mutableStateOf(0) }
    val currentItem = playlist.items.getOrNull(currentIndex)

    fun nextItem() {
        Log.d("PlayerScreen", "nextItem() called, current index: $currentIndex")
        if (playlist.items.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % playlist.items.size
            playbackKey++
        }
    }

    LaunchedEffect(currentIndex, playbackKey, playlist) {
        Log.d("PlayerScreen", "LaunchedEffect triggered: index=$currentIndex, type=${currentItem?.media?.type}")
        if (currentItem != null) {
            if (currentItem.media.type == "image") {
                Log.d("PlayerScreen", "Waiting ${currentItem.duration}s for image")
                delay(currentItem.duration * 1000L)
                nextItem()
            }
        }
    }

    if (currentItem == null) {
        Log.w("PlayerScreen", "currentItem is null, playlist items: ${playlist.items.size}")
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Empty Playlist (Items: ${playlist.items.size})", color = Color.White)
        }
        return
    }

    val mediaUrl = currentItem.media.url ?: ""
    // 如果传入的 serverHost 已经是一个有效的 IP（由 MainActivity 处理过），直接使用它
    val host = if (serverHost.isBlank()) "127.0.0.1" else serverHost.trim()
    
    val url = if (mediaUrl.contains("localhost") || mediaUrl.contains("10.0.2.2")) {
        mediaUrl.replace("localhost", host).replace("10.0.2.2", host)
    } else {
        mediaUrl.replace("xplay.local", host)
    }
    
    val fullUrl = if (url.startsWith("http")) url else "http://$host:3000$url"
    
    // ✅ 优化：统一使用 ExoPlayer 内部缓存逻辑，删除手动下载代码
    val playUrl = fullUrl

    Log.d("PlayerScreen", "Displaying media: type=${currentItem.media.type}, url=$playUrl")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (currentItem.media.type == "video") {
            VideoPlayer(
                url = playUrl, 
                playbackKey = playbackKey, 
                onFinished = {
                    nextItem()
                }
            )
        } else {
            // 图片离线缓存策略
            val context = LocalContext.current
            val imageRequest = remember(fullUrl) {
                ImageRequest.Builder(context)
                    .data(fullUrl)
                    .diskCachePolicy(CachePolicy.DISABLED) // 禁用磁盘缓存以避免潜在的 I/O 阻塞或损坏
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .listener(
                        onError = { _, result -> 
                            Log.e("PlayerScreen", "Image load failed: $fullUrl", result.throwable)
                        },
                        onSuccess = { _, _ -> 
                            Log.d("PlayerScreen", "Image loaded successfully: $fullUrl")
                        }
                    )
                    .build()
            }
            
            Image(
                painter = rememberAsyncImagePainter(imageRequest),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(url: String, playbackKey: Int, onFinished: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 增加状态追踪
    var isLoading by remember { mutableStateOf(true) }
    var errorCount by remember { mutableStateOf(0) }
    
    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)

        val cacheDataSourceFactory = VideoCache.getDataSourceFactory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // 生命周期管理 (保持不变)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                Lifecycle.Event.ON_DESTROY -> exoPlayer.release()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(url, playbackKey, errorCount) {
        Log.d("VideoPlayer", "Playing URL: $url (Attempt: ${errorCount + 1})")
        
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
                if (state == Player.STATE_ENDED) {
                    onFinished()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("VideoPlayer", "ExoPlayer Error: ${error.message}", error)
                if (errorCount < 2) { // 失败重试 2 次
                    errorCount++
                } else {
                    onFinished() // 彻底失败再跳过
                }
            }
        }
        
        exoPlayer.addListener(listener)
        
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop() 
        }
    }
    
    // 确保彻底释放
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                LayoutInflater.from(ctx).inflate(R.layout.player_view_container, null).apply {
                    val playerView = findViewById<PlayerView>(R.id.player_view)
                    playerView.player = exoPlayer
                    playerView.useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (isLoading) {
            CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f))
        }
    }
}
