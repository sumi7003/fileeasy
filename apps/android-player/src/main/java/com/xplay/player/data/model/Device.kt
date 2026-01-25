package com.xplay.player.data.model

data class Device(
    val id: String,
    val serialNumber: String,
    val name: String,
    val status: String,
    val lastHeartbeat: String?,
    val playlists: List<Playlist> = emptyList()
)

data class RegisterRequest(
    val serialNumber: String,
    val name: String? = null,
    val version: String? = null,
    val ipAddress: String? = null
)
