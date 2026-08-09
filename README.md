# Servedcold / RevengeLauncher

Minimalist Android home launcher with maximum customization.

**Package:** `com.revenge.launcher`  
**Version:** 1.1.0  
**minSdk / targetSdk:** 28 / 35  
**UI:** Jetpack Compose + Material 3

## Features

- **4 layout modes** — Vertical List, Radial Orbit (animated), Grid Snap, Minimal Center
- Full color themes (background / primary / secondary / accent / line / dot)
- Per-role custom fonts (Clock, Pinned, Drawer, UI)
- Wallpaper tint, opacity, date/seconds + image / live / black actions
- Animation controls (spring damping/stiffness, orbit speed, ripple)
- Gesture options (swipe-up drawer, long-press edit mode)
- Pinned apps + folders with popup
- Searchable app drawer (long-press to pin)
- Edit mode for removing pins
- Geometric dashed frame + monochrome aesthetic

## Build & run

1. Open in Android Studio (Giraffe+ / Ladybug recommended).
2. Let Gradle sync.
3. Run on a device or emulator.
4. Set **Revenge Launcher** as the default home app when prompted.

## Structure

```
app/src/main/kotlin/com/revenge/launcher/
├── MainActivity.kt
├── data/
│   ├── LauncherModels.kt
│   └── LauncherRepository.kt
└── ui/
    ├── RevengeLauncherApp.kt
    ├── LauncherViewModel.kt
    ├── LauncherText.kt
    └── theme/
```

## Recent polish (v1.1.0)

- Fixed `LauncherText` parameter order (required args first) so all call sites compile
- Modernized `MainActivity` window insets handling
- Cleaned Gradle files (removed machine-specific path comments, bumped version, opt-in flags)
- Consistent named parameters and color helper names in the main UI
- Safer Typeface loading and single-line handling in text renderer
