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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.revenge.launcher.data.ColorTheme
import com.revenge.launcher.data.FolderChild
import com.revenge.launcher.data.FontConfig
import com.revenge.launcher.data.InstalledApp
import com.revenge.launcher.data.LayoutMode
import com.revenge.launcher.data.PinnedItem
import com.revenge.launcher.data.PinnedItemType
import com.revenge.launcher.data.SettingsTab
import com.revenge.launcher.data.TextRole
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
            .background(theme.bg())
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
                onLaunch = viewModel::launchPinned,
                onRemove = { viewModel.removePinned(it.id) },
                onOpenFolder = { viewModel.openFolder(it.id) }
            )
            LayoutMode.RADIAL_ORBIT -> RadialOrbitLayout(
                items = prefs.pinnedItems, theme = theme, font = pinnedFont,
                orbitSpeed = prefs.animation.orbitSpeed, editMode = state.editMode,
                onLaunch = viewModel::launchPinned,
                onRemove = { viewModel.removePinned(it.id) }
            )
            LayoutMode.GRID_SNAP -> GridSnapLayout(
                items = prefs.pinnedItems, theme = theme, font = pinnedFont,
                editMode = state.editMode,
                onLaunch = viewModel::launchPinned,
                onRemove = { viewModel.removePinned(it.id) }
            )
            LayoutMode.MINIMAL_CENTER -> MinimalCenterLayout(
                items = prefs.pinnedItems, theme = theme, font = pinnedFont,
                editMode = state.editMode,
                onLaunch = viewModel::launchPinned,
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
            ChromeButton("⚙", theme, viewModel::toggleSettings)
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
                query = state.drawerQuery, theme = theme, font = drawerFont,
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
            prefs.pinnedItems.firstOrNull { it.id == folderId && it.type == PinnedItemType.FOLDER }?.let { folder ->
                FolderPopup(
                    folder, theme, pinnedFont,
                    onChildClick = { viewModel.launchChild(it); viewModel.closeFolder() },
                    onDismiss = viewModel::closeFolder
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
        infiniteRepeatable(
            tween((12_000 / orbitSpeed.coerceIn(0.2f, 4f)).toInt(), easing = LinearEasing),
            RepeatMode.Restart
        ),
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
        Canvas(Modifier.size(10.dp)) { drawCircle(theme.dot(), size.minDimension / 2f) }
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
            Spacer(Modifier.height(8.dp))
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
        Box(
            Modifier.width(3.dp).height(18.dp)
                .background(if (item.type == PinnedItemType.FOLDER) theme.accent() else theme.line())
        )
        Spacer(Modifier.width(14.dp))
        LauncherText(
            text = item.label.uppercase(Locale.getDefault()),
            color = theme.primary(),
            size = 15.sp,
            fontConfig = font,
            letterSpacing = 0.12f,
            weight = FontWeight.Medium
        )
        if (item.type == PinnedItemType.FOLDER) {
            Spacer(Modifier.width(8.dp))
            LauncherText(text = "${item.children.size}", color = theme.secondary(), size = 11.sp, fontConfig = font)
        }
        if (editMode) {
            Spacer(Modifier.weight(1f))
            LauncherText(text = "✕", color = theme.secondary(), size = 14.sp, fontConfig = font)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedChip(
    item: PinnedItem, theme: ColorTheme, font: FontConfig, editMode: Boolean,
    onClick: () -> Unit, onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(6.dp)
    ) {
        Box(
            Modifier.size(48.dp).border(1.dp, theme.line(), CircleShape)
                .background(
                    if (item.type == PinnedItemType.FOLDER) theme.accent().copy(alpha = 0.12f)
                    else Color.Transparent, CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            LauncherText(
                text = item.label.take(1).uppercase(Locale.getDefault()),
                color = theme.primary(), size = 16.sp, fontConfig = font, weight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        LauncherText(
            text = item.label, color = theme.secondary(), size = 10.sp,
            fontConfig = font, maxLines = 1, letterSpacing = 0.04f
        )
        if (editMode) {
            LauncherText(text = "remove", color = theme.dot(), size = 9.sp, fontConfig = font)
        }
    }
}

@Composable
private fun ClockCluster(
    theme: ColorTheme, font: FontConfig, showSeconds: Boolean, showDate: Boolean,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(showSeconds) {
        while (true) {
            now = Date()
            delay(if (showSeconds) 1_000L else 30_000L)
        }
    }
    val timeFmt = remember(showSeconds) {
        SimpleDateFormat(if (showSeconds) "HH:mm:ss" else "HH:mm", Locale.getDefault())
    }
    val dateFmt = remember { SimpleDateFormat("EEE  d MMM", Locale.getDefault()) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LauncherText(
            text = timeFmt.format(now), color = theme.primary(), size = 42.sp,
            fontConfig = font, letterSpacing = 0.08f, weight = FontWeight.Light
        )
        if (showDate) {
            Spacer(Modifier.height(4.dp))
            LauncherText(
                text = dateFmt.format(now).uppercase(Locale.getDefault()),
                color = theme.secondary(), size = 11.sp, fontConfig = font, letterSpacing = 0.18f
            )
        }
    }
}

@Composable
private fun GeometricFrame(theme: ColorTheme) {
    Canvas(Modifier.fillMaxSize()) {
        val inset = 18.dp.toPx()
        val stroke = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f)))
        drawRect(
            theme.line().copy(alpha = 0.35f),
            Offset(inset, inset),
            size.copy(width = size.width - inset * 2, height = size.height - inset * 2),
            style = stroke
        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChromeButton(label: String, theme: ColorTheme, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).border(1.dp, theme.line(), CircleShape).combinedClickable(onClick = onClick) {},
        contentAlignment = Alignment.Center
    ) {
        LauncherText(text = label, color = theme.secondary(), size = 16.sp, fontConfig = FontConfig(TextRole.UI))
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
            Modifier.border(1.dp, theme.accent(), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .combinedClickable(onClick = onClick) {},
            contentAlignment = Alignment.Center
        ) {
            LauncherText(
                text = "EDIT", color = theme.accent(), size = 10.sp,
                fontConfig = FontConfig(TextRole.UI), letterSpacing = 0.15f
            )
        }
    }
}

@Composable
private fun DrawerSheet(
    apps: List<InstalledApp>, query: String, theme: ColorTheme, font: FontConfig,
    onQueryChange: (String) -> Unit, onAppClick: (InstalledApp) -> Unit,
    onAppLongClick: (InstalledApp) -> Unit, onDismiss: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(theme.bg().copy(alpha = 0.97f))) {
        Column(
            Modifier.fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = query, onValueChange = onQueryChange,
                    textStyle = TextStyle(color = theme.primary(), fontSize = 16.sp, letterSpacing = 0.06.sp),
                    cursorBrush = SolidColor(theme.accent()), singleLine = true,
                    modifier = Modifier.weight(1f).border(1.dp, theme.line(), RoundedCornerShape(2.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                LauncherText(
                                    text = "SEARCH APPS",
                                    color = theme.secondary().copy(alpha = 0.5f),
                                    size = 13.sp, fontConfig = font, letterSpacing = 0.12f
                                )
                            }
                            inner()
                        }
                    }
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier.border(1.dp, theme.line(), RoundedCornerShape(2.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .combinedClickable(onClick = onDismiss) {},
                    contentAlignment = Alignment.Center
                ) {
                    LauncherText(text = "CLOSE", color = theme.secondary(), size = 11.sp, fontConfig = font, letterSpacing = 0.1f)
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                items(apps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                    DrawerRow(app, theme, font, { onAppClick(app) }, { onAppLongClick(app) })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerRow(
    app: InstalledApp, theme: ColorTheme, font: FontConfig,
    onClick: () -> Unit, onLongClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(theme.dot(), CircleShape))
        Spacer(Modifier.width(14.dp))
        LauncherText(text = app.label, color = theme.primary(), size = 14.sp, fontConfig = font, letterSpacing = 0.04f)
    }
}

@Composable
private fun FolderPopup(
    folder: PinnedItem, theme: ColorTheme, font: FontConfig,
    onChildClick: (FolderChild) -> Unit, onDismiss: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth(0.82f).border(1.dp, theme.line(), RoundedCornerShape(4.dp))
                .background(theme.bg()).padding(20.dp)
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            LauncherText(
                text = folder.label.uppercase(Locale.getDefault()),
                color = theme.accent(), size = 12.sp, fontConfig = font, letterSpacing = 0.2f
            )
            Spacer(Modifier.height(16.dp))
            folder.children.forEach { child ->
                Row(
                    Modifier.fillMaxWidth().combinedClickable(onClick = { onChildClick(child) }) {}
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).background(theme.primary(), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    LauncherText(text = child.label, color = theme.primary(), size = 14.sp, fontConfig = font)
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
    val wallpaperPicker = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
        uri?.let { viewModel.setWallpaperFromUri(it) }
    }

    Box(Modifier.fillMaxSize().background(theme.bg().copy(alpha = 0.98f))) {
        Column(
            Modifier.fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LauncherText(
                    text = "SETTINGS", color = theme.primary(), size = 14.sp,
                    fontConfig = uiFont, letterSpacing = 0.2f, weight = FontWeight.Medium
                )
                Box(
                    Modifier.border(1.dp, theme.line(), RoundedCornerShape(2.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .combinedClickable(onClick = viewModel::toggleSettings) {},
                    contentAlignment = Alignment.Center
                ) {
                    LauncherText(text = "DONE", color = theme.secondary(), size = 11.sp, fontConfig = uiFont, letterSpacing = 0.1f)
                }
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsTab.entries.forEach { t ->
                    val selected = t == tab
                    Box(
                        Modifier.border(1.dp, if (selected) theme.accent() else theme.line(), RoundedCornerShape(2.dp))
                            .background(if (selected) theme.accent().copy(alpha = 0.12f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .combinedClickable(onClick = { viewModel.setSettingsTab(t) }) {},
                        contentAlignment = Alignment.Center
                    ) {
                        LauncherText(
                            text = t.name,
                            color = if (selected) theme.accent() else theme.secondary(),
                            size = 10.sp, fontConfig = uiFont, letterSpacing = 0.08f
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (tab) {
                    SettingsTab.LAYOUT -> {
                        LauncherText(text = "LAYOUT MODE", color = theme.secondary(), size = 11.sp, fontConfig = uiFont, letterSpacing = 0.12f)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LayoutMode.entries.forEach { mode ->
                                ToggleChip(
                                    mode.name.replace('_', ' '),
                                    prefs.layoutMode == mode,
                                    theme
                                ) { viewModel.setLayoutMode(mode) }
                            }
                        }
                    }
                    SettingsTab.COLORS -> {
                        LauncherText(text = "ACTIVE THEME", color = theme.secondary(), size = 11.sp, fontConfig = uiFont, letterSpacing = 0.12f)
                        prefs.themes.forEach { t ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (t.id == prefs.activeThemeId) theme.accent() else theme.line(),
                                        RoundedCornerShape(2.dp)
                                    )
                                    .padding(12.dp)
                                    .combinedClickable(onClick = { viewModel.setActiveTheme(t.id) }) {},
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(16.dp)
                                        .background(Color(t.accentArgb.toULong()), CircleShape)
                                        .border(1.dp, theme.line(), CircleShape)
                                )
                                Spacer(Modifier.width(12.dp))
                                LauncherText(text = t.name, color = theme.primary(), size = 13.sp, fontConfig = uiFont)
                            }
                        }
                        ToggleLine("SINGLE ACCENT MODE", prefs.singleAccentMode, theme, viewModel::toggleSingleAccent)
                    }
                    SettingsTab.FONTS -> {
                        TextRole.entries.forEach { role ->
                            val cfg = prefs.fonts.firstOrNull { it.role == role } ?: FontConfig(role)
                            FontRow(role, cfg, theme) { viewModel.resetFont(role) }
                        }
                    }
                    SettingsTab.ANIMATION -> {
                        SliderBlock("SPRING DAMPING", prefs.animation.springDamping, 0.2f..1.2f, theme) {
                            viewModel.updateAnimation { copy(springDamping = it) }
                        }
                        SliderBlock("SPRING STIFFNESS", prefs.animation.springStiffness, 100f..900f, theme) {
                            viewModel.updateAnimation { copy(springStiffness = it) }
                        }
                        SliderBlock("ORBIT SPEED", prefs.animation.orbitSpeed, 0.2f..3f, theme) {
                            viewModel.updateAnimation { copy(orbitSpeed = it) }
                        }
                        SliderBlock("RIPPLE STRENGTH", prefs.animation.rippleStrength, 0f..1.5f, theme) {
                            viewModel.updateAnimation { copy(rippleStrength = it) }
                        }
                    }
                    SettingsTab.WALLPAPER -> {
                        SliderBlock("TINT OPACITY", prefs.wallpaper.opacity, 0f..0.8f, theme) {
                            viewModel.updateWallpaper { copy(opacity = it) }
                        }
                        ToggleLine("SHOW DATE", prefs.wallpaper.showDate, theme) {
                            viewModel.updateWallpaper { copy(showDate = it) }
                        }
                        ToggleLine("SHOW SECONDS", prefs.wallpaper.showSeconds, theme) {
                            viewModel.updateWallpaper { copy(showSeconds = it) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleChip("PICK IMAGE", false, theme) { wallpaperPicker.launch("image/*") }
                            ToggleChip("BLACK", false, theme) { viewModel.resetWallpaperToBlack() }
                            ToggleChip("LIVE", false, theme) { viewModel.openLiveWallpaperChooser() }
                        }
                    }
                    SettingsTab.GESTURES -> {
                        ToggleLine("SWIPE UP OPENS DRAWER", prefs.gestures.swipeUpOpensDrawer, theme) {
                            viewModel.updateGestures { copy(swipeUpOpensDrawer = it) }
                        }
                        ToggleLine("LONG PRESS EDIT MODE", prefs.gestures.longPressEditMode, theme) {
                            viewModel.updateGestures { copy(longPressEditMode = it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontRow(role: TextRole, config: FontConfig, theme: ColorTheme, onReset: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().border(1.dp, theme.line(), RoundedCornerShape(2.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            LauncherText(text = role.name, color = theme.secondary(), size = 10.sp, fontConfig = FontConfig(TextRole.UI), letterSpacing = 0.1f)
            LauncherText(text = config.displayName, color = theme.primary(), size = 13.sp, fontConfig = config)
        }
        ToggleChip("RESET", false, theme, onReset)
    }
}

@Composable
private fun ToggleLine(text: String, enabled: Boolean, theme: ColorTheme, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = { onToggle(!enabled) }) {}.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LauncherText(text = text, color = theme.primary(), size = 12.sp, fontConfig = FontConfig(TextRole.UI), letterSpacing = 0.06f)
        Box(
            Modifier.width(36.dp).height(18.dp)
                .border(1.dp, if (enabled) theme.accent() else theme.line(), RoundedCornerShape(9.dp))
                .background(
                    if (enabled) theme.accent().copy(alpha = 0.25f) else Color.Transparent,
                    RoundedCornerShape(9.dp)
                ),
            contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                Modifier.padding(2.dp).size(14.dp)
                    .background(if (enabled) theme.accent() else theme.secondary(), CircleShape)
            )
        }
    }
}

@Composable
private fun SliderBlock(
    label: String, value: Float, range: ClosedFloatingPointRange<Float>,
    theme: ColorTheme, onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LauncherText(text = label, color = theme.secondary(), size = 10.sp, fontConfig = FontConfig(TextRole.UI), letterSpacing = 0.1f)
            LauncherText(text = "%.2f".format(value), color = theme.primary(), size = 11.sp, fontConfig = FontConfig(TextRole.UI))
        }
        Slider(
            value, onChange, valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = theme.accent(),
                activeTrackColor = theme.accent(),
                inactiveTrackColor = theme.line()
            )
        )
    }
}

@Composable
private fun ToggleChip(text: String, selected: Boolean, theme: ColorTheme, onClick: () -> Unit) {
    Box(
        Modifier.border(1.dp, if (selected) theme.accent() else theme.line(), RoundedCornerShape(2.dp))
            .background(if (selected) theme.accent().copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .combinedClickable(onClick = onClick) {},
        contentAlignment = Alignment.Center
    ) {
        LauncherText(
            text = text,
            color = if (selected) theme.accent() else theme.secondary(),
            size = 10.sp, fontConfig = FontConfig(TextRole.UI), letterSpacing = 0.06f
        )
    }
}

private fun ColorTheme.bg() = Color(backgroundArgb.toULong())
private fun ColorTheme.primary() = Color(primaryArgb.toULong())
private fun ColorTheme.secondary() = Color(secondaryArgb.toULong())
private fun ColorTheme.accent() = Color(accentArgb.toULong())
private fun ColorTheme.line() = Color(lineArgb.toULong())
private fun ColorTheme.dot() = Color(dotArgb.toULong())
