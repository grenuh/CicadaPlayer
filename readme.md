# Cicada Player

A lightweight Android music player focused on rapid playlist curation with two prominent **F** and **D** actions for quickly discarding tracks.

## Features
- Reads music from user-supplied folders and builds an in-app playlist.
- Standard transport controls (play/pause, previous/next) with a seek slider and current position readout.
- Volume slider persisted in settings.
- Equalizer controls mapped to common frequency bands.
- Tag reading via `MediaMetadataRetriever` for title, artist, album, and duration.
- Playlist tools to remove the current song (F) or move it to a specified directory before skipping (D).
- Folder and move-target settings stored with Jetpack DataStore.

## Project layout
- `app/src/main/java/com/example/cicadaplayer/MainActivity.kt` — Compose UI and screen layout.
- `app/src/main/java/com/example/cicadaplayer/ui/MainViewModel.kt` — UI logic, playlist orchestration, and settings updates.
- `app/src/main/java/com/example/cicadaplayer/player/MusicPlayerController.kt` — ExoPlayer wrapper for playback control.
- `app/src/main/java/com/example/cicadaplayer/player/EqualizerController.kt` — Basic equalizer band mapping.
- `app/src/main/java/com/example/cicadaplayer/data/MusicLibrary.kt` — Folder scanning, tag extraction, and file-moving helpers.
- `app/src/main/java/com/example/cicadaplayer/data/SettingsRepository.kt` — DataStore-backed settings.

## Running
Open the project in Android Studio (Giraffe or newer). The app targets SDK 34 with Compose UI and Media3 playback.
