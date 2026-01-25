package com.xplay.player

import android.content.Context
import android.util.Log
import com.xplay.player.data.api.NetworkModule
import com.xplay.player.data.model.Device
import com.xplay.player.data.model.Playlist
import com.xplay.player.data.model.RegisterRequest
import com.xplay.player.utils.DeviceUtils
import com.xplay.player.utils.ApkInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DeviceRepository(private val context: Context) {
    companion object {
        private const val RETRY_DELAY_MS = 5000L
        private const val HEARTBEAT_INTERVAL_MS = 30000L
        private const val API_WAIT_DELAY_MS = 3000L
        private const val TAG = "DeviceRepository"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _deviceState = MutableStateFlow<Device?>(null)
    val deviceState = _deviceState.asStateFlow()

    private val _status = MutableStateFlow("初始化中...")
    val status = _status.asStateFlow()

    private val _currentPlaylist = MutableStateFlow<Playlist?>(null)
    val currentPlaylist = _currentPlaylist.asStateFlow()

    private val _isHostMode = MutableStateFlow(false)
    val isHostMode = _isHostMode.asStateFlow()

    private val _isPlayerEnabled = MutableStateFlow(false) // 默认关闭播放器，让用户手动开启
    val isPlayerEnabled = _isPlayerEnabled.asStateFlow()

    private val _serverHost = MutableStateFlow("xplay.local")
    val serverHost = _serverHost.asStateFlow()

    private val _hasConnectedBefore = MutableStateFlow(false)
    val hasConnectedBefore = _hasConnectedBefore.asStateFlow()

    private var currentPlaylistIds = listOf<String>()
    private val _discoveredIp = MutableStateFlow<String?>(null)
    val discoveredIp = _discoveredIp.asStateFlow()
    private var retryCount = 0
    private val MAX_RETRIES = 10

    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName = _deviceName.asStateFlow()

    init {
        val prefs = context.getSharedPreferences("xplay_prefs", Context.MODE_PRIVATE)
        _isHostMode.value = prefs.getBoolean("host_mode", false)
        _isPlayerEnabled.value = prefs.getBoolean("player_enabled", false)
        _serverHost.value = prefs.getString("server_host", "xplay.local") ?: "xplay.local"
        _hasConnectedBefore.value = prefs.getBoolean("has_connected", false)
        _deviceName.value = prefs.getString("device_name", null)
        
        Log.i(TAG, "=== DeviceRepository initialized ===")
        Log.d(TAG, "Host mode: ${_isHostMode.value}")
        Log.d(TAG, "Player enabled: ${_isPlayerEnabled.value}")
        Log.d(TAG, "Server host: ${_serverHost.value}")
        Log.d(TAG, "Has connected before: ${_hasConnectedBefore.value}")
        Log.d(TAG, "Device name: ${_deviceName.value ?: "(auto)"}")
    }

    fun setDeviceName(name: String) {
        _deviceName.value = name
        context.getSharedPreferences("xplay_prefs", Context.MODE_PRIVATE)
            .edit().putString("device_name", name).apply()
        // 名称改变后，如果已连接，需要重新注册更新信息
        if (_isPlayerEnabled.value) {
            resetConnection()
        }
    }

    private fun setConnected() {
        if (!_hasConnectedBefore.value) {
            _hasConnectedBefore.value = true
            context.getSharedPreferences("xplay_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("has_connected", true).apply()
        }
    }

    private var heartbeatJob: kotlinx.coroutines.Job? = null

    fun initialize() {
        if (_isPlayerEnabled.value) {
            startPlayerInternal()
        }
    }

    fun startPlayer() {
        _isPlayerEnabled.value = true
        context.getSharedPreferences("xplay_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("player_enabled", true).apply()
        startPlayerInternal()
    }

    fun stopPlayer() {
        _isPlayerEnabled.value = false
        context.getSharedPreferences("xplay_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("player_enabled", false).apply()
        heartbeatJob?.cancel()
        heartbeatJob = null
        _deviceState.value = null
        _currentPlaylist.value = null
        _status.value = "播放器已停止"
    }

    private fun startPlayerInternal() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            if (registerDevice()) {
                // 1. 注册成功后，直接从 deviceState 中获取初始播放列表，减少一次心跳请求
                _deviceState.value?.let { device ->
                    val ids = device.playlists.map { it.id }
                    if (ids.isNotEmpty() && ids != currentPlaylistIds) {
                        currentPlaylistIds = ids
                        fetchAndCombinePlaylists(ids)
                    }
                }
                
                // 2. 开始心跳循环
                startHeartbeatLoop()
            }
        }
    }

    fun setHostMode(enabled: Boolean) {
        _isHostMode.value = enabled
        val prefs = context.getSharedPreferences("xplay_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("host_mode", enabled).apply()
        
        // 如果开启主机模式，且当前播放器正在运行，重连到本地
        if (_isPlayerEnabled.value) {
            resetConnection()
        }
    }

    fun setServerHost(host: String) {
        _serverHost.value = host
        val prefs = context.getSharedPreferences("xplay_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("server_host", host).apply()
        if (!_isHostMode.value) {
            resetConnection()
        }
    }

    fun setServerHostFromDiscovery(host: String) {
        if (_isHostMode.value) return
        
        if (_discoveredIp.value != host) {
            Log.i(TAG, "✅ Discovered Xplay host via NSD: $host")
            _discoveredIp.value = host
            // 如果当前是 xplay.local 模式，发现 IP 后立即尝试重连
            // 或者如果当前未连接且播放器已启用，也尝试连接
            if (_serverHost.value == "xplay.local") {
                Log.d(TAG, "xplay.local mode, initiating connection with discovered IP")
                resetConnection()
            } else if (_deviceState.value == null && _isPlayerEnabled.value) {
                Log.d(TAG, "Not connected yet but player enabled, trying discovered IP")
                // 保存原来的配置
                val originalHost = _serverHost.value
                // 临时切换到发现的IP
                _serverHost.value = host
                resetConnection()
            }
        }
    }

    private fun resetConnection() {
        _deviceState.value = null
        _currentPlaylist.value = null
        currentPlaylistIds = emptyList()
        _status.value = "正在重新连接..."
        if (_isPlayerEnabled.value) {
            startPlayerInternal()
        }
    }

    private suspend fun registerDevice(): Boolean {
        // 使用循环而不是递归，避免栈溢出
        while (retryCount < MAX_RETRIES) {
            try {
                val baseUrl = buildBaseUrl()
                if (baseUrl == null) {
                    if (_serverHost.value == "xplay.local") {
                        _status.value = "正在寻找主机... (${retryCount + 1}/$MAX_RETRIES)\n请确保主机已启动或在设置中手动输入IP"
                        Log.w(TAG, "Waiting for NSD discovery, retry count: $retryCount. No xplay service found in network.")
                    } else {
                        _status.value = "请先配置服务器IP"
                        Log.w(TAG, "Server host is blank, retry count: $retryCount")
                    }
                    retryCount++
                    delay(RETRY_DELAY_MS)
                    continue
                }
                
                val api = NetworkModule.getApi(baseUrl)
                _status.value = "正在连接主机... (${retryCount + 1}/$MAX_RETRIES)"
                Log.d(TAG, "Attempting to register, baseUrl: $baseUrl, retry: $retryCount")
                
                val serial = DeviceUtils.getSerialNumber(context)
                
                // 默认名称逻辑：
                // 1. 如果有自定义名称 (deviceName) 则优先使用
                // 2. 否则尝试获取系统设备名称 (getSystemDeviceName)
                // 3. 实在不行，回退到 Pad-IP后缀
                val defaultName = if (!_deviceName.value.isNullOrBlank()) {
                    _deviceName.value!!
                } else {
                    val systemName = DeviceUtils.getSystemDeviceName(context)
                    // 如果系统名看起来是默认的"厂商 型号"格式，或者太长，我们可能还是想加上IP段以便区分
                    // 但如果用户在系统设置里专门改过名字（通常比较短且有意义），就直接用
                    if (systemName.length < 20 && !systemName.contains(android.os.Build.MANUFACTURER, ignoreCase = true)) {
                        systemName
                    } else {
                        // 结合一下：Pad-105 (系统名)
                        "Pad-${DeviceUtils.getIpLastSegment()} ($systemName)"
                    }
                }
                
                val appVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: Exception) {
                    "1.0"
                }
                
                val request = RegisterRequest(
                    serialNumber = serial,
                    name = defaultName,
                    version = appVersion,
                    ipAddress = null
                )
                
                val response = api.register(request)
                Log.d(TAG, "Register response: code=${response.code()}, body: ${response.body()}")
                
                if (response.isSuccessful && response.body() != null) {
                    _deviceState.value = response.body()
                    _status.value = "已连接"
                    setConnected()
                    retryCount = 0 // 成功后重置
                    Log.i(TAG, "Successfully registered device: ${response.body()?.name}")
                    return true
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "未知错误"
                    _status.value = "主机拒绝连接 (${response.code()})"
                    Log.e(TAG, "Registration rejected: code=${response.code()}, error=$errorMsg")
                    retryCount++
                    delay(RETRY_DELAY_MS * minOf(retryCount, 6))
                }
            } catch (e: java.net.UnknownHostException) {
                _status.value = "无法解析主机地址 (${_serverHost.value})"
                Log.e(TAG, "DNS resolution failed for host: ${_serverHost.value}", e)
                retryCount++
                delay(RETRY_DELAY_MS * minOf(retryCount, 6))
            } catch (e: java.net.SocketTimeoutException) {
                _status.value = "连接超时 (${retryCount + 1}/$MAX_RETRIES)"
                Log.e(TAG, "Connection timeout to: ${buildBaseUrl()}", e)
                retryCount++
                delay(RETRY_DELAY_MS * minOf(retryCount, 6))
            } catch (e: java.net.ConnectException) {
                _status.value = "无法连接到主机 (${retryCount + 1}/$MAX_RETRIES)"
                Log.e(TAG, "Connection refused: ${e.message}", e)
                retryCount++
                delay(RETRY_DELAY_MS * minOf(retryCount, 6))
            } catch (e: Exception) {
                _status.value = "网络错误: ${e.javaClass.simpleName}"
                Log.e(TAG, "Unexpected error during registration", e)
                retryCount++
                delay(RETRY_DELAY_MS * minOf(retryCount, 6))
            }
        }
        
        // 达到最大重试次数
        _status.value = "连接失败，请检查网络设置"
        Log.e(TAG, "Failed to register after $MAX_RETRIES attempts")
        retryCount = 0
        return false
    }

    private suspend fun startHeartbeatLoop() {
        while (_isPlayerEnabled.value) {
            delay(HEARTBEAT_INTERVAL_MS) // 使用常量
            if (_deviceState.value != null) {
                syncOnce()
            }
        }
    }

    private suspend fun syncOnce() {
        val device = _deviceState.value ?: return
        try {
            val baseUrl = buildBaseUrl()
            if (baseUrl == null) {
                Log.w(TAG, "Base URL is null during heartbeat, skipping")
                return
            }
            
            val api = NetworkModule.getApi(baseUrl)
            val response = api.heartbeat(device.id)
            if (response.isSuccessful) {
                setConnected()
                val body = response.body()
                Log.d(TAG, "Heartbeat successful. Playlists: ${body?.playlistIds}")
                
                val newIds = body?.playlistIds ?: emptyList()
                Log.d(TAG, "Current ids: $currentPlaylistIds, New ids: $newIds")
                if (newIds != currentPlaylistIds) {
                    currentPlaylistIds = newIds
                    if (newIds.isNotEmpty()) {
                        fetchAndCombinePlaylists(newIds)
                    } else {
                        _currentPlaylist.value = null
                    }
                }
                body?.updateInfo?.let { checkAndDownloadUpdate(it) }
            } else {
                Log.w(TAG, "Heartbeat failed with code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Heartbeat error: ${e.javaClass.simpleName} - ${e.message}", e)
        }
    }

    private suspend fun fetchAndCombinePlaylists(ids: List<String>) {
        try {
            Log.d(TAG, "Fetching details for playlists: $ids")
            _status.value = "正在获取内容清单..."
            
            val baseUrl = buildBaseUrl()
            if (baseUrl == null) {
                Log.w(TAG, "Base URL is null, cannot fetch playlists")
                _status.value = "无法连接服务器"
                return
            }
            
            val api = NetworkModule.getApi(baseUrl)
            val allItems = mutableListOf<com.xplay.player.data.model.PlaylistItem>()
            var combinedName = ""
            
            for (id in ids) {
                val response = api.getPlaylist(id)
                Log.d(TAG, "Playlist $id response: ${response.code()}")
                if (response.isSuccessful) {
                    val pl = response.body()
                    if (pl != null) {
                        Log.d(TAG, "Playlist ${pl.name} has ${pl.items.size} items")
                        allItems.addAll(pl.items)
                        combinedName += (if (combinedName.isEmpty()) "" else " + ") + pl.name
                    }
                } else {
                    Log.e(TAG, "Failed to get playlist $id: ${response.errorBody()?.string()}")
                }
            }
            
            if (allItems.isNotEmpty()) {
                Log.d(TAG, "Combining ${allItems.size} items for playback")
                _currentPlaylist.value = Playlist(
                    id = "combined",
                    name = combinedName,
                    items = allItems
                )
                _status.value = "就绪: $combinedName"
            } else {
                Log.w(TAG, "No items found in any of the assigned playlists")
                _currentPlaylist.value = null
                _status.value = "清单内无媒体内容"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch playlists: ${e.javaClass.simpleName} - ${e.message}", e)
            _status.value = "获取清单失败"
        }
    }

    private var isDownloadingUpdate = false

    private suspend fun checkAndDownloadUpdate(info: com.xplay.player.data.api.UpdateInfo) {
        if (isDownloadingUpdate) return
        
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersionCode = packageInfo.versionCode
            
            Log.d(TAG, "Checking update: current=$currentVersionCode, latest=${info.versionCode}")
            
            if (info.versionCode > currentVersionCode) {
                Log.d(TAG, "New version detected: ${info.versionName} (${info.versionCode})")
                downloadAndInstallUpdate()
            } else {
                Log.d(TAG, "No update needed: current version is up to date or newer")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check update: ${e.message}", e)
        }
    }

    private suspend fun downloadAndInstallUpdate() {
        val baseUrl = buildBaseUrl() ?: return
        val downloadUrl = baseUrl.replace("/api/v1/", "/api/v1/update/download")
        
        Log.d(TAG, "Starting download from: $downloadUrl")
        if (isDownloadingUpdate) {
            Log.d(TAG, "Download already in progress, skipping")
            return
        }
        
        isDownloadingUpdate = true
        _status.value = "正在下载更新..."
        
        try {
            Log.d(TAG, "Executing download request (async)...")
            val client = OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder().url(downloadUrl).build()
            
            // 使用 withContext 确保在 IO 线程执行阻塞操作
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            
            Log.d(TAG, "Download response code: ${response.code}")
            if (response.isSuccessful) {
                val apkFile = File(context.cacheDir, "update.apk")
                if (apkFile.exists()) {
                    val deleted = apkFile.delete()
                    Log.d(TAG, "Old update.apk deleted: $deleted")
                }
                
                Log.d(TAG, "Start writing to file: ${apkFile.absolutePath}")
                withContext(Dispatchers.IO) {
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(apkFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytes = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytes += bytesRead
                            }
                            Log.d(TAG, "Bytes copied: $totalBytes")
                        }
                    }
                }
                
                Log.d(TAG, "Update APK downloaded successfully, file size: ${apkFile.length()}")
                _status.value = "下载完成，准备安装"
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Calling ApkInstaller.install")
                    ApkInstaller.install(context, apkFile)
                }
            } else {
                Log.e(TAG, "Download failed with code ${response.code}")
                _status.value = "下载更新失败: ${response.code}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during download and install", e)
            _status.value = "下载更新出错"
        } finally {
            isDownloadingUpdate = false
        }
    }


    private fun buildBaseUrl(): String? {
        return if (_isHostMode.value) {
            "http://127.0.0.1:3000/api/v1/"
        } else {
            val host = if (_serverHost.value == "xplay.local") {
                // 优先使用 NSD 发现的 IP，但如果没有就尝试用 xplay.local
                // 某些系统可以通过其他方式解析 xplay.local（如系统 mDNS 客户端）
                val discovered = _discoveredIp.value
                if (!discovered.isNullOrBlank()) {
                    Log.d(TAG, "Using NSD discovered IP: $discovered")
                    discovered
                } else {
                    Log.d(TAG, "No NSD discovery yet, trying xplay.local directly")
                    _serverHost.value  // 使用 xplay.local，让系统尝试解析
                }
            } else {
                _serverHost.value
            }
            if (host.isBlank()) {
                Log.w(TAG, "Server host is blank")
                null
            } else {
                val url = "http://$host:3000/api/v1/"
                Log.d(TAG, "Built base URL: $url")
                url
            }
        }
    }
}
