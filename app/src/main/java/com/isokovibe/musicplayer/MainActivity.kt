package com.isokovibe.musicplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.isokovibe.musicplayer.ui.LibraryScreen
import com.isokovibe.musicplayer.ui.NowPlayingScreen
import com.isokovibe.musicplayer.ui.PlaylistDetailScreen
import com.isokovibe.musicplayer.ui.PlaylistsScreen
import com.isokovibe.musicplayer.ui.SettingsScreen
import com.isokovibe.musicplayer.ui.components.BannerAdView
import com.isokovibe.musicplayer.ui.components.BrandTopBar
import com.isokovibe.musicplayer.ui.components.MiniPlayer
import com.isokovibe.musicplayer.ui.theme.IsokoVibeTheme

private const val ROUTE_LIBRARY = "library"
private const val ROUTE_PLAYLISTS = "playlists"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_NOW_PLAYING = "now_playing"
private const val ROUTE_PLAYLIST_DETAIL = "playlist_detail/{playlistId}"
private fun playlistDetailRoute(id: String) = "playlist_detail/$id"

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            IsokoVibeTheme(themeMode = themeMode) {
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

    val libraryUiState by viewModel.libraryUiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val animateAlbumArt by viewModel.animateAlbumArt.collectAsState()
    val miniPlayerColorArgb by viewModel.miniPlayerColor.collectAsState()
    val showAds by viewModel.showAds.collectAsState()
    val adUnitId by viewModel.adUnitId.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val currentSong = viewModel.songById(playbackState.currentSongId)
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isTopLevelRoute = currentRoute == ROUTE_LIBRARY || currentRoute == ROUTE_PLAYLISTS || currentRoute == ROUTE_SETTINGS
    val miniPlayerColor = miniPlayerColorArgb?.let { Color(it) }

    Scaffold(
        topBar = { if (isTopLevelRoute) BrandTopBar() },
        bottomBar = {
            if (isTopLevelRoute) {
                Column {
                    if (currentSong != null) {
                        MiniPlayer(
                            song = currentSong,
                            isPlaying = playbackState.isPlaying,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSkipNext = viewModel::skipToNext,
                            onSkipPrevious = viewModel::skipToPrevious,
                            onOpenNowPlaying = { navController.navigate(ROUTE_NOW_PLAYING) },
                            backgroundColor = miniPlayerColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == ROUTE_LIBRARY,
                            onClick = { navController.navigate(ROUTE_LIBRARY) { launchSingleTop = true } },
                            icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                            label = { Text("Library") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == ROUTE_PLAYLISTS,
                            onClick = { navController.navigate(ROUTE_PLAYLISTS) { launchSingleTop = true } },
                            icon = { Icon(Icons.Filled.QueueMusic, contentDescription = null) },
                            label = { Text("Playlists") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == ROUTE_SETTINGS,
                            onClick = { navController.navigate(ROUTE_SETTINGS) { launchSingleTop = true } },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            label = { Text("Settings") }
                        )
                    }
                    // Sticky footer ad banner, pinned below the tab bar.
                    if (showAds) {
                        BannerAdView(adUnitId = adUnitId)
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_LIBRARY,
            // Instant screen switches — Compose Navigation's default fade
            // was reported as sluggish, so cut animation entirely for a
            // snappier feel.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(ROUTE_LIBRARY) {
                LibraryScreen(
                    state = libraryUiState,
                    searchQuery = searchQuery,
                    sortOption = sortOption,
                    showFavoritesOnly = showFavoritesOnly,
                    favorites = favorites,
                    playlists = playlists,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onSortOptionChange = viewModel::setSortOption,
                    onToggleFavoritesOnly = viewModel::setShowFavoritesOnly,
                    onSongClick = { song -> viewModel.playSong(song) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onAddToPlaylist = { playlist, song -> viewModel.addSongToPlaylist(playlist.id, song.id) },
                    onCreatePlaylistAndAdd = { name, song -> viewModel.createPlaylistAndAddSong(name, song.id) },
                    onRequestPermission = { permissionState.launchPermissionRequest() },
                    contentPadding = padding
                )
            }
            composable(ROUTE_PLAYLISTS) {
                PlaylistsScreen(
                    playlists = playlists,
                    onPlaylistClick = { navController.navigate(playlistDetailRoute(it.id)) },
                    onCreatePlaylist = viewModel::createPlaylist,
                    contentPadding = padding
                )
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = viewModel::setThemeMode,
                    playbackSpeed = playbackState.playbackSpeed,
                    onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
                    sleepTimerRemainingMs = playbackState.sleepTimerRemainingMs,
                    onSetSleepTimer = viewModel::startSleepTimer,
                    animateAlbumArt = animateAlbumArt,
                    onAnimateAlbumArtChange = viewModel::setAnimateAlbumArt,
                    miniPlayerColorArgb = miniPlayerColorArgb,
                    onMiniPlayerColorChange = viewModel::setMiniPlayerColor,
                    showAds = showAds,
                    onShowAdsChange = viewModel::setShowAds,
                    adUnitId = adUnitId,
                    onAdUnitIdChange = viewModel::setAdUnitId,
                    notificationsEnabled = notificationsEnabled,
                    onNotificationsEnabledChange = viewModel::setNotificationsEnabled,
                    contentPadding = padding
                )
            }
            composable(
                ROUTE_PLAYLIST_DETAIL,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId")
                val playlist = playlists.firstOrNull { it.id == playlistId }
                if (playlist != null) {
                    PlaylistDetailScreen(
                        playlist = playlist,
                        songs = viewModel.songsForPlaylist(playlist),
                        onSongClick = { song -> viewModel.playPlaylist(playlist, song.id) },
                        onPlayAll = { viewModel.playPlaylist(playlist) },
                        onRemoveSong = { song -> viewModel.removeSongFromPlaylist(playlist.id, song.id) },
                        onRename = { newName -> viewModel.renamePlaylist(playlist.id, newName) },
                        onDelete = {
                            viewModel.deletePlaylist(playlist.id)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(ROUTE_NOW_PLAYING) {
                NowPlayingScreen(
                    song = currentSong,
                    playback = playbackState,
                    isFavorite = currentSong?.let { it.id in favorites } ?: false,
                    animateAlbumArt = animateAlbumArt,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSkipNext = viewModel::skipToNext,
                    onSkipPrevious = viewModel::skipToPrevious,
                    onSeek = viewModel::seekTo,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeatMode,
                    onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it.id) } },
                    onSetPlaybackSpeed = viewModel::setPlaybackSpeed,
                    onSetSleepTimer = viewModel::startSleepTimer,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
