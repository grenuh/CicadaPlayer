package com.example.cicadaplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cicadaplayer.data.MusicLibrary
import com.example.cicadaplayer.data.MusicLibrary.ScanEvent
import com.example.cicadaplayer.data.PlaybackState
import com.example.cicadaplayer.data.PlayerSettings
import com.example.cicadaplayer.data.Playlist
import com.example.cicadaplayer.data.SettingsRepository
import com.example.cicadaplayer.data.Track
import com.example.cicadaplayer.player.MusicPlayerController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val musicLibrary: MusicLibrary,
    private val settingsRepository: SettingsRepository,
    private val playerController: MusicPlayerController,
) : ViewModel() {

    private val _playlist = MutableStateFlow<Playlist?>(null)
    private val _playbackState: StateFlow<PlaybackState> = playerController.state
    private val _isScanning = MutableStateFlow(false)

    private val _toastMessages = MutableSharedFlow<String>()
    val toastMessages: SharedFlow<String> = _toastMessages

    val artworkBytes: StateFlow<ByteArray?> = playerController.artworkBytes

    val uiState: StateFlow<PlayerUiState> = combine(
        settingsRepository.settings,
        _playlist,
        _playbackState,
        _isScanning,
    ) { settings, playlist, playback, isScanning ->
        PlayerUiState(
            settings = settings,
            playlist = playlist,
            playback = playback.copy(currentPlaylist = playlist ?: playback.currentPlaylist),
            isScanning = isScanning,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUiState())

    private var previousTrack: Track? = null

    init {
        viewModelScope.launch {
            val initial = settingsRepository.settings.first()
            initial.equalizerBands.forEach { (freq, gain) ->
                playerController.setEqualizerBand(freq, gain)
            }
            settingsRepository.settings.collect { settings ->
                playerController.setVolume(settings.playbackVolume)
            }
        }
        viewModelScope.launch {
            val tracks = settingsRepository.playlist.first()
            if (tracks.isNotEmpty()) {
                val playlist = Playlist(
                    id = "persisted",
                    name = "Library",
                    tracks = tracks
                )
                _playlist.value = playlist
                playerController.setPlaylistMetadata(playlist.name, playlist.id)
                playerController.loadPlaylist(playlist.tracks)
            }
        }
        viewModelScope.launch {
            _playbackState.collect { playback ->
                val current = playback.currentTrack
                val prev = previousTrack
                if (prev != null && current != null && prev.uri != current.uri) {
                    if (uiState.value.settings.removeOnEnd) {
                        removeTrack(prev)
                    }
                }
                previousTrack = current
            }
        }
    }

    fun refreshLibrary(folders: List<String> = uiState.value.settings.selectedFolders) {
        viewModelScope.launch {
            _isScanning.value = true
            val playlistId = java.util.UUID.randomUUID().toString()
            val playlist = Playlist(id = playlistId, name = "Quick Mix", tracks = emptyList())
            _playlist.value = playlist
            playerController.setPlaylistMetadata(playlist.name, playlist.id)
            playerController.clearPlaylist()

            val tracks = mutableListOf<Track>()
            musicLibrary.scanFolders(folders).collect { event ->
                when (event) {
                    is ScanEvent.TrackFound -> {
                        tracks.add(event.track)
                        _playlist.value = _playlist.value?.copy(tracks = tracks.toList())
                        playerController.addTrack(event.track)
                    }
                    is ScanEvent.Error -> {
                        _toastMessages.emit(event.message)
                    }
                }
            }

            settingsRepository.savePlaylist(tracks)
            _isScanning.value = false
        }
    }

    fun togglePlayback() {
        if (_playbackState.value.isPlaying) playerController.pause() else playerController.play()
    }

    fun seek(position: Float) {
        val duration = _playbackState.value.duration
        val target = (duration * position).toLong()
        playerController.seekTo(target)
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(playbackVolume = volume) }
            playerController.setVolume(volume)
        }
    }

    fun updateFolders(folders: List<String>) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(selectedFolders = folders) }
            refreshLibrary(folders)
        }
    }

    fun updateMoveTarget(path: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(moveTargetDirectory = path) }
        }
    }

    fun updateEqualizerBand(freq: Int, gain: Short) {
        playerController.setEqualizerBand(freq, gain)
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(equalizerBands = it.equalizerBands.toMutableMap().apply { put(freq, gain) })
            }
        }
    }

    fun skipNext() = playerController.skipNext()

    fun skipPrevious() = playerController.skipPrevious()

    fun playTrackAt(index: Int) {
        val track = _playlist.value?.tracks?.getOrNull(index) ?: return
        playerController.playByUri(track.uri.toString())
    }

    fun removeCurrentTrack() {
        val current = _playbackState.value.currentTrack ?: return
        removeTrack(current)
    }

    private fun removeTrack(track: Track) {
        val playlist = _playlist.value ?: return
        viewModelScope.launch {
            val updated = musicLibrary.removeTrackFromPlaylist(playlist, track)
            _playlist.value = updated
            settingsRepository.savePlaylist(updated.tracks)
        }
    }

    fun toggleRemoveOnEnd(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(removeOnEnd = enabled) }
        }
    }

    fun moveCurrentTrack() {
        val current = _playbackState.value.currentTrack ?: return
        val targetDir = uiState.value.settings.moveTargetDirectory
        if (targetDir.isBlank()) return
        viewModelScope.launch {
            val moved = musicLibrary.moveTrackToDirectory(current, targetDir)
            if (moved) removeCurrentTrack()
        }
    }

    fun shufflePlaylist() {
        val playlist = _playlist.value ?: return
        viewModelScope.launch {
            val shuffled = playlist.copy(tracks = playlist.tracks.shuffled())
            _playlist.value = shuffled
            playerController.loadPlaylist(shuffled.tracks)
            settingsRepository.savePlaylist(shuffled.tracks)
        }
    }

    fun refreshProgress() {
        playerController.refreshProgress()
    }
}

data class PlayerUiState(
    val settings: PlayerSettings = PlayerSettings(),
    val playlist: Playlist? = null,
    val playback: PlaybackState = PlaybackState(),
    val isScanning: Boolean = false,
)
