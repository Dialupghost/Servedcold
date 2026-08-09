package com.revenge.launcher.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.revenge.launcher.data.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RevengeLauncherApp(viewModel: LauncherViewModel) {
    val state by viewModel.state.collectAsState()
    val prefs = state.preferences
    val theme = prefs.activeTheme()
    val clockFont = prefs.fonts.firstOrNull { it.role == TextRole.CLOCK } ?: FontConfig(TextRole.CLOCK)
    val pinnedFont = prefs.fonts.firstOrNull { it.role == TextRole.PINNED } ?: FontConfig(TextRole.PINNED)
    val drawerFont = prefs.fonts.firstOrNull { it.role == TextRole.DRAWER } ?: FontConfig(TextRole.DRAWER)
    val uiFont = prefs.fonts.firstOrNull { it.role == TextRole.UI } ?: FontConfig(TextRole.UI)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background())
            .pointerInput(prefs.gestures.longPressEditMode) {
                detectTapGestures(onLongPress = {
                    if (prefs.gestures.longPressEditMode) viewModel.toggleEditMode()
                })
            }
    ) {
        GeometricFrame(theme)

        when (prefs.layoutMode) {
            LayoutMode.VERTICAL_LIST -> VerticalListLayout(
                items = prefs.pinnedItems, theme = theme, font = pinnedFont,
                editMode = state.editMode,
                onLaunch = { viewModel.launchPinned(it) },
                onRemove = { viewModel.removePinned(it.id) },
                onOpenFolder = { viewModel.openFolder(it.id) }
            )
            LayoutMode.RADIAL_ORBIT -> RadialOrbitLayout(
                items = prefs.pinnedItems, theme = theme, font = pinnedFont,
                orbitSpeed = prefs.animation.orbitSpeed, editMode = state.editMode,
                onLaunch = { viewModel.launchPinned(it) },
                onRemove = { viewModel.removePinned(it.id) }
            )
            LayoutMode.GRID_SNAP -> GridSnapLayout(
                items = prefs.pinnedItems, theme = theme, font = pinnedFont,
                editMode = state.editMode,
                onLaunch = { viewModel.launchPinned(it) },
                onRemove = { viewModel.removePinned(it.id) }
            )
            LayoutMode.MINIMAL_CENTER -> MinimalCenterLayout(
                items = prefs.pinnedItems, theme = theme, font = pinnedFont,
                editMode = state.editMode,
                onLaunch = { viewModel.launchPinned(it) },
                onRemove = { viewModel.removePinned(it.id) }
            )
        }

        ClockCluster(
            theme = theme, font = clockFont,
            showSeconds = prefs.wallpaper.showSeconds,
            showDate = prefs.wallpaper.showDate,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(top = 24.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GearButton(theme) { viewModel.toggleSettings() }
            SwipeHandle(theme) {
                if (prefs.gestures.swipeUpOpensDrawer) viewModel.toggleDrawer(true)
            }
            EditBadge(state.editMode, theme) { viewModel.toggleEditMode(false) }
        }

        AnimatedVisibility(
            visible = state.drawerVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            DrawerSheet(
                apps = state.installedApps.filter {
                    state.drawerQuery.isBlank() || it.label.contains(state.drawerQuery, true)
                },
                query = state.drawerQuery,
                theme = theme,
                font = drawerFont,
                onQueryChange = viewModel::setDrawerQuery,
                onAppClick = { viewModel.launchApp(it); viewModel.toggleDrawer(false) },
                onAppLongClick = { viewModel.pinApp(it); viewModel.toggleDrawer(false) },
                onDismiss = { viewModel.toggleDrawer(false) }
            )
        }

        AnimatedVisibility(
            visible = state.settingsVisible,
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut() + slideOutVertically { it / 3 }
        ) {
            SettingsOverlay(viewModel, theme, uiFont)
        }

        state.activeFolderId?.let { folderId ->
            prefs.pinnedItems.firstOrNull { it.id == folderId }?.takeIf { it.type == PinnedItemType.FOLDER }?.let { folder ->
                FolderPopup(
                    folder = folder, theme = theme, font = pinnedFont,
                    onChildClick = { viewModel.launchChild(it); viewModel.closeFolder() },
                    onDismiss = { viewModel.closeFolder() }
                )
            }
        }
    }
}

