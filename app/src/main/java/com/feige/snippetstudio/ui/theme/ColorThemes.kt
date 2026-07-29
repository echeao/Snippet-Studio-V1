package com.feige.snippetstudio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * [ColorThemeStyle] 配色风格枚举。
 * 每种风格在 [ColorThemeRegistry] 中拥有独立的 Light/Dark 双色板定义。
 *
 * @property id 持久化标识符（存入 DataStore）
 * @property displayName 界面显示名称（中文）
 * @property iconSuffix 语言类型图标资源后缀（用于选择对应主题的 Markdown/Spark 图标变体）
 */
enum class ColorThemeStyle(val id: String, val displayName: String, val iconSuffix: String) {
    /** 森林绿（默认基准配色） */
    FOREST("forest", "森林绿", "forest"),
    
    /** 海洋蓝 */
    OCEAN("ocean", "海洋蓝", "ocean"),
    
    /** 暮光橙 */
    SUNSET("sunset", "暮光橙", "sunset"),
    
    /** 薰衣草紫 */
    LAVENDER("lavender", "薰衣草紫", "lavender"),
    
    /** 极简灰 */
    MONO("mono", "极简灰", "mono"),

    /** 深夜蓝黑（AMOLED 纯黑 + VS Code 深蓝点缀） */
    MIDNIGHT("midnight", "深夜蓝黑", "mono");

    companion object {
        /**
         * 根据字符串 ID 查找对应的 [ColorThemeStyle] 实例。
         *
         * @param id 数据存储中的主题 ID
         * @return 对应的 [ColorThemeStyle] 枚举值，若未匹配到则兜底返回 [FOREST]
         */
        fun fromId(id: String): ColorThemeStyle =
            entries.find { it.id == id } ?: FOREST
    }
}

/**
 * [TypeIconPalette] 代码片段语言类型专属前景色组合。
 * 每种语言在不同配色主题下拥有协调的前景色，背景色由 TypeIcon 根据明暗模式自动派生。
 *
 * @property html HTML 图标前景色
 * @property js JavaScript 图标前景色
 * @property md Markdown 图标前景色
 * @property prompt Prompt 图标前景色
 */
data class TypeIconPalette(
    val html: Color,
    val js: Color,
    val md: Color,
    val prompt: Color,
)

/**
 * [ThemePalette] 单个配色主题的完整色板。
 * 包含品牌主色 + Light 模式 7 色 + Dark 模式 7 色 + 语言类型图标色。
 *
 * @property primary 品牌主色 (用于高亮按钮、关键指示器)
 * @property primary2 辅助主色 (用于次要高亮或渐变点缀)
 * @property primarySoft 主色柔和背景 (浅色模式下的浅色容器背景)
 * @property primaryLine 主色边界线 (描边与边框)
 * @property bgLight 浅色模式全局背景色
 * @property surfaceLight 浅色模式卡片/容器表面色
 * @property surface2Light 浅色模式次级卡片/输入框表面色
 * @property textLight 浅色模式一级文本色 (高对比度)
 * @property text2Light 浅色模式二级文本色 (中对比度)
 * @property text3Light 浅色模式三级文本色/占位符色 (低对比度)
 * @property lineLight 浅色模式分割线与边框色
 * @property bgDark 深色模式全局背景色
 * @property surfaceDark 深色模式卡片/容器表面色
 * @property surface2Dark 深色模式次级卡片/输入框表面色
 * @property textDark 深色模式一级文本色 (高对比度)
 * @property text2Dark 深色模式二级文本色 (中对比度)
 * @property text3Dark 深色模式三级文本色/占位符色 (低对比度)
 * @property lineDark 深色模式分割线与边框色
 * @property typeIcons 语言类型图标前景色组合
 * @property codeBgLight 浅色模式代码预览区背景色
 * @property codeBgDark 深色模式代码预览区背景色
 * @property codeTextLight 浅色模式代码预览区文字色
 * @property codeTextDark 深色模式代码预览区文字色
 */
data class ThemePalette(
    val primary: Color,
    val primary2: Color,
    val primarySoft: Color,
    val primaryLine: Color,
    val bgLight: Color,
    val surfaceLight: Color,
    val surface2Light: Color,
    val textLight: Color,
    val text2Light: Color,
    val text3Light: Color,
    val lineLight: Color,
    val bgDark: Color,
    val surfaceDark: Color,
    val surface2Dark: Color,
    val textDark: Color,
    val text2Dark: Color,
    val text3Dark: Color,
    val lineDark: Color,
    val typeIcons: TypeIconPalette,
    val codeBgLight: Color,
    val codeBgDark: Color,
    val codeTextLight: Color,
    val codeTextDark: Color,
)

