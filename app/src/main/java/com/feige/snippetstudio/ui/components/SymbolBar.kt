package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.*

/**
 * [SymbolLanguage] 快捷符号盘语言枚举及莫兰迪色系 (Morandi Color Palette) 数据模型。
 *
 * 设计规范：
 * 采用低饱和度、灰调柔和的莫兰迪高级色彩，淡雅不突兀，在深色/浅色模式下均保持极度舒适优雅的视觉观感。
 *
 * @param displayName 语言名称 (直接展示在下拉列表中，如 "Java", "JavaScript")
 * @param badgeTag 28dp 微型图章内部的极简印记 (如 "J", "</>", "<>", "M↓")
 * @param morandiColor 莫兰迪主视觉色 (灰调低饱和)
 * @param morandiBgLight 浅色模式下的淡雅软衬底
 * @param morandiBgDark 深色模式下的淡雅软衬底
 */
enum class SymbolLanguage(
    val displayName: String,
    val badgeTag: String,
    val morandiColor: Color,
    val morandiBgLight: Color,
    val morandiBgDark: Color
) {
    JAVA(
        displayName = "Java",
        badgeTag = "J",
        morandiColor = Color(0xFFB85B56),        // 莫兰迪豆沙红
        morandiBgLight = Color(0xFFF7EBEB),
        morandiBgDark = Color(0xFF382322)
    ),
    JS(
        displayName = "JavaScript",
        badgeTag = "</>",
        morandiColor = Color(0xFFB09144),        // 莫兰迪燕麦黄
        morandiBgLight = Color(0xFFF9F5EB),
        morandiBgDark = Color(0xFF37311F)
    ),
    HTML(
        displayName = "HTML",
        badgeTag = "<>",
        morandiColor = Color(0xFFC86D51),        // 莫兰迪陶土橙
        morandiBgLight = Color(0xFFF9EDE8),
        morandiBgDark = Color(0xFF3B241C)
    ),
    MARKDOWN(
        displayName = "Markdown",
        badgeTag = "M↓",
        morandiColor = Color(0xFF4C7B9B),        // 莫兰迪雾蓝
        morandiBgLight = Color(0xFFEBF2F7),
        morandiBgDark = Color(0xFF1B2A36)
    ),
    PROMPT(
        displayName = "Prompt",
        badgeTag = "AI",
        morandiColor = Color(0xFFB55D75),        // 莫兰迪干燥玫瑰粉
        morandiBgLight = Color(0xFFF7EBF0),
        morandiBgDark = Color(0xFF382128)
    ),
    CSS(
        displayName = "CSS",
        badgeTag = "#",
        morandiColor = Color(0xFF53748B),        // 莫兰迪烟灰蓝
        morandiBgLight = Color(0xFFECEFF2),
        morandiBgDark = Color(0xFF1C2730)
    ),
    SQL(
        displayName = "SQL",
        badgeTag = "DB",
        morandiColor = Color(0xFF5A8F82),        // 莫兰迪鼠尾草绿
        morandiBgLight = Color(0xFFECF3F1),
        morandiBgDark = Color(0xFF1E302C)
    ),
    PYTHON(
        displayName = "Python",
        badgeTag = "Py",
        morandiColor = Color(0xFF4A6B82),        // 莫兰迪石墨蓝
        morandiBgLight = Color(0xFFEAF0F4),
        morandiBgDark = Color(0xFF1B2730)
    ),
    JSON(
        displayName = "JSON",
        badgeTag = "{}",
        morandiColor = Color(0xFFB87846),        // 莫兰迪暖驼色
        morandiBgLight = Color(0xFFF7F0EA),
        morandiBgDark = Color(0xFF38271B)
    );

    /**
     * 根据当前系统的深浅色模式获取莫兰迪淡雅衬底色彩。
     */
    fun getBgColor(isDark: Boolean): Color = if (isDark) morandiBgDark else morandiBgLight

    companion object {
        /**
         * 根据代码片段业务类型推断默认匹配的快捷符号盘。
         */
        fun fromSnippetType(type: SnippetType): SymbolLanguage {
            return when (type) {
                SnippetType.HTML -> HTML
                SnippetType.JS -> JS
                SnippetType.MARKDOWN -> MARKDOWN
                SnippetType.PROMPT -> PROMPT
                SnippetType.JAVA -> JAVA
                SnippetType.GENERAL -> PROMPT
            }
        }
    }
}

/**
 * [SymbolBar] 编辑器顶部代码快捷符号条组件。
 *
 * 极简极雅重构：
 * 1. 采用低饱和莫兰迪色系，去掉了箭头图标，左侧仅保留 28dp 极简淡雅图章；
 * 2. 下拉菜单去掉“快捷盘”字样，直接纯粹展示语言名称。
 *
 * @param snippetType 当前代码片段的业务类型
 * @param onInsertSymbol 点击符号按键向光标处插入字符的回调
 */
