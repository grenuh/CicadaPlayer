package com.example.cicadaplayer.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun CicadaPlayerApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        val updated = (state.settings.selectedFolders + uri.toString()).distinct()
        viewModel.updateFolders(updated)
    }

    val moveTargetPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        viewModel.updateMoveTarget(uri.toString())
    }

    MaterialTheme {
        CicadaNavigation(
            state = state,
            onPlayPause = { viewModel.togglePlayback() },
            onSeek = { viewModel.seek(it) },
            onRefreshLibrary = { viewModel.refreshLibrary() },
            onVolumeChange = { viewModel.setVolume(it) },
            onFoldersChanged = { viewModel.updateFolders(it) },
            onEqualizerChange = { freq, gain -> viewModel.updateEqualizerBand(freq, gain) },
            onSkipNext = { viewModel.skipNext() },
            onSkipPrevious = { viewModel.skipPrevious() },
            onForget = { viewModel.removeCurrentTrack(); viewModel.skipNext() },
            onDiscard = { viewModel.moveCurrentTrack(); viewModel.skipNext() },
            onPickFolder = { folderPicker.launch(null) },
            onPickMoveTarget = { moveTargetPicker.launch(null) },
            onShuffle = { viewModel.shufflePlaylist() },
            onTrackTap = { index -> viewModel.playTrackAt(index) },
            onRemoveOnEndChange = { viewModel.toggleRemoveOnEnd(it) }
        )
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.refreshProgress()
            delay(500)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CicadaNavigation(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onRefreshLibrary: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onFoldersChanged: (List<String>) -> Unit,
    onEqualizerChange: (Int, Short) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onForget: () -> Unit,
    onDiscard: () -> Unit,
    onPickFolder: () -> Unit,
    onPickMoveTarget: () -> Unit,
    onShuffle: () -> Unit,
    onTrackTap: (Int) -> Unit,
    onRemoveOnEndChange: (Boolean) -> Unit,
) {
    val destinations = listOf(
        AppDestination("player", Icons.Default.PlayArrow, "Player"),
        AppDestination("library", Icons.Default.LibraryMusic, "Library"),
        AppDestination("equalizer", Icons.Default.GraphicEq, "Equalizer"),
        AppDestination("settings", Icons.Default.Settings, "Settings"),
    )

    val pagerState = rememberPagerState(pageCount = { destinations.size })
    val coroutineScope = rememberCoroutineScope()

    // Navigate to player when a track is tapped
    val onTrackTapWithNav: (Int) -> Unit = { index ->
        onTrackTap(index)
        coroutineScope.launch { pagerState.animateScrollToPage(0) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> PlayerScreen(
                    state = state,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onVolumeChange = onVolumeChange,
                    onForget = onForget,
                    onDiscard = onDiscard,
                )
                1 -> LibraryScreen(
                    state = state,
                    onRefreshLibrary = onRefreshLibrary,
                    onTrackTap = onTrackTapWithNav,
                    onShuffle = onShuffle,
                    removeOnEnd = state.settings.removeOnEnd,
                    onRemoveOnEndChange = onRemoveOnEndChange,
                )
                2 -> EqualizerScreen(
                    bands = state.settings.equalizerBands,
                    onEqualizerChange = onEqualizerChange
                )
                3 -> SettingsScreen(
                    selectedFolders = state.settings.selectedFolders,
                    moveTargetDirectory = state.settings.moveTargetDirectory,
                    onFoldersChanged = onFoldersChanged,
                    onPickFolder = onPickFolder,
                    onPickMoveTarget = onPickMoveTarget
                )
            }
        }

        NavigationBar {
            destinations.forEachIndexed { index, destination ->
                val selected = pagerState.currentPage == index
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                    colors = NavigationBarItemDefaults.colors(),
                )
            }
        }
    }
}

data class AppDestination(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
