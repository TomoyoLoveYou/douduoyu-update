# DouDouYu Android

This repo is now an Android-only APK project built with Kotlin and Jetpack Compose.

Current scope:

- Local image import
- Native crop flow
- Bead-pattern generation adapted from `Jett-Wu/Perler_Beads_Generator`
- Native pattern editor with pencil, eraser, fill, remove, recolor, picker, and mirror tools
- Usage summary, isolated bead detection, and local project storage

## Build debug APK

From `douduoyu-app`:

```powershell
.\build-android-debug.ps1
```

Or directly:

```powershell
cd .\android
.\gradlew.bat assembleDebug
```

Output APK:

`android/app/build/outputs/apk/debug/app-debug.apk`

## Run unit tests

```powershell
cd .\android
.\gradlew.bat testDebugUnitTest
```

## Main folders

- `android/`: the only runtime project kept for this repo
- `THIRD_PARTY_NOTICES.md`: upstream open-source attribution
