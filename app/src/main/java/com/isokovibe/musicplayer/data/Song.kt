package com.isokovibe.musicplayer.data

import android.net.Uri

/**
 * A single track read from the device's MediaStore.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?
)
