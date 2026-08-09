package com.revenge.launcher.ui

import android.app.Application
import android.app.WallpaperManager
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.revenge.launcher.data.ColorTheme
import com.revenge.launcher.data.DragState
import com.revenge.launcher.data.FolderChild
import com.revenge.launcher.data.FontConfig
import com.revenge.launcher.data.InstalledApp
import com.revenge.launcher.data.LayoutMode
import com.revenge.launcher.data.LauncherPreferences
import com.revenge.launcher.data.LauncherRepository
import com.revenge.launcher.data.LauncherUiState
import com.revenge.launcher.data.PinnedItem
import com.revenge.launcher.data.PinnedItemType
import com.revenge.launcher.data.SettingsTab
import com.revenge.launcher.data.TextRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LauncherRepository(application.applicationContext)
    private val _state = MutableStateFlow(LauncherUiState())
    val state: StateFlow<LauncherUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.preferences.collect { preferences ->
                _state.update { current ->
                    current.copy(
                        preferences = preferences,
                        installedApps = if (current.installedApps.isEmpty()) repository.loadInstalledApps() else current.installedApps
                    )
                }
            }
        }
        refreshInstalledApps()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = repository.loadInstalledApps()
            _state.update { it.copy(installedApps = apps) }
        }
    }

    fun setLayoutMode(mode: LayoutMode) = updatePreferences { copy(layoutMode = mode) }

    fun toggleDrawer(visible: Boolean = !_state.value.drawerVisible) {
        _state.update { it.copy(drawerVisible = visible) }
    }

    fun setDrawerQuery(query: String) {
        _state.update { it.copy(drawerQuery = query) }
    }

    fun toggleSettings() {
        _state.update { it.copy(settingsVisible = !it.settingsVisible) }
    }

    fun setSettingsTab(tab: SettingsTab) {
        _state.update { it.copy(settingsTab = tab) }
    }

    fun pinApp(app: InstalledApp) {
        val item = PinnedItem(
            type = PinnedItemType.APP,
            packageName = app.packageName,
            activityName = app.activityName,
            label = app.label
        )
        updatePreferences { copy(pinnedItems = pinnedItems + item) }
    }

    fun removePinned(itemId: String) {
        updatePreferences {
            copy(pinnedItems = pinnedItems.filterNot { it.id == itemId })
        }
    }

    fun reorderPinned(from: Int, to: Int) {
        updatePreferences {
            val list = pinnedItems.toMutableList()
            if (from in list.indices && to in list.indices) {
                val item = list.removeAt(from)
                list.add(to, item)
            }
            copy(pinnedItems = list)
        }
    }

    fun mergeIntoFolder(sourceId: String, targetId: String) {
        updatePreferences {
            val source = pinnedItems.firstOrNull { it.id == sourceId } ?: return@updatePreferences this
            val target = pinnedItems.firstOrNull { it.id == targetId } ?: return@updatePreferences this
            if (target.type != PinnedItemType.FOLDER) {
                val folder = PinnedItem(
                    type = PinnedItemType.FOLDER,
                    label = target.label,
                    children = listOf(target.asFolderChild(), source.asFolderChild())
                )
                copy(pinnedItems = pinnedItems.filterNot { it.id == sourceId || it.id == targetId } + folder)
            } else {
                val updated = target.copy(children = target.children + source.asFolderChild())
                copy(pinnedItems = pinnedItems.filterNot { it.id == sourceId }.map { if (it.id == targetId) updated else it })
            }
        }
    }

    fun createTheme(theme: ColorTheme) {
        val normalized = repository.normalizedTheme(theme)
        updatePreferences {
            copy(
                themes = themes + normalized,
                activeThemeId = normalized.id
            )
        }
    }

    fun updateTheme(theme: ColorTheme) {
        val normalized = repository.normalizedTheme(theme)
        updatePreferences {
            copy(
                themes = themes.filterNot { it.id == normalized.id } + normalized,
                activeThemeId = normalized.id
            )
        }
    }

    fun setActiveTheme(themeId: String) = updatePreferences { copy(activeThemeId = themeId) }

    fun toggleSingleAccent(enabled: Boolean) = updatePreferences { copy(singleAccentMode = enabled) }

    fun replaceFont(role: TextRole, uri: Uri) {
        val config = repository.saveFontFromUri(uri, role) ?: return
        updatePreferences {
            copy(fonts = fonts.map { if (it.role == role) config else it })
        }
    }

    fun resetFont(role: TextRole) {
        updatePreferences {
            copy(fonts = fonts.map { if (it.role == role) FontConfig(role = role) else it })
        }
    }

    fun setWallpaperFromUri(uri: Uri): Boolean = repository.setWallpaperFromUri(uri)

    fun resetWallpaperToBlack(): Boolean = repository.resetWallpaperToBlack()

    fun openLiveWallpaperChooser() {
        val context = getApplication<Application>()
        val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun beginDrag(itemId: String, index: Int) {
        _state.update { it.copy(dragState = DragState(itemId = itemId, startIndex = index, currentIndex = index, active = true)) }
    }

    fun updateDrag(dragY: Float, currentIndex: Int) {
        _state.update { it.copy(dragState = it.dragState.copy(dragY = dragY, currentIndex = currentIndex)) }
    }

    fun endDrag(mergeTargetId: String? = null, trash: Boolean = false) {
        val drag = _state.value.dragState
        if (!drag.active) return
        when {
            trash -> removePinned(drag.itemId)
            mergeTargetId != null -> mergeIntoFolder(drag.itemId, mergeTargetId)
            drag.startIndex != drag.currentIndex && drag.currentIndex >= 0 -> reorderPinned(drag.startIndex, drag.currentIndex)
        }
        _state.update { it.copy(dragState = DragState()) }
    }

    private fun updatePreferences(transform: LauncherPreferences.() -> LauncherPreferences) {
        val updated = _state.value.preferences.transform()
        _state.update { it.copy(preferences = updated) }
        viewModelScope.launch { repository.savePreferences(updated) }
    }

    private fun PinnedItem.asFolderChild(): FolderChild = FolderChild(
        label = label,
        packageName = packageName.orEmpty(),
        activityName = activityName.orEmpty()
    )
}
