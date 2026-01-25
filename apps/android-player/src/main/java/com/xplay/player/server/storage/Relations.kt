package com.xplay.player.server.storage

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlaylistItemWithMedia(
    @Embedded val item: PlaylistItemEntity,
    @Relation(
        parentColumn = "mediaId",
        entityColumn = "id"
    )
    val media: MediaEntity
)

data class PlaylistWithItems(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        entity = PlaylistItemEntity::class,
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val items: List<PlaylistItemWithMedia>
)

data class DeviceWithPlaylists(
    @Embedded val device: DeviceEntity,
    @Relation(
        entity = PlaylistEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = DevicePlaylistCrossRef::class,
            parentColumn = "deviceId",
            entityColumn = "playlistId"
        )
    )
    val playlists: List<PlaylistEntity>
)
