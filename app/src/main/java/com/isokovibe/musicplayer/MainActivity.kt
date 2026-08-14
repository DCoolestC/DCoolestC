package com.isokovibe.musicplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.isokovibe.musicplayer.ui.LibraryScreen
import com.isokovibe.musicplayer.ui.NowPlayingScreen
import com.isokovibe.musicplayer.ui.components.MiniPlayer
import com.isokovibe.musicplayer.ui.theme.IsokoVibeTheme

private const val ROUTE_LIBRARY = "library"
private const val ROUTE_NOW_PLAYING = "now_playing"

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IsokoVibeTheme {
                Surface {
                    IsokoVibeApp(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun IsokoVibeApp(viewModel: MainViewModel) {
    val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(audioPermission)

    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    val libraryState by viewModel.libraryState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentSong = viewModel.songById(playbackState.currentSongId)
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentSong != null && currentRoute != ROUTE_NOW_PLAYING) {
                MiniPlayer(
                    song = currentSong,
                    isPlaying = playbackState.isPlaying,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSkipNext = viewModel::skipToNext,
                    onOpenNowPlaying = { navController.navigate(ROUTE_NOW_PLAYING) },
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = ROUTE_LIBRARY) {
            composable(ROUTE_LIBRARY) {
                LibraryScreen(
                    state = libraryState,
                    onSongClick = viewModel::playSong,
                    onRequestPermission = { permissionState.launchPermissionRequest() },
                    contentPadding = padding
                )
            }
            composable(ROUTE_NOW_PLAYING) {
                NowPlayingScreen(
                    song = currentSong,
                    playback = playbackState,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSkipNext = viewModel::skipToNext,
                    onSkipPrevious = viewModel::skipToPrevious,
                    onSeek = viewModel::seekTo,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeatMode,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
