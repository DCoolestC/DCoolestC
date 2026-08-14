package com.isokovibe.musicplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isokovibe.musicplayer.data.MusicRepository
import com.isokovibe.musicplayer.data.Song
import com.isokovibe.musicplayer.playback.PlaybackController
import com.isokovibe.musicplayer.playback.PlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface LibraryState {
    data object Loading : LibraryState
    data object PermissionRequired : LibraryState
    data object Empty : LibraryState
    data class Loaded(val songs: List<Song>) : LibraryState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val playback = PlaybackController(application, viewModelScope)

    private val _libraryState = MutableStateFlow<LibraryState>(LibraryState.Loading)
    val libraryState: StateFlow<LibraryState> = _libraryState

    val playbackState: StateFlow<PlaybackUiState> = playback.uiState

    private var library: List<Song> = emptyList()

    init {
        playback.connect()
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            _libraryState.value = LibraryState.Loading
            library = repository.loadLibrary()
            _libraryState.value = if (library.isEmpty()) LibraryState.Empty else LibraryState.Loaded(library)
        }
    }

    fun onPermissionDenied() {
        _libraryState.value = LibraryState.PermissionRequired
    }

    fun playSong(song: Song) {
        val index = library.indexOf(song).coerceAtLeast(0)
        playback.playQueue(library, index)
    }

    fun togglePlayPause() = playback.togglePlayPause()
    fun skipToNext() = playback.skipToNext()
    fun skipToPrevious() = playback.skipToPrevious()
    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)
    fun toggleShuffle() = playback.toggleShuffle()
    fun cycleRepeatMode() = playback.cycleRepeatMode()

    fun songById(id: Long?): Song? = library.firstOrNull { it.id == id }

    override fun onCleared() {
        playback.release()
        super.onCleared()
    }
}
