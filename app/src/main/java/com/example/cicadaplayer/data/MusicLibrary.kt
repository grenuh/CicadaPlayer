package com.example.cicadaplayer.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicLibrary(private val context: Context) {
    private val audioExtensions = setOf("mp3", "wav", "flac", "ogg")

    suspend fun loadFromFolders(folders: List<String>): Playlist = withContext(Dispatchers.IO) {
        val tracks = folders.flatMap { folderUriString ->
            val treeUri = Uri.parse(folderUriString)
            listAudioFiles(treeUri)
        }
        Playlist(id = UUID.randomUUID().toString(), name = "Quick Mix", tracks = tracks)
    }

    /**
     * Single ContentResolver query per folder. Skips MediaMetadataRetriever
     * entirely — uses display name as title. ExoPlayer reads metadata on playback.
     */
    private fun listAudioFiles(treeUri: Uri): List<Track> {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val tracks = mutableListOf<Track>()

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )

        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol) ?: continue
                val ext = name.substringAfterLast('.').lowercase()
                if (ext !in audioExtensions) continue

                val documentId = cursor.getString(idCol)
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

                tracks.add(
                    Track(
                        uri = documentUri,
                        title = name.substringBeforeLast('.'),
                        artist = null,
                        album = null,
                        durationMs = 0L,
                        filePath = documentUri.toString(),
                    )
                )
            }
        }

        return tracks
    }

    suspend fun removeTrackFromPlaylist(playlist: Playlist, track: Track): Playlist = withContext(Dispatchers.IO) {
        playlist.copy(tracks = playlist.tracks.filterNot { it.uri == track.uri })
    }

    suspend fun moveTrackToDirectory(track: Track, targetUriString: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val targetTreeUri = Uri.parse(targetUriString)
            val targetDir = DocumentFile.fromTreeUri(context, targetTreeUri)
                ?: return@withContext false

            val cursor = context.contentResolver.query(
                track.uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )
            val fileName = cursor?.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: "${track.title}.mp3"

            val mimeType = context.contentResolver.getType(track.uri) ?: "audio/mpeg"
            val newFile = targetDir.createFile(mimeType, fileName)
                ?: return@withContext false

            context.contentResolver.openInputStream(track.uri)?.use { input ->
                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    input.copyTo(output)
                }
            }

            DocumentsContract.deleteDocument(context.contentResolver, track.uri)
            true
        }.getOrDefault(false)
    }

    fun deleteFromStorage(track: Track) {
        runCatching {
            DocumentsContract.deleteDocument(context.contentResolver, track.uri)
        }
    }
}
