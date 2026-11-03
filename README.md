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

## Credits

- **OtoKana** by [kumorinka](https://github.com/kumorinka): the original desktop app ([kumorinka/otokana](https://github.com/kumorinka/otokana)). Its design, palette, kana table, practice rules, voicebank handling and app icon are theirs; this app ports them to Android.
- **Klee One**: Copyright 2020 The Klee Project Authors ([fontworks-fonts/Klee](https://github.com/fontworks-fonts/Klee)), SIL Open Font License 1.1.
- **Zen Kaku Gothic New**: Copyright 2022 The Zen Kaku Gothic Project Authors ([googlefonts/zen-kakugothic](https://github.com/googlefonts/zen-kakugothic)), SIL Open Font License 1.1.
- **UTAU** voicebank format and **[OpenUtau](https://github.com/stakira/OpenUtau)**'s way of reading oto.ini, character.yaml, prefix.map and voice colors, which the import follows.
- **[Jetpack Compose](https://developer.android.com/jetpack/compose)**, **Material 3** and **AndroidX** (Apache License 2.0), and **[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)** (Apache License 2.0).

The font licenses are in `licenses/` and ship inside the APK under `assets/licenses/`. No voicebank or audio comes with the app: use one you have the right to use.
