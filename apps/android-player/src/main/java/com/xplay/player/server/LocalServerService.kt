package com.xplay.player.server

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.util.Log
import com.xplay.player.ProductFlavorConfig
import com.xplay.player.server.storage.DeviceEntity
import com.xplay.player.server.storage.DevicePlaylistCrossRef
import com.xplay.player.server.storage.LocalDatabaseProvider
import com.xplay.player.server.storage.MediaEntity
import com.xplay.player.server.storage.PlaylistEntity
import com.xplay.player.server.storage.PlaylistItemEntity
import com.xplay.player.server.storage.PlaylistItemWithMedia
import com.xplay.player.server.storage.PlaylistWithItems
import com.xplay.player.server.storage.TransferFileEntity
import com.xplay.player.server.storage.TransferIpQuotaEntity
import com.xplay.player.server.storage.TransferLogEntity
import com.xplay.player.server.storage.UploadChunkEntity
import com.xplay.player.server.storage.UploadSessionEntity
import com.xplay.player.utils.FileEasyUploadCore
import com.xplay.player.utils.WebAdminInitializer
import com.xplay.player.utils.QRCodeUtil
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentDisposition
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.http.content.LocalFileContent
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private fun isFileEasyRemoteManagementPath(path: String): Boolean {
    return path == "/api/v1/files" ||
        path.startsWith("/api/v1/files/") ||
        path == "/uploads" ||
        path.startsWith("/uploads/") ||
        path == "/admin" ||
        path.startsWith("/admin/")
}

class LocalServerService : Service() {

    private var server: io.ktor.server.engine.ApplicationEngine? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_APP_BACKGROUND -> {
                Log.d(TAG, "App moved to background")
                isAppInForeground = false
                LocalStore.init(this)
                requestStopIfIdle()
                return START_NOT_STICKY
            }

            ACTION_RESTART_SERVER -> {
                Log.d(TAG, "Restarting LocalServerService by user request")
                isAppInForeground = true
                ensureServerStarted(forceRestart = true)
                return START_NOT_STICKY
            }

