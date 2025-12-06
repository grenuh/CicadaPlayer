package com.example.cicadaplayer.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.util.UUID

class MusicLibrary(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun loadFromFolders(folders: List<String>): Playlist = withContext(Dispatchers.IO) {
        val tracks = folders.flatMap { folder ->
            val directory = File(folder)
            if (!directory.exists() || !directory.isDirectory) return@flatMap emptyList<Track>()
            directory.listFiles { file -> file.extension.lowercase() in setOf("mp3", "wav", "flac", "ogg") }
                ?.mapNotNull { file -> fileToTrack(file) }
                ?: emptyList()
        }
        Playlist(id = UUID.randomUUID().toString(), name = "Quick Mix", tracks = tracks)
    }

    private fun fileToTrack(file: File): Track? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: file.nameWithoutExtension
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            Track(
                uri = Uri.fromFile(file),
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                filePath = file.absolutePath,
            )
        } catch (error: Exception) {
            null
        }
    }

    suspend fun removeTrackFromPlaylist(playlist: Playlist, track: Track): Playlist = withContext(Dispatchers.IO) {
        playlist.copy(tracks = playlist.tracks.filterNot { it.filePath == track.filePath })
    }

    suspend fun moveTrackToDirectory(track: Track, directoryPath: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val targetDir = File(directoryPath)
            if (!targetDir.exists()) targetDir.mkdirs()
            val source = File(track.filePath)
            val target = File(targetDir, source.name)
            moveFile(source, target)
            true
        }.getOrDefault(false)
    }

    private fun moveFile(source: File, target: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(target).use { output ->
                val inChannel: FileChannel = input.channel
                val outChannel: FileChannel = output.channel
                inChannel.transferTo(0, inChannel.size(), outChannel)
            }
        }
        source.delete()
        notifyMediaScanner(target)
    }

    private fun notifyMediaScanner(file: File) {
        val uri = Uri.fromFile(file)
        resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ContentValues())
        resolver.notifyChange(uri, null)
    }

    fun deleteFromStorage(track: Track) {
        val uri = Uri.fromFile(File(track.filePath))
        resolver.delete(uri, null, null)
    }
}
