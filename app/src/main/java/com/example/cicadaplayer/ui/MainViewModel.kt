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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val musicLibrary: MusicLibrary,
    private val settingsRepository: SettingsRepository,
    private val playerController: MusicPlayerController,
) : ViewModel() {

    private val _playlist = MutableStateFlow<Playlist?>(null)
    private val _playbackState: StateFlow<PlaybackState> = playerController.state

    val uiState: StateFlow<PlayerUiState> = combine(
        settingsRepository.settings,
        _playlist,
        _playbackState,
    ) { settings, playlist, playback ->
        PlayerUiState(
            settings = settings,
            playlist = playlist,
            playback = playback.copy(currentPlaylist = playlist ?: playback.currentPlaylist)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUiState())

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                playerController.setVolume(settings.playbackVolume)
            }
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            val folders = uiState.value.settings.selectedFolders
            val playlist = musicLibrary.loadFromFolders(folders)
            _playlist.value = playlist
            playerController.setPlaylistMetadata(playlist.name, playlist.id)
            playerController.loadPlaylist(playlist.tracks)
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
            refreshLibrary()
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

    fun refreshProgress() {
        playerController.refreshProgress()
    }
}

data class PlayerUiState(
    val settings: PlayerSettings = PlayerSettings(),
    val playlist: Playlist? = null,
    val playback: PlaybackState = PlaybackState(),
)
