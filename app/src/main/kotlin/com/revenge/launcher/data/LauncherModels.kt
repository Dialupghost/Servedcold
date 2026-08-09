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
    val lineArgb: Long,
    val dotArgb: Long
) {
    companion object {
        fun monochrome() = ColorTheme(
            name = "Monochrome",
            backgroundArgb = 0xFF000000,
            primaryArgb = 0xFFF5F5F5,
            secondaryArgb = 0xFF5A5A5A,
            accentArgb = 0xFFE0E0E0,
            lineArgb = 0xFF404040,
            dotArgb = 0xFF777777
        )
    }
}

@Serializable
data class FolderChild(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val packageName: String,
    val activityName: String
)

@Serializable
data class PinnedItem(
    val id: String = UUID.randomUUID().toString(),
    val type: PinnedItemType = PinnedItemType.APP,
    val label: String,
    val packageName: String? = null,
    val activityName: String? = null,
    val children: List<FolderChild> = emptyList()
)

@Serializable
data class LauncherPreferences(
    val layoutMode: LayoutMode = LayoutMode.VERTICAL_LIST,
    val pinnedItems: List<PinnedItem> = emptyList(),
    val themes: List<ColorTheme> = listOf(ColorTheme.monochrome()),
    val activeThemeId: String = "",
    val wallpaper: WallpaperConfig = WallpaperConfig(),
    val animation: AnimationConfig = AnimationConfig(),
    val gestures: GestureConfig = GestureConfig(),
    val fonts: List<FontConfig> = TextRole.entries.map { FontConfig(role = it) },
    val selectedTab: SettingsTab = SettingsTab.COLORS,
    val singleAccentMode: Boolean = false
) {
    fun activeTheme(): ColorTheme = themes.firstOrNull { it.id == activeThemeId } ?: themes.first()
}

data class InstalledApp(
    val label: String,
    val packageName: String,
    val activityName: String
)

data class DragState(
    val itemId: String = "",
    val startIndex: Int = -1,
    val currentIndex: Int = -1,
    val dragY: Float = 0f,
    val itemBounds: Rect = Rect.Zero,
    val active: Boolean = false
)

data class LauncherUiState(
    val preferences: LauncherPreferences = LauncherPreferences(),
    val installedApps: List<InstalledApp> = emptyList(),
    val drawerQuery: String = "",
    val drawerVisible: Boolean = false,
    val settingsVisible: Boolean = false,
    val editMode: Boolean = false,
    val activeFolderId: String? = null,
    val dragState: DragState = DragState()
)
