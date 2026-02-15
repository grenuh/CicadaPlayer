package com.example.cicadaplayer.data

import android.net.Uri

/** Basic metadata for a track discovered on disk. */
data class Track(
    val uri: Uri,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val filePath: String,
    val fileFormat: String = "",
    val bitrateKbps: Int = 0,
    val fileSizeMb: Float = 0f,
)

/** Playlist containing an ordered set of tracks. */
data class Playlist(
    val id: String,
    val name: String,
    val tracks: List<Track>
)

/** Settings the user can adjust at runtime. */
data class PlayerSettings(
    val selectedFolders: List<String> = emptyList(),
    val moveTargetDirectory: String = "",
    val equalizerBands: Map<Int, Short> = mapOf(60 to 0, 230 to 0, 910 to 0, 3600 to 0, 14000 to 0),
    val playbackVolume: Float = 0.5f,
    val removeOnEnd: Boolean = false,
)

/** UI state representing the current playback position. */
data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentPlaylist: Playlist? = null,
    val queueIndex: Int = 0,
    val artworkBytes: ByteArray? = null,
)
