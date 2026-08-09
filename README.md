# Servedcold / RevengeLauncher

Minimalist Android home launcher with maximum customization.

**Package:** `com.revenge.launcher`  
**minSdk / targetSdk:** 28 / 35  
**UI:** Jetpack Compose + Material 3  
**Status:** Fully expanded and improved from the original zip.

## Features

- **4 layout modes**
  - Vertical List
  - Radial Orbit (animated)
  - Grid Snap
  - Minimal Center
- Full color themes (background / primary / secondary / accent / line / dot)
- Per-role custom fonts (Clock, Pinned, Drawer, UI)
- Wallpaper tint, opacity, date/seconds toggles + image / live wallpaper pickers
- Animation controls (spring damping/stiffness, orbit speed, ripple)
- Gesture options (swipe-up drawer, long-press edit mode)
- Pinned apps + folders with open/close popup
- Searchable app drawer (long-press to pin)
- Edit mode for removing pins
- Geometric dashed frame + monochrome aesthetic

## Project structure

```
app/src/main/kotlin/com/revenge/launcher/
├── MainActivity.kt
├── data/
│   ├── LauncherModels.kt
│   └── LauncherRepository.kt
└── ui/
    ├── RevengeLauncherApp.kt      ← full Compose UI
    ├── LauncherViewModel.kt       ← improved API
    ├── LauncherText.kt
    └── theme/
```

## Build & run

1. Open the project in Android Studio.
2. Let Gradle sync (if the wrapper jar is missing, run `gradle wrapper` once).
3. Deploy to a device or emulator.
4. Set **Revenge Launcher** as the default home app when prompted.

## Notes

- Original zip is still present in the repo for reference.
- The ViewModel and main UI have been rewritten for clarity, consistency with the data models, and completeness of features.
