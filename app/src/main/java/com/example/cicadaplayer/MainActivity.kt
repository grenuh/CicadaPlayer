package com.example.cicadaplayer

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.cicadaplayer.data.MusicLibrary
import com.example.cicadaplayer.data.SettingsRepository
import com.example.cicadaplayer.player.MusicPlayerController
import com.example.cicadaplayer.ui.CicadaPlayerApp
import com.example.cicadaplayer.ui.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent {
            val context = LocalContext.current
            val viewModel = remember {
                MainViewModel(
                    appContext = context.applicationContext,
                    musicLibrary = MusicLibrary(context),
                    settingsRepository = SettingsRepository(context),
                    playerController = MusicPlayerController(context)
                )
            }
            CicadaPlayerApp(viewModel)
        }
    }
}
