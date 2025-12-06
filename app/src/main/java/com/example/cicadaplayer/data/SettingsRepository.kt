package com.example.cicadaplayer.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private const val SETTINGS_STORE = "player_settings"
private val FOLDERS_KEY = stringPreferencesKey("folders")
private val MOVE_TARGET_KEY = stringPreferencesKey("move_target")
private val VOLUME_KEY = floatPreferencesKey("volume")
private val EQ_KEY = stringPreferencesKey("equalizer")

class SettingsRepository(private val context: Context) {
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(SETTINGS_STORE) }
    )

    val settings: Flow<PlayerSettings> = dataStore.data.map { prefs ->
        PlayerSettings(
            selectedFolders = prefs[FOLDERS_KEY]?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
            moveTargetDirectory = prefs[MOVE_TARGET_KEY] ?: "",
            equalizerBands = prefs[EQ_KEY]?.takeIf { it.isNotBlank() }
                ?.split(",")
                ?.mapNotNull {
                    val parts = it.split(":")
                    parts.getOrNull(0)?.toIntOrNull()?.let { band ->
                        band to (parts.getOrNull(1)?.toShortOrNull() ?: 0)
                    }
                }
                ?.toMap() ?: PlayerSettings().equalizerBands,
            playbackVolume = prefs[VOLUME_KEY] ?: 0.5f,
        )
    }

    suspend fun updateSettings(transform: (PlayerSettings) -> PlayerSettings) {
        dataStore.edit { prefs ->
            val current = settings.firstOrNull() ?: PlayerSettings()
            val updated = transform(current)
            prefs[FOLDERS_KEY] = updated.selectedFolders.joinToString("|")
            prefs[MOVE_TARGET_KEY] = updated.moveTargetDirectory
            prefs[VOLUME_KEY] = updated.playbackVolume
            prefs[EQ_KEY] = updated.equalizerBands.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }
}
