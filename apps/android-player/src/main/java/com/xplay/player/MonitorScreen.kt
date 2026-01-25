package com.xplay.player

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import com.xplay.player.utils.QRCodeUtil
import com.xplay.player.data.api.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

/**
 * 主Pad监控面板 - 实时显示系统状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(serverHost: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var monitorData by remember { mutableStateOf<MonitorData?>(null) }
    var devices by remember { mutableStateOf<List<DeviceInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 自动刷新
    LaunchedEffect(Unit) {
        while (true) {
            try {
                monitorData = fetchMonitorData(serverHost)
                devices = fetchDevices(serverHost)
                errorMessage = null
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "获取数据失败: ${e.message}"
                isLoading = false
            }
            delay(5000) // 5秒刷新一次
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系统监控") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 刷新按钮
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                monitorData = fetchMonitorData(serverHost)
                                devices = fetchDevices(serverHost)
                                errorMessage = null
                            } catch (e: Exception) {
                                errorMessage = "刷新失败: ${e.message}"
                            }
                            isLoading = false
                        }
                    }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading && monitorData == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, "错误", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 系统概览
                item {
                    SystemOverviewCard(monitorData, devices.size)
                }

                // 主机信息与二维码
                item {
                    MasterInfoCard(serverHost)
                }
                
                // 资源使用情况
                item {
                    ResourceUsageCard(monitorData)
                }
                
                // 设备列表
                item {
                    Text(
                        text = "连接设备 (${devices.count { it.status == "online" }}/${devices.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(devices) { device ->
                    DeviceCard(device)
                }
            }
        }
    }
}

@Composable
fun MasterInfoCard(serverHost: String) {
    var qrCodeBitmap by remember(serverHost) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(serverHost) {
        // 生成二维码内容: {"type":"xplay_master","ip":"...","port":3000}
        val content = """{"type":"xplay_master","ip":"$serverHost","port":3000}"""
        qrCodeBitmap = QRCodeUtil.createQRCodeBitmap(content, 400, 400)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // QR Code
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.White, shape = MaterialTheme.shapes.small)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                qrCodeBitmap?.let { bitmap ->
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Server QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "服务端连接信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "IP地址: $serverHost",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "端口: 3000",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "使用其他Pad扫描此二维码可快速配网连接",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun SystemOverviewCard(monitorData: MonitorData?, totalDevices: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "系统概览",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // 运行时长
                monitorData?.server?.uptime?.let { uptime ->
                    Text(
                        text = formatUptime(uptime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 在线设备数
                StatItem(
                    icon = Icons.Default.Devices,
                    label = "在线设备",
                    value = "${monitorData?.devices?.online ?: 0}",
                    total = "/${totalDevices}",
                    color = MaterialTheme.colorScheme.primary
                )
                
                // 素材数量
                StatItem(
                    icon = Icons.Default.VideoLibrary,
                    label = "素材数量",
                    value = "${monitorData?.storage?.mediaCount ?: 0}",
                    total = "",
                    color = MaterialTheme.colorScheme.secondary
                )
                
                // 请求数
                StatItem(
                    icon = Icons.Default.CloudDone,
                    label = "总请求数",
                    value = "${monitorData?.server?.requestCount ?: 0}",
                    total = "",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, total: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (total.isNotEmpty()) {
                Text(
                    text = total,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ResourceUsageCard(monitorData: MonitorData?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "资源使用",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 内存使用
            monitorData?.memory?.let { memory ->
                val memoryPercent = if (memory.total > 0) {
                    (memory.used.toFloat() / memory.total * 100).toInt()
                } else 0
                
                ResourceProgressItem(
                    label = "Java堆内存 (Max: ${memory.max}MB)",
                    value = "${memory.used}MB / ${memory.total}MB",
                    progress = if (memory.max > 0) memory.used.toFloat() / memory.max else 0f,
                    color = when {
                        (if (memory.max > 0) memory.used.toFloat() / memory.max else 0f) > 0.8f -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 存储空间
            monitorData?.storage?.let { storage ->
                val totalUsedMB = storage.uploadsMB + storage.transferMB
                val freeMB = storage.freeMB
                val totalMB = totalUsedMB + freeMB
                val storagePercent = if (totalMB > 0) {
                    (totalUsedMB.toFloat() / totalMB * 100).toInt()
                } else 0
                
                ResourceProgressItem(
                    label = "存储",
                    value = "${totalUsedMB}MB / ${totalMB}MB (素材${storage.fileCount}个 + 中转${storage.transferFileCount}个)",
                    progress = storagePercent / 100f,
                    color = when {
                        storagePercent > 80 -> MaterialTheme.colorScheme.error
                        storagePercent > 60 -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.secondary
                    }
                )

            if (storage.transferMB >= 10_240 || storage.freeMB <= 5_120) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "告警：文件中转占用过高或磁盘剩余不足，建议及时清理",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            }
        }
    }
}

@Composable
fun ResourceProgressItem(label: String, value: String, progress: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun DeviceCard(device: DeviceInfo) {
    val isOnline = device.status == "online"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态指示灯
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336))
            ) {
                // 在线时的呼吸动画
                if (isOnline) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = alpha))
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "序列号: ${device.serialNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 播放列表信息
                if (device.playlists.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = device.playlists.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // 最后心跳时间
                device.lastHeartbeat?.let { heartbeat ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "最后心跳: ${formatDateTime(heartbeat)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 状态标签
            Surface(
                shape = CircleShape,
                color = if (isOnline) 
                    Color(0xFF4CAF50).copy(alpha = 0.2f) 
                else 
                    Color(0xFFF44336).copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (isOnline) "在线" else "离线",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }
    }
}

// 数据模型
@Serializable
data class MonitorData(
    val timestamp: Long,
    val memory: MemoryInfo,
    val devices: DevicesInfo,
    val storage: StorageInfo,
    val server: ServerInfo
)

@Serializable
data class MemoryInfo(
    val used: Long,
    val free: Long,
    val total: Long,
    val max: Long
)

@Serializable
data class DevicesInfo(
    val total: Int,
    val online: Int,
    val offline: Int
)

@Serializable
data class StorageInfo(
    val mediaCount: Int,
    val uploadsMB: Long,
    val freeMB: Long,
    val fileCount: Int,
    val transferMB: Long = 0,
    val transferFileCount: Int = 0
)

@Serializable
data class ServerInfo(
    val uptime: Long,
    val requestCount: Long
)

@Serializable
data class DeviceInfo(
    val id: String,
    val name: String,
    val serialNumber: String,
    val status: String,
    val lastHeartbeat: String?,
    val playlists: List<PlaylistInfo> = emptyList()
)

@Serializable
data class PlaylistInfo(
    val id: String,
    val name: String
)

// 工具函数
private suspend fun fetchMonitorData(serverHost: String): MonitorData = kotlinx.coroutines.withContext(Dispatchers.IO) {
    val response = okhttp3.OkHttpClient().newCall(
        okhttp3.Request.Builder()
            .url("http://$serverHost:3000/api/v1/system/monitor")
            .header("Cookie", "xplay_auth=admin-token") // 增加鉴权信息
            .build()
    ).execute()
    
    if (!response.isSuccessful) {
        throw Exception("HTTP ${response.code}")
    }
    
    val json = response.body?.string() ?: throw Exception("Empty response")
    Json { ignoreUnknownKeys = true }.decodeFromString<MonitorData>(json)
}

private suspend fun fetchDevices(serverHost: String): List<DeviceInfo> = kotlinx.coroutines.withContext(Dispatchers.IO) {
    val response = okhttp3.OkHttpClient().newCall(
        okhttp3.Request.Builder()
            .url("http://$serverHost:3000/api/v1/devices")
            .header("Cookie", "xplay_auth=admin-token") // 增加鉴权信息
            .build()
    ).execute()
    
    if (!response.isSuccessful) {
        throw Exception("HTTP ${response.code}")
    }
    
    val json = response.body?.string() ?: throw Exception("Empty response")
    Json { ignoreUnknownKeys = true }.decodeFromString<List<DeviceInfo>>(json)
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    
    return when {
        days > 0 -> "${days}天${hours}小时"
        hours > 0 -> "${hours}小时${minutes}分钟"
        else -> "${minutes}分钟"
    }
}

private fun formatDateTime(dateStr: String?): String {
    if (dateStr == null) return "未知"
    
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateStr)
        date?.let { outputFormat.format(it) } ?: dateStr
    } catch (e: Exception) {
        dateStr
    }
}
