package com.isokovibe.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.isokovibe.musicplayer.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val currentSongId: Long? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    @Player.RepeatMode val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1f,
    val sleepTimerRemainingMs: Long? = null,
    /** Position in the queue, so the Up Next sheet can tell apart two
     *  entries of the same track rather than highlighting both. */
    val currentIndex: Int = 0
)

/**
 * Thin wrapper around a [MediaController] connected to [MusicService].
 * The UI layer only ever talks to this class, never to ExoPlayer directly.
 */
class PlaybackController(private val context: Context, private val scope: CoroutineScope) {

    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState

    // The songs currently loaded as the playback queue, in order — powers
    // the Now Playing "Up next" sheet. Not derived from the controller
    // itself (Media3's MediaItems don't carry the full Song back out), just
    // snapshotted whenever a new queue is loaded.
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionUpdates() else positionJob?.cancel()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _uiState.update {
                it.copy(
                    currentSongId = mediaItem?.mediaId?.toLongOrNull(),
                    durationMs = controller?.duration?.coerceAtLeast(0) ?: 0L,
                    currentIndex = controller?.currentMediaItemIndex ?: 0
                )
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.update { it.copy(repeatMode = repeatMode) }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _uiState.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }
    }

    fun connect(onReady: () -> Unit = {}) {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener(
            {
                controller = future.get().also { it.addListener(playerListener) }
                onReady()
            },
            MoreExecutors.directExecutor()
        )
    }

    fun release() {
        positionJob?.cancel()
        sleepTimerJob?.cancel()
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
    }

    /** Loads [songs] as the playback queue and starts playing at [startIndex]. */
    fun playQueue(songs: List<Song>, startIndex: Int) {
        _queue.value = songs
        val items = songs.map(::toMediaItem)
        controller?.apply {
            setMediaItems(items, startIndex, 0L)
            prepare()
            play()
        }
    }

    /** Jumps straight to [index] within the currently loaded queue — what
     *  tapping a song in the "Up next" sheet does. */
    fun playFromQueue(index: Int) {
        controller?.seekTo(index, 0L)
    }

    // The queue mutations below each update ExoPlayer *and* the _queue
    // mirror. Media3's MediaItems don't carry the full Song back out, so the
    // mirror is the only place the UI can read titles/art from — letting the
    // two drift would leave the Up Next sheet showing a stale order.

    /** Appends to the end of the queue. */
    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val c = controller ?: return
        c.addMediaItems(songs.map(::toMediaItem))
        _queue.update { it + songs }
    }

    /** Inserts directly after whatever is playing. */
    fun playNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        val c = controller ?: return
        val insertAt = (c.currentMediaItemIndex + 1).coerceIn(0, c.mediaItemCount)
        c.addMediaItems(insertAt, songs.map(::toMediaItem))
        _queue.update { current ->
            current.toMutableList().apply { addAll(insertAt.coerceIn(0, size), songs) }
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        val size = _queue.value.size
        if (from == to || from !in 0 until size || to !in 0 until size) return
        c.moveMediaItem(from, to)
        _queue.update { current ->
            current.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index !in 0 until _queue.value.size) return
        c.removeMediaItem(index)
        _queue.update { current ->
            current.toMutableList().apply { removeAt(index) }
        }
    }

    fun togglePlayPause() {
        controller?.apply { if (isPlaying) pause() else play() }
    }

    fun skipToNext() = controller?.seekToNextMediaItem()

    fun skipToPrevious() = controller?.seekToPreviousMediaItem()

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _uiState.update { it.copy(positionMs = positionMs) }
    }

    fun toggleShuffle() {
        controller?.apply { shuffleModeEnabled = !shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        val next = when (controller?.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller?.repeatMode = next
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    /** Pauses playback after [minutes]. Pass 0 or less to cancel an active timer. */
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.update { it.copy(sleepTimerRemainingMs = null) }
            return
        }
        val endAtMs = System.currentTimeMillis() + minutes * 60_000L
        sleepTimerJob = scope.launch {
            while (isActive) {
                val remaining = endAtMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    controller?.pause()
                    _uiState.update { it.copy(sleepTimerRemainingMs = null) }
                    break
                }
                _uiState.update { it.copy(sleepTimerRemainingMs = remaining) }
                delay(1000)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(sleepTimerRemainingMs = null) }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                val c = controller ?: break
                _uiState.update {
                    it.copy(
                        positionMs = c.currentPosition.coerceAtLeast(0),
                        durationMs = c.duration.coerceAtLeast(0),
                        currentIndex = c.currentMediaItemIndex
                    )
                }
                delay(500)
            }
        }
    }

    private fun toMediaItem(song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.albumArtUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.contentUri)
            .setMediaMetadata(metadata)
            .build()
    }
}
