package com.example.cicadaplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EqualizerScreen(
    bands: Map<Int, Short>,
    onEqualizerChange: (Int, Short) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Equalizer", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bands.forEach { (freq, gain) ->
                    EqualizerSlider(frequency = freq, gain = gain, onEqualizerChange = onEqualizerChange)
                }
            }
        }
    }
}

@Composable
fun EqualizerSlider(frequency: Int, gain: Short, onEqualizerChange: (Int, Short) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${frequency}Hz : ${gain}dB")
        Slider(
            value = gain.toFloat(),
            onValueChange = { onEqualizerChange(frequency, it.toInt().toShort()) },
            valueRange = -15f..15f
        )
    }
}
