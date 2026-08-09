# Servedcold / RevengeLauncher

Minimalist Android home launcher with maximum customization.

## Status

This repository contains the expanded source from the original `RevengeLauncher-github-upload.zip`.

**Package:** `com.revenge.launcher`  
**minSdk / targetSdk:** 28 / 35  
**UI:** Jetpack Compose + Material 3

### Features (from models)
- Layout modes: Vertical List, Radial Orbit, Grid Snap, Minimal Center
- Full color themes, custom fonts, wallpaper tint/opacity
- Animation & gesture configuration
- Pinned apps + folders with drag-and-drop
- App drawer with search
- Settings tabs for Colors / Fonts / Layout / Animation / Wallpaper / Gestures

### Project structure
```
app/
  src/main/
    kotlin/com/revenge/launcher/
      MainActivity.kt
      data/
        LauncherModels.kt
        LauncherRepository.kt
      ui/
        LauncherViewModel.kt
        LauncherText.kt
        RevengeLauncherApp.kt   ← still present in the original zip (large Compose UI)
        theme/
    res/
AndroidManifest.xml (HOME + DEFAULT categories)
```

### Build
1. Open in Android Studio
2. Let Gradle sync (wrapper jar can be regenerated with `gradle wrapper` if needed)
3. Run on a device/emulator
4. Set as default home app when prompted

### Note on RevengeLauncherApp.kt
The main Compose UI file (~59 KB / 1354 lines) remains inside the original zip for now due to size limits during automated expansion. Extract it from `RevengeLauncher-github-upload.zip` into:
`app/src/main/kotlin/com/revenge/launcher/ui/RevengeLauncherApp.kt`

The zip is kept in the repository root for convenience.
