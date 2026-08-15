package com.isokovibe.musicplayer.data

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long> = emptyList()
)

/**
 * A snapshot of the playback queue, persisted so closing the app doesn't
 * lose your place. Stores song *ids* rather than whole Songs — the library
 * is rescanned on launch anyway, and ids let stale entries (deleted files)
 * simply drop out of the restored queue instead of resurrecting ghosts.
 */
@Serializable
data class SavedQueue(
    val songIds: List<Long> = emptyList(),
    val index: Int = 0,
    val positionMs: Long = 0L
)
