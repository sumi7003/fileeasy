package com.xplay.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.xplay.player.discovery.NsdHelper
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.xplay.player.server.LocalServerService
import com.xplay.player.server.LocalStore
import com.xplay.player.utils.WebAdminInitializer
import com.xplay.player.utils.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {
    private lateinit var repository: DeviceRepository
    var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingFileChooserParams: WebChromeClient.FileChooserParams? = null

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
        
        // 1. 尽早初始化 LocalStore
        com.xplay.player.server.LocalStore.init(this)
        
        repository = DeviceRepository(this)
        
        if (repository.isHostMode.value) {
            val intent = Intent(this, LocalServerService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        repository.initialize()
        
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
                            // 使用 FileChooserParams 提供的 Intent
                            val intent = params?.createIntent()
                            if (intent != null) {
                                Log.d("MainActivity", "Launching file picker with intent: $intent")
                                filePickerLauncher.launch(intent)
                            } else {
                                // 降级方案：创建自定义 Intent
                                Log.d("MainActivity", "Creating fallback file picker intent")
                                val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                }
                                filePickerLauncher.launch(Intent.createChooser(fallbackIntent, "选择文件"))
                            }
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
    var showMonitor by remember { mutableStateOf(false) }  // ✅ 添加监控面板状态
    var forceShowSettings by remember { mutableStateOf(false) }
    
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
    
    val context = LocalContext.current
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
                showMonitor && isHostMode -> {
                    MonitorScreen(serverHost = monitorHost, onClose = { showMonitor = false })
                }
                showAdmin -> {
                    WebAdminScreen(host = host, onClose = { showAdmin = false }, onPickFiles = onPickFiles)
                }
                else -> {
                    DeviceStatusScreen(
                        repository = repository, 
                        onOpenAdmin = { showAdmin = true },
                        onOpenMonitor = { showMonitor = true },
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1677FF).copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0xFF1677FF), Color(0xFF003EB3))
                )),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "X",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "Xplay",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1677FF)
            )
            Text(
                text = "控制中心",
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
            text = "欢迎进入 Xplay 终端管理系统",
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
                containerColor = Color(0xFF1677FF)
            )
        ) {
            Text("即刻进入", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "v1.1.3-Build11",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray.copy(alpha = 0.5f)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DeviceStatusScreen(
    repository: DeviceRepository, 
    onOpenAdmin: () -> Unit,
    onOpenMonitor: () -> Unit,  // ✅ 添加监控面板回调
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
                                // 开启服务端需要验证密码
                                pendingEnabledState = true
                                passwordInput = ""
                                passwordError = null
                                showPasswordDialog = true
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
                    
                    // ✅ 监控面板按钮（主Pad专属）
                    Button(
                        onClick = onOpenMonitor,
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
                        Text(text = "管理素材与播放列表")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 角色 2: 播放器 (Player)
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
                            // 设备名称设置
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