            else -> {
                Log.d(TAG, "Starting LocalServerService...")
                isAppInForeground = true
                ensureServerStarted()
                return START_NOT_STICKY
            }
        }
    }

    private fun ensureServerStarted(forceRestart: Boolean = false) {
        LocalStore.init(this)
        if (LocalStore.isTransferEnabled()) {
            LocalStore.startTransferMaintenance()
        }

        if (forceRestart) {
            stopServer()
        } else if (server != null) {
            updateRuntimeState(ServiceRuntimeState.RUNNING)
            return
        }

        startForegroundNotification(ServiceRuntimeState.STARTING)
        updateRuntimeState(ServiceRuntimeState.STARTING)
        startServer()
    }
    
    private fun startForegroundNotification(state: ServiceRuntimeState) {
        val channelId = "xplay_server_channel"
        val channelName = ProductFlavorConfig.serverNotificationName
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                channelName,
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ProductFlavorConfig.serverRunningDescription
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        startForeground(NOTIFICATION_ID, buildNotification(channelId, state))
        Log.d(TAG, "Foreground service started")
    }

    override fun onDestroy() {
        updateRuntimeState(ServiceRuntimeState.STOPPED)
        super.onDestroy()
        stopServer()
    }

    private fun startServer() {
        if (server != null) {
            updateRuntimeState(ServiceRuntimeState.RUNNING)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Initializing Netty server on port 3000...")
                server = embeddedServer(Netty, host = "0.0.0.0", port = 3000) {
                    install(ContentNegotiation) {
                        json(Json {
                            ignoreUnknownKeys = true
                        })
                    }
                    install(CORS) {
                        anyHost()
                    }
                    install(io.ktor.server.plugins.partialcontent.PartialContent) {
                        maxRangeCount = 10
                    }
                    install(io.ktor.server.plugins.autohead.AutoHeadResponse)
                    install(StatusPages) {
                        // ... 之前的代码
                    }
                    
                    intercept(io.ktor.server.application.ApplicationCallPipeline.Plugins) {
                        val path = call.request.path()
                        if (ProductFlavorConfig.isFileEasy && isFileEasyRemoteManagementPath(path)) {
                            call.respond(
                                HttpStatusCode.Gone,
                                mapOf("message" to "FileEasy v1 does not provide remote admin or file management")
                            )
                            finish()
                            return@intercept
                        }
                        if (path.startsWith("/api/v1/") && 
                            path != "/api/v1/auth/login" && 
                            path != "/api/v1/auth/logout" && 
                            path != "/api/v1/home/summary" &&
                            path != "/api/v1/ping" && 
                            path != "/api/v1/system/monitor" && // 允许访问监控数据
                            !path.startsWith("/api/v1/devices") && // 允许终端注册心跳
                            !path.startsWith("/api/v1/update") && // 允许检查和下载更新
                            !path.startsWith("/api/v1/playlists") // 允许设备获取播放列表
                        ) {
                            if (!LocalStore.isPasswordConfigured()) {
                                return@intercept
                            }
                            val authCookie = call.request.cookies["xplay_auth"]
                            if (authCookie != "admin-token") {
                                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "未授权访问"))
                                finish()
                            }
                        }
                    }

                    routing {
                        get("/") {
                            val (bytes, contentType) = LocalStore.readWebAsset("index.html", fallbackToIndex = true)
                            call.respondBytes(bytes, contentType = contentType)
                        }
                        get("/api/v1/ping") {
                            call.respondText("pong")
                        }
                        get("/api/v1/home/summary") {
                            call.respond(LocalStore.getHomeSummary { LocalStore.buildUploadPageUrl(call) })
                        }
                        post("/api/v1/auth/login") {
                            val request = call.receive<LoginRequest>()
                            if (LocalStore.verifyPassword(request.username, request.password)) {
                                call.respond(mapOf("status" to "ok", "token" to "admin-token"))
                            } else {
                                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "用户名或密码错误"))
                            }
                        }
                        post("/api/v1/auth/logout") {
                            call.response.headers.append(
                                HttpHeaders.SetCookie,
                                "xplay_auth=; Path=/; Max-Age=0; SameSite=Lax"
                            )
                            call.respond(mapOf("status" to "ok"))
                        }
                        post("/api/v1/auth/password") {
                            val request = call.receive<UpdatePasswordRequest>()
                            if (LocalStore.verifyPassword("admin", request.oldPassword)) {
                                LocalStore.updatePassword(request.newPassword)
                                call.respond(mapOf("status" to "ok"))
                            } else {
                                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "旧密码错误"))
                            }
                        }
                        get("/uploads/{path...}") {
                            if (ProductFlavorConfig.isFileEasy) {
                                call.respond(
                                    HttpStatusCode.Gone,
                                    mapOf("message" to "FileEasy v1 does not provide remote file downloads")
                                )
                                return@get
                            }
                            val relativePath = call.parameters.getAll("path")?.joinToString("/")?.trim('/')
                            if (relativePath.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing filename"))
                                return@get
                            }
                            val file = LocalStore.getUploadFile(relativePath)
                            if (!file.exists()) {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to "File not found"))
                                return@get
                            }
                            // 根据文件扩展名设置 Content-Type
                            val contentType = when (file.extension.lowercase()) {
                                "mp4" -> io.ktor.http.ContentType.Video.MP4
                                "webm" -> io.ktor.http.ContentType.Video.Any
                                "avi" -> io.ktor.http.ContentType.Video.Any
                                "mov" -> io.ktor.http.ContentType.Video.QuickTime
                                "png" -> io.ktor.http.ContentType.Image.PNG
                                "jpg", "jpeg" -> io.ktor.http.ContentType.Image.JPEG
                                "gif" -> io.ktor.http.ContentType.Image.GIF
                                "webp" -> io.ktor.http.ContentType("image", "webp")
                                else -> io.ktor.http.ContentType.Application.OctetStream
                            }
                            call.response.headers.append(io.ktor.http.HttpHeaders.ContentType, contentType.toString())
                            call.respondFile(file)
                        }
                        get("/api/v1/media") {
                            call.respond(LocalStore.listMedia())
                        }
                        get("/api/v1/files") {
                            if (ProductFlavorConfig.isFileEasy) {
                                call.respond(
                                    HttpStatusCode.Gone,
                                    mapOf("message" to "FileEasy v1 does not provide remote file management")
                                )
                                return@get
                            }
                            call.respond(LocalStore.listManagedFiles())
                        }
                        get("/api/v1/files/{id}") {
                            if (ProductFlavorConfig.isFileEasy) {
                                call.respond(
                                    HttpStatusCode.Gone,
                                    mapOf("message" to "FileEasy v1 does not provide remote file management")
                                )
                                return@get
                            }
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing file id"))
                                return@get
                            }
                            val file = LocalStore.getManagedFile(id)
                            if (file == null) {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to "File not found"))
                                return@get
                            }
                            call.respond(file)
                        }
                        get("/api/v1/files/{id}/preview") {
                            if (ProductFlavorConfig.isFileEasy) {
                                call.respond(
                                    HttpStatusCode.Gone,
                                    mapOf("message" to "FileEasy v1 does not provide remote file preview")
                                )
                                return@get
                            }
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing file id"))
                                return@get
                            }
                            try {
                                val preview = LocalStore.getManagedFilePreview(id)
                                if (preview == null) {
                                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "File not found"))
                                    return@get
                                }
                                call.response.headers.append(
                                    HttpHeaders.ContentType,
                                    preview.contentType.toString()
                                )
                                call.respondFile(preview.file)
                            } catch (e: IllegalArgumentException) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "当前类型不支持在线预览")))
                            }
                        }
                        get("/api/v1/files/{id}/download") {
                            if (ProductFlavorConfig.isFileEasy) {
                                call.respond(
                                    HttpStatusCode.Gone,
                                    mapOf("message" to "FileEasy v1 does not provide remote file downloads")
                                )
                                return@get
                            }
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing file id"))
                                return@get
                            }
                            val download = LocalStore.getManagedFileDownload(id)
                            if (download == null) {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to "File not found"))
                                return@get
                            }
                            call.response.headers.append(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Attachment.withParameter(
                                    ContentDisposition.Parameters.FileName,
                                    download.displayName
                                ).toString()
                            )
                            call.response.headers.append(
                                HttpHeaders.ContentType,
                                download.contentType.toString()
                            )
                            call.respondFile(download.file)
                        }
                        patch("/api/v1/files/{id}") {
                            if (ProductFlavorConfig.isFileEasy) {
                                call.respond(
                                    HttpStatusCode.Gone,
                                    mapOf("message" to "FileEasy v1 does not provide remote file rename")
                                )
                                return@patch
                            }
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing file id"))
                                return@patch
                            }
                            try {
                                val request = call.receive<RenameFileRequest>()
                                call.respond(LocalStore.renameManagedFile(id, request.baseName))
                            } catch (e: NoSuchElementException) {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to (e.message ?: "File not found")))
                            } catch (e: IllegalArgumentException) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Invalid rename request")))
                            }
                        }
                        delete("/api/v1/files/{id}") {
                            if (ProductFlavorConfig.isFileEasy) {
                                call.respond(
                                    HttpStatusCode.Gone,
                                    mapOf("message" to "FileEasy v1 does not provide remote file deletion")
                                )
                                return@delete
                            }
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing file id"))
                                return@delete
                            }
                            val deleted = LocalStore.deleteManagedFile(id)
                            if (!deleted) {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to "File not found"))
                                return@delete
                            }
                            call.respond(mapOf("status" to "ok"))
                        }
                        post("/api/v1/files/batch-delete") {
                            if (ProductFlavorConfig.isFileEasy) {
                                call.respond(
                                    HttpStatusCode.Gone,
                                    mapOf("message" to "FileEasy v1 does not provide remote batch deletion")
                                )
                                return@post
                            }
                            val request = call.receive<BatchDeleteFilesRequest>()
                            if (request.ids.isEmpty()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "文件列表不能为空"))
                                return@post
                            }
                            call.respond(LocalStore.batchDeleteManagedFiles(request.ids))
                        }
                        delete("/api/v1/media/{id}") {
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing media id"))
                                return@delete
                            }
                            val deleted = LocalStore.deleteMedia(id)
                            if (!deleted) {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to "Media not found"))
                                return@delete
                            }
                            call.respond(mapOf("status" to "ok"))
                        }
                        post("/api/v1/media/upload") {
                            if (ProductFlavorConfig.isFileEasy) {
                                call.respond(
                                    HttpStatusCode.Gone,
                                    mapOf("message" to "FileEasy 已禁用旧上传接口，请使用 /api/v1/upload/* 分片上传接口")
                                )
                                return@post
                            }
                            val multipart = call.receiveMultipart()
                            var result: MediaResponse? = null
                            try {
                                multipart.forEachPart { part ->
                                    if (part is PartData.FileItem && part.name == "file") {
                                        result = LocalStore.saveUpload(part)
                                    }
                                    part.dispose()
                                }
                            } catch (e: IllegalArgumentException) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Invalid media upload request")))
                                return@post
                            }
                            if (result == null) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "File is required"))
                                return@post
                            }
                            call.respond(result!!)
                        }
                        post("/api/v1/upload/init") {
                            try {
                                val request = call.receive<UploadInitRequest>()
                                call.respond(LocalStore.initUploadSession(request))
                            } catch (e: IllegalArgumentException) {
                                val message = e.message ?: "Invalid upload init request"
                                val status = when (message) {
                                    "单文件不能超过 4GB" -> HttpStatusCode.PayloadTooLarge
                                    "当前文件类型不在 FileEasy v1 支持范围内" -> HttpStatusCode.BadRequest
                                    else -> HttpStatusCode.BadRequest
                                }
                                call.respond(status, mapOf("message" to message))
                            } catch (e: IllegalStateException) {
                                call.respond(HttpStatusCode.InsufficientStorage, mapOf("message" to (e.message ?: "存储空间不足，无法开始上传")))
                            }
                        }
                        post("/api/v1/upload/chunk") {
                            val multipart = call.receiveMultipart()
                            var uploadId: String? = null
                            var chunkIndex: Int? = null
                            var totalChunks: Int? = null
                            var fileName: String? = null
                            var fileSize: Long? = null
                            var chunkBytes: ByteArray? = null

                            multipart.forEachPart { part ->
                                when (part) {
                                    is PartData.FormItem -> {
                                        when (part.name) {
                                            "uploadId" -> uploadId = part.value
                                            "chunkIndex" -> chunkIndex = part.value.toIntOrNull()
                                            "totalChunks" -> totalChunks = part.value.toIntOrNull()
                                            "fileName" -> fileName = part.value
                                            "fileSize" -> fileSize = part.value.toLongOrNull()
                                        }
                                    }

                                    is PartData.FileItem -> {
                                        if (part.name == "file" || part.name == "chunk") {
                                            chunkBytes = part.streamProvider().use { it.readBytes() }
                                        }
                                    }

                                    else -> Unit
                                }
                                part.dispose()
                            }

                            if (uploadId.isNullOrBlank() || chunkIndex == null || chunkBytes == null) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing upload chunk fields"))
                                return@post
                            }

                            try {
                                val response = LocalStore.saveUploadChunk(
                                    uploadId = uploadId!!,
                                    chunkIndex = chunkIndex!!,
                                    totalChunks = totalChunks,
                                    fileName = fileName,
                                    fileSize = fileSize,
                                    chunkBytes = chunkBytes!!
                                )
                                call.respond(response)
                                requestStopIfIdle()
                            } catch (e: IllegalArgumentException) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Invalid upload chunk request")))
                            } catch (e: IllegalStateException) {
                                val message = e.message ?: "上传会话状态异常"
                                val status = when (message) {
                                    "上传会话已过期" -> HttpStatusCode.Gone
                                    "存储空间不足，无法写入分片" -> HttpStatusCode.InsufficientStorage
                                    else -> HttpStatusCode.Conflict
                                }
                                call.respond(status, mapOf("message" to message))
                            }
                        }
                        get("/api/v1/upload/status/{uploadId}") {
                            val uploadId = call.parameters["uploadId"]
                            if (uploadId.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing upload id"))
                                return@get
                            }
                            try {
                                val status = LocalStore.getUploadSessionStatus(uploadId)
                                if (status == null) {
                                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "Upload session not found"))
                                    return@get
                                }
                                call.respond(status)
                            } catch (e: IllegalStateException) {
                                call.respond(HttpStatusCode.Gone, mapOf("message" to (e.message ?: "上传会话已过期")))
                            }
                        }
                        delete("/api/v1/upload/status/{uploadId}") {
                            val uploadId = call.parameters["uploadId"]
                            if (uploadId.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing upload id"))
                                return@delete
                            }
                            val deleted = LocalStore.cancelUploadSession(uploadId)
                            if (!deleted) {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to "Upload session not found"))
                                return@delete
                            }
                            call.respond(mapOf("status" to "ok"))
                            requestStopIfIdle()
                        }
                        post("/api/v1/upload/complete") {
                            val request = call.receive<UploadCompleteRequest>()
                            try {
                                call.respond(LocalStore.completeUploadSession(request.uploadId))
                                requestStopIfIdle()
                            } catch (e: IllegalArgumentException) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Invalid upload complete request")))
                            } catch (e: IllegalStateException) {
                                val message = e.message ?: "上传完成失败"
                                val status = when (message) {
                                    "上传会话已过期" -> HttpStatusCode.Gone
                                    "存储空间不足，无法合并文件" -> HttpStatusCode.InsufficientStorage
                                    else -> HttpStatusCode.Conflict
                                }
                                call.respond(status, mapOf("message" to message))
                            }
                        }
                        get("/api/v1/playlists") {
                            call.respond(LocalStore.listPlaylists())
                        }
                        get("/api/v1/playlists/{id}") {
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing playlist id"))
                                return@get
                            }
                            val playlist = LocalStore.getPlaylist(id)
                            if (playlist == null) {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to "Playlist not found"))
                                return@get
                            }
                            call.respond(playlist)
                        }
                        post("/api/v1/playlists") {
                            val request = call.receive<CreatePlaylistRequest>()
                            val playlist = LocalStore.createPlaylist(request)
                            call.respond(playlist)
                        }
                        put("/api/v1/playlists/{id}") {
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing playlist id"))
                                return@put
                            }
                            val request = call.receive<CreatePlaylistRequest>()
                            try {
                                val playlist = LocalStore.updatePlaylist(id, request)
                                call.respond(playlist)
                            } catch (e: Exception) {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to (e.message ?: "Update failed")))
                            }
                        }
                        delete("/api/v1/playlists/{id}") {
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing playlist id"))
                                return@delete
                            }
                            val deleted = LocalStore.deletePlaylist(id)
                            if (!deleted) {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to "Playlist not found"))
                                return@delete
                            }
                            call.respond(mapOf("status" to "ok"))
                        }
                        get("/api/v1/devices") {
                            call.respond(LocalStore.listDevices())
                        }
                        post("/api/v1/devices/register") {
                            val request = call.receive<RegisterRequest>()
                            val device = LocalStore.registerDevice(request)
                            call.respond(device)
                        }
                        put("/api/v1/devices/{id}/heartbeat") {
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing device id"))
                                return@put
                            }
                            val response = LocalStore.heartbeat(id)
                            if (response == null) {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to "Device not found"))
                                return@put
                            }
                            call.respond(response)
                        }
                        patch("/api/v1/devices/{id}/playlist") {
                            val id = call.parameters["id"]
                            if (id.isNullOrBlank()) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing device id"))
                                return@patch
                            }
                            val request = call.receive<AssignPlaylistsRequest>()
                            LocalStore.assignPlaylists(id, request.playlistIds)
                            call.respond(mapOf("status" to "ok"))
                        }
                        get("/api/v1/update/check") {
                            call.respond(LocalStore.getUpdateInfo())
                        }
                        post("/api/v1/update/upload") {
                            val multipart = call.receiveMultipart()
                            var success = false
                            multipart.forEachPart { part ->
                                if (part is PartData.FileItem && part.name == "file") {
                                    success = LocalStore.saveUpdateApk(part)
                                }
                                part.dispose()
                            }
                            if (success) {
                                call.respond(mapOf("status" to "ok"))
                            } else {
                                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to save APK"))
                            }
                        }
                        get("/api/v1/update/download") {
                            val file = LocalStore.getUpdateApkFile()
                            if (file.exists()) {
                                call.response.headers.append(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(
                                        ContentDisposition.Parameters.FileName,
                                        "${ProductFlavorConfig.apkBaseName}-update.apk"
                                    ).toString()
                                )
                                call.respond(LocalFileContent(file, ContentType("application", "vnd.android.package-archive")))
                            } else {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to "No update APK found"))
                            }
                        }
                        // ✅ 新增：下载当前运行的 APK
                        get("/api/v1/system/app-apk") {
                            val appFile = File(this@LocalServerService.packageCodePath)
                            if (appFile.exists()) {
                                call.response.headers.append(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(
                                        ContentDisposition.Parameters.FileName,
                                        "${ProductFlavorConfig.apkBaseName}-current.apk"
                                    ).toString()
                                )
                                call.respond(LocalFileContent(appFile, ContentType("application", "vnd.android.package-archive")))
                            } else {
                                call.respond(HttpStatusCode.NotFound, mapOf("message" to "App APK not found"))
                            }
                        }
                        // ✅ 监控API
                        get("/api/v1/system/monitor") {
                            call.respond(LocalStore.getSystemMonitor())
                        }

                        if (LocalStore.isTransferEnabled()) {
                            // ----------------------------
                            // File Transfer (Admin APIs)
                            // ----------------------------
                            post("/api/v1/transfer/upload") {
                                if (!LocalStore.canAcceptTransferUpload()) {
                                    call.respond(
                                        HttpStatusCode.Forbidden,
                                        mapOf("message" to "存储空间不足，已禁止上传")
                                    )
                                    return@post
                                }
                                val multipart = call.receiveMultipart()
                                var uploaded: TransferFileResponse? = null
                                try {
                                    multipart.forEachPart { part ->
                                        if (part is PartData.FileItem && part.name == "file") {
                                            uploaded = LocalStore.saveTransferUpload(
                                                part = part,
                                                uploaderIp = LocalStore.getClientIp(call),
                                                uploaderUserAgent = call.request.header("User-Agent")
                                            ) { fileId ->
                                                LocalStore.buildShareUrl(call, fileId)
                                            }
                                        }
                                        part.dispose()
                                    }
                                } catch (e: IllegalArgumentException) {
                                    val msg = e.message ?: "Invalid file"
                                    when (msg) {
                                        "FILE_TOO_LARGE" -> call.respond(HttpStatusCode.PayloadTooLarge, mapOf("message" to "单文件大小不能超过 2GB"))
                                        "NAME_TOO_LONG" -> call.respond(HttpStatusCode.BadRequest, mapOf("message" to "文件名长度不能超过 255 字符"))
                                        else -> call.respond(HttpStatusCode.BadRequest, mapOf("message" to msg))
                                    }
                                    return@post
                                }
                                if (uploaded == null) {
                                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "File is required"))
                                    return@post
                                }
                                call.respond(uploaded!!)
                            }

                            get("/api/v1/transfer/files") {
                                val files = LocalStore.listTransferFiles { fileId ->
                                    LocalStore.buildShareUrl(call, fileId)
                                }
                                call.respond(files)
                            }

                            delete("/api/v1/transfer/files/{id}") {
                                val id = call.parameters["id"]
                                if (id.isNullOrBlank()) {
                                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing file id"))
                                    return@delete
                                }
                                val ok = LocalStore.deleteTransferFile(
                                    id = id,
                                    deleterIp = LocalStore.getClientIp(call),
                                    deleterUserAgent = call.request.header("User-Agent")
                                )
                                if (!ok) {
                                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "File not found"))
                                    return@delete
                                }
                                call.respond(mapOf("status" to "ok"))
                            }

                            get("/api/v1/transfer/files/{id}/share") {
                                val id = call.parameters["id"]
                                if (id.isNullOrBlank()) {
                                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing file id"))
                                    return@get
                                }
                                val exists = LocalStore.getTransferFile(id)
                                if (exists == null) {
                                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "File not found"))
                                    return@get
                                }
                                val shareUrl = LocalStore.buildShareUrl(call, id)
                                call.respond(mapOf("shareUrl" to shareUrl, "qrContent" to shareUrl))
                            }

                            get("/api/v1/transfer/files/{id}/logs") {
                                val id = call.parameters["id"]
                                if (id.isNullOrBlank()) {
                                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing file id"))
                                    return@get
                                }
                                val logs = LocalStore.getTransferLogs(id)
                                call.respond(logs)
                            }

                            get("/api/v1/transfer/storage") {
                                call.respond(LocalStore.getTransferStorageStatus())
                            }

                            get("/api/v1/transfer/files/{id}/qr") {
                                val id = call.parameters["id"]
                                if (id.isNullOrBlank()) {
                                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing file id"))
                                    return@get
                                }
                                val file = LocalStore.getTransferFile(id)
                                if (file == null) {
                                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "File not found"))
                                    return@get
                                }
                                val shareUrl = LocalStore.buildShareUrl(call, id)
                                val bitmap = QRCodeUtil.createQRCodeBitmap(
                                    content = shareUrl,
                                    width = 300,
                                    height = 300,
                                    errorCorrection = "M",
                                    margin = "2"
                                )
                                if (bitmap == null) {
                                    call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to generate QR code"))
                                    return@get
                                }
                                val baos = ByteArrayOutputStream()
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)
                                val bytes = baos.toByteArray()
                                call.response.header(HttpHeaders.ContentType, "image/png")
                                call.response.header(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(
                                        ContentDisposition.Parameters.FileName,
                                        "transfer-$id.png"
                                    ).toString()
                                )
                                call.respondBytes(bytes, ContentType.Image.PNG)
                            }

                            // ----------------------------
                            // File Transfer (Share/Download)
                            // ----------------------------
                            get("/t/{fileId}") {
                                val fileId = call.parameters["fileId"]
                                if (fileId.isNullOrBlank()) {
                                    call.respond(HttpStatusCode.NotFound)
                                    return@get
                                }
                                call.respondText(LocalStore.renderTransferSharePage(fileId), ContentType.Text.Html)
                            }

                            post("/t/{fileId}/auth") {
                                val fileId = call.parameters["fileId"]
                                if (fileId.isNullOrBlank()) {
                                    call.respond(HttpStatusCode.NotFound)
                                    return@post
                                }
                                val req = call.receive<TransferAuthRequest>()
                                val ip = LocalStore.getClientIp(call)
                                val ua = call.request.header("User-Agent")

                                val file = LocalStore.getTransferFile(fileId)
                                if (file == null) {
                                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "文件不存在"))
                                    return@post
                                }
                                if (LocalStore.isTransferExpired(file)) {
                                    call.respond(HttpStatusCode.Gone, mapOf("message" to "文件已过期"))
                                    return@post
                                }

                                val ok = LocalStore.verifyTransferDownloadAccount(req.username, req.password)
                                LocalStore.logTransferAction(
                                    fileId = fileId,
                                    action = "AUTH",
                                    ip = ip,
                                    userAgent = ua,
                                    authResult = ok,
                                    clientRemark = null,
                                    result = if (ok) "Success" else "Invalid credentials"
                                )
                                if (!ok) {
                                    call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "账号或密码错误"))
                                    return@post
                                }

                                val token = LocalStore.issueTransferToken(fileId = fileId, ip = ip ?: "unknown")
                                // non-httpOnly: front page uses fetch; cookie also works for download
                                call.response.cookies.append(
                                    name = "xplay_transfer_token",
                                    value = token,
                                    path = "/t",
                                    maxAge = 3600L
                                )
                                call.respond(TransferAuthResponse(token = token))
                            }

                            get("/t/{fileId}/info") {
                                val fileId = call.parameters["fileId"]
                                if (fileId.isNullOrBlank()) {
                                    call.respond(HttpStatusCode.NotFound)
                                    return@get
                                }
                                val file = LocalStore.getTransferFile(fileId)
                                if (file == null) {
                                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "文件不存在"))
                                    return@get
                                }
                                if (LocalStore.isTransferExpired(file)) {
                                    call.respond(HttpStatusCode.Gone, mapOf("message" to "文件已过期"))
                                    return@get
                                }
                                val ip = LocalStore.getClientIp(call)
                                val token = call.request.cookies["xplay_transfer_token"]
                                if (!LocalStore.verifyTransferToken(token = token, fileId = fileId, ip = ip ?: "unknown")) {
                                    call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "未授权"))
                                    return@get
                                }

                                val quota = LocalStore.getTransferIpQuota(ip ?: "unknown")
                                call.respond(
                                    mapOf(
                                        "file" to LocalStore.toTransferFileResponse(file) { id -> LocalStore.buildShareUrl(call, id) },
                                        "quotaUsed" to quota.first,
                                        "quotaRemaining" to quota.second
                                    )
                                )
                            }

                            get("/t/{fileId}/download") {
                                val fileId = call.parameters["fileId"]
                                if (fileId.isNullOrBlank()) {
                                    call.respond(HttpStatusCode.NotFound)
                                    return@get
                                }
                                val file = LocalStore.getTransferFile(fileId)
                                if (file == null) {
                                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "文件不存在"))
                                    return@get
                                }
                                if (LocalStore.isTransferExpired(file)) {
                                    call.respond(HttpStatusCode.Gone, mapOf("message" to "文件已过期"))
                                    return@get
                                }

                                val ip = LocalStore.getClientIp(call)
                                val ua = call.request.header("User-Agent")
                                val remark = call.request.queryParameters["remark"]
                                val token = call.request.cookies["xplay_transfer_token"]
                                if (!LocalStore.verifyTransferToken(token = token, fileId = fileId, ip = ip ?: "unknown")) {
                                    LocalStore.logTransferAction(
                                        fileId = fileId,
                                        action = "DOWNLOAD",
                                        ip = ip,
                                        userAgent = ua,
                                        authResult = false,
                                        clientRemark = remark,
                                        result = "Unauthorized"
                                    )
                                    call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "请先输入账号密码"))
                                    return@get
                                }

                                val quotaOk = LocalStore.tryConsumeTransferQuota(ip ?: "unknown")
                                if (!quotaOk) {
                                    call.respond(
                                        HttpStatusCode.TooManyRequests,
                                        mapOf("message" to "您今日下载次数已达上限（100次），请明天再试")
                                    )
                                    return@get
                                }

                                val diskFile = LocalStore.getTransferDiskFile(file)
                                if (!diskFile.exists()) {
                                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "文件不存在"))
                                    return@get
                                }

                                LocalStore.onTransferDownloaded(fileId = fileId, ip = ip, userAgent = ua, remark = remark)
                                call.response.headers.append(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.Attachment.withParameter(
                                        ContentDisposition.Parameters.FileName,
                                        file.originalName
                                    ).toString()
                                )
                                call.respondFile(diskFile)
                            }
                        }

                        get("{path...}") {
                            val path = call.parameters.getAll("path")?.joinToString("/") ?: ""
                            if (ProductFlavorConfig.isFileEasy && (path == "admin" || path.startsWith("admin/"))) {
                                call.respond(
                                    HttpStatusCode.Gone,
                                    mapOf("message" to "FileEasy v1 does not provide remote admin")
                                )
                                return@get
                            }
                            if (path.startsWith("api") || path.startsWith("uploads")) {
                                call.respond(HttpStatusCode.NotFound)
                                return@get
                            }
                            val target = if (path.isBlank()) "index.html" else path
                            val (bytes, contentType) = LocalStore.readWebAsset(target, fallbackToIndex = true)
                            call.respondBytes(bytes, contentType = contentType)
                        }
                    }
                }
                server?.start(wait = false)
                updateRuntimeState(ServiceRuntimeState.RUNNING)
            } catch (e: Exception) {
                server = null
                updateRuntimeState(ServiceRuntimeState.ERROR)
                Log.e(TAG, "Failed to start server", e)
            }
        }
    }

    private fun stopServer() {
        server?.stop(1000, 2000)
        server = null
        Log.d(TAG, "Local Server stopped")
    }

    private fun requestStopIfIdle() {
        CoroutineScope(Dispatchers.IO).launch {
            val shouldStop = !isAppInForeground && !LocalStore.hasActiveUploadSessions()
            if (!shouldStop) {
                return@launch
            }

            Log.d(TAG, "Stopping service because app is backgrounded and no uploads are active")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(
        channelId: String,
        state: ServiceRuntimeState
    ): android.app.Notification {
        val statusText = when (state) {
            ServiceRuntimeState.STARTING -> "服务启动中"
            ServiceRuntimeState.RUNNING -> "服务运行中"
            ServiceRuntimeState.ERROR -> "服务启动失败"
            ServiceRuntimeState.STOPPED -> "服务已停止"
        }

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(this)
        }.apply {
            setContentTitle(ProductFlavorConfig.serverNotificationName)
            setContentText("$statusText · ${ProductFlavorConfig.serverRunningDescription}")
            setSmallIcon(android.R.drawable.ic_media_play)
            setOngoing(true)
        }.build()
    }

    private fun updateRuntimeState(state: ServiceRuntimeState) {
        _runtimeState.value = state
        if (state != ServiceRuntimeState.STOPPED) {
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, buildNotification("xplay_server_channel", state))
        }
    }

    companion object {
        private const val TAG = "LocalServerService"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_APP_BACKGROUND = "com.xplay.player.server.APP_BACKGROUND"
        private const val ACTION_RESTART_SERVER = "com.xplay.player.server.RESTART"

        private val _runtimeState = MutableStateFlow(ServiceRuntimeState.STOPPED)
        val runtimeState = _runtimeState.asStateFlow()
        @Volatile
        private var isAppInForeground: Boolean = true

        fun notifyAppBackground(context: Context) {
            if (runtimeState.value == ServiceRuntimeState.STOPPED) {
                return
            }
            context.startService(
                Intent(context, LocalServerService::class.java).apply {
                    action = ACTION_APP_BACKGROUND
                }
            )
        }

        fun restart(context: Context) {
            val intent = Intent(context, LocalServerService::class.java).apply {
                action = ACTION_RESTART_SERVER
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

enum class ServiceRuntimeState {
    STARTING,
    RUNNING,
    ERROR,
    STOPPED
}

@Serializable
data class RegisterRequest(
    val serialNumber: String,
    val name: String? = null,
    val version: String? = null,
    val ipAddress: String? = null
)

@Serializable
data class HeartbeatResponse(
    val status: String,
    val playlistIds: List<String>,
    val updateInfo: UpdateResponse? = null
)

@Serializable
data class UpdateResponse(
    val versionCode: Int,
    val versionName: String,
    val hasUpdate: Boolean
)

@Serializable
data class LoginRequest(val username: String? = null, val password: String)

@Serializable
data class UpdatePasswordRequest(val oldPassword: String, val newPassword: String)

@Serializable
data class UploadInitRequest(
    val fileName: String,
    val fileSize: Long,
    val mimeType: String? = null,
    val chunkSize: Long? = null
)

@Serializable
data class UploadInitResponse(
    val uploadId: String,
    val fileName: String,
    val fileSize: Long,
    val chunkSize: Long,
    val totalChunks: Int,
    val status: String,
    val expiresAt: Long,
    val createdAt: Long
)

@Serializable
data class UploadChunkResponse(
    val uploadId: String,
    val chunkIndex: Int,
    val uploadedChunks: Int,
    val totalChunks: Int,
    val uploadedChunkIndexes: List<Int>,
    val duplicate: Boolean,
    val status: String
)

@Serializable
data class UploadStatusResponse(
    val uploadId: String,
    val fileName: String,
    val totalChunks: Int,
    val uploadedChunks: Int,
    val uploadedChunkIndexes: List<Int>,
    val missingChunkIndexes: List<Int>,
    val status: String,
    val expiresAt: Long,
    val createdAt: Long
)

@Serializable
data class UploadCompleteRequest(
    val uploadId: String
)

@Serializable
data class UploadCompleteResponse(
    val uploadId: String,
    val fileId: String,
    val fileName: String,
    val displayName: String,
    val storedFilename: String,
    val size: Long,
    val status: String,
    val url: String
)

object LocalStore {
    private val formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault())
    private val invalidRenameChars = Regex("[\\\\/:*?\"<>|]")
    private var initialized = false
    private lateinit var appContext: Context
    private var startTime = System.currentTimeMillis()
    private var requestCounter = java.util.concurrent.atomic.AtomicLong(0)

    fun init(context: Context) {
        if (!initialized) {
            appContext = context.applicationContext
            LocalDatabaseProvider.init(appContext)
            startTime = System.currentTimeMillis()
            initialized = true
        }
        if (WebAdminInitializer.hasAssets(appContext)) {
            WebAdminInitializer.copyAssetsToWebRoot(appContext)
        }
        startUploadMaintenance()
    }

    private fun getPrefs() = appContext.getSharedPreferences("xplay_settings", Context.MODE_PRIVATE)

    fun isPasswordConfigured(): Boolean {
        return !getPrefs().getString("server_password", null).isNullOrBlank()
    }

    fun verifyPassword(username: String?, password: String): Boolean {
        val savedUsername = "admin" // 固定为 admin
        val savedPassword = getPrefs().getString("server_password", null)
        if (savedPassword.isNullOrBlank()) {
            return username == null || username == savedUsername
        }
        return (username == null || username == savedUsername) && savedPassword == password
    }

    fun getCurrentPassword(): String {
        return getPrefs().getString("server_password", null).orEmpty()
    }

    fun updatePassword(newPassword: String) {
        val normalized = newPassword.trim()
        if (normalized.isBlank()) {
            getPrefs().edit().remove("server_password").apply()
            return
        }
        getPrefs().edit().putString("server_password", normalized).apply()
    }

    private fun db() = LocalDatabaseProvider.get()

    private fun getLegacyUploadDir(): File {
        val uploads = File(appContext.filesDir, "uploads")
        if (!uploads.exists()) {
            uploads.mkdirs()
        }
        return uploads
    }

    private fun getUploadDir(): File {
        val externalDocumentsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val root = File(externalDocumentsDir ?: File(appContext.filesDir, "documents"), "FileEasy")
        if (!root.exists()) {
            root.mkdirs()
        }
        return root
    }

    private fun getUploadCategoryDir(category: String): File {
        val folderName = when (category) {
            "document" -> "document"
            "image" -> "image"
            "video" -> "video"
            "audio" -> "audio"
            "archive" -> "archive"
            else -> "other"
        }
        return File(getUploadDir(), folderName).also { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    private fun buildStoredUploadPath(category: String, storedFilename: String): String {
        return "${getUploadCategoryDir(category).name}/$storedFilename"
    }

    private fun getUploadSessionRootDir(): File {
        val dir = File(appContext.filesDir, "upload_sessions")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getUploadSessionDir(uploadId: String): File {
        val dir = File(getUploadSessionRootDir(), uploadId)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getUploadChunkFile(uploadId: String, chunkIndex: Int): File {
        return File(getUploadSessionDir(uploadId), "chunk-$chunkIndex.part")
    }

    private fun getUploadMergedTempFile(uploadId: String): File {
        return File(getUploadSessionDir(uploadId), "merge.tmp")
    }

    private fun getAvailableUploadBytes(): Long {
        val stat = android.os.StatFs(getUploadDir().path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }

    private fun ensureUploadStorageAvailable(requiredBytes: Long, errorMessage: String) {
        if (getAvailableUploadBytes() < requiredBytes) {
            throw IllegalStateException(errorMessage)
        }
    }

    private fun normalizeClientFileName(fileName: String): String = FileEasyUploadCore.normalizeClientFileName(fileName)

    private fun extractExtension(fileName: String): String = FileEasyUploadCore.extractExtension(fileName)

    private fun getUploadCategory(extension: String): String? = FileEasyUploadCore.getUploadCategory(extension)

    private fun isPreviewSupported(extension: String, category: String): Boolean =
        FileEasyUploadCore.isPreviewSupported(extension, category)

    private fun resolveMimeCategory(mimeType: String?): String? = FileEasyUploadCore.resolveMimeCategory(mimeType)

    private fun persistBytesAtomically(target: File, bytes: ByteArray) {
        val parent = requireNotNull(target.parentFile)
        if (!parent.exists()) {
            parent.mkdirs()
        }
        val tempFile = File(parent, "${target.name}.tmp")
        tempFile.outputStream().buffered().use { output ->
            output.write(bytes)
            output.flush()
        }
        if (target.exists()) {
            target.delete()
        }
        if (!tempFile.renameTo(target)) {
            tempFile.copyTo(target, overwrite = true)
            tempFile.delete()
        }
    }

    private fun moveFile(source: File, target: File) {
        val parent = requireNotNull(target.parentFile)
        if (!parent.exists()) {
            parent.mkdirs()
        }
        if (!source.renameTo(target)) {
            source.copyTo(target, overwrite = true)
            source.delete()
        }
    }

    private fun safeDeleteRecursively(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                safeDeleteRecursively(child)
            }
        }
        file.delete()
    }

    // ----------------------------
    // FileEasy upload: config & helpers
    // ----------------------------
    private const val UPLOAD_CHUNK_SIZE_BYTES: Long = FileEasyUploadCore.CHUNK_SIZE_BYTES
    private const val UPLOAD_MAX_FILE_BYTES: Long = FileEasyUploadCore.MAX_FILE_BYTES
    private const val UPLOAD_SESSION_RETENTION_MS: Long = 24L * 60 * 60 * 1000
    private val uploadMaintenanceStarted = java.util.concurrent.atomic.AtomicBoolean(false)

    fun startUploadMaintenance() {
        if (!uploadMaintenanceStarted.compareAndSet(false, true)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cleanupExpiredUploadSessions()
            } catch (e: Exception) {
                Log.e("LocalStore", "Upload cleanup on start failed", e)
            }
            while (true) {
                try {
                    cleanupExpiredUploadSessions()
                } catch (e: Exception) {
                    Log.e("LocalStore", "Upload periodic cleanup failed", e)
                }
                delay(60 * 60 * 1000L)
            }
        }
    }

    private suspend fun cleanupExpiredUploadSessions() {
        val now = System.currentTimeMillis()
        val expiredSessions = db().uploadSessionDao().getExpiredSessions(now)
        expiredSessions.forEach { session ->
            db().uploadSessionDao().deleteChunksByUploadId(session.uploadId)
            db().uploadSessionDao().deleteSessionById(session.uploadId)
            safeDeleteRecursively(File(getUploadSessionRootDir(), session.uploadId))
        }
    }

    private suspend fun loadUploadSession(uploadId: String): UploadSessionEntity? {
        val session = db().uploadSessionDao().getSessionById(uploadId) ?: return null
        if (session.expiresAt < System.currentTimeMillis()) {
            db().uploadSessionDao().deleteChunksByUploadId(uploadId)
            db().uploadSessionDao().deleteSessionById(uploadId)
            safeDeleteRecursively(File(getUploadSessionRootDir(), uploadId))
            throw IllegalStateException("上传会话已过期")
        }
        return session
    }

    private suspend fun buildUploadStatusResponse(session: UploadSessionEntity): UploadStatusResponse {
        val uploadedChunkIndexes = db().uploadSessionDao().getChunkIndexes(session.uploadId)
        val missingChunkIndexes = FileEasyUploadCore.computeMissingChunkIndexes(
            totalChunks = session.totalChunks,
            uploadedChunkIndexes = uploadedChunkIndexes
        )
        return UploadStatusResponse(
            uploadId = session.uploadId,
            fileName = session.fileName,
            totalChunks = session.totalChunks,
            uploadedChunks = uploadedChunkIndexes.size,
            uploadedChunkIndexes = uploadedChunkIndexes,
            missingChunkIndexes = missingChunkIndexes,
            status = session.status,
            expiresAt = session.expiresAt,
            createdAt = session.createdAt
        )
    }

    suspend fun listActiveUploadSessions(limit: Int = 4): List<HomeUploadTaskResponse> {
        val now = System.currentTimeMillis()
        return db().uploadSessionDao().getActiveSessions(now, limit).map { session ->
            val progress = if (session.totalChunks <= 0) {
                0
            } else {
                ((session.uploadedChunks.toDouble() / session.totalChunks.toDouble()) * 100.0).toInt().coerceIn(0, 99)
            }
            val uploadedBytes = (session.uploadedChunks.toLong() * session.chunkSize).coerceAtMost(session.fileSize)
            HomeUploadTaskResponse(
                uploadId = session.uploadId,
                fileName = session.finalDisplayName ?: session.fileName,
                uploadedChunks = session.uploadedChunks,
                totalChunks = session.totalChunks,
                status = session.status,
                progress = progress,
                fileSize = session.fileSize,
                uploadedBytes = uploadedBytes,
                createdAt = session.createdAt,
                updatedAt = session.updatedAt
            )
        }
    }

    suspend fun listRecentManagedFiles(limit: Int = 4): List<HomeRecentFileResponse> {
        return db().mediaDao().getAll().take(limit).map { media ->
            HomeRecentFileResponse(
                id = media.id,
                fileName = media.displayName.ifBlank { media.originalName },
                category = media.category.ifBlank { media.type },
                size = media.size,
                createdAt = media.createdAt,
                relativeDirectory = media.filename.substringBeforeLast('/', "").ifBlank { null }
            )
        }
    }

    suspend fun getHomeSummary(uploadUrlBuilder: () -> String): HomeSummaryResponse {
        return HomeSummaryResponse(
            uploadUrl = uploadUrlBuilder(),
            passwordRequired = isPasswordConfigured(),
            activeUploads = listActiveUploadSessions(),
            recentFiles = listRecentManagedFiles()
        )
    }

    suspend fun hasActiveUploadSessions(): Boolean {
        return db().uploadSessionDao().getActiveSessions(System.currentTimeMillis(), 1).isNotEmpty()
    }

    fun getFileEasyFolderHint(): String = "文件/FileEasy"

    fun getFileEasyFolderCategoriesHint(): String = "文档 / 图片 / 视频 / 音频 / 压缩包"

    private fun validateUploadInit(fileName: String, fileSize: Long, mimeType: String?): Pair<String, String> {
        val validation = FileEasyUploadCore.validateUploadInit(fileName, fileSize, mimeType)
        return validation.normalizedFileName to validation.category
    }

    suspend fun initUploadSession(request: UploadInitRequest): UploadInitResponse {
        val (normalizedName, _) = validateUploadInit(
            fileName = request.fileName,
            fileSize = request.fileSize,
            mimeType = request.mimeType
        )
        ensureUploadStorageAvailable(
            requiredBytes = request.fileSize.coerceAtLeast(UPLOAD_CHUNK_SIZE_BYTES),
            errorMessage = "存储空间不足，无法开始上传"
        )

        val now = System.currentTimeMillis()
        val uploadId = UUID.randomUUID().toString()
        val totalChunks = FileEasyUploadCore.calculateTotalChunks(request.fileSize, UPLOAD_CHUNK_SIZE_BYTES)
        val session = UploadSessionEntity(
            uploadId = uploadId,
            fileName = normalizedName,
            fileSize = request.fileSize,
            chunkSize = UPLOAD_CHUNK_SIZE_BYTES,
            totalChunks = totalChunks,
            uploadedChunks = 0,
            status = "initialized",
            mimeType = request.mimeType?.trim()?.takeIf { it.isNotEmpty() },
            expiresAt = now + UPLOAD_SESSION_RETENTION_MS,
            createdAt = now,
            updatedAt = now
        )
        db().uploadSessionDao().insertSession(session)
        getUploadSessionDir(uploadId)
        return UploadInitResponse(
            uploadId = uploadId,
            fileName = session.fileName,
            fileSize = session.fileSize,
            chunkSize = session.chunkSize,
            totalChunks = session.totalChunks,
            status = session.status,
            expiresAt = session.expiresAt,
            createdAt = session.createdAt
        )
    }

    suspend fun saveUploadChunk(
        uploadId: String,
        chunkIndex: Int,
        totalChunks: Int?,
        fileName: String?,
        fileSize: Long?,
        chunkBytes: ByteArray
    ): UploadChunkResponse {
        val session = loadUploadSession(uploadId)
            ?: throw IllegalArgumentException("Upload session not found")
        if (chunkIndex < 0 || chunkIndex >= session.totalChunks) {
            throw IllegalArgumentException("Invalid chunk index")
        }
        if (totalChunks != null && totalChunks != session.totalChunks) {
            throw IllegalArgumentException("Chunk total does not match upload session")
        }
        if (fileName != null && normalizeClientFileName(fileName) != session.fileName) {
            throw IllegalArgumentException("Chunk file name does not match upload session")
        }
        if (fileSize != null && fileSize != session.fileSize) {
            throw IllegalArgumentException("Chunk file size does not match upload session")
        }
        if (session.status == "completed") {
            throw IllegalStateException("上传会话已完成")
        }

        val chunkFile = getUploadChunkFile(uploadId, chunkIndex)
        val existingChunkIndexes = db().uploadSessionDao().getChunkIndexes(uploadId)
        val duplicate = existingChunkIndexes.contains(chunkIndex) && chunkFile.exists()
        if (!duplicate) {
            ensureUploadStorageAvailable(
                requiredBytes = chunkBytes.size.toLong().coerceAtLeast(1L),
                errorMessage = "存储空间不足，无法写入分片"
            )
            persistBytesAtomically(chunkFile, chunkBytes)
            db().uploadSessionDao().insertChunk(
                UploadChunkEntity(
                    uploadId = uploadId,
                    chunkIndex = chunkIndex,
                    size = chunkBytes.size.toLong(),
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        val uploadedChunkIndexes = db().uploadSessionDao().getChunkIndexes(uploadId)
        val updatedSession = session.copy(
            uploadedChunks = uploadedChunkIndexes.size,
            status = if (uploadedChunkIndexes.size == session.totalChunks) "ready" else "uploading",
            updatedAt = System.currentTimeMillis()
        )
        db().uploadSessionDao().insertSession(updatedSession)
        return UploadChunkResponse(
            uploadId = uploadId,
            chunkIndex = chunkIndex,
            uploadedChunks = uploadedChunkIndexes.size,
            totalChunks = session.totalChunks,
            uploadedChunkIndexes = uploadedChunkIndexes,
            duplicate = duplicate,
            status = updatedSession.status
        )
    }

    suspend fun getUploadSessionStatus(uploadId: String): UploadStatusResponse? {
        val session = loadUploadSession(uploadId) ?: return null
        return buildUploadStatusResponse(session)
    }

    suspend fun cancelUploadSession(uploadId: String): Boolean {
        val session = db().uploadSessionDao().getSessionById(uploadId) ?: return false
        db().uploadSessionDao().deleteChunksByUploadId(uploadId)
        db().uploadSessionDao().deleteSessionById(uploadId)
        safeDeleteRecursively(File(getUploadSessionRootDir(), session.uploadId))
        return true
    }

    private suspend fun resolveFinalDisplayName(originalName: String): String {
        val extension = extractExtension(originalName)
        val baseName = if (extension.isBlank()) {
            originalName
        } else {
            originalName.removeSuffix(".$extension")
        }
        var candidate = originalName
        var suffix = 1
        while (db().mediaDao().countByDisplayName(candidate) > 0) {
            candidate = if (extension.isBlank()) {
                "$baseName($suffix)"
            } else {
                "$baseName($suffix).$extension"
            }
            suffix += 1
        }
        return candidate
    }

    private suspend fun resolveRenamedDisplayName(
        media: MediaEntity,
        requestedBaseName: String
    ): String {
        val extension = media.extension.ifBlank {
            extractExtension(media.displayName.ifBlank { media.originalName })
        }
        val baseName = requestedBaseName.trim()
        var candidate = if (extension.isBlank()) {
            baseName
        } else {
            "$baseName.$extension"
        }
        var suffix = 1
        while (db().mediaDao().countByDisplayNameExcludingId(candidate, media.id) > 0) {
            candidate = if (extension.isBlank()) {
                "$baseName($suffix)"
            } else {
                "$baseName($suffix).$extension"
            }
            suffix += 1
        }
        return candidate
    }

    private fun validateRenameBaseName(baseName: String, currentExtension: String) {
        val normalized = baseName.trim()
        if (normalized.isBlank()) {
            throw IllegalArgumentException("主文件名不能为空")
        }
        if (invalidRenameChars.containsMatchIn(normalized)) {
            throw IllegalArgumentException("主文件名不能包含 \\ / : * ? \" < > |")
        }
        if (currentExtension.isNotBlank() && normalized.contains('.')) {
            throw IllegalArgumentException("重命名时不允许修改扩展名")
        }
    }

    private fun buildUploadCompleteResponse(
        session: UploadSessionEntity,
        media: MediaEntity
    ): UploadCompleteResponse {
        val displayName = media.displayName.ifBlank { media.originalName }
        return UploadCompleteResponse(
            uploadId = session.uploadId,
            fileId = media.id,
            fileName = session.fileName,
            displayName = displayName,
            storedFilename = media.filename,
            size = media.size,
            status = session.status,
            url = media.url
        )
    }

    suspend fun completeUploadSession(uploadId: String): UploadCompleteResponse {
        val session = loadUploadSession(uploadId)
            ?: throw IllegalArgumentException("Upload session not found")
        if (session.status == "completed") {
            val finalFileId = session.finalFileId
                ?: throw IllegalStateException("上传完成记录缺失")
            val media = db().mediaDao().getById(finalFileId)
                ?: throw IllegalStateException("上传完成记录缺失")
            return buildUploadCompleteResponse(session, media)
        }

        val uploadedChunkIndexes = db().uploadSessionDao().getChunkIndexes(uploadId)
        val missingChunkIndexes = FileEasyUploadCore.computeMissingChunkIndexes(
            totalChunks = session.totalChunks,
            uploadedChunkIndexes = uploadedChunkIndexes
        )
        if (missingChunkIndexes.isNotEmpty()) {
            throw IllegalStateException("上传分片不完整")
        }

        ensureUploadStorageAvailable(
            requiredBytes = session.fileSize.coerceAtLeast(1L),
            errorMessage = "存储空间不足，无法合并文件"
        )

        val mergedTempFile = getUploadMergedTempFile(uploadId)
        var finalFile: File? = null
        try {
            var mergedSize = 0L
            mergedTempFile.outputStream().buffered().use { output ->
                for (index in 0 until session.totalChunks) {
                    val chunkFile = getUploadChunkFile(uploadId, index)
                    if (!chunkFile.exists()) {
                        throw IllegalStateException("上传分片不完整")
                    }
                    chunkFile.inputStream().buffered().use { input ->
                        mergedSize += input.copyTo(output)
                    }
                }
                output.flush()
            }
            if (mergedSize != session.fileSize || mergedTempFile.length() != session.fileSize) {
                throw IllegalStateException("合并后的文件大小校验失败")
            }

            val finalDisplayName = resolveFinalDisplayName(session.fileName)
            val extension = extractExtension(session.fileName)
            val category = getUploadCategory(extension) ?: "other"
            val storedFilename = if (extension.isNotBlank()) {
                "${UUID.randomUUID()}.$extension"
            } else {
                UUID.randomUUID().toString()
            }
            finalFile = File(getUploadCategoryDir(category), storedFilename)
            moveFile(mergedTempFile, finalFile)
            val storedPath = buildStoredUploadPath(category, storedFilename)

            val now = System.currentTimeMillis()
            val media = MediaEntity(
                id = UUID.randomUUID().toString(),
                originalName = session.fileName,
                displayName = finalDisplayName,
                filename = storedPath,
                url = "/uploads/$storedPath",
                type = category,
                extension = extension,
                mimeType = session.mimeType ?: "application/octet-stream",
                category = category,
                previewSupported = isPreviewSupported(extension, category),
                size = finalFile.length(),
                createdAt = now,
                updatedAt = now
            )
            db().mediaDao().insert(media)

            val completedSession = session.copy(
                uploadedChunks = session.totalChunks,
                status = "completed",
                updatedAt = now,
                finalFileId = media.id,
                finalDisplayName = finalDisplayName
            )
            db().uploadSessionDao().insertSession(completedSession)

            for (index in 0 until session.totalChunks) {
                getUploadChunkFile(uploadId, index).delete()
            }

            return buildUploadCompleteResponse(completedSession, media)
        } catch (e: Exception) {
            if (mergedTempFile.exists()) {
                mergedTempFile.delete()
            }
            if (finalFile != null && finalFile.exists()) {
                finalFile.delete()
            }
            if (e is IllegalArgumentException || e is IllegalStateException) {
                throw e
            }
            Log.e("LocalStore", "Failed to complete upload session: $uploadId", e)
            throw IllegalStateException("上传完成失败")
        }
    }

    // ----------------------------
    // Transfer: config & helpers
    // ----------------------------
    private const val TRANSFER_RETENTION_DAYS: Long = 30
    private const val TRANSFER_DAILY_DOWNLOAD_LIMIT: Int = 100
    private const val TRANSFER_DOWNLOAD_USERNAME = "eastai"
    private const val TRANSFER_DOWNLOAD_PASSWORD = "888888"
    private const val TRANSFER_MAX_FILENAME_LENGTH = 255
    private const val TRANSFER_MAX_FILE_BYTES: Long = 2L * 1024 * 1024 * 1024

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private data class TransferToken(val fileId: String, val ip: String, val expiresAt: Long)
    private val transferTokens = ConcurrentHashMap<String, TransferToken>()
    private val transferMaintenanceStarted = java.util.concurrent.atomic.AtomicBoolean(false)

    fun isTransferEnabled(): Boolean {
        // 默认关闭，避免影响旧版本稳定性
        return false
    }

    fun startTransferMaintenance() {
        if (!transferMaintenanceStarted.compareAndSet(false, true)) return
        CoroutineScope(Dispatchers.IO).launch {
            // 启动补偿清理
            try {
                cleanupExpiredTransfers()
            } catch (e: Exception) {
                Log.e("LocalStore", "Transfer cleanup on start failed", e)
            }
            // 周期性清理（简化：每小时一次，覆盖“凌晨清理”要求）
            while (true) {
                try {
                    cleanupExpiredTransfers()
                } catch (e: Exception) {
                    Log.e("LocalStore", "Transfer periodic cleanup failed", e)
                }
                delay(60 * 60 * 1000L)
            }
        }
    }

    private fun getTransferDir(): File {
        val dir = File(appContext.filesDir, "transfer_files")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getTransferUsedMB(): Long {
        val files = getTransferDir().listFiles() ?: emptyArray()
        return files.sumOf { it.length() } / 1024 / 1024
    }

    private fun getFreeSpaceMB(): Long {
        val stat = android.os.StatFs(getTransferDir().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        return bytesAvailable / 1024 / 1024
    }

    fun getTransferStorageStatus(): TransferStorageResponse {
        val usedMB = getTransferUsedMB()
        val freeMB = getFreeSpaceMB()
        val warn = usedMB >= 10_240 || freeMB <= 5_120
        val blocked = usedMB >= 10_240 || freeMB <= 5_120
        return TransferStorageResponse(
            usedMB = usedMB,
            freeMB = freeMB,
            warn = warn,
            blocked = blocked
        )
    }

    fun canAcceptTransferUpload(): Boolean {
        val status = getTransferStorageStatus()
        return !status.blocked
    }

    private fun startOfTodayMillis(now: Long = System.currentTimeMillis()): Long {
        val date = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun computeExpiresAt(now: Long = System.currentTimeMillis()): Long {
        val dayStart = startOfTodayMillis(now)
        return dayStart + ChronoUnit.DAYS.duration.toMillis() * TRANSFER_RETENTION_DAYS
    }

    private fun remainingDays(expiresAt: Long, now: Long = System.currentTimeMillis()): Int {
        val todayStart = startOfTodayMillis(now)
        val diff = expiresAt - todayStart
        val days = (diff / ChronoUnit.DAYS.duration.toMillis()).toInt()
        return days.coerceAtLeast(0)
    }

    fun isTransferExpired(file: TransferFileEntity, now: Long = System.currentTimeMillis()): Boolean {
        return file.expiresAt <= now
    }

    fun verifyTransferDownloadAccount(username: String, password: String): Boolean {
        return username == TRANSFER_DOWNLOAD_USERNAME && password == TRANSFER_DOWNLOAD_PASSWORD
    }

    fun issueTransferToken(fileId: String, ip: String): String {
        val token = UUID.randomUUID().toString()
        transferTokens[token] = TransferToken(fileId = fileId, ip = ip, expiresAt = System.currentTimeMillis() + 3600_000L)
        return token
    }

    fun verifyTransferToken(token: String?, fileId: String, ip: String): Boolean {
        if (token.isNullOrBlank()) return false
        val data = transferTokens[token] ?: return false
        if (data.expiresAt < System.currentTimeMillis()) {
            transferTokens.remove(token)
            return false
        }
        return data.fileId == fileId && data.ip == ip
    }

    fun buildShareUrl(call: io.ktor.server.application.ApplicationCall, fileId: String): String {
        val scheme = call.request.header("X-Forwarded-Proto") ?: "http"
        val host = call.request.header("X-Forwarded-Host") ?: call.request.header("Host") ?: "localhost:3000"
        return "$scheme://$host/t/$fileId"
    }

    fun buildUploadPageUrl(call: io.ktor.server.application.ApplicationCall): String {
        val scheme = call.request.header("X-Forwarded-Proto") ?: "http"
        val host = call.request.header("X-Forwarded-Host") ?: call.request.header("Host") ?: "localhost:3000"
        return "$scheme://$host/"
    }

    fun getClientIp(call: io.ktor.server.application.ApplicationCall): String? {
        val forwarded = call.request.header("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
        val realIp = call.request.header("X-Real-IP")
        return forwarded ?: realIp
    }

    fun renderTransferSharePage(fileId: String): String {
        // 轻量下载页（服务端渲染 HTML），前端通过 /t/{id}/auth & /t/{id}/info 驱动状态
        return """
            <!doctype html>
            <html lang="zh-CN">
            <head>
              <meta charset="utf-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"/>
              <title>文件下载</title>
              <style>
                body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial; margin:0; background:#f5f7fb; color:#111;}
                .wrap{max-width:520px; margin:0 auto; padding:18px;}
                .card{background:#fff; border-radius:14px; box-shadow:0 6px 18px rgba(0,0,0,.06); padding:16px;}
                .title{font-size:18px; font-weight:700; margin:0 0 10px;}
                .muted{color:#666; font-size:13px;}
                .row{display:flex; gap:10px; align-items:center;}
                input{width:100%; padding:12px 12px; border:1px solid #e5e7eb; border-radius:10px; outline:none;}
                button{width:100%; padding:12px; border:0; border-radius:10px; font-weight:700; background:#1677ff; color:#fff;}
                button:disabled{opacity:.6;}
                .danger{color:#d92d20;}
                .kv{display:flex; justify-content:space-between; margin:8px 0; font-size:14px;}
                .kv b{font-weight:600;}
                .divider{height:1px; background:#eee; margin:12px 0;}
              </style>
            </head>
            <body>
              <div class="wrap">
                <div class="card">
                  <div class="title">文件下载</div>
                  <div id="err" class="muted danger" style="display:none"></div>
                  <div id="login">
                    <div class="muted" style="margin-bottom:10px">请输入下载账号密码</div>
                    <div class="row" style="margin-bottom:10px">
                      <input id="un" placeholder="账号" value="eastai"/>
                    </div>
                    <div class="row" style="margin-bottom:12px">
                      <input id="pw" placeholder="密码" type="password"/>
                    </div>
                    <button id="btn" onclick="doAuth()">验证并查看文件</button>
                  </div>
                  <div id="info" style="display:none">
                    <div class="kv"><span class="muted">文件名</span><b id="fn"></b></div>
                    <div class="kv"><span class="muted">大小</span><b id="fs"></b></div>
                    <div class="kv"><span class="muted">上传时间</span><b id="ft"></b></div>
                    <div class="kv"><span class="muted">剩余有效天数</span><b id="rd"></b></div>
                    <div class="muted" style="margin:6px 0">此文件保存 30 天，到期自动删除</div>
                    <div class="divider"></div>
                    <div class="kv"><span class="muted">今日已下载</span><b id="qu"></b></div>
                    <div class="kv"><span class="muted">今日剩余次数</span><b id="qr"></b></div>
                    <div class="divider"></div>
                    <div class="muted" style="margin-bottom:6px">下载备注（可选）</div>
                    <div class="row" style="margin-bottom:12px">
                      <input id="remark" placeholder="姓名/备注（可选）" />
                    </div>
                    <button onclick="download()">下载文件</button>
                  </div>
                </div>
                <div class="muted" style="text-align:center; margin-top:12px">
                  Powered by ${ProductFlavorConfig.productName}
                </div>
              </div>
              <script>
                const fileId = ${'"'}$fileId${'"'};
                function showErr(msg){
                  const el=document.getElementById('err');
                  el.style.display='block'; el.innerText=msg||'';
                }
                function hideErr(){const el=document.getElementById('err'); el.style.display='none'; el.innerText='';}
                function fmtSize(bytes){
                  if(!bytes && bytes!==0) return '-';
                  const units=['B','KB','MB','GB','TB']; let v=bytes; let i=0;
                  while(v>=1024 && i<units.length-1){v/=1024; i++;}
                  return (i===0? v : v.toFixed(2))+' '+units[i];
                }
                async function doAuth(){
                  hideErr();
                  const btn=document.getElementById('btn'); btn.disabled=true;
                  try{
                    const res=await fetch(`/t/${fileId}/auth`, {
                      method:'POST',
                      headers:{'Content-Type':'application/json'},
                      body: JSON.stringify({username:document.getElementById('un').value, password:document.getElementById('pw').value})
                    });
                    const data=await res.json().catch(()=>({}));
                    if(!res.ok){ showErr(data.message||'验证失败'); return; }
                    await loadInfo();
                  }finally{ btn.disabled=false; }
                }
                async function loadInfo(){
                  hideErr();
                  const res=await fetch(`/t/${fileId}/info`);
                  const data=await res.json().catch(()=>({}));
                  if(!res.ok){
                    if(res.status===401){ document.getElementById('login').style.display='block'; document.getElementById('info').style.display='none'; return; }
                    showErr(data.message||'获取文件信息失败'); return;
                  }
                  const f=data.file;
                  document.getElementById('fn').innerText=f.originalName;
                  document.getElementById('fs').innerText=fmtSize(f.size);
                  document.getElementById('rd').innerText=f.remainingDays+' 天';
                  const created = new Date(f.createdAt || 0);
                  document.getElementById('ft').innerText = isNaN(created.getTime()) ? '-' : created.toLocaleString();
                  document.getElementById('qu').innerText=String(data.quotaUsed);
                  document.getElementById('qr').innerText=String(data.quotaRemaining);
                  document.getElementById('login').style.display='none';
                  document.getElementById('info').style.display='block';
                }
                function download(){
                  const remark = document.getElementById('remark').value || '';
                  const query = remark ? '?remark=' + encodeURIComponent(remark) : '';
                  window.location.href = '/t/' + fileId + '/download' + query;
                }
                // 初始尝试（如果已有 cookie token）
                loadInfo();
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    suspend fun saveTransferUpload(
        part: PartData.FileItem,
        uploaderIp: String?,
        uploaderUserAgent: String?,
        shareUrlBuilder: (String) -> String
    ): TransferFileResponse {
        val originalName = part.originalFileName ?: "file"
        if (originalName.length > TRANSFER_MAX_FILENAME_LENGTH) {
            throw IllegalArgumentException("NAME_TOO_LONG")
        }
        val ext = originalName.substringAfterLast('.', "")
        val storedFilename = if (ext.isNotBlank()) "${UUID.randomUUID()}.$ext" else UUID.randomUUID().toString()
        val diskFile = File(getTransferDir(), storedFilename)

        try {
            part.streamProvider().use { input ->
                diskFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            if (diskFile.length() > TRANSFER_MAX_FILE_BYTES) {
                if (diskFile.exists()) diskFile.delete()
                throw IllegalArgumentException("FILE_TOO_LARGE")
            }
        } catch (e: Exception) {
            if (diskFile.exists()) diskFile.delete()
            Log.e("LocalStore", "Transfer upload failed: $originalName", e)
            throw e
        }

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val entity = TransferFileEntity(
            id = id,
            originalName = originalName,
            storedFilename = storedFilename,
            size = diskFile.length(),
            uploaderIp = uploaderIp,
            uploaderUserAgent = uploaderUserAgent,
            downloadCount = 0,
            expiresAt = computeExpiresAt(now),
            createdAt = now
        )
        db().transferDao().insertFile(entity)

        logTransferAction(
            fileId = id,
            action = "UPLOAD",
            ip = uploaderIp,
            userAgent = uploaderUserAgent,
            authResult = true,
            clientRemark = null,
            result = "Success"
        )
        return toTransferFileResponse(entity, shareUrlBuilder)
    }

    suspend fun listTransferFiles(shareUrlBuilder: (String) -> String): List<TransferFileResponse> {
        val files = db().transferDao().getAllFiles()
        return files.map { toTransferFileResponse(it, shareUrlBuilder) }
    }

    suspend fun getTransferFile(id: String): TransferFileEntity? {
        return db().transferDao().getFileById(id)
    }

    fun getTransferDiskFile(file: TransferFileEntity): File {
        return File(getTransferDir(), file.storedFilename)
    }

    suspend fun deleteTransferFile(id: String, deleterIp: String?, deleterUserAgent: String?): Boolean {
        val file = db().transferDao().getFileById(id) ?: return false
        val disk = getTransferDiskFile(file)
        if (disk.exists()) disk.delete()
        db().transferDao().deleteFileById(id)
        logTransferAction(
            fileId = id,
            action = "DELETE",
            ip = deleterIp,
            userAgent = deleterUserAgent,
            authResult = true,
            clientRemark = null,
            result = "Success"
        )
        return true
    }

    suspend fun getTransferLogs(fileId: String): List<TransferLogResponse> {
        return db().transferDao().getLogsByFileId(fileId).map {
            TransferLogResponse(
                id = it.id,
                action = it.action,
                ip = it.ip,
                clientRemark = it.clientRemark,
                result = it.result,
                time = it.time
            )
        }
    }

    fun toTransferFileResponse(file: TransferFileEntity, shareUrlBuilder: (String) -> String): TransferFileResponse {
        val rd = remainingDays(file.expiresAt)
        return TransferFileResponse(
            id = file.id,
            originalName = file.originalName,
            size = file.size,
            downloadCount = file.downloadCount,
            expiresAt = file.expiresAt,
            createdAt = file.createdAt,
            uploaderIp = file.uploaderIp,
            shareUrl = shareUrlBuilder(file.id),
            remainingDays = rd
        )
    }

    suspend fun logTransferAction(
        fileId: String?,
        action: String,
        ip: String?,
        userAgent: String?,
        authResult: Boolean,
        clientRemark: String?,
        result: String?
    ) {
        db().transferDao().insertLog(
            TransferLogEntity(
                id = UUID.randomUUID().toString(),
                fileId = fileId,
                action = action,
                ip = ip,
                userAgent = userAgent,
                authResult = authResult,
                clientRemark = clientRemark,
                result = result,
                time = System.currentTimeMillis()
            )
        )
    }

    suspend fun getTransferIpQuota(ip: String): Pair<Int, Int> {
        val today = LocalDate.now(zoneId).format(dateFormatter)
        val quota = db().transferDao().getQuota(ip, today)
        val used = quota?.downloadCount ?: 0
        return used to (TRANSFER_DAILY_DOWNLOAD_LIMIT - used).coerceAtLeast(0)
    }

    suspend fun tryConsumeTransferQuota(ip: String): Boolean {
        val today = LocalDate.now(zoneId).format(dateFormatter)
        val current = db().transferDao().getQuota(ip, today)
        val used = current?.downloadCount ?: 0
        if (used >= TRANSFER_DAILY_DOWNLOAD_LIMIT) return false
        db().transferDao().updateQuota(
            TransferIpQuotaEntity(
                ip = ip,
                date = today,
                downloadCount = used + 1
            )
        )
        return true
    }

    suspend fun onTransferDownloaded(fileId: String, ip: String?, userAgent: String?, remark: String?) {
        db().transferDao().incrementDownloadCount(fileId)
        logTransferAction(
            fileId = fileId,
            action = "DOWNLOAD",
            ip = ip,
            userAgent = userAgent,
            authResult = true,
            clientRemark = remark,
            result = "Success"
        )
    }

    private suspend fun cleanupExpiredTransfers() {
        val now = System.currentTimeMillis()
        val expired = db().transferDao().getExpiredFiles(now)
        for (f in expired) {
            val disk = getTransferDiskFile(f)
            if (disk.exists()) disk.delete()
            db().transferDao().deleteFileById(f.id)
            logTransferAction(
                fileId = f.id,
                action = "EXPIRE_DELETE",
                ip = null,
                userAgent = null,
                authResult = true,
                clientRemark = null,
                result = "Auto cleanup"
            )
        }
        // 清理旧配额（只保留当天）
        val today = LocalDate.now(zoneId).format(dateFormatter)
        db().transferDao().deleteOldQuotas(today)
    }

    fun getUploadFile(filename: String): File {
        val normalized = filename.trim().removePrefix("/")
        val currentFile = File(getUploadDir(), normalized)
        if (currentFile.exists()) {
            return currentFile
        }

        val legacyFile = File(getLegacyUploadDir(), normalized.substringAfterLast('/'))
        return if (legacyFile.exists()) legacyFile else currentFile
    }

    fun getUpdateApkFile(): File {
        return File(appContext.filesDir, "update.apk")
    }

    fun getUpdateInfo(): UpdateResponse {
        val file = getUpdateApkFile()
        if (!file.exists()) {
            // Log.d("LocalStore", "No update.apk found at ${file.absolutePath}")
            return UpdateResponse(0, "", false)
        }
        
        return try {
            val pm = appContext.packageManager
            val info = pm.getPackageArchiveInfo(file.absolutePath, 0)
            if (info != null) {
                val versionCode = info.versionCode
                val versionName = info.versionName ?: "unknown"
                Log.d("LocalStore", "Found update APK: $versionName ($versionCode)")
                UpdateResponse(versionCode, versionName, true)
            } else {
                Log.e("LocalStore", "Failed to parse APK: getPackageArchiveInfo returned null")
                UpdateResponse(0, "", false)
            }
        } catch (e: Exception) {
            Log.e("LocalStore", "Failed to parse APK", e)
            UpdateResponse(0, "", false)
        }
    }

    suspend fun saveUpdateApk(part: PartData.FileItem): Boolean {
        val originalName = part.originalFileName ?: "unknown"
        Log.d("LocalStore", "Saving update APK: $originalName")
        return try {
            val file = getUpdateApkFile()
            if (file.exists()) {
                val deleted = file.delete()
                Log.d("LocalStore", "Deleted old update APK: $deleted")
            }
            part.streamProvider().use { input ->
                file.outputStream().use { output ->
                    val bytesCopied = input.copyTo(output)
                    Log.d("LocalStore", "Saved update APK: $bytesCopied bytes to ${file.absolutePath}")
                }
            }
            true
        } catch (e: Exception) {
            Log.e("LocalStore", "Failed to save update APK", e)
            false
        }
    }

    fun readWebAsset(path: String, fallbackToIndex: Boolean = false): Pair<ByteArray, ContentType> {
        if (WebAdminInitializer.hasAssets(appContext) && !WebAdminInitializer.isInitialized(appContext)) {
            WebAdminInitializer.copyAssetsToWebRoot(appContext)
        }

        val contentType = contentTypeForPath(path)
        val webRoot = WebAdminInitializer.getWebRootDir(appContext)
        val targetFile = File(webRoot, path)

        if (targetFile.exists() && targetFile.isFile) {
            return targetFile.readBytes() to contentType
        }

        val assetPath = "web-admin/$path"
        return try {
            val bytes = appContext.assets.open(assetPath).use { it.readBytes() }
            bytes to contentType
        } catch (_: Exception) {
            if (fallbackToIndex) {
                val indexFile = File(webRoot, "index.html")
                if (indexFile.exists()) {
                    return indexFile.readBytes() to ContentType.Text.Html
                }
                val bytes = appContext.assets.open("web-admin/index.html").use { it.readBytes() }
                bytes to ContentType.Text.Html
            } else {
                "<h1>Missing web-admin assets</h1>".toByteArray(Charsets.UTF_8) to ContentType.Text.Html
            }
        }
    }

    suspend fun saveUpload(part: PartData.FileItem): MediaResponse {
        val originalName = part.originalFileName ?: "file"
        Log.d("LocalStore", "Starting upload: $originalName")
        val fileSizeHint = part.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        val mime = part.contentType?.toString() ?: "application/octet-stream"
        val extension = extractExtension(originalName)
        val fileEasyValidation = if (ProductFlavorConfig.isFileEasy) {
            FileEasyUploadCore.validateUploadInit(
                fileName = originalName,
                fileSize = fileSizeHint ?: 0L,
                mimeType = mime
            )
        } else {
            null
        }
        val filename = if (extension.isNotBlank()) {
            "${UUID.randomUUID()}.$extension"
        } else {
            UUID.randomUUID().toString()
        }
        val category = when {
            fileEasyValidation != null -> fileEasyValidation.category
            getUploadCategory(extension) != null -> getUploadCategory(extension)!!
            mime.startsWith("video") -> "video"
            else -> "image"
        }
        val storedPath = buildStoredUploadPath(category, filename)
        val file = File(getUploadCategoryDir(category), filename)
        
        try {
            part.streamProvider().use { input ->
                file.outputStream().buffered().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        // 每 10MB 打印一次日志
                        if (totalRead % (10 * 1024 * 1024) < 8192) {
                            Log.d("LocalStore", "Uploading $originalName: ${totalRead / 1024 / 1024}MB")
                        }
                    }
                    output.flush()
                }
            }
            Log.d("LocalStore", "Upload finished: $originalName, total: ${file.length()} bytes")
        } catch (e: Exception) {
            Log.e("LocalStore", "Upload failed for $originalName", e)
            if (file.exists()) file.delete()
            throw e
        }

        if (ProductFlavorConfig.isFileEasy && file.length() > UPLOAD_MAX_FILE_BYTES) {
            if (file.exists()) file.delete()
            throw IllegalArgumentException("单文件不能超过 4GB")
        }

        val now = System.currentTimeMillis()
        val media = MediaEntity(
            id = UUID.randomUUID().toString(),
            originalName = originalName,
            displayName = originalName,
            filename = storedPath,
            url = "/uploads/$storedPath",
            type = category,
            extension = extension,
            mimeType = mime,
            category = category,
            previewSupported = isPreviewSupported(extension, category),
            size = file.length(),
            createdAt = now,
            updatedAt = now
        )
        db().mediaDao().insert(media)
        return media.toResponse()
    }

    suspend fun listMedia(): List<MediaResponse> {
        return db().mediaDao().getAll().map { it.toResponse() }
    }

    suspend fun deleteMedia(id: String): Boolean {
        val media = db().mediaDao().getById(id) ?: return false
        val file = getUploadFile(media.filename)
        if (file.exists()) {
            file.delete()
        }
        db().playlistDao().deleteItemsByMediaId(id)
        db().mediaDao().deleteById(id)
        return true
    }

    suspend fun listManagedFiles(): List<ManagedFileResponse> {
        return db().mediaDao().getAll().map { it.toManagedResponse() }
    }

    suspend fun getManagedFile(id: String): ManagedFileResponse? {
        return db().mediaDao().getById(id)?.toManagedResponse()
    }

    suspend fun renameManagedFile(id: String, baseName: String): ManagedFileResponse {
        val media = db().mediaDao().getById(id) ?: throw NoSuchElementException("文件不存在")
        val currentExtension = media.extension.ifBlank {
            extractExtension(media.displayName.ifBlank { media.originalName })
        }
        validateRenameBaseName(baseName, currentExtension)
        val finalDisplayName = resolveRenamedDisplayName(media, baseName)
        val updated = media.copy(
            displayName = finalDisplayName,
            updatedAt = System.currentTimeMillis()
        )
        db().mediaDao().insert(updated)
        return updated.toManagedResponse()
    }

    suspend fun deleteManagedFile(id: String): Boolean {
        return deleteMedia(id)
    }

    suspend fun batchDeleteManagedFiles(ids: List<String>): BatchDeleteFilesResponse {
        val deletedIds = mutableListOf<String>()
        ids.distinct().forEach { id ->
            if (deleteManagedFile(id)) {
                deletedIds += id
            }
        }
        return BatchDeleteFilesResponse(
            deletedIds = deletedIds,
            deletedCount = deletedIds.size
        )
    }

    suspend fun getManagedFilePreview(id: String): ManagedFileBinaryResponse? {
        val media = db().mediaDao().getById(id) ?: return null
        if (!media.previewSupported) {
            throw IllegalArgumentException("当前类型不支持在线预览")
        }
        val file = getUploadFile(media.filename)
        if (!file.exists()) {
            return null
        }
        return ManagedFileBinaryResponse(
            file = file,
            contentType = media.toContentType(),
            displayName = media.displayName.ifBlank { media.originalName }
        )
    }

    suspend fun getManagedFileDownload(id: String): ManagedFileBinaryResponse? {
        val media = db().mediaDao().getById(id) ?: return null
        val file = getUploadFile(media.filename)
        if (!file.exists()) {
            return null
        }
        return ManagedFileBinaryResponse(
            file = file,
            contentType = media.toContentType(),
            displayName = media.displayName.ifBlank { media.originalName }
        )
    }

    suspend fun createPlaylist(request: CreatePlaylistRequest): PlaylistResponse {
        val playlistId = UUID.randomUUID().toString()
        val playlist = PlaylistEntity(
            id = playlistId,
            name = request.name,
            description = request.description,
            startTime = request.startTime ?: "08:00:00",
            endTime = request.endTime ?: "22:00:22",
            daysOfWeek = request.daysOfWeek ?: "1,2,3,4,5,6,7",
            createdAt = System.currentTimeMillis()
        )
        db().playlistDao().insertPlaylist(playlist)
        db().playlistDao().deleteItemsForPlaylist(playlistId)

        val items = request.items.map {
            PlaylistItemEntity(
                id = UUID.randomUUID().toString(),
                playlistId = playlistId,
                mediaId = it.mediaId,
                orderIndex = it.order,
                duration = it.duration
            )
        }
        db().playlistDao().insertItems(items)
        return requireNotNull(getPlaylist(playlistId))
    }

    suspend fun updatePlaylist(id: String, request: CreatePlaylistRequest): PlaylistResponse {
        val existing = db().playlistDao().getById(id) ?: throw Exception("Playlist not found")
        val updated = existing.copy(
            name = request.name,
            description = request.description,
            startTime = request.startTime ?: "08:00:00",
            endTime = request.endTime ?: "22:00:22",
            daysOfWeek = request.daysOfWeek ?: "1,2,3,4,5,6,7"
        )
        db().playlistDao().insertPlaylist(updated) // insert with same ID will replace (if DAO uses OnConflictStrategy.REPLACE)
        
        db().playlistDao().deleteItemsForPlaylist(id)
        val items = request.items.map {
            PlaylistItemEntity(
                id = UUID.randomUUID().toString(),
                playlistId = id,
                mediaId = it.mediaId,
                orderIndex = it.order,
                duration = it.duration
            )
        }
        db().playlistDao().insertItems(items)
        return requireNotNull(getPlaylist(id))
    }

    suspend fun deletePlaylist(id: String): Boolean {
        Log.d("LocalStore", "Deleting playlist: $id")
        db().playlistDao().deleteItemsForPlaylist(id)
        db().deviceDao().deletePlaylistRefs(id) // ✅ 改回正确的方法名
        db().playlistDao().deletePlaylist(id)
        return true
    }

    suspend fun listPlaylists(): List<PlaylistResponse> {
        return db().playlistDao().getAllWithItems().map { it.toResponse() }
    }

    suspend fun getPlaylist(id: String): PlaylistResponse? {
        return db().playlistDao().getByIdWithItems(id)?.toResponse()
    }

    suspend fun registerDevice(request: RegisterRequest): DeviceResponse {
        val existing = db().deviceDao().findBySerial(request.serialNumber)
        val now = System.currentTimeMillis()
        val device = if (existing == null) {
            val newId = UUID.randomUUID().toString()
            DeviceEntity(
                id = newId,
                serialNumber = request.serialNumber,
                name = request.name ?: "Device-${request.serialNumber.take(6)}",
                status = "online",
                lastHeartbeat = now,
                ipAddress = request.ipAddress,
                version = request.version,
                createdAt = now
            )
        } else {
            existing.copy(
                name = request.name ?: existing.name,
                status = "online",
                lastHeartbeat = now,
                ipAddress = request.ipAddress ?: existing.ipAddress,
                version = request.version ?: existing.version
            )
        }
        db().deviceDao().insert(device)

        // 默认播放列表逻辑：如果该设备没有任何播放列表，则自动分配最近创建的一个
        val currentPlaylists = db().deviceDao().getPlaylistIdsForDevice(device.id)
        if (currentPlaylists.isEmpty()) {
            val allPlaylists = db().playlistDao().getAllWithItems()
            if (allPlaylists.isNotEmpty()) {
                val latestPlaylist = allPlaylists.first().playlist // 数据库查询已按 createdAt DESC 排序
                db().deviceDao().addDevicePlaylists(listOf(
                    DevicePlaylistCrossRef(deviceId = device.id, playlistId = latestPlaylist.id)
                ))
                Log.d("LocalStore", "Auto-assigned latest playlist ${latestPlaylist.name} to new device ${device.name}")
            }
        }

        // 返回包含播放列表信息的响应
        val withPlaylists = db().deviceDao().getAllWithPlaylists().find { it.device.id == device.id }
        return device.toResponse(withPlaylists?.playlists?.map { it.toResponseMinimal() } ?: emptyList())
    }

    suspend fun heartbeat(id: String): HeartbeatResponse? {
        val device = db().deviceDao().getById(id) ?: return null
        val updated = device.copy(
            status = "online",
            lastHeartbeat = System.currentTimeMillis()
        )
        db().deviceDao().insert(updated)
        
        // 获取所有关联的播放列表详情，进行时间过滤
        val playlistsWithItems = db().deviceDao().getAllWithPlaylists().find { it.device.id == id }?.playlists ?: emptyList()
        
        // ✅ 关键修复：确保清单 ID 在 playlists 表中确实存在 (Room 联查已经保证了这一点，但为了双重保险，如果 lists 依然为空，尝试重置)
        if (playlistsWithItems.isEmpty()) {
            val allPlaylists = db().playlistDao().getAllWithItems()
            if (allPlaylists.isNotEmpty()) {
                val latestPlaylist = allPlaylists.first().playlist
                db().deviceDao().addDevicePlaylists(listOf(
                    DevicePlaylistCrossRef(deviceId = id, playlistId = latestPlaylist.id)
                ))
                Log.d("LocalStore", "Re-assigned latest playlist ${latestPlaylist.name} to device $id due to empty assignment")
            }
        }
        
        val now = java.util.Calendar.getInstance()
        val currentDay = when (now.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            java.util.Calendar.SUNDAY -> 7
            else -> 1
        }
        val currentTime = String.format("%02d:%02d:%02d", 
            now.get(java.util.Calendar.HOUR_OF_DAY), 
            now.get(java.util.Calendar.MINUTE), 
            now.get(java.util.Calendar.SECOND))

        val activePlaylistIds = playlistsWithItems.filter { p ->
            // 如果没有设置时间段，则认为始终有效
            if (p.startTime.isNullOrBlank() || p.endTime.isNullOrBlank()) return@filter true

            // 检查星期
            val days = p.daysOfWeek?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: listOf(1, 2, 3, 4, 5, 6, 7)
            if (!days.contains(currentDay)) return@filter false

            // 检查时间范围 (HH:mm:ss 格式可以直接字符串比较)
            currentTime >= p.startTime && currentTime <= p.endTime
        }.map { it.id }

        val updateInfo = getUpdateInfo().let { if (it.hasUpdate) it else null }
        if (updateInfo != null) {
            Log.d("LocalStore", "Sending update info in heartbeat: ${updateInfo.versionName} (${updateInfo.versionCode})")
        }
        return HeartbeatResponse(
            status = "ok",
            playlistIds = activePlaylistIds,
            updateInfo = updateInfo
        )
    }

    suspend fun listDevices(): List<DeviceResponse> {
        val now = System.currentTimeMillis()
        return db().deviceDao().getAllWithPlaylists().map { withPlaylists ->
            val online = withPlaylists.device.lastHeartbeat?.let { now - it < 60_000 } ?: false
            withPlaylists.device.copy(status = if (online) "online" else "offline")
                .toResponse(withPlaylists.playlists.map { it.toResponseMinimal() })
        }
    }

    suspend fun assignPlaylists(deviceId: String, playlistIds: List<String>) {
        val device = db().deviceDao().getById(deviceId) ?: return
        db().deviceDao().clearDevicePlaylists(deviceId)
        val refs = playlistIds.map { DevicePlaylistCrossRef(deviceId = deviceId, playlistId = it) }
        db().deviceDao().addDevicePlaylists(refs)
        db().deviceDao().insert(device.copy(lastHeartbeat = device.lastHeartbeat))
    }

    private fun MediaEntity.toResponse(): MediaResponse {
        return MediaResponse(
            id = id,
            originalName = displayName.ifBlank { originalName },
            filename = filename,
            url = url,
            type = type,
            size = size,
            createdAt = formatter.format(Instant.ofEpochMilli(createdAt))
        )
    }

    private fun MediaEntity.toManagedResponse(): ManagedFileResponse {
        return ManagedFileResponse(
            id = id,
            originalName = originalName,
            displayName = displayName.ifBlank { originalName },
            storedFilename = filename,
            filename = filename,
            url = url,
            type = type,
            category = category,
            extension = extension,
            mimeType = mimeType,
            previewSupported = previewSupported,
            size = size,
            createdAt = formatter.format(Instant.ofEpochMilli(createdAt)),
            updatedAt = formatter.format(Instant.ofEpochMilli(updatedAt.takeIf { it > 0L } ?: createdAt))
        )
    }

    private fun MediaEntity.toContentType(): ContentType {
        return when (extension.lowercase()) {
            "pdf" -> ContentType.Application.Pdf
            "jpg", "jpeg" -> ContentType.Image.JPEG
            "png" -> ContentType.Image.PNG
            "gif" -> ContentType.Image.GIF
            "webp" -> ContentType("image", "webp")
            "mp4" -> ContentType.Video.MP4
            "mov" -> ContentType.Video.QuickTime
            "mp3" -> ContentType("audio", "mpeg")
            "wav" -> ContentType("audio", "wav")
            "m4a" -> ContentType("audio", "mp4")
            else -> runCatching { ContentType.parse(mimeType) }.getOrDefault(ContentType.Application.OctetStream)
        }
    }

    private fun PlaylistWithItems.toResponse(): PlaylistResponse {
        return PlaylistResponse(
            id = playlist.id,
            name = playlist.name,
            description = playlist.description ?: "",
            startTime = playlist.startTime,
            endTime = playlist.endTime,
            daysOfWeek = playlist.daysOfWeek,
            items = items.sortedBy { it.item.orderIndex }.map { it.toResponse() },
            createdAt = formatter.format(Instant.ofEpochMilli(playlist.createdAt))
        )
    }

    private fun PlaylistEntity.toResponseMinimal(): PlaylistResponse {
        return PlaylistResponse(
            id = id,
            name = name,
            description = description ?: "",
            startTime = startTime,
            endTime = endTime,
            daysOfWeek = daysOfWeek,
            items = emptyList(),
            createdAt = formatter.format(Instant.ofEpochMilli(createdAt))
        )
    }

    private fun PlaylistItemWithMedia.toResponse(): PlaylistItemResponse {
        return PlaylistItemResponse(
            id = item.id,
            order = item.orderIndex,
            duration = item.duration,
            media = media.toResponse()
        )
    }

    private fun DeviceEntity.toResponse(playlists: List<PlaylistResponse>): DeviceResponse {
        return DeviceResponse(
            id = id,
            serialNumber = serialNumber,
            name = name,
            status = status,
            lastHeartbeat = lastHeartbeat?.let { formatter.format(Instant.ofEpochMilli(it)) },
            ipAddress = ipAddress,
            version = version,
            createdAt = formatter.format(Instant.ofEpochMilli(createdAt)),
            playlists = playlists
        )
    }

    private fun contentTypeForPath(path: String): ContentType {
        return when {
            path.endsWith(".html") -> ContentType.Text.Html
            path.endsWith(".js") -> ContentType.Application.JavaScript
            path.endsWith(".css") -> ContentType.Text.CSS
            path.endsWith(".json") -> ContentType.Application.Json
            path.endsWith(".svg") -> ContentType.Image.SVG
            path.endsWith(".png") -> ContentType.Image.PNG
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> ContentType.Image.JPEG
            path.endsWith(".gif") -> ContentType.Image.GIF
            path.endsWith(".webp") -> ContentType.parse("image/webp")
            path.endsWith(".woff") -> ContentType.parse("font/woff")
            path.endsWith(".woff2") -> ContentType.parse("font/woff2")
            else -> ContentType.Application.OctetStream
        }
    }
    
    /**
     * 获取系统监控数据
     */
    suspend fun getSystemMonitor(): SystemMonitorResponse {
        requestCounter.incrementAndGet()
        
        // 获取内存信息
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        
        // 获取设备统计
        val allDevices = db().deviceDao().getAll()
        val now = System.currentTimeMillis()
        val onlineDevices = allDevices.count { (it.lastHeartbeat ?: 0L) > (now - 60_000) }
        
        // 获取存储信息
        val allMedia = db().mediaDao().getAll()
        val uploadsDir = getUploadDir()
        val uploadFiles = uploadsDir.listFiles() ?: emptyArray()
        val totalUploadSize = uploadFiles.sumOf { it.length() } / 1024 / 1024

        // 文件中转目录占用
        val transferDir = getTransferDir()
        val transferFiles = transferDir.listFiles() ?: emptyArray()
        val totalTransferSize = transferFiles.sumOf { it.length() } / 1024 / 1024
        
        // 获取存储空间
        val stat = android.os.StatFs(uploadsDir.path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val freeSpaceMB = bytesAvailable / 1024 / 1024
        
        // 运行时长（秒）
        val uptime = (System.currentTimeMillis() - startTime) / 1000
        
        return SystemMonitorResponse(
            timestamp = System.currentTimeMillis(),
            memory = MemoryInfoResponse(
                used = usedMemory,
                free = freeMemory,
                total = totalMemory,
                max = maxMemory
            ),
            devices = DevicesInfoResponse(
                total = allDevices.size,
                online = onlineDevices,
                offline = allDevices.size - onlineDevices
            ),
            storage = StorageInfoResponse(
                mediaCount = allMedia.size,
                uploadsMB = totalUploadSize,
                freeMB = freeSpaceMB,
                fileCount = uploadFiles.size,
                transferMB = totalTransferSize,
                transferFileCount = transferFiles.size
            ),
            server = ServerInfoResponse(
                uptime = uptime,
                requestCount = requestCounter.get()
            )
        )
    }
}

@Serializable
data class MediaResponse(
    val id: String,
    val originalName: String,
    val filename: String,
    val url: String,
    val type: String,
    val size: Long,
    val createdAt: String
)

@Serializable
data class ManagedFileResponse(
    val id: String,
    val originalName: String,
    val displayName: String,
    val storedFilename: String,
    val filename: String,
    val url: String,
    val type: String,
    val category: String,
    val extension: String,
    val mimeType: String,
    val previewSupported: Boolean,
    val size: Long,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class RenameFileRequest(
    val baseName: String
)

@Serializable
data class BatchDeleteFilesRequest(
    val ids: List<String>
)

@Serializable
data class BatchDeleteFilesResponse(
    val deletedIds: List<String>,
    val deletedCount: Int
)

data class ManagedFileBinaryResponse(
    val file: File,
    val contentType: ContentType,
    val displayName: String
)

@Serializable
data class PlaylistItemResponse(
    val id: String,
    val order: Int,
    val duration: Int,
    val media: MediaResponse
)

@Serializable
data class PlaylistResponse(
    val id: String,
    val name: String,
    val description: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val daysOfWeek: String? = null,
    val items: List<PlaylistItemResponse>,
    val createdAt: String
)

@Serializable
data class CreatePlaylistRequest(
    val name: String,
    val description: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val daysOfWeek: String? = null,
    val items: List<CreatePlaylistItem>
)

@Serializable
data class CreatePlaylistItem(
    val mediaId: String,
    val duration: Int,
    val order: Int
)

@Serializable
data class AssignPlaylistsRequest(
    val playlistIds: List<String>
)

@Serializable
data class DeviceResponse(
    val id: String,
    val serialNumber: String,
    val name: String,
    val status: String,
    val lastHeartbeat: String?,
    val ipAddress: String?,
    val version: String?,
    val createdAt: String,
    val playlists: List<PlaylistResponse> = emptyList()
)

// ✅ 监控API响应数据结构
@Serializable
data class SystemMonitorResponse(
    val timestamp: Long,
    val memory: MemoryInfoResponse,
    val devices: DevicesInfoResponse,
    val storage: StorageInfoResponse,
    val server: ServerInfoResponse
)

@Serializable
data class MemoryInfoResponse(
    val used: Long,
    val free: Long,
    val total: Long,
    val max: Long
)

@Serializable
data class DevicesInfoResponse(
    val total: Int,
    val online: Int,
    val offline: Int
)

@Serializable
data class StorageInfoResponse(
    val mediaCount: Int,
    val uploadsMB: Long,
    val freeMB: Long,
    val fileCount: Int,
    // 文件中转（transfer_files）统计
    val transferMB: Long = 0,
    val transferFileCount: Int = 0
)

@Serializable
data class ServerInfoResponse(
    val uptime: Long,
    val requestCount: Long
)

@Serializable
data class HomeSummaryResponse(
    val uploadUrl: String,
    val passwordRequired: Boolean,
    val activeUploads: List<HomeUploadTaskResponse>,
    val recentFiles: List<HomeRecentFileResponse>
)

@Serializable
data class HomeUploadTaskResponse(
    val uploadId: String,
    val fileName: String,
    val uploadedChunks: Int,
    val totalChunks: Int,
    val status: String,
    val progress: Int,
    val fileSize: Long,
    val uploadedBytes: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class HomeRecentFileResponse(
    val id: String,
    val fileName: String,
    val category: String,
    val size: Long,
    val createdAt: Long,
    val relativeDirectory: String? = null
)

// --- Transfer Data Classes ---
@Serializable
data class TransferFileResponse(
    val id: String,
    val originalName: String,
    val size: Long,
    val downloadCount: Int,
    val expiresAt: Long,
    val createdAt: Long,
    val uploaderIp: String? = null,
    val shareUrl: String? = null,
    val remainingDays: Int
)

@Serializable
data class TransferLogResponse(
    val id: String,
    val action: String,
    val ip: String?,
    val clientRemark: String?,
    val result: String?,
    val time: Long
)

@Serializable
data class TransferAuthRequest(
    val username: String,
    val password: String
)

@Serializable
data class TransferAuthResponse(
    val token: String
)

@Serializable
data class TransferStorageResponse(
    val usedMB: Long,
    val freeMB: Long,
    val warn: Boolean,
    val blocked: Boolean
)
