package com.xplay.player.data.model

data class Playlist(
    val id: String,
    val name: String,
    val items: List<PlaylistItem>
)

data class PlaylistItem(
    val id: String,
    val order: Int,
    val duration: Int,
    val media: Media
)

data class Media(
    val id: String,
    val url: String,
    val type: String, // 'image' | 'video'
    val originalName: String
)

