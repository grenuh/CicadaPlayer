package com.example.cicadaplayer.player

import android.media.audiofx.Equalizer

class EqualizerController(sessionId: Int) {
    private val equalizer = Equalizer(0, sessionId).apply { enabled = true }

    /** Band level range in millibels, e.g. [-1500, 1500]. */
    val bandLevelRange: Pair<Short, Short>
        get() = equalizer.bandLevelRange[0] to equalizer.bandLevelRange[1]

    /**
     * Set gain for the band closest to [frequency] Hz.
     * [gainDb] is in dB (e.g. -15..15), converted to millibels internally.
     */
    fun setBandGain(frequency: Int, gainDb: Short) {
        val targetBand: Short = (0 until equalizer.numberOfBands)
            .map { band -> band.toShort() to equalizer.getCenterFreq(band.toShort()) / 1000 }
            .minBy { (_, centerFreq) -> kotlin.math.abs(centerFreq - frequency) }
            .first
        val gainMb = (gainDb * 100).toShort()
        val clampedGain = gainMb.coerceIn(equalizer.bandLevelRange[0], equalizer.bandLevelRange[1])
        equalizer.setBandLevel(targetBand, clampedGain)
    }

    fun release() {
        equalizer.release()
    }
}
