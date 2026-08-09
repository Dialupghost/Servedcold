package com.revenge.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenge.launcher.ui.RevengeLauncherApp
import com.revenge.launcher.ui.theme.RevengeLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            RevengeLauncherTheme {
                val view = LocalView.current
                SideEffect {
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = false
                    WindowInsetsControllerCompat(window, view).isAppearanceLightNavigationBars = false
                }
                RevengeLauncherApp(viewModel = viewModel())
            }
        }
    }
}
