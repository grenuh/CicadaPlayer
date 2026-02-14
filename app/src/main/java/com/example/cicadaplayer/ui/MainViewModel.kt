package com.example.cicadaplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cicadaplayer.data.MusicLibrary
import com.example.cicadaplayer.data.PlaybackState
import com.example.cicadaplayer.data.PlayerSettings
import com.example.cicadaplayer.data.Playlist
import com.example.cicadaplayer.data.SettingsRepository
import com.example.cicadaplayer.player.MusicPlayerController
import kotlinx.coroutines.flow.MutableStateFlow
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

    init {
        viewModelScope.launch {
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
    }

    fun refreshLibrary(folders: List<String> = uiState.value.settings.selectedFolders) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val playlist = musicLibrary.loadFromFolders(folders)
                _playlist.value = playlist
                playerController.setPlaylistMetadata(playlist.name, playlist.id)
                playerController.loadPlaylist(playlist.tracks)
                settingsRepository.savePlaylist(playlist.tracks)
            } finally {
                _isScanning.value = false
            }
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
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(equalizerBands = it.equalizerBands.toMutableMap().apply { put(freq, gain) })
            }
        }
    }

    fun skipNext() = playerController.skipNext()

    fun skipPrevious() = playerController.skipPrevious()

    fun removeCurrentTrack() {
        val current = _playbackState.value.currentTrack ?: return
        val playlist = _playlist.value ?: return
        viewModelScope.launch {
            val updated = musicLibrary.removeTrackFromPlaylist(playlist, current)
            _playlist.value = updated
            playerController.loadPlaylist(updated.tracks)
            settingsRepository.savePlaylist(updated.tracks)
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
