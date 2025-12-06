package com.example.cicadaplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cicadaplayer.data.MusicLibrary
import com.example.cicadaplayer.data.Track
import com.example.cicadaplayer.data.SettingsRepository
import com.example.cicadaplayer.player.MusicPlayerController
import com.example.cicadaplayer.ui.MainViewModel
import com.example.cicadaplayer.ui.PlayerUiState
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val viewModel = remember {
                MainViewModel(
                    musicLibrary = MusicLibrary(context),
                    settingsRepository = SettingsRepository(context),
                    playerController = MusicPlayerController(context)
                )
            }
            CicadaPlayerApp(viewModel)
        }
    }
}

@Composable
fun CicadaPlayerApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MaterialTheme {
        MainScreen(
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
fun MainScreen(
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
) {
    var folderInput by remember { mutableStateOf("") }
    var moveTarget by remember { mutableStateOf(state.settings.moveTargetDirectory) }

    LaunchedEffect(state.settings.moveTargetDirectory) {
        moveTarget = state.settings.moveTargetDirectory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cicada Player", style = MaterialTheme.typography.headlineMedium)
        state.playback.currentTrack?.let { track ->
            TrackCard(track = track, position = state.playback.currentPosition, duration = state.playback.duration)
        }
        PlaybackControls(
            isPlaying = state.playback.isPlaying,
            position = state.playback.currentPosition,
            duration = state.playback.duration,
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            volume = state.settings.playbackVolume,
            onVolumeChange = onVolumeChange,
        )

        FdButtons(onForget = onForget, onDiscard = onDiscard)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Folders", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = folderInput,
                    onValueChange = { folderInput = it },
                    label = { Text("Add folder path") },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (folderInput.isNotBlank()) {
                                onFoldersChanged(state.settings.selectedFolders + folderInput)
                                folderInput = ""
                            }
                        }) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                state.settings.selectedFolders.forEach { folder ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(folder)
                        TextButton(onClick = {
                            onFoldersChanged(state.settings.selectedFolders - folder)
                        }) { Text("Remove") }
                    }
                }
                Button(onClick = onRefreshLibrary, modifier = Modifier.fillMaxWidth()) { Text("Scan and build playlist") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Move target directory", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = moveTarget,
                    onValueChange = { moveTarget = it },
                    label = { Text("Directory used for discard button") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { onMoveTargetChanged(moveTarget) }) { Text("Save target") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Equalizer", fontWeight = FontWeight.Bold)
                state.settings.equalizerBands.forEach { (freq, gain) ->
                    EqualizerSlider(frequency = freq, gain = gain, onEqualizerChange = onEqualizerChange)
                }
            }
        }
    }
}

@Composable
private fun TrackCard(track: Track, position: Long, duration: Long) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(track.title, style = MaterialTheme.typography.titleLarge)
            Text(track.artist ?: "Unknown artist")
            Text(track.album ?: "")
            Text("${position / 1000}s / ${duration / 1000}s")
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onSkipPrevious) { Icon(Icons.Default.FastRewind, contentDescription = "Previous") }
                Button(onClick = onPlayPause) { Text(if (isPlaying) "Pause" else "Play") }
                IconButton(onClick = onSkipNext) { Icon(Icons.Default.FastForward, contentDescription = "Next") }
            }
            Slider(value = if (duration == 0L) 0f else position.toFloat() / duration, onValueChange = onSeek)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Volume")
                Slider(value = volume, onValueChange = onVolumeChange)
            }
        }
    }
}

@Composable
private fun EqualizerSlider(frequency: Int, gain: Short, onEqualizerChange: (Int, Short) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${frequency}Hz : ${gain}dB")
        Slider(
            value = gain.toFloat(),
            onValueChange = { onEqualizerChange(frequency, it.toInt().toShort()) },
            valueRange = -10f..10f
        )
    }
}

@Composable
private fun FdButtons(onForget: () -> Unit, onDiscard: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onForget,
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("F", style = MaterialTheme.typography.headlineLarge)
                Text("Forget and skip")
            }
        }
        Button(
            onClick = onDiscard,
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("D", style = MaterialTheme.typography.headlineLarge)
                Text("Move then skip")
            }
        }
    }
}
