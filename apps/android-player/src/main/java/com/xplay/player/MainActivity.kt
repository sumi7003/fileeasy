package com.xplay.player

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.ContentScale
import com.xplay.player.discovery.NsdHelper
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.xplay.player.server.LocalServerService
import com.xplay.player.server.LocalStore
import com.xplay.player.server.HomeRecentFileResponse
import com.xplay.player.server.HomeUploadTaskResponse
import com.xplay.player.server.ServiceRuntimeState
import com.xplay.player.update.AppHttpUpdateController
import com.xplay.player.utils.LanAccessInfo
import com.xplay.player.utils.LanAddressSource
import com.xplay.player.utils.LanAddressResolver
import com.xplay.player.utils.WebAdminInitializer
import com.xplay.player.utils.DeviceUtils
import com.xplay.player.utils.QRCodeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.os.StatFs
import java.net.NetworkInterface
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val XplayBrandColor = Color(0xFF1677FF)
private val XplayBrandColorDark = Color(0xFF003EB3)
private val FileEasyBrandColor = Color(0xFF1F8A57)
private val FileEasyBrandColorDark = Color(0xFF0F5C38)
private const val FILE_EASY_ROOT_FOLDER = "Download/易传输"

private fun formatLanSegment(host: String?): String? {
    if (host.isNullOrBlank()) return null
    val octets = host.split(".")
    if (octets.size != 4 || octets.any { it.toIntOrNull() == null }) return null
    return "${octets[0]}.${octets[1]}.${octets[2]}.x"
}

class MainActivity : ComponentActivity() {
    private lateinit var repository: DeviceRepository
    var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingFileChooserParams: WebChromeClient.FileChooserParams? = null

    private fun normalizeMimeTypes(rawAcceptTypes: Array<String>?): Array<String> {
        if (rawAcceptTypes.isNullOrEmpty()) return emptyArray()

        val extensionMimeMap = mapOf(
            ".pdf" to "application/pdf",
            ".doc" to "application/msword",
            ".docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ".xls" to "application/vnd.ms-excel",
            ".xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ".ppt" to "application/vnd.ms-powerpoint",
            ".pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            ".txt" to "text/plain",
            ".zip" to "application/zip",
            ".apk" to "application/vnd.android.package-archive",
            ".jpg" to "image/*",
            ".jpeg" to "image/*",
            ".png" to "image/*",
            ".gif" to "image/*",
            ".webp" to "image/*",
            ".mp4" to "video/*",
            ".mov" to "video/*",
            ".mp3" to "audio/*",
            ".wav" to "audio/*",
            ".m4a" to "audio/*",
        )

        return rawAcceptTypes
            .flatMap { value -> value.split(",") }
            .map { value -> value.trim().lowercase(Locale.getDefault()) }
            .mapNotNull { value ->
                when {
                    value.isBlank() -> null
                    value.contains("/") -> value
                    value.startsWith(".") -> extensionMimeMap[value]
                    else -> null
                }
            }
            .distinct()
            .toTypedArray()
    }

    private fun buildFileChooserIntent(params: WebChromeClient.FileChooserParams?): Intent {
        val normalizedMimeTypes = normalizeMimeTypes(params?.acceptTypes)
        val baseIntent = try {
            params?.createIntent()
        } catch (e: Exception) {
            Log.w("MainActivity", "Failed to create chooser intent from params", e)
            null
        }

        return (baseIntent ?: Intent(Intent.ACTION_OPEN_DOCUMENT)).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

            if (normalizedMimeTypes.isNotEmpty()) {
                type = if (normalizedMimeTypes.size == 1) normalizedMimeTypes.first() else "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, normalizedMimeTypes)
            } else if (type.isNullOrBlank()) {
                type = "*/*"
            }
        }
    }

    // 使用 StartActivityForResult 来正确处理 WebView 的文件选择
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("MainActivity", "File picker result: ${result.resultCode}, data: ${result.data}")
        val callback = filePathCallback
        filePathCallback = null
        pendingFileChooserParams = null
        
        if (callback == null) {
            Log.w("MainActivity", "filePathCallback is null, ignoring result")
            return@registerForActivityResult
        }
        
        if (result.resultCode == RESULT_OK && result.data != null) {
            val uris = mutableListOf<Uri>()
            // 处理多选情况
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i)?.uri?.let { uris.add(it) }
                }
            }
            // 处理单选情况
            if (uris.isEmpty()) {
                result.data?.data?.let { uris.add(it) }
            }
            Log.d("MainActivity", "Selected ${uris.size} files")
            callback.onReceiveValue(uris.toTypedArray())
        } else {
            Log.d("MainActivity", "File picker cancelled or failed")
            callback.onReceiveValue(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 保持屏幕常亮
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 开启全屏沉浸模式
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        AppHttpUpdateController.checkVersion2(this) {
            launchMainContent()
        }
    }

    private fun launchMainContent() {
        // 1. 尽早初始化 LocalStore
        com.xplay.player.server.LocalStore.init(this)
        
        repository = DeviceRepository(this)
        
        if (ProductFlavorConfig.isFileEasy) {
            repository.setHostMode(true)
            startLocalServer(this)
        } else if (repository.isHostMode.value) {
            startLocalServer(this)
        }

        repository.initialize()

        if (ProductFlavorConfig.isFileEasy &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1002)
        }
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(repository, onPickFiles = { callback, params ->
                        // 如果有未完成的回调，先取消它
                        filePathCallback?.onReceiveValue(null)
                        
                        filePathCallback = callback
                        pendingFileChooserParams = params
                        
                        try {
                            val intent = buildFileChooserIntent(params)
                            Log.d("MainActivity", "Launching file picker with intent: $intent")
                            filePickerLauncher.launch(Intent.createChooser(intent, "选择文件"))
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Failed to launch file picker", e)
                            callback.onReceiveValue(null)
                            filePathCallback = null
                            pendingFileChooserParams = null
                        }
                    })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (ProductFlavorConfig.isFileEasy) {
            startLocalServer(this)
        }
    }

    override fun onStop() {
        if (ProductFlavorConfig.isFileEasy) {
            LocalServerService.notifyAppBackground(this)
        }
        super.onStop()
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 标记有待处理的文件选择请求
        outState.putBoolean("hasPendingFileCallback", filePathCallback != null)
    }
    
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // 如果 Activity 被重建且有待处理的请求，回调会丢失
        // 这种情况下 WebView 会收到 null 结果，用户需要重新选择
        if (savedInstanceState.getBoolean("hasPendingFileCallback", false)) {
            Log.w("MainActivity", "Activity restored with pending file callback - callback lost")
        }
    }

}

