package com.example.cicadaplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cicadaplayer.data.Track

@Composable
fun LibraryScreen(
    state: PlayerUiState,
    onRefreshLibrary: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Library", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = onRefreshLibrary, modifier = Modifier.fillMaxWidth()) {
            Text("Scan and build playlist")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Songs", fontWeight = FontWeight.Bold)
                if (state.playlist?.tracks?.isNotEmpty() == true) {
                    state.playlist.tracks.forEachIndexed { index, track ->
                        TrackRow(index = index, track = track)
                    }
                } else {
                    Text("No songs loaded. Select folders and scan to build your playlist.")
                }
            }
        }
    }
}

@Composable
fun TrackRow(index: Int, track: Track) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${index + 1}", fontWeight = FontWeight.Bold)
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.bodyLarge)
            Text(track.artist ?: "Unknown artist", style = MaterialTheme.typography.bodyMedium)
        }
        Text("${track.durationMs / 1000}s", style = MaterialTheme.typography.bodySmall)
    }
}
