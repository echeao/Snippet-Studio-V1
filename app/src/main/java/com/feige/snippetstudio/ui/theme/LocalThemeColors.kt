package com.feige.snippetstudio.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * [ThemeColors] 动态语义色集合。
 *
 * 随「配色风格 + 明暗模式」双维度配置实时变化。
 * UI 组件层可以通过 [LocalThemeColors.current] 快捷访问当前环境生效的语义色，
 * 彻底消除组件内部编写大量 `if (isDark) XxxDark else XxxLight` 硬编码。
 *
 * @property primary 品牌主色
 * @property primary2 辅助主色
 * @property primarySoft 主色柔和容器色
 * @property primaryLine 主色边界与分割线
 * @property bg 全局背景色
 * @property surface 卡片/容器表面色
 * @property surface2 次级卡片/输入框表面色
 * @property text 一级主要文本色 (对比度最高)
 * @property text2 二级次要文本色
 * @property text3 三级说明/占位符文本色
 * @property line 通用描边与分割线颜色
 * @property codeBg 代码预览区背景色（随明暗模式自动切换）
 * @property codeText 代码预览区文字色（随明暗模式自动切换）
 * @property isDark 当前主题是否运行在深色模式
 */
@Immutable
data class ThemeColors(
    val primary: Color,
    val primary2: Color,
    val primarySoft: Color,
    val primaryLine: Color,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val line: Color,
    val codeBg: Color,
    val codeText: Color,
    val isDark: Boolean,
)

/**
 * [LocalThemeColors] 全局动态语义色 CompositionLocal 提供器。
 * 默认初始值为森林绿浅色色板 (Forest Light)，与 AppSettings 的默认配置匹配。
 */
val LocalThemeColors = staticCompositionLocalOf {
    ThemeColors(
        primary = Color(0xFF4B635A),
        primary2 = Color(0xFF717E68),
        primarySoft = Color(0xFFE7EDDE),
        primaryLine = Color(0xFFC8D3C3),
        bg = Color(0xFFFDF8F3),
        surface = Color(0xFFFFFFFF),
        surface2 = Color(0xFFF2F0E9),
        text = Color(0xFF1C1B1F),
        text2 = Color(0xFF484944),
        text3 = Color(0xFF797871),
        line = Color(0xFFE5E2D9),
        codeBg = Color(0xFFF4F6F2),
        codeText = Color(0xFF333B35),
        isDark = false,
    )
}