/**
 * [ColorThemeRegistry] 全局配色主题注册表。
 * 集中管理所有可选配色方案的色板数据，方便在不同皮肤风格间快速查询与切换。
 */
object ColorThemeRegistry {

    /** 森林绿色板定义 (Forest) */
    val Forest = ThemePalette(
        primary = Color(0xFF4B635A),
        primary2 = Color(0xFF717E68),
        primarySoft = Color(0xFFE7EDDE),
        primaryLine = Color(0xFFC8D3C3),
        bgLight = Color(0xFFFDF8F3),
        surfaceLight = Color(0xFFFFFFFF),
        surface2Light = Color(0xFFF2F0E9),
        textLight = Color(0xFF1C1B1F),
        text2Light = Color(0xFF484944),
        text3Light = Color(0xFF797871),
        lineLight = Color(0xFFE5E2D9),
        bgDark = Color(0xFF171B18),
        surfaceDark = Color(0xFF202521),
        surface2Dark = Color(0xFF2B322D),
        textDark = Color(0xFFF0F2ED),
        text2Dark = Color(0xFFBCC5B8),
        text3Dark = Color(0xFF879283),
        lineDark = Color(0xFF38413B),
        typeIcons = TypeIconPalette(
            html = Color(0xFFB4533C),
            js = Color(0xFF8F752C),
            md = Color(0xFF3B6B78),
            prompt = Color(0xFF4B635A),
        ),
        codeBgLight = Color(0xFFF4F6F2),
        codeBgDark = Color(0xFF1B211D),
        codeTextLight = Color(0xFF333B35),
        codeTextDark = Color(0xFFC5D0C8),
    )

    /** 海洋蓝色板定义 (Ocean) */
    val Ocean = ThemePalette(
        primary = Color(0xFF3D6B8E),
        primary2 = Color(0xFF5E8FA8),
        primarySoft = Color(0xFFE3EFF5),
        primaryLine = Color(0xFFBDD8E5),
        bgLight = Color(0xFFF7FAFC),
        surfaceLight = Color(0xFFFFFFFF),
        surface2Light = Color(0xFFEDF3F7),
        textLight = Color(0xFF1A1D21),
        text2Light = Color(0xFF3E4A52),
        text3Light = Color(0xFF6E7B84),
        lineLight = Color(0xFFDDE5EA),
        bgDark = Color(0xFF131820),
        surfaceDark = Color(0xFF1C232D),
        surface2Dark = Color(0xFF263040),
        textDark = Color(0xFFE8EEF3),
        text2Dark = Color(0xFFAEBFCB),
        text3Dark = Color(0xFF7A8E9C),
        lineDark = Color(0xFF33414E),
        typeIcons = TypeIconPalette(
            html = Color(0xFFC4695B),
            js = Color(0xFF9A8B3E),
            md = Color(0xFF3D7A8E),
            prompt = Color(0xFF3D6B8E),
        ),
        codeBgLight = Color(0xFFF2F6F9),
        codeBgDark = Color(0xFF1A2129),
        codeTextLight = Color(0xFF2E3A42),
        codeTextDark = Color(0xFFC0D0DC),
    )

    /** 暮光橙色板定义 (Sunset) */
    val Sunset = ThemePalette(
        primary = Color(0xFFB05A3C),
        primary2 = Color(0xFFC87E5A),
        primarySoft = Color(0xFFFBEDE5),
        primaryLine = Color(0xFFEDCDBA),
        bgLight = Color(0xFFFDF9F5),
        surfaceLight = Color(0xFFFFFFFF),
        surface2Light = Color(0xFFF7F0EA),
        textLight = Color(0xFF211A16),
        text2Light = Color(0xFF4E423A),
        text3Light = Color(0xFF7D6E63),
        lineLight = Color(0xFFEAE0D7),
        bgDark = Color(0xFF1C1512),
        surfaceDark = Color(0xFF271E19),
        surface2Dark = Color(0xFF342822),
        textDark = Color(0xFFF3EDE8),
        text2Dark = Color(0xFFCBB9AC),
        text3Dark = Color(0xFF97816F),
        lineDark = Color(0xFF43342B),
        typeIcons = TypeIconPalette(
            html = Color(0xFFB05A3C),
            js = Color(0xFFC4954A),
            md = Color(0xFF7A7A5E),
            prompt = Color(0xFFC87E5A),
        ),
        codeBgLight = Color(0xFFF9F4F0),
        codeBgDark = Color(0xFF221B17),
        codeTextLight = Color(0xFF3D332C),
        codeTextDark = Color(0xFFD4C4B8),
    )