private fun startLocalServer(context: Context) {
    val intent = Intent(context, LocalServerService::class.java)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun openFileEasyFolder(context: Context, relativeDirectory: String? = null) {
    val basePath = FILE_EASY_ROOT_FOLDER
    val normalizedDirectory = relativeDirectory
        ?.trim()
        ?.removePrefix("/")
        ?.removeSuffix("/")
        ?.takeIf { it.isNotBlank() }
    val relativePath = if (normalizedDirectory != null) "$basePath/$normalizedDirectory" else basePath
    val folderUri = DocumentsContract.buildDocumentUri(
        "com.android.externalstorage.documents",
        "primary:$relativePath"
    )

    val directOpenIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val treeIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val fallbackIntent = Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching { context.startActivity(directOpenIntent) }
        .recoverCatching { context.startActivity(treeIntent) }
        .recoverCatching { context.startActivity(fallbackIntent) }
}

@Composable
fun MainContent(
    repository: DeviceRepository, 
    onPickFiles: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams?) -> Unit
) {
    val playlist by repository.currentPlaylist.collectAsState()
    val isHostMode by repository.isHostMode.collectAsState()
    val serverHost by repository.serverHost.collectAsState()
    val discoveredIp by repository.discoveredIp.collectAsState()
    val hasConnectedBefore by repository.hasConnectedBefore.collectAsState()
    val isPlayerEnabled by repository.isPlayerEnabled.collectAsState()
    
    var showAdmin by remember { mutableStateOf(false) }
    var forceShowSettings by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    if (ProductFlavorConfig.isFileEasy) {
        LaunchedEffect(isHostMode) {
            if (!isHostMode) {
                repository.setHostMode(true)
            }
            startLocalServer(context)
        }

        FileEasyShellScreen()
        return
    }

    var showHostDashboard by remember { mutableStateOf(false) }

    // ✅ 启动逻辑：如果是主机模式且曾经配置成功过，默认进入管理页面
    LaunchedEffect(Unit) {
        if (isHostMode) {
            forceShowSettings = true
            if (hasConnectedBefore) {
                showAdmin = true
            }
        }
    }
    
    // ✅ App 端登录状态（本会话有效）
    // 如果之前成功连接过，则默认视为已登录系统
    var isAppLoggedIn by remember { mutableStateOf(hasConnectedBefore) }
    
    val nsdHelper = remember { NsdHelper(context) }

    // 调试日志
    LaunchedEffect(playlist, isPlayerEnabled, forceShowSettings) {
        Log.d("MainActivity", "State check: playlist=${playlist != null}, isPlayerEnabled=$isPlayerEnabled, forceShowSettings=$forceShowSettings")
    }

    DisposableEffect(isHostMode) {
        if (isHostMode) {
            nsdHelper.stopDiscovery()
            nsdHelper.registerHost()
        } else {
            nsdHelper.unregisterHost()
            nsdHelper.startDiscovery { host ->
                repository.setServerHostFromDiscovery(host)
            }
        }
        onDispose {
            nsdHelper.stopDiscovery()
            nsdHelper.unregisterHost()
        }
    }
    
    val host = if (isHostMode) {
        "127.0.0.1"
    } else {
        if (serverHost == "xplay.local") discoveredIp ?: serverHost else serverHost
    }

    // ✅ 监控面板需要的真实 IP（用于生成正确的二维码）
    val monitorHost = if (isHostMode) {
        DeviceUtils.getLocalIpAddress() ?: "127.0.0.1"
    } else {
        host
    }

    // ✅ 拦截逻辑优化：
    // 只要有内容 (playlist != null) 且 播放器已开启 (isPlayerEnabled) 且 未强制显示设置 (!forceShowSettings)
    // 则显示播放全屏。
    // 注：如果是主机模式，LaunchedEffect(Unit) 会在启动时将 forceShowSettings 设为 true，从而防止自动全屏。
    val shouldShowPlayer = playlist != null && isPlayerEnabled && !forceShowSettings

    if (shouldShowPlayer) {
        PlayerScreen(playlist!!, host)
        
        // 响应返回键回到设置
        BackHandler {
            forceShowSettings = true
        }
    } else {
        // 只有在不显示播放器时，才走登录和控制中心流程
        if (!isAppLoggedIn) {
            AppLoginScreen(onLoginSuccess = { isAppLoggedIn = true })
        } else {
            // ... (显示监控、后台或控制中心)
            when {
                showHostDashboard && isHostMode -> {
                    MonitorScreen(serverHost = monitorHost, onClose = { showHostDashboard = false })
                }
                showAdmin -> {
                    WebAdminScreen(host = host, onClose = { showAdmin = false }, onPickFiles = onPickFiles)
                }
                else -> {
                    DeviceStatusScreen(
                        repository = repository, 
                        onOpenAdmin = { showAdmin = true },
                        onOpenHostDashboard = { showHostDashboard = true },
                        hasPlaylist = playlist != null,
                        hasConnectedBefore = hasConnectedBefore, // ✅ 传入此状态
                        onReturnToPlayer = { forceShowSettings = false }
                    )
                }
            }
        }
    }
}

