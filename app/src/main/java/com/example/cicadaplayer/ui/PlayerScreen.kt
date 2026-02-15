package com.example.cicadaplayer.ui

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.example.cicadaplayer.R
import com.example.cicadaplayer.databinding.ScreenPlayerBinding

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onForget: () -> Unit,
    onDiscard: () -> Unit,
    onShuffle: () -> Unit,
    removeOnEnd: Boolean,
    onRemoveOnEndChange: (Boolean) -> Unit,
) {
    AndroidViewBinding(ScreenPlayerBinding::inflate) {
        // --- Track card ---
        val track = state.playback.currentTrack
        if (track != null) {
            trackCard.visibility = View.VISIBLE
            trackTitle.text = track.title
            trackArtist.text = track.artist
                ?: root.context.getString(R.string.player_unknown_artist)
            trackAlbum.text = track.album ?: ""
            trackTime.text = root.context.getString(
                R.string.player_time_format,
                state.playback.currentPosition / 1000,
                state.playback.duration / 1000,
            )
        } else {
            trackCard.visibility = View.GONE
        }

        // --- Play / Pause button ---
        if (state.playback.isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_pause_24)
            btnPlayPause.contentDescription = root.context.getString(R.string.player_pause)
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play_arrow_24)
            btnPlayPause.contentDescription = root.context.getString(R.string.player_play)
        }
        btnPlayPause.setOnClickListener { onPlayPause() }

        // --- Transport ---
        btnPrevious.setOnClickListener { onSkipPrevious() }
        btnNext.setOnClickListener { onSkipNext() }

        // --- Seek slider ---
        // Clear listener before setting value to avoid feedback loop
        seekSlider.clearOnChangeListeners()
        val seekValue = if (state.playback.duration == 0L) 0f
            else state.playback.currentPosition.toFloat() / state.playback.duration
        seekSlider.value = seekValue.coerceIn(0f, 1f)
        seekSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) onSeek(value)
        }

        // --- Volume slider ---
        volumeSlider.clearOnChangeListeners()
        volumeSlider.value = state.settings.playbackVolume.coerceIn(0f, 1f)
        volumeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) onVolumeChange(value)
        }

        // --- F / D buttons ---
        btnForget.setOnClickListener { onForget() }
        btnDiscard.setOnClickListener { onDiscard() }
        btnShuffle.setOnClickListener { onShuffle() }

                      // --- Remove on end switch ---
         removeOnEndSwitch.setOnCheckedChangeListener(null)
         removeOnEndSwitch.isChecked = removeOnEnd
         removeOnEndSwitch.setOnCheckedChangeListener { _, isChecked ->
                     onRemoveOnEndChange(isChecked)
                 }
    }
}
