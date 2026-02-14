package com.example.cicadaplayer.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicLibrary(private val context: Context) {
    private val audioExtensions = setOf("mp3", "wav", "flac", "ogg")

    sealed class ScanEvent {
        data class TrackFound(val track: Track) : ScanEvent()
        data class Error(val message: String) : ScanEvent()
    }

    fun scanFolders(folders: List<String>): Flow<ScanEvent> = flow {
        for (folderUriString in folders) {
            val treeUri = Uri.parse(folderUriString)
            runCatching {
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                )

                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameCol) ?: continue
                        val ext = name.substringAfterLast('.').lowercase()
                        if (ext !in audioExtensions) continue

                        val documentId = cursor.getString(idCol)
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                        val sizeBytes = cursor.getLong(sizeCol)
                        val sizeMb = sizeBytes / (1024f * 1024f)

                        val bitrateKbps = extractBitrate(documentUri)

                        emit(ScanEvent.TrackFound(
                            Track(
                                uri = documentUri,
                                title = name.substringBeforeLast('.'),
                                artist = null,
                                album = null,
                                durationMs = 0L,
                                filePath = documentUri.toString(),
                                fileFormat = ext.uppercase(),
                                bitrateKbps = bitrateKbps,
                                fileSizeMb = sizeMb,
                            )
                        ))
                    }
                }
            }.onFailure { e ->
                emit(ScanEvent.Error("Cannot access folder: ${e.message}"))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun extractBitrate(uri: Uri): Int {
        return runCatching {
            MediaMetadataRetriever().use { mmr ->
                mmr.setDataSource(context, uri)
                val bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                (bitrate?.toIntOrNull() ?: 0) / 1000
            }
        }.getOrDefault(0)
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
