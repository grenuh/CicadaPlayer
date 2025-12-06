package com.example.cicadaplayer.player

import android.media.audiofx.Equalizer

class EqualizerController(sessionId: Int) {
    private val equalizer = Equalizer(0, sessionId).apply { enabled = true }

    fun setBandGain(frequency: Int, gainDb: Short) {
        val targetBand: Short = (0 until equalizer.numberOfBands)
            .map { band -> band to equalizer.getCenterFreq(band.toShort()) / 1000 }
            .minBy { (_, centerFreq) -> kotlin.math.abs(centerFreq - frequency) }
            .first
        val clampedGain = gainDb.coerceIn(equalizer.bandLevelRange[0], equalizer.bandLevelRange[1])
        equalizer.setBandLevel(targetBand, clampedGain)
    }

    fun release() {
        equalizer.release()
    }
}
