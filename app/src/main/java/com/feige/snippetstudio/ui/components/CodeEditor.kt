package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.SyntaxHighlighter

import androidx.compose.ui.unit.Dp

@Composable
fun CodeEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontSp: Float,
    currentLineIndex: Int,
    snippetType: SnippetType = SnippetType.HTML,
    isWordWrap: Boolean = true,
    showLineNumbers: Boolean = true,
    highlightCurrentLine: Boolean = true,
    topContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val gutterText = if (isDark) Text3Dark else Text3Light
    val gutterBg = if (isDark) Surface2Dark else Surface2Light
    val editorBg = if (isDark) SurfaceDark else SurfaceLight
    val highlightLineBg = if (isDark) PrimarySoft.copy(alpha = 0.15f) else PrimarySoft

    val syntaxTransformation = remember(snippetType, isDark) {
        VisualTransformation { text ->
            val highlighted = SyntaxHighlighter.highlight(text.text, snippetType, isDark)
            TransformedText(highlighted, OffsetMapping.Identity)
        }
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

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
        if (showLineNumbers) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(44.dp)
                    .background(gutterBg)
                    .verticalScroll(verticalScrollState)
                    .padding(top = Spacing.S3 + topContentPadding, bottom = Spacing.S3),
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
        }

        // Code Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(verticalScrollState)
                .padding(top = Spacing.S3 + topContentPadding, bottom = Spacing.S3, start = Spacing.S3, end = Spacing.S3)
        ) {
            // Current line highlight background layer
            if (highlightCurrentLine && currentLineIndex in 0 until linesCount) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (currentLineIndex * (fontSp * 1.6f)).dp)
                        .height((fontSp * 1.6f).dp)
                        .background(highlightLineBg)
                )
            }

            if (isWordWrap) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = onValueChange,
                    visualTransformation = syntaxTransformation,
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
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = onValueChange,
                        visualTransformation = syntaxTransformation,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSp.sp,
                            lineHeight = (fontSp * 1.6f).sp,
                            color = textPrimary
                        ),
                        cursorBrush = SolidColor(Primary),
                        modifier = Modifier
                            .widthIn(min = 1200.dp)
                            .fillMaxHeight()
                            .testTag("code_editor_input")
                    )
                }
            }
        }
    }
}