@Composable
fun SymbolBar(
    snippetType: SnippetType,
    onInsertSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    val isDark = tc.isDark
    var activeLang by remember(snippetType) { mutableStateOf(SymbolLanguage.fromSnippetType(snippetType)) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val symbols = remember(activeLang) {
        when (activeLang) {
            SymbolLanguage.HTML -> listOf("<", ">", "</", "/>", "=", "\"", "'", "<!--", "-->", "div", "span", "class=", "id=", "style=", "{", "}", "(", ")")
            SymbolLanguage.JS -> listOf("{", "}", "(", ")", "[", "]", ";", ":", "=>", "=", "==", "===", "!=", "\"", "'", "`", "const ", "let ", "function ", "console.log()")
            SymbolLanguage.MARKDOWN -> listOf("# ", "## ", "### ", "**", "*", "```", "- ", "1. ", "[", "]", "(", ")", "> ", "![", "]", "~~", "`")
            SymbolLanguage.PROMPT -> listOf("{", "}", "[", "]", "\"", "'", ":", ",", "system:", "user:", "assistant:", "# ", "->", "?", "!", "-", "(", ")")
            SymbolLanguage.JAVA -> listOf("{", "}", "(", ")", ";", "=", "==", "\"", "'", "@", ".", ",", "public ", "class ", "void ", "return ", "System.out.println()", "if ", "else ")
            SymbolLanguage.CSS -> listOf("{", "}", ":", ";", ".", "#", "px", "rem", "%", "color:", "margin:", "padding:", "/*", "*/", "!important")
            SymbolLanguage.SQL -> listOf("SELECT", "FROM", "WHERE", "AND", "OR", "INSERT", "UPDATE", "DELETE", "JOIN", "ON", "GROUP BY", "ORDER BY", "*", ";", "'")
            SymbolLanguage.PYTHON -> listOf(":", "def ", "class ", "import ", "return ", "if ", "elif ", "else:", "for ", "in ", "True", "False", "#", "\"\"", "''", "print()")
            SymbolLanguage.JSON -> listOf("{", "}", "[", "]", "\"", ":", ",", "true", "false", "null")
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(tc.surface2)
            .padding(horizontal = Spacing.S3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ===== 左侧：28dp 莫兰迪色系极简图章卡片 (去除下拉箭头) =====
        Box(modifier = Modifier.padding(end = Spacing.S2)) {
            val pillBg = activeLang.getBgColor(isDark)
            val pillColor = activeLang.morandiColor

            Surface(
                color = pillBg,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, pillColor.copy(alpha = 0.35f)),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { dropdownExpanded = true }
                    .testTag("symbol_lang_picker")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = activeLang.badgeTag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W900,
                        fontFamily = FontFamily.Monospace,
                        color = pillColor
                    )
                }
            }

            // 莫兰迪风格下拉菜单
            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                Text(
                    text = "切换快捷符号",
                    style = CaptionStyle,
                    color = tc.text3,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
                HorizontalDivider(color = tc.line, modifier = Modifier.padding(vertical = 4.dp))

                SymbolLanguage.entries.forEach { lang ->
                    val isSelected = (lang == activeLang)
                    val langBg = lang.getBgColor(isDark)
                    val langColor = lang.morandiColor

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 莫兰迪彩色小图章
                                Surface(
                                    color = langBg,
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, langColor.copy(alpha = 0.35f)),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = lang.badgeTag,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.W900,
                                            fontFamily = FontFamily.Monospace,
                                            color = langColor
                                        )
                                    }
                                }

                                // 纯语言名称展示 (如 Java, JavaScript，不含“快捷盘”字样)
                                Text(
                                    text = lang.displayName,
                                    style = CaptionStyle,
                                    color = if (isSelected) langColor else tc.text,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        modifier = Modifier.background(if (isSelected) langBg.copy(alpha = 0.6f) else Color.Transparent),
                        onClick = {
                            activeLang = lang
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        // ===== 右侧：可横向滑动的代码快捷键按钮组 =====
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.S2),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight()
        ) {
            items(symbols) { symbol ->
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(vertical = 1.dp)
                        .background(tc.surface, RoundedCornerShape(R_SM))
                        .border(1.dp, tc.line, RoundedCornerShape(R_SM))
                        .clickable { onInsertSymbol(symbol) }
                        .padding(horizontal = Spacing.S3)
                        .testTag("symbol_btn_$symbol"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = symbol,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W600,
                        color = tc.text
                    )
                }
            }
        }
    }
}
