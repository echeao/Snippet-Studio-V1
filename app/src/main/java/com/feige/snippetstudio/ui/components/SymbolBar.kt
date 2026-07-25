package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.*

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

@Composable
fun SymbolBar(
    snippetType: SnippetType,
    onInsertSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    var activeLang by remember(snippetType) { mutableStateOf(SymbolLanguage.fromSnippetType(snippetType)) }
    var dropdownExpanded by remember { mutableStateOf(false) }

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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(tc.surface2)
            .padding(horizontal = Spacing.S3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.padding(end = Spacing.S2)) {
            Surface(
                color = tc.primarySoft,
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
                        color = tc.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                        contentDescription = "Select language symbols",
                        tint = tc.primary,
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
