package com.example.cicadaplayer.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cicadaplayer.data.Track

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onForget: () -> Unit,
    onDiscard: () -> Unit,
    onShuffle: () -> Unit,
) {
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
        FdButtons(onForget = onForget, onDiscard = onDiscard, onShuffle = onShuffle)
    }
}

@Composable
fun TrackCard(track: Track, position: Long, duration: Long) {
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
fun PlaybackControls(
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
fun FdButtons(onForget: () -> Unit, onDiscard: () -> Unit, onShuffle: () -> Unit) {
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
        Button(
            onClick = onShuffle,
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("R", style = MaterialTheme.typography.headlineLarge)
                Text("Shuffle")
            }
        }
    }
}
