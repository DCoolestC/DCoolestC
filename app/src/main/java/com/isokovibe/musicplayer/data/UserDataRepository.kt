package com.isokovibe.musicplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.userDataStore by preferencesDataStore(name = "isokovibe_user_data")

/**
 * Everything the app remembers locally between launches: theme choice,
 * favorites, and playlists. No account, no network — just DataStore.
 */
class UserDataRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FAVORITES = stringPreferencesKey("favorites")
        val PLAYLISTS = stringPreferencesKey("playlists")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val themeMode: Flow<ThemeMode> = context.userDataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() }
            ?: ThemeMode.DARK
    }

    val favorites: Flow<Set<Long>> = context.userDataStore.data.map { prefs ->
        prefs[Keys.FAVORITES]?.let { raw ->
            runCatching { json.decodeFromString<List<Long>>(raw).toSet() }.getOrNull()
        } ?: emptySet()
    }

    val playlists: Flow<List<Playlist>> = context.userDataStore.data.map { prefs ->
        prefs[Keys.PLAYLISTS]?.let { raw ->
            runCatching { json.decodeFromString<List<Playlist>>(raw) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.userDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun toggleFavorite(songId: Long) {
        context.userDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES]?.let {
                runCatching { json.decodeFromString<List<Long>>(it).toMutableSet() }.getOrNull()
            } ?: mutableSetOf()
            if (!current.add(songId)) current.remove(songId)
            prefs[Keys.FAVORITES] = json.encodeToString(current.toList())
        }
    }

    suspend fun createPlaylist(name: String): Playlist {
        val newPlaylist = Playlist(id = UUID.randomUUID().toString(), name = name)
        updatePlaylists { it + newPlaylist }
        return newPlaylist
    }

    suspend fun deletePlaylist(id: String) {
        updatePlaylists { list -> list.filterNot { it.id == id } }
    }

    suspend fun renamePlaylist(id: String, newName: String) {
        updatePlaylists { list -> list.map { if (it.id == id) it.copy(name = newName) else it } }
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: Long) {
        updatePlaylists { list ->
            list.map {
                if (it.id == playlistId && songId !in it.songIds) it.copy(songIds = it.songIds + songId) else it
            }
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        updatePlaylists { list ->
            list.map {
                if (it.id == playlistId) it.copy(songIds = it.songIds.filterNot { id -> id == songId }) else it
            }
        }
    }

    private suspend fun updatePlaylists(transform: (List<Playlist>) -> List<Playlist>) {
        context.userDataStore.edit { prefs ->
            val current = prefs[Keys.PLAYLISTS]?.let {
                runCatching { json.decodeFromString<List<Playlist>>(it) }.getOrNull()
            } ?: emptyList()
            prefs[Keys.PLAYLISTS] = json.encodeToString(transform(current))
        }
    }
}
