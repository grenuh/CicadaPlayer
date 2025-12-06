package com.example.cicadaplayer.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cicadaplayer.util.treeUriToFilePath
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun CicadaPlayerApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
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
        val path = treeUriToFilePath(uri)
        if (path != null) {
            val updated = (state.settings.selectedFolders + path).distinct()
            viewModel.updateFolders(updated)
        } else {
            Toast.makeText(context, "Unable to read selected folder", Toast.LENGTH_SHORT).show()
        }
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
        val path = treeUriToFilePath(uri)
        if (path != null) {
            viewModel.updateMoveTarget(path)
        } else {
            Toast.makeText(context, "Unable to read selected folder", Toast.LENGTH_SHORT).show()
        }
    }

    MaterialTheme {
        CicadaNavigation(
            navController = navController,
            state = state,
            onPlayPause = { viewModel.togglePlayback() },
            onSeek = { viewModel.seek(it) },
            onRefreshLibrary = { viewModel.refreshLibrary() },
            onVolumeChange = { viewModel.setVolume(it) },
            onFoldersChanged = { viewModel.updateFolders(it) },
            onMoveTargetChanged = { viewModel.updateMoveTarget(it) },
            onEqualizerChange = { freq, gain -> viewModel.updateEqualizerBand(freq, gain) },
            onSkipNext = { viewModel.skipNext() },
            onSkipPrevious = { viewModel.skipPrevious() },
            onForget = { viewModel.removeCurrentTrack(); viewModel.skipNext() },
            onDiscard = { viewModel.moveCurrentTrack(); viewModel.skipNext() },
            onPickFolder = { folderPicker.launch(null) },
            onPickMoveTarget = { moveTargetPicker.launch(null) }
        )
    }

    LaunchedEffect(Unit) { viewModel.refreshLibrary() }

    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.refreshProgress()
            delay(500)
        }
    }
}

@Composable
fun CicadaNavigation(
    navController: NavHostController,
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onRefreshLibrary: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onFoldersChanged: (List<String>) -> Unit,
    onMoveTargetChanged: (String) -> Unit,
    onEqualizerChange: (Int, Short) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onForget: () -> Unit,
    onDiscard: () -> Unit,
    onPickFolder: () -> Unit,
    onPickMoveTarget: () -> Unit,
) {
    val destinations = listOf(
        AppDestination("player", Icons.Default.PlayArrow, "Player"),
        AppDestination("library", Icons.Default.LibraryMusic, "Library"),
        AppDestination("equalizer", Icons.Default.GraphicEq, "Equalizer"),
        AppDestination("settings", Icons.Default.Settings, "Settings"),
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: destinations.first().route

    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = destinations.first().route,
            modifier = Modifier.weight(1f)
        ) {
            composable("player") {
                PlayerScreen(
                    state = state,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onVolumeChange = onVolumeChange,
                    onForget = onForget,
                    onDiscard = onDiscard,
                )
            }
            composable("library") {
                LibraryScreen(
                    state = state,
                    onRefreshLibrary = onRefreshLibrary,
                )
            }
            composable("equalizer") {
                EqualizerScreen(
                    bands = state.settings.equalizerBands,
                    onEqualizerChange = onEqualizerChange
                )
            }
            composable("settings") {
                SettingsScreen(
                    selectedFolders = state.settings.selectedFolders,
                    moveTargetDirectory = state.settings.moveTargetDirectory,
                    onMoveTargetChanged = onMoveTargetChanged,
                    onFoldersChanged = onFoldersChanged,
                    onPickFolder = onPickFolder,
                    onPickMoveTarget = onPickMoveTarget
                )
            }
        }

        NavigationBar {
            destinations.forEach { destination ->
                val selected = destination.route == currentRoute
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(destination.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                        }
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
