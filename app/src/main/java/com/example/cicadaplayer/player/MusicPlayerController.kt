package com.example.cicadaplayer.player

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.example.cicadaplayer.data.PlaybackState
import com.example.cicadaplayer.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MusicPlayerController(context: Context) {
    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .build()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val equalizerController = EqualizerController(player.audioSessionId)

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                _state.update { state ->
                    val track = state.currentPlaylist?.tracks?.getOrNull(index)
                    state.copy(currentTrack = track, queueIndex = index, duration = player.duration)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.update { it.copy(duration = player.duration) }
                }
            }
        })
    }

    fun loadPlaylist(tracks: List<Track>) {
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setAlbumTitle(track.album)
                        .setArtist(track.artist)
                        .build()
                )
                .build()
        }
        player.setMediaItems(mediaItems, /* resetPosition = */ true)
        player.prepare()
        _state.update { state ->
            state.copy(
                currentPlaylist = state.currentPlaylist?.copy(tracks = tracks),
                currentTrack = tracks.firstOrNull(),
                queueIndex = 0,
                duration = player.duration,
            )
        }
    }

    fun setPlaylistMetadata(name: String, id: String) {
        _state.update { state ->
            state.copy(currentPlaylist = state.currentPlaylist?.copy(name = name, id = id)
                ?: com.example.cicadaplayer.data.Playlist(id, name, emptyList()))
        }
    }

    fun clearPlaylist() {
        player.clearMediaItems()
        _state.update {
            it.copy(
                currentPlaylist = it.currentPlaylist?.copy(tracks = emptyList()),
                currentTrack = null,
                queueIndex = 0,
            )
        }
    }

    fun addTrack(track: Track) {
        val mediaItem = MediaItem.Builder()
            .setUri(track.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setAlbumTitle(track.album)
                    .setArtist(track.artist)
                    .build()
            )
            .build()
        player.addMediaItem(mediaItem)
        if (player.mediaItemCount == 1) {
            player.prepare()
        }
        _state.update { state ->
            val updatedTracks = (state.currentPlaylist?.tracks ?: emptyList()) + track
            state.copy(
                currentPlaylist = state.currentPlaylist?.copy(tracks = updatedTracks),
                currentTrack = state.currentTrack ?: track,
            )
        }
    }

    fun play() {
        player.playWhenReady = true
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _state.update { it.copy(currentPosition = positionMs) }
    }

    fun setVolume(volume: Float) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val index = (volume * maxVol).toInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
    }

    fun getVolume(): Float {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) current.toFloat() / max else 0f
    }

    fun skipNext() {
        player.seekToNextMediaItem()
        _state.update { it.copy(queueIndex = player.currentMediaItemIndex) }
    }

    fun skipPrevious() {
        player.seekToPreviousMediaItem()
        _state.update { it.copy(queueIndex = player.currentMediaItemIndex) }
    }

    fun playAt(index: Int) {
        player.seekToDefaultPosition(index)
        player.playWhenReady = true
        player.play()
        _state.update { state ->
            state.copy(
                currentTrack = state.currentPlaylist?.tracks?.getOrNull(index),
                queueIndex = index,
            )
        }
    }

    fun playByUri(uri: String) {
        val index = (0 until player.mediaItemCount).firstOrNull { i ->
            player.getMediaItemAt(i).localConfiguration?.uri?.toString() == uri
        } ?: return
        playAt(index)
    }

    fun refreshProgress() {
        _state.update { it.copy(currentPosition = player.currentPosition, duration = player.duration) }
    }

    fun setEqualizerBand(frequency: Int, gainDb: Short) {
        equalizerController.setBandGain(frequency, gainDb)
    }

    fun clear() {
        equalizerController.release()
        player.stop()
        player.release()
    }
}