@Composable
fun XplayLogo(modifier: Modifier = Modifier) {
    val brandColor = if (ProductFlavorConfig.isFileEasy) FileEasyBrandColor else XplayBrandColor
    val brandColorDark = if (ProductFlavorConfig.isFileEasy) FileEasyBrandColorDark else XplayBrandColorDark
    val brandInitial = if (ProductFlavorConfig.isFileEasy) "易" else "X"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(brandColor.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(brandColor, brandColorDark)
                )),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = brandInitial,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = ProductFlavorConfig.productName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = brandColor
            )
            Text(
                text = ProductFlavorConfig.controlCenterName,
                fontSize = 12.sp,
                color = Color.Gray,
                letterSpacing = 2.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLoginScreen(onLoginSuccess: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        XplayLogo()
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "系统登录",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = ProductFlavorConfig.loginSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = {
                // 不需要验证密码，直接登录
                onLoginSuccess()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = XplayBrandColor
            )
        ) {
            Text("即刻进入", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = BuildConfig.VERSION_NAME,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileEasyShellScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val serviceState by LocalServerService.runtimeState.collectAsState()
    var lanAccessInfo by remember { mutableStateOf(LanAddressResolver.resolve(context)) }
    var qrCodeBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var uploadQueue by remember { mutableStateOf<List<HomeUploadTaskResponse>>(emptyList()) }
    var recentFiles by remember { mutableStateOf<List<HomeRecentFileResponse>>(emptyList()) }
    var totalStorageBytes by remember { mutableStateOf(0L) }
    var freeStorageBytes by remember { mutableStateOf(0L) }
    var preferWaitingAfterCompletion by remember { mutableStateOf(true) }
    var scanSessionStartedAt by remember { mutableStateOf(System.currentTimeMillis()) }

    val uploadUrl = if (serviceState == ServiceRuntimeState.RUNNING) {
        lanAccessInfo.uploadUrl
    } else {
        null
    }
    val latestReceiveDirectory = recentFiles.firstOrNull()?.relativeDirectory
    val networkNameLabel = lanAccessInfo.networkName ?: "等待网络"
    val networkSegmentLabel = formatLanSegment(lanAccessInfo.host)
    val preciseWifiName = lanAccessInfo.networkName?.takeUnless { it == "Wi-Fi" || it.isBlank() }
    val pendingUploads = uploadQueue.filter { it.status != "completed" }
    val currentUploadQueue = uploadQueue.filter { task ->
        task.status != "completed" || task.updatedAt >= scanSessionStartedAt
    }
    val completedUploads = currentUploadQueue.filter { it.status == "completed" }
    val receivingUploads = currentUploadQueue.filter { it.status == "receiving" }
    val queuedUploads = currentUploadQueue.filter { it.status == "queued" }
    val latestRecentFile = recentFiles.firstOrNull()
    val hasCurrentSessionCompletion = latestRecentFile?.createdAt?.let { it >= scanSessionStartedAt } == true ||
        completedUploads.any { it.updatedAt >= scanSessionStartedAt }
    val homeState = when {
        pendingUploads.isNotEmpty() -> "receiving"
        hasCurrentSessionCompletion || (recentFiles.isNotEmpty() && !preferWaitingAfterCompletion) -> "completed"
        else -> "waiting"
    }
    val homeStateTitle = when (homeState) {
        "receiving" -> "正在接收文件"
        "completed" -> "传输完成"
        else -> "等待扫码"
    }
    val homeStateDescription = when (homeState) {
        "receiving" -> "文件传输进行中，App 退到后台后会在最后一个任务完成后自动结束服务。"
        "completed" -> "本轮传输已经结束，最新接收的文件会保留在下方列表里。"
        else -> "手机或电脑必须连接到同一网络或当前热点后，才能扫码或打开网址发送文件。"
    }
    val waitingNetworkInline = when (lanAccessInfo.source) {
        LanAddressSource.WIFI -> preciseWifiName?.let {
            "当前网络：$it，手机/电脑需接入同一网络后再扫码或打开网址"
        } ?: "手机/电脑需接入同一网络后再扫码或打开网址"
        LanAddressSource.HOTSPOT -> "当前热点：$networkNameLabel，手机/电脑需接入当前热点后再扫码或打开网址"
        LanAddressSource.OTHER -> "当前网络：$networkNameLabel，手机/电脑需接入同一局域网后再扫码或打开网址"
        LanAddressSource.UNAVAILABLE -> "请先让设备连接 Wi‑Fi 或热点，再让手机/电脑接入同网后扫码"
    }
    val refreshLanAccessInfo = remember(context) {
        {
            lanAccessInfo = LanAddressResolver.resolve(context)
        }
    }

    val refreshHomePanel = remember(context, scope) {
        {
            scope.launch(Dispatchers.IO) {
                val uploads = runCatching { LocalStore.listUploadQueueSessions(limit = 24) }
                    .getOrElse { emptyList() }
                val files = runCatching { LocalStore.listRecentManagedFiles(limit = 4) }
                    .getOrElse { emptyList() }
                val stat = runCatching { StatFs(context.filesDir.path) }.getOrNull()
                val total = stat?.let { it.blockSizeLong * it.blockCountLong } ?: 0L
                val free = stat?.let { it.blockSizeLong * it.availableBlocksLong } ?: 0L

                launch(Dispatchers.Main) {
                    uploadQueue = uploads
                    recentFiles = files
                    totalStorageBytes = total
                    freeStorageBytes = free
                }
            }
        }
    }

    LaunchedEffect(serviceState) {
        refreshLanAccessInfo()
        refreshHomePanel()
    }

    LaunchedEffect(pendingUploads) {
        if (pendingUploads.isNotEmpty()) {
            preferWaitingAfterCompletion = false
        }
    }

    LaunchedEffect(refreshHomePanel) {
        while (true) {
            refreshHomePanel()
            delay(1500)
        }
    }

    DisposableEffect(context, lifecycleOwner) {
        refreshLanAccessInfo()

        val connectivityManager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCallback = if (connectivityManager != null) {
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    refreshLanAccessInfo()
                    refreshHomePanel()
                }

                override fun onLost(network: Network) {
                    refreshLanAccessInfo()
                    refreshHomePanel()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    refreshLanAccessInfo()
                    refreshHomePanel()
                }

                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    refreshLanAccessInfo()
                    refreshHomePanel()
                }
            }
        } else {
            null
        }

        networkCallback?.let { callback ->
            runCatching {
                connectivityManager!!.registerNetworkCallback(
                    NetworkRequest.Builder().build(),
                    callback
                )
            }
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshLanAccessInfo()
                refreshHomePanel()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            networkCallback?.let { callback ->
                runCatching {
                    connectivityManager?.unregisterNetworkCallback(callback)
                }
            }
        }
    }

    LaunchedEffect(uploadUrl) {
        qrCodeBitmap = uploadUrl?.let { QRCodeUtil.createQRCodeBitmap(it, 400, 400) }
    }
    val usedStorageBytes = (totalStorageBytes - freeStorageBytes).coerceAtLeast(0L)
    val storageProgress = if (totalStorageBytes > 0L) {
        usedStorageBytes.toFloat() / totalStorageBytes.toFloat()
    } else {
        0f
    }
    val totalUploadBytes = currentUploadQueue.sumOf { it.fileSize }
    val uploadedBytes = currentUploadQueue.sumOf { it.uploadedBytes }.coerceAtMost(totalUploadBytes)
    val totalUploadProgress = if (totalUploadBytes > 0L) {
        (uploadedBytes.toFloat() / totalUploadBytes.toFloat()).coerceIn(0f, 0.99f)
    } else {
        0f
    }
    val remainingUploadBytes = (totalUploadBytes - uploadedBytes).coerceAtLeast(0L)
    val earliestUploadCreatedAt = currentUploadQueue.minOfOrNull { it.createdAt } ?: System.currentTimeMillis()
    val uploadElapsedMs = (System.currentTimeMillis() - earliestUploadCreatedAt).coerceAtLeast(1L)
    val uploadBytesPerSecond = if (uploadedBytes > 0L) {
        (uploadedBytes.toDouble() * 1000.0) / uploadElapsedMs.toDouble()
    } else {
        0.0
    }
    val estimatedRemainingMs = if (uploadBytesPerSecond >= 32 * 1024) {
        ((remainingUploadBytes.toDouble() / uploadBytesPerSecond) * 1000.0).toLong().coerceAtLeast(1000L)
    } else {
        null
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF4F2EE), Color(0xFFF8F5F0), Color(0xFFF2F5F8))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        val contentMaxWidth = if (maxWidth >= 920.dp) 860.dp else maxWidth
        val useWideTopSection = contentMaxWidth >= 700.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.widthIn(max = contentMaxWidth).fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFFF7F3EE),
                border = BorderStroke(1.dp, Color(0xFFD9D0C7))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B1B1B))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        YiTransferLogoLockup(
                            subtitle = "局域网扫码传文件",
                            onDark = true
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(serviceState.toDisplayColor())
                            )
                            Text(
                                text = serviceState.toDisplayText(),
                                color = serviceState.toDisplayColor(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (uploadUrl != null) {
                            when (homeState) {
                                "waiting" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "扫码即可发送文件",
                                            style = if (useWideTopSection) {
                                                MaterialTheme.typography.headlineLarge
                                            } else {
                                                MaterialTheme.typography.headlineMedium
                                            },
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF222222),
                                            textAlign = TextAlign.Center
                                        )
                                        FileEasyNetworkGuide(text = waitingNetworkInline)
                                        qrCodeBitmap?.let { bitmap ->
                                            Surface(
                                                shape = RoundedCornerShape(30.dp),
                                                color = Color.White,
                                                border = BorderStroke(1.dp, Color(0xFFDDD6CD)),
                                                shadowElevation = 6.dp
                                            ) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "易传输上传二维码",
                                                    modifier = Modifier
                                                        .size(if (useWideTopSection) 280.dp else 224.dp)
                                                        .padding(if (useWideTopSection) 18.dp else 14.dp)
                                                )
                                            }
                                        }
                                        FileEasyStorageOverview(
                                            storageProgress = storageProgress,
                                            usedStorageBytes = usedStorageBytes,
                                            totalStorageBytes = totalStorageBytes
                                        )
                                        FileEasyRecordShortcut(
                                            recentFileCount = recentFiles.size,
                                            onOpenFolder = { openFileEasyFolder(context) }
                                        )
                                    }
                                }

                                "receiving" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(18.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = homeStateTitle,
                                                style = if (useWideTopSection) {
                                                    MaterialTheme.typography.headlineLarge
                                                } else {
                                                    MaterialTheme.typography.headlineMedium
                                                },
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF222222),
                                                textAlign = TextAlign.Center
                                            )
                                            FileEasyStateBadge(homeState = homeState, label = homeStateTitle)
                                        }
                                        FileEasyReceivingSummaryCard(
                                            progress = totalUploadProgress,
                                            progressLabel = "${(totalUploadProgress * 100).toInt().coerceIn(0, 100)}%",
                                            remainingLabel = estimatedRemainingMs?.let(::formatRemainingDuration) ?: "计算中",
                                            speedLabel = formatTransferRate(uploadBytesPerSecond),
                                            fileCountLabel = "${currentUploadQueue.size} 个任务",
                                            uploadedLabel = formatStorageValue(uploadedBytes),
                                            totalLabel = formatStorageValue(totalUploadBytes),
                                            totalCount = currentUploadQueue.size,
                                            completedCount = completedUploads.size,
                                            receivingCount = receivingUploads.size,
                                            queuedCount = queuedUploads.size
                                        )
                                        if (receivingUploads.isNotEmpty()) {
                                            FileEasyPanelSection(
                                                title = "正在接收",
                                                countLabel = "${receivingUploads.size} 个任务"
                                            ) {
                                                receivingUploads.forEach { task ->
                                                    FileEasyUploadOverviewCard(task = task)
                                                }
                                            }
                                        }
                                        if (queuedUploads.isNotEmpty()) {
                                            FileEasyPanelSection(
                                                title = "排队中",
                                                countLabel = "${queuedUploads.size} 个任务"
                                            ) {
                                                queuedUploads.forEach { task ->
                                                    FileEasyUploadOverviewCard(task = task)
                                                }
                                            }
                                        }
                                        if (completedUploads.isNotEmpty()) {
                                            FileEasyPanelSection(
                                                title = "已完成",
                                                countLabel = "${completedUploads.size} 个任务"
                                            ) {
                                                completedUploads.forEach { task ->
                                                    FileEasyUploadOverviewCard(task = task)
                                                }
                                            }
                                        }
                                        if (currentUploadQueue.isEmpty()) {
                                            FileEasyPanelSection(
                                                title = "接收列表"
                                            ) {
                                                FileEasyEmptyPanelHint("当前还没有接收任务，文件开始传输后会显示在这里。")
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(18.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Text(
                                                text = homeStateTitle,
                                                style = if (useWideTopSection) {
                                                    MaterialTheme.typography.headlineLarge
                                                } else {
                                                    MaterialTheme.typography.headlineMedium
                                                },
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF222222),
                                                textAlign = TextAlign.Center
                                            )
                                            FileEasyStateBadge(homeState = homeState, label = homeStateTitle)
                                            Text(
                                                text = homeStateDescription,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color(0xFF8C867F),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Button(
                                                onClick = { openFileEasyFolder(context, latestReceiveDirectory) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(58.dp),
                                                shape = RoundedCornerShape(18.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFEAF7F1),
                                                    contentColor = Color(0xFF1D6D46)
                                                )
                                            ) {
                                                Text(
                                                    text = "打开目录",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    scanSessionStartedAt = System.currentTimeMillis()
                                                    preferWaitingAfterCompletion = true
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(58.dp),
                                                shape = RoundedCornerShape(18.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = FileEasyBrandColor,
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Text(
                                                    text = "继续扫码传文件",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                        FileEasyPanelSection(
                                            title = "今日接收",
                                            countLabel = if (recentFiles.isNotEmpty()) "${recentFiles.size} 个文件" else null,
                                            helperText = "文件已按类型保存，可在系统文件中继续查看。"
                                        ) {
                                            recentFiles.forEach { file ->
                                                FileEasyRecentFileRow(file = file)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFFFFFBF6),
                                border = BorderStroke(1.dp, Color(0xFFE4D7C7))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = when (serviceState) {
                                            ServiceRuntimeState.RUNNING -> "未检测到可用局域网地址"
                                            ServiceRuntimeState.STARTING -> "正在准备上传入口"
                                            ServiceRuntimeState.ERROR -> "服务启动失败"
                                            ServiceRuntimeState.STOPPED -> "服务未运行"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2B2A28)
                                    )
                                    Text(
                                        text = "请确认设备已连接 Wi‑Fi、手机热点或已开启热点，地址恢复后会自动显示在这里。",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF8A837B)
                                    )
                                }
                            }
                        }
                    }

                    if (homeState == "waiting") {
                        YiTransferScanIllustrationCard(
                            useWideLayout = useWideTopSection,
                            accessUrl = uploadUrl ?: lanAccessInfo.uploadUrl.orEmpty(),
                            networkSegment = networkSegmentLabel,
                            modifier = Modifier.offset(y = (-8).dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
private fun YiTransferLogoLockup(
    subtitle: String,
    onDark: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryTextColor = if (onDark) Color.White else Color(0xFF16201C)
    val secondaryTextColor = if (onDark) Color.White.copy(alpha = 0.78f) else Color(0xFF7A756D)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        YiTransferLogoMark()
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "易传输",
                color = primaryTextColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = secondaryTextColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun YiTransferLogoMark(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF17A36A), Color(0xFF12C3AB))
                )
            )
    ) {
        fun cornerModifier(alignment: Alignment, width: Dp, height: Dp): Modifier {
            val base = Modifier
                .size(width = width, height = height)
                .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.96f)), RoundedCornerShape(5.dp))
            return when (alignment) {
                Alignment.TopStart -> base.align(alignment).padding(start = 7.dp, top = 7.dp)
                Alignment.TopEnd -> base.align(alignment).padding(end = 7.dp, top = 7.dp)
                Alignment.BottomStart -> base.align(alignment).padding(start = 7.dp, bottom = 7.dp)
                else -> base.align(alignment).padding(end = 7.dp, bottom = 7.dp)
            }
        }

        Box(
            modifier = cornerModifier(Alignment.TopStart, 11.dp, 11.dp)
                .background(Color.Transparent)
        )
        Box(
            modifier = cornerModifier(Alignment.TopEnd, 11.dp, 11.dp)
                .background(Color.Transparent)
        )
        Box(
            modifier = cornerModifier(Alignment.BottomStart, 11.dp, 11.dp)
                .background(Color.Transparent)
        )
        Box(
            modifier = cornerModifier(Alignment.BottomEnd, 11.dp, 11.dp)
                .background(Color.Transparent)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .width(4.dp)
                .height(15.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.96f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 22.dp)
                .size(12.dp)
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(10.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.96f), RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(4.dp)
                    .height(10.dp)
                    .background(Color.White.copy(alpha = 0.96f), RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun YiTransferScanIllustrationCard(
    useWideLayout: Boolean,
    accessUrl: String,
    networkSegment: String?,
    modifier: Modifier = Modifier
) {
    val maxImageWidth = if (useWideLayout) 250.dp else 188.dp

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFAFDFC),
        border = BorderStroke(1.dp, Color(0xFFDCEAE5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "电脑访问网页：$accessUrl",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF5E6F67),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = networkSegment?.let { "电脑需连接同一 Wi-Fi/热点，当前网段：$it" }
                    ?: "电脑需连接同一 Wi-Fi/热点后访问",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7D8C85),
                textAlign = TextAlign.Center
            )
            Image(
                painter = painterResource(id = R.drawable.yichuanshu_home_transfer),
                contentDescription = "扫码选择文件上传示意图",
                modifier = Modifier
                    .fillMaxWidth(fraction = if (useWideLayout) 0.52f else 0.72f)
                    .widthIn(max = maxImageWidth)
                    .aspectRatio(1448f / 1086f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun FileEasyNetworkGuide(text: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFEAF7F1),
        border = BorderStroke(1.dp, Color(0xFFCFE8DC))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF315D48),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun FileEasyRecordShortcut(
    recentFileCount: Int,
    onOpenFolder: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, Color(0xFFE0E9E4))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "接收记录",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF25342D),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (recentFileCount > 0) "最近 $recentFileCount 个文件" else "暂无接收文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7D8A82)
                )
            }
            TextButton(onClick = onOpenFolder) {
                Text(
                    text = "打开目录",
                    fontWeight = FontWeight.SemiBold,
                    color = FileEasyBrandColor
                )
            }
        }
    }
}

