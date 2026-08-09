package com.revenge.launcher.data

import androidx.compose.ui.geometry.Rect
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class LayoutMode {
    VERTICAL_LIST,
    RADIAL_ORBIT,
    GRID_SNAP,
    MINIMAL_CENTER
}

@Serializable
enum class SettingsTab {
    COLORS,
    FONTS,
    LAYOUT,
    ANIMATION,
    WALLPAPER,
    GESTURES
}

@Serializable
enum class TextRole {
    CLOCK,
    PINNED,
    DRAWER,
    UI
}

@Serializable
enum class FontWeightMode {
    NORMAL,
    MEDIUM,
    BOLD
}

@Serializable
enum class PinnedItemType {
    APP,
    FOLDER
}

@Serializable
data class FontConfig(
    val role: TextRole,
    val displayName: String = "System",
    val filePath: String? = null
)

@Serializable
data class WallpaperConfig(
    val tintArgb: Long = 0x66000000,
    val opacity: Float = 0.18f,
    val showDate: Boolean = true,
    val showSeconds: Boolean = true
)

@Serializable
data class AnimationConfig(
    val springDamping: Float = 0.74f,
    val springStiffness: Float = 420f,
    val orbitSpeed: Float = 1.0f,
    val rippleStrength: Float = 0.85f
)

@Serializable
data class GestureConfig(
    val swipeUpOpensDrawer: Boolean = true,
    val longPressEditMode: Boolean = true
)

@Serializable
data class ColorTheme(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val backgroundArgb: Long,
    val primaryArgb: Long,
    val secondaryArgb: Long,
    val accentArgb: Long,
    val textArgb: Long
)

@Serializable
data class PinnedItem(
    val id: String = UUID.randomUUID().toString(),
    val type: PinnedItemType,
    val packageName: String? = null,
    val label: String,
    val children: List<FolderChild> = emptyList()
)

@Serializable
data class FolderChild(
    val packageName: String,
    val label: String
)

@Serializable
data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean = false
)

@Serializable
data class LauncherPreferences(
    val layoutMode: LayoutMode = LayoutMode.VERTICAL_LIST,
    val colorTheme: ColorTheme = ColorTheme(
        name = "Nihil Black",
        backgroundArgb = 0xFF000000,
        primaryArgb = 0xFFFFFFFF,
        secondaryArgb = 0xFF888888,
        accentArgb = 0xFF00FFAA,
        textArgb = 0xFFFFFFFF
    ),
    val fonts: List<FontConfig> = listOf(
        FontConfig(TextRole.CLOCK),
        FontConfig(TextRole.PINNED),
        FontConfig(TextRole.DRAWER),
        FontConfig(TextRole.UI)
    ),
    val wallpaper: WallpaperConfig = WallpaperConfig(),
    val animation: AnimationConfig = AnimationConfig(),
    val gestures: GestureConfig = GestureConfig(),
    val pinnedItems: List<PinnedItem> = emptyList(),
    val showClock: Boolean = true,
    val clockFormat24h: Boolean = false
)

data class DragState(
    val isDragging: Boolean = false,
    val itemId: String? = null,
    val bounds: Rect? = null
)
