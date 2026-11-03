# OtoKana for Android

An Android port of [OtoKana](https://github.com/kumorinka/otokana), for practicing hiragana and katakana: type the romaji of each kana that shows up. With a UTAU voicebank, each kana is also sung. Built with Kotlin, Jetpack Compose and Material 3.

## Build

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # core, voicebank and store tests
```

## Layout

| Package | Go original | |
|---|---|---|
| `core` | `internal/kana`, `deck`, `session`, `matcher`, `notes`, `format` | pure Kotlin |
| `audio` | `internal/audio` | WAV read/write, resample, pitch shift; plays through `AudioTrack` |
| `voicebank` | `internal/voicebank` | oto.ini, character.txt/yaml, prefix.map, zip/folder sources (folders through the Storage Access Framework) |
| `store` | `internal/store` | `settings.json`, `history.json`, `voicebanks/<id>/` in the app's files dir |
| `ui` | `view` | Compose screens: Setup, Play, Results, Settings, Import |

## Fonts

Klee One and Zen Kaku Gothic New, under the SIL Open Font License: see `licenses/`.
