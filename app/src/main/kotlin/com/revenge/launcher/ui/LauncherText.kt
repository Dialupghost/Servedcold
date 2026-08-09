package com.revenge.launcher.ui

import android.graphics.Typeface
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.revenge.launcher.data.FontConfig
import java.io.File

/**
 * Lightweight text renderer that supports custom font files via [FontConfig.filePath].
 * Uses a platform TextView so Typeface.createFromFile works without Compose font loading.
 */
@Composable
fun LauncherText(
    text: String,
    color: Color,
    size: TextUnit,
    fontConfig: FontConfig,
    modifier: Modifier = Modifier,
    letterSpacing: Float = 0f,
    maxLines: Int = 1,
    weight: FontWeight = FontWeight.Normal,
    alpha: Float = 1f
) {
    val typeface = remember(fontConfig.filePath) {
        fontConfig.filePath
            ?.takeIf { File(it).exists() }
            ?.let { runCatching { Typeface.createFromFile(File(it)) }.getOrNull() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                includeFontPadding = false
                isAllCaps = false
                setSingleLine(maxLines == 1)
            }
        },
        update = { tv ->
            tv.text = text
            tv.setTextColor(color.copy(alpha = alpha).toArgb())
            tv.textSize = size.value
            tv.maxLines = maxLines
            tv.letterSpacing = letterSpacing
            tv.typeface = when {
                typeface != null && weight >= FontWeight.Bold ->
                    Typeface.create(typeface, Typeface.BOLD)
                typeface != null -> typeface
                weight >= FontWeight.Bold -> Typeface.DEFAULT_BOLD
                weight >= FontWeight.Medium -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                else -> Typeface.SANS_SERIF
            }
        }
    )
}

/** Convenience overload with a default 14.sp size. */
@Composable
fun LauncherText(
    text: String,
    color: Color,
    fontConfig: FontConfig,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    letterSpacing: Float = 0f,
    maxLines: Int = 1,
    weight: FontWeight = FontWeight.Normal,
    alpha: Float = 1f
) {
    LauncherText(
        text = text,
        color = color,
        size = size,
        fontConfig = fontConfig,
        modifier = modifier,
        letterSpacing = letterSpacing,
        maxLines = maxLines,
        weight = weight,
        alpha = alpha
    )
}
