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

/**
 * Persisted form of the audio effect settings. Kept separate from the
 * playback layer's own settings type so the data layer doesn't depend on
 * the platform audio APIs, and so adding a field here can't silently change
 * what gets pushed to the hardware.
 */
@Serializable
data class StoredAudioEffects(
    val equalizerEnabled: Boolean = false,
    val bandLevels: List<Int> = emptyList(),
    val presetIndex: Int = -1,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val reverbPreset: Int = 0,
    val loudnessGain: Int = 0,
    val skipSilence: Boolean = false
)
