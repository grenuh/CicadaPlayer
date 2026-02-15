package com.example.cicadaplayer.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.example.cicadaplayer.R
import com.example.cicadaplayer.databinding.ScreenPlayerBinding

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    artworkBytes: ByteArray?,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onForget: () -> Unit,
    onDiscard: () -> Unit,
) {
    val albumBitmap: Bitmap? = remember(artworkBytes) {
        artworkBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

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

        // --- Album art ---
        if (track != null) {
            albumArt.visibility = View.VISIBLE
            if (albumBitmap != null) {
                albumArt.setImageBitmap(albumBitmap)
            } else {
                albumArt.setImageResource(R.drawable.ic_album_placeholder)
            }
        } else {
            albumArt.visibility = View.GONE
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
        val disallowIntercept = @SuppressLint("ClickableViewAccessibility")
        View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
        seekSlider.setOnTouchListener(disallowIntercept)
        seekSlider.clearOnChangeListeners()
        val seekValue = if (state.playback.duration == 0L) 0f
            else state.playback.currentPosition.toFloat() / state.playback.duration
        seekSlider.value = seekValue.coerceIn(0f, 1f)
        seekSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) onSeek(value)
        }

        // --- Volume slider ---
        val audioManager = root.context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        val currentVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val systemVolume = if (maxVol > 0) currentVol.toFloat() / maxVol else 0f
        volumeSlider.setOnTouchListener(disallowIntercept)
        volumeSlider.clearOnChangeListeners()
        volumeSlider.value = systemVolume.coerceIn(0f, 1f)
        volumeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) onVolumeChange(value)
        }

        // --- F / D buttons ---
        btnForget.setOnClickListener { onForget() }
        btnDiscard.setOnClickListener { onDiscard() }
    }
}