@Composable
private fun FileEasyStateBadge(homeState: String, label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = when (homeState) {
            "receiving" -> Color(0xFFF9EDC7)
            "completed" -> Color(0xFFDDF5E8)
            else -> Color(0xFFECE7DF)
        }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            color = when (homeState) {
                "receiving" -> Color(0xFF8A5B00)
                "completed" -> Color(0xFF1F8A57)
                else -> Color(0xFF7A736B)
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FileEasyStorageOverview(
    storageProgress: Float,
    usedStorageBytes: Long,
    totalStorageBytes: Long
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "存储空间",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFC0B7AD)
            )
            Text(
                text = "${formatStorageValue(usedStorageBytes)} / ${formatStorageValue(totalStorageBytes)}",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF9A938B),
                fontWeight = FontWeight.Medium
            )
        }
        LinearProgressIndicator(
            progress = storageProgress.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = Color(0xFF4C4A47),
            trackColor = Color(0xFFE7E1DA)
        )
    }
}

@Composable
private fun FileEasyPanelSection(
    title: String,
    countLabel: String? = null,
    helperText: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6D6761)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (countLabel != null) {
                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = countLabel,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                action?.invoke()
            }
        }
        if (helperText != null) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8C867F)
            )
        }
        content()
    }
}

@Composable
private fun FileEasyUploadOverviewCard(task: HomeUploadTaskResponse) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDED7CF))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = task.fileName.toExtensionBadgeColor(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(width = 48.dp, height = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = task.fileName.toExtensionBadgeText(),
                            color = task.fileName.toExtensionTextColor(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF262626),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatStorageValue(task.uploadedBytes)} / ${formatStorageValue(task.fileSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0AAA3)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.status.toUploadStatusLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = task.status.toUploadStatusColor()
                    )
                    Text(
                        text = "${task.progress}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF77716A)
                    )
                }
            }
            LinearProgressIndicator(
                progress = (task.progress / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = Color(0xFF252525),
                trackColor = Color(0xFFE4DFD9)
            )
            Text(
                text = "${task.uploadedChunks}/${task.totalChunks} 分片  ·  ${formatRecentTime(task.updatedAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0AAA3)
            )
        }
    }
}

