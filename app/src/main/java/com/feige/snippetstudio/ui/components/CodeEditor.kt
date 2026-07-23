package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.ui.theme.*

@Composable
fun CodeEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontSp: Float,
    currentLineIndex: Int,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val gutterText = if (isDark) Text3Dark else Text3Light
    val gutterBg = if (isDark) Surface2Dark else Surface2Light
    val editorBg = if (isDark) SurfaceDark else SurfaceLight
    val highlightLineBg = if (isDark) PrimarySoft.copy(alpha = 0.15f) else PrimarySoft

    val scrollState = rememberScrollState()

    val linesCount = remember(textFieldValue.text) {
        val count = textFieldValue.text.count { it == '\n' } + 1
        maxOf(1, count)
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(editorBg)
    ) {
        // Line number Gutter
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(44.dp)
                .background(gutterBg)
                .verticalScroll(scrollState)
                .padding(vertical = Spacing.S3),
            horizontalAlignment = Alignment.End
        ) {
            for (i in 0 until linesCount) {
                val isCurrent = (i == currentLineIndex)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((fontSp * 1.6f).dp)
                        .padding(end = Spacing.S2),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "${i + 1}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = (fontSp * 0.85f).sp,
                        fontWeight = if (isCurrent) FontWeight.W800 else FontWeight.W400,
                        color = if (isCurrent) Primary else gutterText
                    )
                }
            }
        }

        // Code Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(scrollState)
                .padding(vertical = Spacing.S3, horizontal = Spacing.S3)
        ) {
            // Current line highlight background layer
            if (currentLineIndex in 0 until linesCount) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (currentLineIndex * (fontSp * 1.6f)).dp)
                        .height((fontSp * 1.6f).dp)
                        .background(highlightLineBg)
                )
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSp.sp,
                    lineHeight = (fontSp * 1.6f).sp,
                    color = textPrimary
                ),
                cursorBrush = SolidColor(Primary),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("code_editor_input")
            )
        }
    }
}