@Composable
private fun VerticalListLayout(
    items: List<PinnedItem>, theme: ColorTheme, font: FontConfig, editMode: Boolean,
    onLaunch: (PinnedItem) -> Unit, onRemove: (PinnedItem) -> Unit, onOpenFolder: (PinnedItem) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        contentPadding = PaddingValues(top = 140.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(items, key = { it.id }) { item ->
            PinnedRow(item, theme, font, editMode,
                onClick = { if (item.type == PinnedItemType.FOLDER) onOpenFolder(item) else onLaunch(item) },
                onLongClick = { if (editMode) onRemove(item) })
        }
    }
}

@Composable
private fun RadialOrbitLayout(
    items: List<PinnedItem>, theme: ColorTheme, font: FontConfig, orbitSpeed: Float, editMode: Boolean,
    onLaunch: (PinnedItem) -> Unit, onRemove: (PinnedItem) -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "orbit")
    val angle by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween((12000 / orbitSpeed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing), RepeatMode.Restart),
        label = "orbitAngle"
    )
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val radius = minOf(maxWidth, maxHeight).value * 0.32f
        items.forEachIndexed { index, item ->
            val step = 360f / items.size.coerceAtLeast(1)
            val theta = Math.toRadians((angle + index * step).toDouble())
            Box(Modifier.offset(x = (cos(theta) * radius).dp, y = (sin(theta) * radius).dp).zIndex(1f)) {
                PinnedChip(item, theme, font, editMode, { onLaunch(item) }, { if (editMode) onRemove(item) })
            }
        }
        Canvas(Modifier.size(12.dp)) { drawCircle(theme.dot(), size.minDimension / 2) }
    }
}

@Composable
private fun GridSnapLayout(
    items: List<PinnedItem>, theme: ColorTheme, font: FontConfig, editMode: Boolean,
    onLaunch: (PinnedItem) -> Unit, onRemove: (PinnedItem) -> Unit
) {
    LazyVerticalGrid(
        GridCells.Adaptive(96.dp),
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 140.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items, key = { it.id }) { item ->
            PinnedChip(item, theme, font, editMode, { onLaunch(item) }, { if (editMode) onRemove(item) })
        }
    }
}

@Composable
private fun MinimalCenterLayout(
    items: List<PinnedItem>, theme: ColorTheme, font: FontConfig, editMode: Boolean,
    onLaunch: (PinnedItem) -> Unit, onRemove: (PinnedItem) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items.take(6).forEach { item ->
            PinnedRow(item, theme, font, editMode, { onLaunch(item) }, { if (editMode) onRemove(item) }, centered = true)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedRow(
    item: PinnedItem, theme: ColorTheme, font: FontConfig, editMode: Boolean,
    onClick: () -> Unit, onLongClick: () -> Unit, centered: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).height(18.dp).background(if (item.type == PinnedItemType.FOLDER) theme.accent() else theme.line()))
        Spacer(Modifier.width(14.dp))
        LauncherText(item.label.uppercase(Locale.getDefault()), theme.primary(), 15.sp, font, letterSpacing = 0.12f, weight = FontWeight.Medium)
        if (item.type == PinnedItemType.FOLDER) {
            Spacer(Modifier.width(8.dp))
            LauncherText("${item.children.size}", theme.secondary(), 11.sp, font)
        }
        if (editMode) {
            Spacer(Modifier.weight(1f))
            LauncherText("✕", theme.secondary(), 14.sp, font)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedChip(
    item: PinnedItem, theme: ColorTheme, font: FontConfig, editMode: Boolean,
    onClick: () -> Unit, onLongClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(6.dp)) {
        Box(
            Modifier.size(48.dp).border(1.dp, theme.line(), CircleShape)
                .background(if (item.type == PinnedItemType.FOLDER) theme.accent().copy(alpha = 0.12f) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            LauncherText(item.label.take(1).uppercase(Locale.getDefault()), theme.primary(), 16.sp, font, weight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        LauncherText(item.label, theme.secondary(), 10.sp, font, maxLines = 1, letterSpacing = 0.04f)
        if (editMode) LauncherText("remove", theme.dot(), 9.sp, font)
    }
}

@Composable
private fun ClockCluster(theme: ColorTheme, font: FontConfig, showSeconds: Boolean, showDate: Boolean, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) { now = Date(); delay(if (showSeconds) 1000L else 30_000L) }
    }
    val timeFmt = SimpleDateFormat(if (showSeconds) "HH:mm:ss" else "HH:mm", Locale.getDefault())
    val dateFmt = SimpleDateFormat("EEE  d MMM", Locale.getDefault())
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LauncherText(timeFmt.format(now), theme.primary(), 42.sp, font, letterSpacing = 0.08f, weight = FontWeight.Light)
        if (showDate) {
            Spacer(Modifier.height(4.dp))
            LauncherText(dateFmt.format(now).uppercase(Locale.getDefault()), theme.secondary(), 11.sp, font, letterSpacing = 0.18f)
        }
    }
}

@Composable
private fun GeometricFrame(theme: ColorTheme) {
    Canvas(Modifier.fillMaxSize()) {
        val inset = 18.dp.toPx()
        val stroke = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f)))
        drawRect(theme.line().copy(alpha = 0.35f), Offset(inset, inset), size.copy(width = size.width - inset * 2, height = size.height - inset * 2), style = stroke)
        val tick = 12.dp.toPx()
        val c = theme.dot().copy(alpha = 0.6f)
        listOf(
            Offset(inset, inset) to listOf(Offset(inset + tick, inset), Offset(inset, inset + tick)),
            Offset(size.width - inset, inset) to listOf(Offset(size.width - inset - tick, inset), Offset(size.width - inset, inset + tick)),
            Offset(inset, size.height - inset) to listOf(Offset(inset + tick, size.height - inset), Offset(inset, size.height - inset - tick)),
            Offset(size.width - inset, size.height - inset) to listOf(Offset(size.width - inset - tick, size.height - inset), Offset(size.width - inset, size.height - inset - tick))
        ).forEach { (o, ends) -> ends.forEach { drawLine(c, o, it, 1.5.dp.toPx()) } }
    }
}