@Composable
private fun FileEasyReceivingSummaryCard(
    progress: Float,
    progressLabel: String,
    remainingLabel: String,
    speedLabel: String,
    fileCountLabel: String,
    uploadedLabel: String,
    totalLabel: String,
    totalCount: Int,
    completedCount: Int,
    receivingCount: Int,
    queuedCount: Int
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1B1B1B),
        border = BorderStroke(1.dp, Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress,
                        modifier = Modifier.size(78.dp),
                        strokeWidth = 7.dp,
                        color = Color.White
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = progressLabel,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "总进度",
                            color = Color.White.copy(alpha = 0.62f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "预计剩余 $remainingLabel",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$speedLabel · $fileCountLabel",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$uploadedLabel / $totalLabel",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FileEasyQueueMetricChip(
                    label = "总任务",
                    value = totalCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                FileEasyQueueMetricChip(
                    label = "已完成",
                    value = completedCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                FileEasyQueueMetricChip(
                    label = "接收中",
                    value = receivingCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                FileEasyQueueMetricChip(
                    label = "排队中",
                    value = queuedCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FileEasyQueueMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.62f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FileEasyRecentFileRow(file: HomeRecentFileResponse) {
    val isNew = System.currentTimeMillis() - file.createdAt < 12 * 60 * 60 * 1000L
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = file.fileName.toExtensionBadgeColor(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.size(width = 44.dp, height = 44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = file.fileName.toExtensionBadgeText(),
                    color = file.fileName.toExtensionTextColor(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = file.fileName,
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF262626),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatStorageValue(file.size)}  ·  ${formatRecentTime(file.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0AAA3)
            )
        }
        Text(
            text = if (isNew) "新" else "已存",
            color = if (isNew) Color(0xFF1F8A57) else Color(0xFFA8A3BD),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FileEasyEmptyPanelHint(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFFCF8),
        border = BorderStroke(1.dp, Color(0xFFE5DDD4))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFAEA69D)
            )
        }
    }
}

private fun formatStorageValue(bytes: Long): String {
    if (bytes <= 0L) return "0 GB"
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    val formatter = DecimalFormat("0.#")
    return if (gb >= 1.0) {
        "${formatter.format(gb)} GB"
    } else {
        "${formatter.format(mb)} MB"
    }
}

private fun formatTransferRate(bytesPerSecond: Double): String {
    if (bytesPerSecond <= 0.0) return "等待中"
    val mbPerSecond = bytesPerSecond / (1024.0 * 1024.0)
    val kbPerSecond = bytesPerSecond / 1024.0
    val formatter = DecimalFormat("0.#")
    return if (mbPerSecond >= 1.0) {
        "${formatter.format(mbPerSecond)} MB/s"
    } else {
        "${formatter.format(kbPerSecond)} KB/s"
    }
}

private fun formatRemainingDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(1L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes >= 60L) {
        val hours = minutes / 60L
        val remainMinutes = minutes % 60L
        if (remainMinutes == 0L) "${hours}小时" else "${hours}小时${remainMinutes}分"
    } else if (minutes > 0L) {
        "约 ${minutes}分${seconds}秒"
    } else {
        "约 ${seconds}秒"
    }
}

private fun formatRecentTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val delta = now - timestamp
    return when {
        delta < 60 * 60 * 1000L -> "刚刚"
        delta < 24 * 60 * 60 * 1000L -> "今天 ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
        delta < 48 * 60 * 60 * 1000L -> "昨天 ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun String.toUploadStatusLabel(): String {
    return when (lowercase(Locale.getDefault())) {
        "receiving", "ready", "uploading" -> "接收中"
        "completed" -> "已完成"
        "queued", "initialized" -> "排队中"
        else -> "处理中"
    }
}

private fun String.toUploadStatusColor(): Color {
    return when (lowercase(Locale.getDefault())) {
        "receiving", "ready", "uploading" -> Color(0xFF1F8A57)
        "completed" -> Color(0xFF2D63D8)
        "queued", "initialized" -> Color(0xFFC58A00)
        else -> Color(0xFF7B746D)
    }
}

private fun String.toExtensionBadgeText(): String {
    val extension = substringAfterLast('.', "").ifBlank { take(3) }
    return extension.uppercase(Locale.getDefault()).take(3)
}

private fun String.toExtensionBadgeColor(): Color {
    return when (substringAfterLast('.', "").lowercase(Locale.getDefault())) {
        "jpg", "jpeg", "png", "gif", "webp" -> Color(0xFFD8F5E4)
        "mp4", "mov" -> Color(0xFFD9E7FF)
        "doc", "docx" -> Color(0xFFE6DEFF)
        "xls", "xlsx" -> Color(0xFFDDF6D8)
        "zip", "rar", "7z" -> Color(0xFFFFF0BC)
        "pdf" -> Color(0xFFFFDFDF)
        else -> Color(0xFFEDE8E2)
    }
}

private fun String.toExtensionTextColor(): Color {
    return when (substringAfterLast('.', "").lowercase(Locale.getDefault())) {
        "jpg", "jpeg", "png", "gif", "webp" -> Color(0xFF1F8A57)
        "mp4", "mov" -> Color(0xFF2D63D8)
        "doc", "docx" -> Color(0xFF6952E6)
        "xls", "xlsx" -> Color(0xFF239A46)
        "zip", "rar", "7z" -> Color(0xFFC58A00)
        "pdf" -> Color(0xFFE03B2F)
        else -> Color(0xFF7B746D)
    }
}

@Composable
private fun FileEasyStatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FileEasyMetricCard(
    label: String,
    value: String,
    helper: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, Color(0xFFDDE7E2))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6A7A70)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF173B28)
            )
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF74847A)
            )
        }
    }
}

