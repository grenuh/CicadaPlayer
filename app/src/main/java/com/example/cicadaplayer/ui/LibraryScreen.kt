package com.example.cicadaplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
    val tracks = state.playlist?.tracks ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Library", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            Button(
                onClick = onRefreshLibrary,
                enabled = !state.isScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isScanning) "Scanning..." else "Scan and build playlist")
            }
        }

        if (state.isScanning) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Scanning folders for music...")
                }
            }
        }

        item {
            Text(
                "Songs (${tracks.size})",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (tracks.isNotEmpty()) {
            itemsIndexed(tracks) { index, track ->
                TrackRow(index = index, track = track)
            }
        } else if (!state.isScanning) {
            item {
                Text("No songs loaded. Select folders and scan to build your playlist.")
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
            Text(
                buildString {
                    append(track.fileFormat)
                    if (track.bitrateKbps > 0) append(" \u00B7 ${track.bitrateKbps} kbps")
                    if (track.fileSizeMb > 0f) append(" \u00B7 ${"%.1f".format(track.fileSizeMb)} MB")
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
