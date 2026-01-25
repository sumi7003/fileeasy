package com.xplay.player.data.api

import com.xplay.player.data.model.Device
import com.xplay.player.data.model.Playlist
import com.xplay.player.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface XplayApi {
    @POST("devices/register")
    suspend fun register(@Body request: RegisterRequest): Response<Device>

    @PUT("devices/{id}/heartbeat")
    suspend fun heartbeat(@Path("id") id: String): Response<HeartbeatResponse>

    @GET("playlists/{id}")
    suspend fun getPlaylist(@Path("id") id: String): Response<Playlist>
}

data class HeartbeatResponse(
    val status: String,
    val playlistIds: List<String>,
    val updateInfo: UpdateInfo? = null
)

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val hasUpdate: Boolean
)