@Composable
private fun FileEasyRuntimeStep(
    title: String,
    detail: String,
    active: Boolean,
    done: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                done -> Color(0xFFF2FBF5)
                active -> Color(0xFFF4F8FB)
                else -> Color(0xFFFBFCFD)
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                done -> FileEasyBrandColor.copy(alpha = 0.18f)
                active -> Color(0xFFD7E2EA)
                else -> Color(0xFFE7EDF1)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = when {
                    done -> FileEasyBrandColor.copy(alpha = 0.14f)
                    active -> Color(0xFFEAF1F7)
                    else -> Color(0xFFF1F4F6)
                },
                shape = RoundedCornerShape(999.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FileEasyBrandColor,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (active) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF44657A),
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF86959D),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7C72)
                )
            }
        }
    }
}

@Composable
private fun ServiceRuntimeState.toDisplayColor(): Color {
    return when (this) {
        ServiceRuntimeState.RUNNING -> FileEasyBrandColor
        ServiceRuntimeState.STARTING -> MaterialTheme.colorScheme.primary
        ServiceRuntimeState.ERROR -> MaterialTheme.colorScheme.error
        ServiceRuntimeState.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun ServiceRuntimeState.toChipContainerColor(): Color {
    return when (this) {
        ServiceRuntimeState.RUNNING -> FileEasyBrandColor.copy(alpha = 0.12f)
        ServiceRuntimeState.STARTING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ServiceRuntimeState.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        ServiceRuntimeState.STOPPED -> MaterialTheme.colorScheme.surfaceVariant
    }
}

private fun ServiceRuntimeState.toDisplayText(): String {
    return when (this) {
        ServiceRuntimeState.STARTING -> "启动中"
        ServiceRuntimeState.RUNNING -> "运行中"
        ServiceRuntimeState.ERROR -> "启动失败"
        ServiceRuntimeState.STOPPED -> "未运行"
    }
}

private fun ServiceRuntimeState.toFileEasySummary(hasUploadEntry: Boolean): String {
    return when (this) {
        ServiceRuntimeState.STARTING -> "本地服务正在启动中"
        ServiceRuntimeState.RUNNING -> if (hasUploadEntry) {
            "局域网上传入口已准备就绪"
        } else {
            "服务已运行，等待解析可访问地址"
        }
        ServiceRuntimeState.ERROR -> "服务启动失败，请检查设备状态"
        ServiceRuntimeState.STOPPED -> "服务当前未运行"
    }
}

private fun LanAddressSource.toDisplayLabel(): String {
    return when (this) {
        LanAddressSource.WIFI -> "Wi-Fi"
        LanAddressSource.HOTSPOT -> "热点共享"
        LanAddressSource.OTHER -> "局域网接口"
        LanAddressSource.UNAVAILABLE -> "未检测到"
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DeviceStatusScreen(
    repository: DeviceRepository, 
    onOpenAdmin: () -> Unit,
    onOpenHostDashboard: () -> Unit,
    hasPlaylist: Boolean,
    hasConnectedBefore: Boolean, // ✅ 新增参数
    onReturnToPlayer: () -> Unit
) {
    val status by repository.status.collectAsState()
    val device by repository.deviceState.collectAsState()
    val isHostMode by repository.isHostMode.collectAsState()
    val serverHost by repository.serverHost.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var showInitDialog by remember { mutableStateOf(false) }
    var isInitializing by remember { mutableStateOf(false) }
    var initError by remember { mutableStateOf<String?>(null) }

    // ✅ 新增：服务端角色切换时的密码验证
    var showPasswordDialog by remember { mutableStateOf(false) }
    var pendingEnabledState by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // 网络状态信息
    var ipAddress by remember { mutableStateOf("获取中...") }
    var wifiSsid by remember { mutableStateOf("未连接") }

    LaunchedEffect(isHostMode) {
        if (isHostMode) {
            while (true) {
                // 获取 IP 地址
                val ip = try {
                    NetworkInterface.getNetworkInterfaces().asSequence()
                        .flatMap { it.inetAddresses.asSequence() }
                        .filter { !it.isLoopbackAddress && it.hostAddress?.contains(".") == true }
                        .map { it.hostAddress ?: "" }
                        .firstOrNull() ?: "127.0.0.1"
                } catch (e: Exception) {
                    "127.0.0.1"
                }
                ipAddress = ip

                // 获取 WiFi 名称
                wifiSsid = try {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val info = wifiManager.connectionInfo
                    val ssid = info.ssid.removeSurrounding("\"")
                    if (ssid == "<unknown ssid>" || ssid == "0x") "移动网络或隐藏WiFi" else ssid
                } catch (e: SecurityException) {
                    // 某些机型/系统在未授予定位等权限时会直接抛异常，避免启动崩溃
                    "无权限读取WiFi名称"
                } catch (e: Exception) {
                    "未连接"
                }
                
                delay(5000) // 每 5 秒刷新一次
            }
        }
    }

    DisposableEffect(isHostMode) {
        if (isHostMode) {
            val needsInit = !WebAdminInitializer.isInitialized(context)
            val hasAssets = WebAdminInitializer.hasAssets(context)
            showInitDialog = needsInit && hasAssets
        } else {
            showInitDialog = false
            isInitializing = false
            initError = null
        }
        onDispose { }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        XplayLogo(modifier = Modifier.padding(bottom = 16.dp))
        
        // 角色 1: 服务端 (Host Mode)
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isHostMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "服务端角色", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = when {
                                isHostMode -> "正在作为主机运行"
                                hasConnectedBefore -> "点击验证密码以管理服务器"
                                else -> "未开启主机模式"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = isHostMode,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (!LocalStore.isPasswordConfigured()) {
                                    repository.setHostMode(true)
                                    val intent = Intent(context, LocalServerService::class.java)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                } else {
                                    // 开启服务端需要验证密码
                                    pendingEnabledState = true
                                    passwordInput = ""
                                    passwordError = null
                                    showPasswordDialog = true
                                }
                            } else {
                                // 关闭不需要验证
                                repository.setHostMode(false)
                                context.stopService(Intent(context, LocalServerService::class.java))
                            }
                        }
                    )
                }
                
                if (showPasswordDialog) {
                    AlertDialog(
                        onDismissRequest = { showPasswordDialog = false },
                        title = { Text("安全验证") },
                        text = {
                            Column {
                                Text("开启服务端角色需要管理员密码")
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("密码") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    isError = passwordError != null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (passwordError != null) {
                                    Text(
                                        text = passwordError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (LocalStore.verifyPassword("admin", passwordInput)) {
                                    showPasswordDialog = false
                                    
                                    if (hasConnectedBefore && !isHostMode) {
                                        // ✅ 关键逻辑：如果曾经作为客户端连接成功过，且当前不是主机模式
                                        // 则不再开启本地服务端，而是直接打开当前连接到的主机的管理页面
                                        onOpenAdmin()
                                    } else {
                                        // ✅ 否则（从未成功过，或已经是主机），正常开启/保持主机模式
                                        repository.setHostMode(true)
                                        val intent = Intent(context, LocalServerService::class.java)
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            context.startForegroundService(intent)
                                        } else {
                                            context.startService(intent)
                                        }
                                        
                                        // 如果是老主机，直接进管理页；如果是新初始化，留在控制中心
                                        if (hasConnectedBefore) {
                                            onOpenAdmin()
                                        }
                                    }
                                } else {
                                    passwordError = "密码不正确"
                                }
                            }) {
                                Text("确认")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPasswordDialog = false }) {
                                Text("取消")
                            }
                        }
                    )
                }
                
                if (isHostMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 网络信息面板
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "当前WiFi: $wifiSsid", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "局域网访问地址 (供其他手机上传):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val url = "http://$ipAddress:3000/"
                            Text(
                                text = url,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(url))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "复制地址", modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!ProductFlavorConfig.isFileEasy) {
                        Button(
                            onClick = onOpenHostDashboard,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Dashboard, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "查看系统监控")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!ProductFlavorConfig.isFileEasy) {
                        // 管理后台按钮
                        Button(
                            onClick = {
                                if (!WebAdminInitializer.isInitialized(context) && WebAdminInitializer.hasAssets(context)) {
                                    scope.launch(Dispatchers.IO) {
                                        WebAdminInitializer.copyAssetsToWebRoot(context)
                                        launch(Dispatchers.Main) { onOpenAdmin() }
                                    }
                                } else {
                                    onOpenAdmin()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = ProductFlavorConfig.adminEntryLabel)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (ProductFlavorConfig.playerFeatureEnabled) {
            val isPlayerEnabled by repository.isPlayerEnabled.collectAsState()
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPlayerEnabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "播放器角色", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = if (isPlayerEnabled) "状态: $status" else "播放器已停用",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = isPlayerEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) repository.startPlayer() else repository.stopPlayer()
                            }
                        )
                    }

                    if (isPlayerEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        if (!isHostMode) {
                            val customName by repository.deviceName.collectAsState()
                            var tempName by remember(customName) { mutableStateOf(customName ?: "") }

                            OutlinedTextField(
                                value = tempName,
                                onValueChange = {
                                    tempName = it
                                    repository.setDeviceName(it)
                                },
                                singleLine = true,
                                label = { Text("设备自定义名称 (可选)") },
                                placeholder = { Text("默认为 Pad-IP后缀") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = serverHost,
                                onValueChange = { repository.setServerHost(it) },
                                singleLine = true,
                                label = { Text(text = if (serverHost == "xplay.local") "自动发现模式" else "手动指定服务器IP") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "已连接到本地服务端 (127.0.0.1)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        device?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "设备名称: ${it.name}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "设备ID: ${it.id.take(8)}...", style = MaterialTheme.typography.bodySmall)
                        }

                        if (hasPlaylist) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onReturnToPlayer,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "进入播放全屏")
                            }
                        } else if (status == "已连接") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "💡 请在管理后台为此设备分配清单",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "纯服务端模式", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "当前安装包已关闭播放器入口，后续功能将围绕局域网文件上传、管理和分享来建设。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (isHostMode && !WebAdminInitializer.hasAssets(context)) {
            Text(
                text = "⚠️ 未检测到 web-admin 资源，请确认 assets 目录配置。",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

    if (showInitDialog) {
        AlertDialog(
            onDismissRequest = { if (!isInitializing) showInitDialog = false },
            title = { Text(text = "首次安装：初始化管理后台") },
            text = {
                Text(
                    text = initError
                        ?: "是否将内置 web-admin 资源复制到本机，以便浏览器访问管理后台？"
                )
            },
            confirmButton = {
                Button(
                    enabled = !isInitializing,
                    onClick = {
                        isInitializing = true
                        initError = null
                        scope.launch(Dispatchers.IO) {
                            val ok = WebAdminInitializer.copyAssetsToWebRoot(context)
                            launch(Dispatchers.Main) {
                                isInitializing = false
                                if (ok) {
                                    showInitDialog = false
                                } else {
                                    initError = "初始化失败，请重试。"
                                }
                            }
                        }
                    }
                ) {
                    Text(text = if (isInitializing) "处理中..." else "确认")
                }
            },
            dismissButton = {
                Button(
                    enabled = !isInitializing,
                    onClick = { showInitDialog = false }
                ) {
                    Text(text = "取消")
                }
            }
        )
    }
}

@Composable
fun WebAdminScreen(
    host: String, 
    onClose: () -> Unit, 
    onPickFiles: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "管理后台", fontSize = 18.sp)
            Button(onClick = onClose) { Text(text = "返回") }
        }

        val targetUrl = "http://$host:3000/"
        
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.WHITE)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    @Suppress("DEPRECATION")
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    webViewClient = WebViewClient()
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            onPickFiles(filePathCallback, fileChooserParams)
                            return true
                        }
                    }
                    loadUrl(targetUrl)
                }
            },
            update = { webView ->
                if (webView.url != targetUrl) {
                    webView.loadUrl(targetUrl)
                }
            }
        )
    }
}
