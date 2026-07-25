package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.*

/**
 * [SymbolLanguage] 快捷符号栏支持的语言/模板分类。
 */
enum class SymbolLanguage(val displayName: String) {
    HTML("HTML"),
    JS("JavaScript"),
    MARKDOWN("Markdown"),
    PROMPT("Prompt"),
    CSS("CSS"),
    SQL("SQL"),
    PYTHON("Python"),
    JSON("JSON");

    companion object {
        /** 从代码片段类型映射默认初始符号语言 */
        fun fromSnippetType(type: SnippetType): SymbolLanguage {
            return when (type) {
                SnippetType.HTML -> HTML
                SnippetType.JS -> JS
                SnippetType.MARKDOWN -> MARKDOWN
                SnippetType.PROMPT -> PROMPT
            }
        }
    }
}

/**
 * [SymbolBar] 编辑器底部的快捷键盘代码符号/短语工具栏。
 *
 * 解决移动端软键盘输入代码符号（如 `<`, `>`, `{`, `}`, `=>`, `const`）不便的问题。
 * 提供支持横向滚动的快捷符号按键，以及切换语言分类的下拉选择器。
 *
 * @param snippetType 当前代码片段类型
 * @param onInsertSymbol 点击符号按键触发的插入符号回调
 */
@Composable
fun SymbolBar(
    snippetType: SnippetType,
    onInsertSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    var activeLang by remember(snippetType) { mutableStateOf(SymbolLanguage.fromSnippetType(snippetType)) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // 根据选中的语言类别动态加载对应的符号与短语列表
    val symbols = remember(activeLang) {
        when (activeLang) {
            SymbolLanguage.HTML -> listOf("<", ">", "</", "/>", "=", "\"", "'", "<!--", "-->", "div", "span", "class=", "id=", "style=", "{", "}", "(", ")")
            SymbolLanguage.JS -> listOf("{", "}", "(", ")", "[", "]", ";", ":", "=>", "=", "==", "===", "!=", "\"", "'", "`", "const ", "let ", "function ", "console.log()")
            SymbolLanguage.MARKDOWN -> listOf("# ", "## ", "### ", "**", "*", "```", "- ", "1. ", "[", "]", "(", ")", "> ", "![", "]", "~~", "`")
            SymbolLanguage.PROMPT -> listOf("{", "}", "[", "]", "\"", "'", ":", ",", "system:", "user:", "assistant:", "# ", "->", "?", "!", "-", "(", ")")
            SymbolLanguage.CSS -> listOf("{", "}", ":", ";", ".", "#", "px", "rem", "%", "color:", "margin:", "padding:", "/*", "*/", "!important")
            SymbolLanguage.SQL -> listOf("SELECT", "FROM", "WHERE", "AND", "OR", "INSERT", "UPDATE", "DELETE", "JOIN", "ON", "GROUP BY", "ORDER BY", "*", ";", "'")
            SymbolLanguage.PYTHON -> listOf(":", "def ", "class ", "import ", "return ", "if ", "elif ", "else:", "for ", "in ", "True", "False", "#", "\"\"", "''", "print()")
            SymbolLanguage.JSON -> listOf("{", "}", "[", "]", "\"", ":", ",", "true", "false", "null")
        }
    }

    val borderColor = if (isDark) LineDark else LineLight
    val btnBg = if (isDark) SurfaceDark else SurfaceLight
    val textPrimary = if (isDark) TextDark else TextLight
    val labelColor = if (isDark) Text3Dark else Text3Light

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(if (isDark) Surface2Dark else Surface2Light)
            .padding(horizontal = Spacing.S3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ===== 语言符号分类切换下拉 Chip =====
        Box(modifier = Modifier.padding(end = Spacing.S2)) {
            Surface(
                color = PrimarySoft,
                shape = RoundedCornerShape(R_SM),
                modifier = Modifier
                    .clickable { dropdownExpanded = true }
                    .testTag("symbol_lang_picker")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = activeLang.displayName,
                        style = BadgeStyle,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Select language symbols",
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                SymbolLanguage.entries.forEach { lang ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = lang.displayName,
                                style = CaptionStyle,
                                fontWeight = if (lang == activeLang) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            activeLang = lang
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        // ===== 符号按钮横向 LazyRow 列表 =====
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
                        .background(btnBg, RoundedCornerShape(R_SM))
                        .border(1.dp, borderColor, RoundedCornerShape(R_SM))
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
                        color = textPrimary
                    )
                }
            }
        }
    }
}

