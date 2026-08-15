package com.isokovibe.musicplayer.data

/**
 * Playlists the app derives on the fly instead of the user curating them.
 * Nothing is stored — each is recomputed from the library plus the play
 * stats already being recorded, so they stay correct without maintenance.
 */
enum class SmartPlaylist(val label: String, val description: String) {
    FAVORITES("Favorites", "Everything you've hearted"),
    RECENTLY_ADDED("Recently added", "Landed on your device in the last 30 days"),
    RECENTLY_PLAYED("Recently played", "The last 50 you listened to"),
    MOST_PLAYED("Most played", "Your top 50 by play count"),
    NEVER_PLAYED("Never played", "Still waiting to be heard")
}

private const val SMART_LIST_LIMIT = 50
private const val THIRTY_DAYS_SECONDS = 30L * 24 * 60 * 60

/**
 * Resolves [kind] against the current library. Ordering is part of each
 * list's meaning — "most played" descends by count, "recently added"
 * by date — so callers should play these in the order returned.
 */
fun resolveSmartPlaylist(
    kind: SmartPlaylist,
    songs: List<Song>,
    playCounts: Map<Long, Int>,
    lastPlayed: Map<Long, Long>,
    favorites: Set<Long>
): List<Song> = when (kind) {
    SmartPlaylist.FAVORITES ->
        songs.filter { it.id in favorites }.sortedBy { it.title.lowercase() }

    SmartPlaylist.RECENTLY_ADDED -> {
        val cutoff = (System.currentTimeMillis() / 1000) - THIRTY_DAYS_SECONDS
        songs.filter { it.dateAddedEpochSec > cutoff }
            .sortedByDescending { it.dateAddedEpochSec }
    }

    SmartPlaylist.RECENTLY_PLAYED ->
        songs.filter { (lastPlayed[it.id] ?: 0L) > 0L }
            .sortedByDescending { lastPlayed[it.id] ?: 0L }
            .take(SMART_LIST_LIMIT)

    SmartPlaylist.MOST_PLAYED ->
        songs.filter { (playCounts[it.id] ?: 0) > 0 }
            .sortedByDescending { playCounts[it.id] ?: 0 }
            .take(SMART_LIST_LIMIT)

    SmartPlaylist.NEVER_PLAYED ->
        songs.filter { (playCounts[it.id] ?: 0) == 0 }
            .sortedBy { it.title.lowercase() }
}

/** Two or more library entries that look like the same recording. */
data class DuplicateGroup(
    val title: String,
    val artist: String,
    val songs: List<Song>
)

/**
 * Finds probable duplicates by normalized title + artist.
 *
 * Duration is deliberately *not* part of the key. Re-downloads of the same
 * track routinely differ by a second or two of encoder padding, so keying
 * on it would split exactly the pairs this is meant to catch. Each copy's
 * duration and folder are surfaced in the UI instead, so the judgement call
 * stays with the user rather than being hidden in a heuristic.
 */
fun List<Song>.findDuplicates(): List<DuplicateGroup> =
    groupBy { normalizeForMatch(it.title) to normalizeForMatch(it.artist) }
        .values
        .filter { it.size > 1 }
        .map { copies ->
            DuplicateGroup(
                title = copies.first().title,
                artist = copies.first().artist,
                songs = copies.sortedBy { it.folderPath }
            )
        }
        .sortedBy { it.title.lowercase() }

/** Lowercases and strips punctuation/whitespace so "Song (1).mp3" style
 *  re-downloads still match their original. */
private fun normalizeForMatch(value: String): String =
    value.lowercase().filter { it.isLetterOrDigit() }
