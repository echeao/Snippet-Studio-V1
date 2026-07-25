package com.feige.snippetstudio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 浅色模式 (Light Theme) 下的 Material 3 颜色方案。
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = Primary,
    secondary = Primary2,
    background = BgLight,
    onBackground = TextLight,
    surface = SurfaceLight,
    onSurface = TextLight,
    surfaceVariant = Surface2Light,
    onSurfaceVariant = Text2Light,
    outline = LineLight,
    error = Danger,
    onError = Color.White
)

/**
 * 深色模式 (Dark Theme) 下的 Material 3 颜色方案。
 */
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimarySoft.copy(alpha = 0.16f),
    onPrimaryContainer = Primary2,
    secondary = Primary2,
    background = BgDark,
    onBackground = TextDark,
    surface = SurfaceDark,
    onSurface = TextDark,
    surfaceVariant = Surface2Dark,
    onSurfaceVariant = Text2Dark,
    outline = LineDark,
    error = Danger,
    onError = Color.White
)

/**
 * CompositionLocal：标记当前 UI 是否运行在深色主题下。
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * [SnippetStudioTheme] 应用程序的主题根容器组件。
 *
 * 职责：
 * 1. 结合用户设置 (浅色/深色/跟随系统) 动态判定是否使用深色主题。
 * 2. 自动设置 Android 原生窗口状态栏 (StatusBar) 颜色与图标亮暗风格。
 * 3. 注入 Material 3 的 ColorScheme, Shapes 与 Typography。
 *
 * @param themeSetting 用户主题配置 ("light", "dark", "system")
 * @param content 包裹的 Compose UI 内容
 */
@Composable
fun SnippetStudioTheme(
    themeSetting: String = "system", // light, dark, system
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = AppTypography,
            content = content
        )
    }
}

