package com.feige.snippetstudio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * CompositionLocal：标记当前 UI 是否运行在深色主题下。
 * 保留向后兼容，供尚未迁移至 [LocalThemeColors] 的旧逻辑使用。
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/** CompositionLocal：当前生效的配色风格标识，供子组件查询用于选择主题化资源 */
val LocalColorThemeStyle = staticCompositionLocalOf { ColorThemeStyle.FOREST }

/**
 * [SnippetStudioTheme] 应用程序的主题根容器组件。
 *
 * 核心职责：
 * 1. 根据 [themeSetting] 判定明暗模式 (System / Light / Dark)。
 * 2. 根据 [colorThemeId] 从 [ColorThemeRegistry] 动态匹配获取对应配色风格色板。
 * 3. 组合生成 Material 3 [MaterialTheme.colorScheme] 与自定义 [LocalThemeColors] 语义色体系。
 * 4. 设置 Android 系统状态栏颜色与沉浸式图标明暗风格。
 * 5. 通过 [CompositionLocalProvider] 向下层 UI 树注入全局主题上下文。
 *
 * @param themeSetting 明暗模式配置 ("light", "dark", "system")
 * @param colorThemeId 配色风格标识 ("forest", "ocean", "sunset", "lavender", "mono")
 * @param content 包裹的 Compose UI 内容
 */
@Composable
fun SnippetStudioTheme(
    themeSetting: String = "system",
    colorThemeId: String = "forest",
    content: @Composable () -> Unit
) {
    // 判定当前是否应该启用深色渲染
    val darkTheme = when (themeSetting) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    // 根据配置查找对应风格的 ThemePalette 色板
    val style = ColorThemeStyle.fromId(colorThemeId)
    val palette = ColorThemeRegistry.paletteOf(style)

    // 构建运行时使用的 ThemeColors 动态语义色（缓存实例避免整树重组）
    val themeColors = remember(darkTheme, style) {
        if (darkTheme) {
            ThemeColors(
                primary = palette.primary,
                primary2 = palette.primary2,
                primarySoft = palette.primarySoft.copy(alpha = 0.16f),
                primaryLine = palette.primaryLine.copy(alpha = 0.3f),
                bg = palette.bgDark,
                surface = palette.surfaceDark,
                surface2 = palette.surface2Dark,
                text = palette.textDark,
                text2 = palette.text2Dark,
                text3 = palette.text3Dark,
                line = palette.lineDark,
                codeBg = palette.codeBgDark,
                codeText = palette.codeTextDark,
                isDark = true,
            )
        } else {
            ThemeColors(
                primary = palette.primary,
                primary2 = palette.primary2,
                primarySoft = palette.primarySoft,
                primaryLine = palette.primaryLine,
                bg = palette.bgLight,
                surface = palette.surfaceLight,
                surface2 = palette.surface2Light,
                text = palette.textLight,
                text2 = palette.text2Light,
                text3 = palette.text3Light,
                line = palette.lineLight,
                codeBg = palette.codeBgLight,
                codeText = palette.codeTextLight,
                isDark = false,
            )
        }
    }

    // 动态构建符合 Material 3 规范的 ColorScheme（缓存实例避免整树重组）
    val colorScheme = remember(darkTheme, style) {
        if (darkTheme) {
            darkColorScheme(
                primary = themeColors.primary,
                onPrimary = Color.White,
                primaryContainer = themeColors.primarySoft,
                onPrimaryContainer = themeColors.primary,
                secondary = themeColors.primary2,
                background = themeColors.bg,
                onBackground = themeColors.text,
                surface = themeColors.surface,
                onSurface = themeColors.text,
                surfaceVariant = themeColors.surface2,
                onSurfaceVariant = themeColors.text2,
                outline = themeColors.line,
                error = Danger,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = themeColors.primary,
                onPrimary = Color.White,
                primaryContainer = themeColors.primarySoft,
                onPrimaryContainer = themeColors.primary,
                secondary = themeColors.primary2,
                background = themeColors.bg,
                onBackground = themeColors.text,
                surface = themeColors.surface,
                onSurface = themeColors.text,
                surfaceVariant = themeColors.surface2,
                onSurfaceVariant = themeColors.text2,
                outline = themeColors.line,
                error = Danger,
                onError = Color.White
            )
        }
    }

    // 状态栏颜色同步与沉浸式处理
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // 全局注入明暗状态、配色风格标识与动态语义色
    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalColorThemeStyle provides style,
        LocalThemeColors provides themeColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = AppTypography,
            content = content
        )
    }
}
