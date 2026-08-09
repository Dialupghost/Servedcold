package com.revenge.launcher.data

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

private val Context.dataStore by preferencesDataStore(name = "revenge_launcher_prefs")

class LauncherRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val preferencesKey = stringPreferencesKey("launcher_preferences")

    val preferences: Flow<LauncherPreferences> = context.dataStore.data.map { prefs ->
        prefs[preferencesKey]?.let {
            runCatching { json.decodeFromString<LauncherPreferences>(it) }.getOrNull()
        } ?: LauncherPreferences()
    }

    suspend fun savePreferences(preferences: LauncherPreferences) {
        context.dataStore.edit { prefs ->
            prefs[preferencesKey] = json.encodeToString(preferences.normalized())
        }
    }

    fun loadInstalledApps(): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return activities
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                if (activityInfo.packageName == context.packageName) return@mapNotNull null
                InstalledApp(
                    label = resolveInfo.loadLabel(pm)?.toString().orEmpty().ifBlank { activityInfo.packageName },
                    packageName = activityInfo.packageName,
                    activityName = activityInfo.name
                )
            }
            .distinctBy { it.packageName to it.activityName }
            .sortedBy { it.label.lowercase() }
    }

    fun launch(item: PinnedItem) {
        if (item.type == PinnedItemType.FOLDER) return
        launchComponent(item.packageName ?: return, item.activityName ?: return)
    }

    fun launch(child: FolderChild) {
        launchComponent(child.packageName, child.activityName)
    }

    private fun launchComponent(packageName: String, activityName: String) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(packageName, activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        runCatching { context.startActivity(intent) }
    }

    fun saveFontFromUri(uri: Uri, role: TextRole): FontConfig? {
        val name = queryFileName(uri).ifBlank { "${role.name.lowercase()}.font" }
        val target = File(context.filesDir, "fonts/${role.name.lowercase()}-$name")
        target.parentFile?.mkdirs()
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            FontConfig(role = role, displayName = name, filePath = target.absolutePath)
        }.getOrNull()
    }

    private fun queryFileName(uri: Uri): String {
        var fileName = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex("_display_name")
            if (index >= 0 && cursor.moveToFirst()) fileName = cursor.getString(index)
        }
        return fileName
    }

    fun setWallpaperFromUri(uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                WallpaperManager.getInstance(context).setBitmap(bitmap)
            }
        }.isSuccess
    }

    fun resetWallpaperToBlack(): Boolean {
        return runCatching {
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawColor(Color.BLACK)
            WallpaperManager.getInstance(context).setBitmap(bitmap)
        }.isSuccess
    }

    fun normalizedTheme(theme: ColorTheme): ColorTheme = theme.copy(name = theme.name.ifBlank { "Custom" })
}

private fun LauncherPreferences.normalized(): LauncherPreferences {
    val resolvedThemes = if (themes.isEmpty()) listOf(ColorTheme.monochrome()) else themes
    val resolvedActive = activeThemeId.ifBlank { resolvedThemes.first().id }
    return copy(
        themes = resolvedThemes,
        activeThemeId = resolvedActive,
        fonts = TextRole.entries.map { role -> fonts.firstOrNull { it.role == role } ?: FontConfig(role = role) }
    )
}