@Composable
private fun GearButton(theme: ColorTheme, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).border(1.dp, theme.line(), CircleShape).combinedClickable(onClick = onClick) {}, contentAlignment = Alignment.Center) {
        LauncherText("⚙", theme.secondary(), 16.sp, FontConfig(TextRole.UI))
    }
}

@Composable
private fun SwipeHandle(theme: ColorTheme, onSwipeUp: () -> Unit) {
    Box(
        Modifier.width(64.dp).height(28.dp).border(1.dp, theme.line(), RoundedCornerShape(14.dp))
            .pointerInput(Unit) { detectTapGestures(onTap = { onSwipeUp() }) },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.width(28.dp).height(3.dp).background(theme.secondary(), RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun EditBadge(visible: Boolean, theme: ColorTheme, onClick: () -> Unit) {
    AnimatedVisibility(visible) {
        Box(
            Modifier.border(1.dp, theme.accent(), RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
                .combinedClickable(onClick = onClick) {},
            contentAlignment = Alignment.Center
        ) {
            LauncherText("EDIT", theme.accent(), 10.sp, FontConfig(TextRole.UI), letterSpacing = 0.15f)
        }
    }
}

@Composable
private fun DrawerSheet(
    apps: List<InstalledApp>, query: String, theme: ColorTheme, font: FontConfig,
    onQueryChange: (String) -> Unit, onAppClick: (InstalledApp) -> Unit,
    onAppLongClick: (InstalledApp) -> Unit, onDismiss: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(theme.background().copy(alpha = 0.97f))) {
        Column(Modifier.fillMaxSize().padding(WindowInsets.statusBars.asPaddingValues()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = query, onValueChange = onQueryChange,
                    textStyle = TextStyle(color = theme.primary(), fontSize = 16.sp, letterSpacing = 0.06.sp),
                    cursorBrush = SolidColor(theme.accent()), singleLine = true,
                    modifier = Modifier.weight(1f).border(1.dp, theme.line(), RoundedCornerShape(2.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) LauncherText("SEARCH APPS", theme.secondary().copy(alpha = 0.5f), 13.sp, font, letterSpacing = 0.12f)
                        inner()
                    }
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier.border(1.dp, theme.line(), RoundedCornerShape(2.dp)).padding(horizontal = 12.dp, vertical = 12.dp)
                        .combinedClickable(onClick = onDismiss) {},
                    contentAlignment = Alignment.Center
                ) { LauncherText("CLOSE", theme.secondary(), 11.sp, font, letterSpacing = 0.1f) }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), contentPadding = PaddingValues(bottom = 48.dp)) {
                items(apps, key = { it.packageName + it.activityName }) { app ->
                    DrawerRow(app, theme, font, { onAppClick(app) }, { onAppLongClick(app) })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerRow(app: InstalledApp, theme: ColorTheme, font: FontConfig, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(theme.dot(), CircleShape))
        Spacer(Modifier.width(14.dp))
        LauncherText(app.label, theme.primary(), 14.sp, font, letterSpacing = 0.04f)
    }
}

@Composable
private fun FolderPopup(
    folder: PinnedItem, theme: ColorTheme, font: FontConfig,
    onChildClick: (FolderChild) -> Unit, onDismiss: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)).pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth(0.82f).border(1.dp, theme.line(), RoundedCornerShape(4.dp)).background(theme.background()).padding(20.dp)
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            LauncherText(folder.label.uppercase(Locale.getDefault()), theme.accent(), 12.sp, font, letterSpacing = 0.2f)
            Spacer(Modifier.height(16.dp))
            folder.children.forEach { child ->
                Row(
                    Modifier.fillMaxWidth().combinedClickable(onClick = { onChildClick(child) }) {}.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).background(theme.primary(), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    LauncherText(child.label, theme.primary(), 14.sp, font)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsOverlay(viewModel: LauncherViewModel, theme: ColorTheme, uiFont: FontConfig) {
    val state by viewModel.state.collectAsState()
    val prefs = state.preferences
    val tab = prefs.selectedTab
    val wallpaperPicker = rememberLauncherForActivityResult(GetContent()) { uri: Uri? -> uri?.let { viewModel.setWallpaperFromUri(it) } }

    Box(Modifier.fillMaxSize().background(theme.background().copy(alpha = 0.98f))) {
        Column(Modifier.fillMaxSize().padding(WindowInsets.statusBars.asPaddingValues()).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                LauncherText("SETTINGS", theme.primary(), 14.sp, uiFont, letterSpacing = 0.2f, weight = FontWeight.Medium)
                Box(
                    Modifier.border(1.dp, theme.line(), RoundedCornerShape(2.dp)).padding(horizontal = 12.dp, vertical = 8.dp)
                        .combinedClickable(onClick = { viewModel.toggleSettings() }) {},
                    contentAlignment = Alignment.Center
                ) { LauncherText("DONE", theme.secondary(), 11.sp, uiFont, letterSpacing = 0.1f) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsTab.entries.forEach { t ->
                    val selected = t == tab
                    Box(
                        Modifier.border(1.dp, if (selected) theme.accent() else theme.line(), RoundedCornerShape(2.dp))
                            .background(if (selected) theme.accent().copy(alpha = 0.12f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .combinedClickable(onClick = { viewModel.setSettingsTab(t) }) {},
                        contentAlignment = Alignment.Center
                    ) {
                        LauncherText(t.name, if (selected) theme.accent() else theme.secondary(), 10.sp, uiFont, letterSpacing = 0.08f)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (tab) {
                    SettingsTab.LAYOUT -> {
                        LauncherText("LAYOUT MODE", theme.secondary(), 11.sp, uiFont, letterSpacing = 0.12f)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LayoutMode.entries.forEach { mode ->
                                ToggleChip(mode.name.replace('_', ' '), prefs.layoutMode == mode, theme) { viewModel.setLayoutMode(mode) }
                            }
                        }
                    }
                    SettingsTab.COLORS -> {
                        LauncherText("ACTIVE THEME", theme.secondary(), 11.sp, uiFont, letterSpacing = 0.12f)
                        prefs.themes.forEach { t ->
                            Row(
                                Modifier.fillMaxWidth().border(1.dp, if (t.id == prefs.activeThemeId) theme.accent() else theme.line(), RoundedCornerShape(2.dp))
                                    .padding(12.dp).combinedClickable(onClick = { viewModel.setActiveTheme(t.id) }) {},
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(16.dp).background(Color(t.accentArgb.toULong()), CircleShape).border(1.dp, theme.line(), CircleShape))
                                Spacer(Modifier.width(12.dp))
                                LauncherText(t.name, theme.primary(), 13.sp, uiFont)
                            }
                        }
                        ToggleLine("SINGLE ACCENT MODE", prefs.singleAccentMode, theme) { viewModel.toggleSingleAccent(it) }
                    }
                    SettingsTab.FONTS -> {
                        TextRole.entries.forEach { role ->
                            val cfg = prefs.fonts.firstOrNull { it.role == role } ?: FontConfig(role)
                            FontRow(role, cfg, theme, {}, { viewModel.resetFont(role) })
                        }
                    }
                    SettingsTab.ANIMATION -> {
                        SliderBlock("SPRING DAMPING", prefs.animation.springDamping, 0.2f..1.2f, theme) { viewModel.updateAnimation { copy(springDamping = it) } }
                        SliderBlock("SPRING STIFFNESS", prefs.animation.springStiffness, 100f..900f, theme) { viewModel.updateAnimation { copy(springStiffness = it) } }
                        SliderBlock("ORBIT SPEED", prefs.animation.orbitSpeed, 0.2f..3f, theme) { viewModel.updateAnimation { copy(orbitSpeed = it) } }
                        SliderBlock("RIPPLE STRENGTH", prefs.animation.rippleStrength, 0f..1.5f, theme) { viewModel.updateAnimation { copy(rippleStrength = it) } }
                    }
                    SettingsTab.WALLPAPER -> {
                        SliderBlock("TINT OPACITY", prefs.wallpaper.opacity, 0f..0.8f, theme) { viewModel.updateWallpaper { copy(opacity = it) } }
                        ToggleLine("SHOW DATE", prefs.wallpaper.showDate, theme) { viewModel.updateWallpaper { copy(showDate = it) } }
                        ToggleLine("SHOW SECONDS", prefs.wallpaper.showSeconds, theme) { viewModel.updateWallpaper { copy(showSeconds = it) } }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleChip("PICK IMAGE", false, theme) { wallpaperPicker.launch("image/*") }
                            ToggleChip("BLACK", false, theme) { viewModel.resetWallpaperToBlack() }
                            ToggleChip("LIVE", false, theme) { viewModel.openLiveWallpaperChooser() }
                        }
                    }
                    SettingsTab.GESTURES -> {
                        ToggleLine("SWIPE UP OPENS DRAWER", prefs.gestures.swipeUpOpensDrawer, theme) { viewModel.updateGestures { copy(swipeUpOpensDrawer = it) } }
                        ToggleLine("LONG PRESS EDIT MODE", prefs.gestures.longPressEditMode, theme) { viewModel.updateGestures { copy(longPressEditMode = it) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontRow(role: TextRole, config: FontConfig, theme: ColorTheme, onPick: () -> Unit, onReset: () -> Unit) {
    Row(Modifier.fillMaxWidth().border(1.dp, theme.line(), RoundedCornerShape(2.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            LauncherText(role.name, theme.secondary(), 10.sp, FontConfig(TextRole.UI), letterSpacing = 0.1f)
            LauncherText(config.displayName, theme.primary(), 13.sp, config)
        }
        ToggleChip("PICK", false, theme, onPick)
        Spacer(Modifier.width(6.dp))
        ToggleChip("RST", false, theme, onReset)
    }
}

@Composable
private fun ToggleLine(text: String, enabled: Boolean, theme: ColorTheme, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = { onToggle(!enabled) }) {}.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        LauncherText(text, theme.primary(), 12.sp, FontConfig(TextRole.UI), letterSpacing = 0.06f)
        Box(
            Modifier.width(36.dp).height(18.dp).border(1.dp, if (enabled) theme.accent() else theme.line(), RoundedCornerShape(9.dp))
                .background(if (enabled) theme.accent().copy(alpha = 0.25f) else Color.Transparent, RoundedCornerShape(9.dp)),
            contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(Modifier.padding(2.dp).size(14.dp).background(if (enabled) theme.accent() else theme.secondary(), CircleShape))
        }
    }
}

@Composable
private fun SliderBlock(label: String, value: Float, range: ClosedFloatingPointRange<Float>, theme: ColorTheme, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LauncherText(label, theme.secondary(), 10.sp, FontConfig(TextRole.UI), letterSpacing = 0.1f)
            LauncherText("%.2f".format(value), theme.primary(), 11.sp, FontConfig(TextRole.UI))
        }
        Slider(value, onChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = theme.accent(), activeTrackColor = theme.accent(), inactiveTrackColor = theme.line()))
    }
}

@Composable
private fun ToggleChip(text: String, selected: Boolean, theme: ColorTheme, onClick: () -> Unit) {
    Box(
        Modifier.border(1.dp, if (selected) theme.accent() else theme.line(), RoundedCornerShape(2.dp))
            .background(if (selected) theme.accent().copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 7.dp).combinedClickable(onClick = onClick) {},
        contentAlignment = Alignment.Center
    ) {
        LauncherText(text, if (selected) theme.accent() else theme.secondary(), 10.sp, FontConfig(TextRole.UI), letterSpacing = 0.06f)
    }
}

private fun ColorTheme.background() = Color(backgroundArgb.toULong())
private fun ColorTheme.primary() = Color(primaryArgb.toULong())
private fun ColorTheme.secondary() = Color(secondaryArgb.toULong())
private fun ColorTheme.accent() = Color(accentArgb.toULong())
private fun ColorTheme.line() = Color(lineArgb.toULong())
private fun ColorTheme.dot() = Color(dotArgb.toULong())
