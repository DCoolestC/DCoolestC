package com.isokovibe.musicplayer.data

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long> = emptyList()
)
