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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cicadaplayer.util.treeUriToDisplayName

@Composable
fun SettingsScreen(
    selectedFolders: List<String>,
    moveTargetDirectory: String,
    onFoldersChanged: (List<String>) -> Unit,
    onPickFolder: () -> Unit,
    onPickMoveTarget: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Music folders", fontWeight = FontWeight.Bold)
                Text(
                    "Select folders to scan for music. Library scanning happens immediately after you add or remove a folder.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onPickFolder, modifier = Modifier.fillMaxWidth()) {
                    Text("Add folder")
                }
                selectedFolders.forEach { folder ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(treeUriToDisplayName(folder), modifier = Modifier.weight(1f))
                        TextButton(onClick = { onFoldersChanged(selectedFolders - folder) }) { Text("Remove") }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Move target directory", fontWeight = FontWeight.Bold)
                Text("Folder used when using the discard button.", style = MaterialTheme.typography.bodyMedium)
                if (moveTargetDirectory.isNotBlank()) {
                    Text(
                        treeUriToDisplayName(moveTargetDirectory),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(onClick = onPickMoveTarget, modifier = Modifier.fillMaxWidth()) {
                    Text(if (moveTargetDirectory.isBlank()) "Select directory" else "Change directory")
                }
            }
        }
    }
}