    /** 薰衣草紫色板定义 (Lavender) */
    val Lavender = ThemePalette(
        primary = Color(0xFF6B5B95),
        primary2 = Color(0xFF8B7BB0),
        primarySoft = Color(0xFFEEEAF5),
        primaryLine = Color(0xFFD4CBE8),
        bgLight = Color(0xFFFBF9FD),
        surfaceLight = Color(0xFFFFFFFF),
        surface2Light = Color(0xFFF3F0F8),
        textLight = Color(0xFF1D1A22),
        text2Light = Color(0xFF45404F),
        text3Light = Color(0xFF736E80),
        lineLight = Color(0xFFE4E0EC),
        bgDark = Color(0xFF16141C),
        surfaceDark = Color(0xFF201D28),
        surface2Dark = Color(0xFF2B2736),
        textDark = Color(0xFFEFECF5),
        text2Dark = Color(0xFFBEB6CE),
        text3Dark = Color(0xFF8A8298),
        lineDark = Color(0xFF3A3546),
        typeIcons = TypeIconPalette(
            html = Color(0xFFB0607A),
            js = Color(0xFF9A8B6B),
            md = Color(0xFF5B6B9A),
            prompt = Color(0xFF6B5B95),
        ),
        codeBgLight = Color(0xFFF5F3F9),
        codeBgDark = Color(0xFF1D1A24),
        codeTextLight = Color(0xFF353040),
        codeTextDark = Color(0xFFCCC5DA),
    )

    /** 极简灰色板定义 (Mono) */
    val Mono = ThemePalette(
        primary = Color(0xFF424242),
        primary2 = Color(0xFF6B6B6B),
        primarySoft = Color(0xFFEEEEEE),
        primaryLine = Color(0xFFD5D5D5),
        bgLight = Color(0xFFFAFAFA),
        surfaceLight = Color(0xFFFFFFFF),
        surface2Light = Color(0xFFF2F2F2),
        textLight = Color(0xFF1A1A1A),
        text2Light = Color(0xFF454545),
        text3Light = Color(0xFF757575),
        lineLight = Color(0xFFE0E0E0),
        bgDark = Color(0xFF141414),
        surfaceDark = Color(0xFF1E1E1E),
        surface2Dark = Color(0xFF2A2A2A),
        textDark = Color(0xFFF0F0F0),
        text2Dark = Color(0xFFBDBDBD),
        text3Dark = Color(0xFF8A8A8A),
        lineDark = Color(0xFF3A3A3A),
        typeIcons = TypeIconPalette(
            html = Color(0xFF757575),
            js = Color(0xFF9E9E9E),
            md = Color(0xFF424242),
            prompt = Color(0xFF616161),
        ),
        codeBgLight = Color(0xFFF5F5F5),
        codeBgDark = Color(0xFF1C1C1C),
        codeTextLight = Color(0xFF333333),
        codeTextDark = Color(0xFFCCCCCC),
    )

    /** 深夜蓝黑色板定义 (Midnight - 真·纯黑 AMOLED) */
    val Midnight = ThemePalette(
        primary = Color(0xFF007ACC),
        primary2 = Color(0xFF0098FF),
        primarySoft = Color(0xFF1E2A38),
        primaryLine = Color(0xFF005999),
        bgLight = Color(0xFFF5F7FA),
        surfaceLight = Color(0xFFFFFFFF),
        surface2Light = Color(0xFFEAEFF5),
        textLight = Color(0xFF181C20),
        text2Light = Color(0xFF404850),
        text3Light = Color(0xFF707880),
        lineLight = Color(0xFFD8E0E8),
        bgDark = Color(0xFF000000),      // 真 OLED 纯黑
        surfaceDark = Color(0xFF12151A),
        surface2Dark = Color(0xFF1A1F26),
        textDark = Color(0xFFE6EDF3),
        text2Dark = Color(0xFF8B949E),
        text3Dark = Color(0xFF6E7681),
        lineDark = Color(0xFF21262D),
        typeIcons = TypeIconPalette(
            html = Color(0xFFE44D26),
            js = Color(0xFFF7DF1E),
            md = Color(0xFF58A6FF),
            prompt = Color(0xFF007ACC),
        ),
        codeBgLight = Color(0xFFEFF2F5),
        codeBgDark = Color(0xFF0D1117),
        codeTextLight = Color(0xFF24292E),
        codeTextDark = Color(0xFFC9D1D9),
    )

    /**
     * 根据风格枚举获取对应的完整色板。
     *
     * @param style 主题风格枚举 [ColorThemeStyle]
     * @return 对应的 [ThemePalette] 实例
     */
    fun paletteOf(style: ColorThemeStyle): ThemePalette = when (style) {
        ColorThemeStyle.FOREST -> Forest
        ColorThemeStyle.OCEAN -> Ocean
        ColorThemeStyle.SUNSET -> Sunset
        ColorThemeStyle.LAVENDER -> Lavender
        ColorThemeStyle.MONO -> Mono
        ColorThemeStyle.MIDNIGHT -> Midnight
    }
}
