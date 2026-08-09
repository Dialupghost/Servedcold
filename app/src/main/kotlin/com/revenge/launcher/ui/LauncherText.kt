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
import androidx.compose.ui.viewinterop.AndroidView
import com.revenge.launcher.data.FontConfig
import java.io.File

@Composable
fun LauncherText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color,
    size: TextUnit,
    fontConfig: FontConfig,
    letterSpacing: Float = 0f,
    maxLines: Int = 1,
    weight: FontWeight = FontWeight.Normal,
    alpha: Float = 1f
) {
    val typeface = remember(fontConfig.filePath) {
        fontConfig.filePath
            ?.takeIf { File(it).exists() }
            ?.let { Typeface.createFromFile(File(it)) }
    }
    AndroidView(
        modifier = modifier,
        factory = { TextView(it) },
        update = {
            it.text = text
            it.setTextColor(color.copy(alpha = alpha).toArgb())
            it.textSize = size.value
            it.maxLines = maxLines
            it.letterSpacing = letterSpacing
            it.typeface = when {
                typeface != null && weight >= FontWeight.Bold -> Typeface.create(typeface, Typeface.BOLD)
                typeface != null -> typeface
                weight >= FontWeight.Bold -> Typeface.DEFAULT_BOLD
                else -> Typeface.SANS_SERIF
            }
            it.includeFontPadding = false
            it.isAllCaps = false
        }
    )
}
