package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.LocalThemeColors
import com.feige.snippetstudio.ui.theme.Spacing
import com.feige.snippetstudio.util.SyntaxHighlighter
import com.feige.snippetstudio.util.SyntaxLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

private val LineNumberGutterWidth = 48.dp
private val ScrollBeyondLastLine = 120.dp

/**
 * Code editor with a single scroll owner.  The gutter is a Canvas overlay instead of a Column of
 * Text composables, so only line numbers in the viewport are drawn even for very large files.
 */
@Deprecated(
    message = "旧版 CodeEditor 基于 BasicTextField + Canvas 渲染，容易发生大文件卡顿。已全面升级为原生 SoraCodeEditor。",
    replaceWith = ReplaceWith("SoraCodeEditor(text, onTextChange, onCursorChange, language, isDark, themeColors, fontSp)")
)
@Composable
fun CodeEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontSp: Float,
    currentLineIndex: Int,
    lineCount: Int = 1,
    snippetType: SnippetType = SnippetType.HTML,
    syntaxLanguage: SyntaxLanguage? = null,
    isWordWrap: Boolean = true,
    showLineNumbers: Boolean = true,
    highlightCurrentLine: Boolean = true,
    topContentPadding: Dp = 0.dp,
    onFontSizeChange: ((Float) -> Unit)? = null,
    fontFamily: FontFamily = FontFamily.Monospace,
    modifier: Modifier = Modifier
) {
    val colors = LocalThemeColors.current
    val density = LocalDensity.current
    val language = syntaxLanguage ?: when (snippetType) {
        SnippetType.HTML -> SyntaxLanguage.HTML
        SnippetType.JS -> SyntaxLanguage.JS
        SnippetType.MARKDOWN -> SyntaxLanguage.MARKDOWN
        SnippetType.PROMPT -> SyntaxLanguage.PROMPT
        SnippetType.JAVA -> SyntaxLanguage.JAVA
        SnippetType.GENERAL -> SyntaxLanguage.PLAIN
    }
    val contentTopPadding = Spacing.S3 + topContentPadding
    val gutterWidth = if (showLineNumbers) LineNumberGutterWidth else 0.dp

    var highlighted by remember(language) { mutableStateOf(AnnotatedString(textFieldValue.text)) }
    LaunchedEffect(textFieldValue.text, language, colors.isDark) {
        delay(150)
        highlighted = withContext(Dispatchers.Default) {
            SyntaxHighlighter.highlightByLanguage(textFieldValue.text, language, colors.isDark)
        }
    }
    val transformation = remember(highlighted) {
        VisualTransformation { source ->
            TransformedText(if (highlighted.length == source.length) highlighted else source, OffsetMapping.Identity)
        }
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    var viewportHeightPx by remember { mutableStateOf(0) }
    var internalValue by remember { mutableStateOf(textFieldValue) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnFontSizeChange by rememberUpdatedState(onFontSizeChange)
    val handleValueChange: (TextFieldValue) -> Unit = remember {
        { newValue ->
            internalValue = newValue
            latestOnValueChange(newValue)
        }
    }

    LaunchedEffect(textFieldValue.text) {
        if (internalValue.text != textFieldValue.text) internalValue = textFieldValue
    }

    // Line-start offsets are calculated off the main thread. They let the Canvas map a visible
    // visual line back to its logical number without composing every line in the document.
    var lineStarts by remember { mutableStateOf(intArrayOf(0)) }
    LaunchedEffect(internalValue.text) {
        val source = internalValue.text
        lineStarts = withContext(Dispatchers.Default) {
            IntArray(source.count { it == '\n' } + 1).also { starts ->
                var index = 1
                source.forEachIndexed { offset, char ->
                    if (char == '\n') starts[index++] = offset + 1
                }
            }
        }
    }

    val textStyle = remember(fontSp, fontFamily, colors.text) {
        TextStyle(fontFamily = fontFamily, fontSize = fontSp.sp, lineHeight = (fontSp * 1.6f).sp, color = colors.text)
    }
    val currentLineTop = remember(layoutResult, internalValue.selection, fontSp) {
        layoutResult?.let { layout ->
            if (internalValue.text.isNotEmpty()) {
                val offset = internalValue.selection.start.coerceIn(0, layout.layoutInput.text.length)
                with(density) { layout.getLineTop(layout.getLineForOffset(offset)).toDp() }
            } else 0.dp
        } ?: 0.dp
    }
    val currentLineHeight = remember(layoutResult, internalValue.selection, fontSp) {
        layoutResult?.let { layout ->
            if (internalValue.text.isNotEmpty()) {
                val line = layout.getLineForOffset(internalValue.selection.start.coerceIn(0, layout.layoutInput.text.length))
                with(density) { (layout.getLineBottom(line) - layout.getLineTop(line)).toDp() }
            } else (fontSp * 1.6f).dp
        } ?: (fontSp * 1.6f).dp
    }

    LaunchedEffect(currentLineTop, contentTopPadding) {
        val target = with(density) { (currentLineTop + contentTopPadding).toPx() }.toInt()
        if (target > verticalScroll.value + 300 || target < verticalScroll.value) {
            verticalScroll.animateScrollTo(target.coerceIn(0, verticalScroll.maxValue))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .onSizeChanged { viewportHeightPx = it.height }
            .pointerInput(Unit) {
                if (latestOnFontSizeChange != null) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (abs(zoom - 1f) > 0.03f) {
                            latestOnFontSizeChange?.invoke((zoom - 1f).coerceIn(-3f, 3f))
                        }
                    }
                }
            }
    ) {
        // This is deliberately the only verticalScroll modifier. Previously the same ScrollState
        // was attached to both the gutter and text columns, making their measured max scroll values
        // compete and causing the final gutter lines to disappear.
        Column(Modifier.fillMaxSize().verticalScroll(verticalScroll)) {
            Box(Modifier.fillMaxWidth()) {
                if (highlightCurrentLine && currentLineIndex in 0 until lineCount) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = gutterWidth)
                            .offset(y = contentTopPadding + currentLineTop)
                            .height(currentLineHeight)
                            .background(colors.primarySoft.copy(alpha = if (colors.isDark) 0.15f else 0.5f))
                    )
                }

                val textModifier = Modifier
                    .fillMaxWidth()
                    .padding(start = gutterWidth, top = contentTopPadding, end = Spacing.S3)
                    .defaultMinSize(minHeight = 300.dp)
                    .testTag("code_editor_input")

                if (isWordWrap) {
                    BasicTextField(
                        value = internalValue,
                        onValueChange = handleValueChange,
                        onTextLayout = { layoutResult = it },
                        visualTransformation = transformation,
                        textStyle = textStyle,
                        cursorBrush = SolidColor(colors.primary),
                        modifier = textModifier
                    )
                } else {
                    Box(Modifier.fillMaxWidth().horizontalScroll(horizontalScroll)) {
                        BasicTextField(
                            value = internalValue,
                            onValueChange = handleValueChange,
                            onTextLayout = { layoutResult = it },
                            visualTransformation = transformation,
                            textStyle = textStyle,
                            cursorBrush = SolidColor(colors.primary),
                            modifier = textModifier.widthIn(min = 1200.dp)
                        )
                    }
                }

                if (showLineNumbers) {
                    LineNumberGutter(
                        layoutResult = layoutResult,
                        lineStarts = lineStarts,
                        currentLineIndex = currentLineIndex,
                        scrollY = verticalScroll.value,
                        viewportHeightPx = viewportHeightPx,
                        topPadding = contentTopPadding,
                        fontSp = fontSp,
                        fontFamily = fontFamily,
                        // The overlay must match the text height, but it draws only inside the
                        // gutter width. Applying width before matchParentSize made it cover the
                        // editor and positioned labels at the right screen edge.
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
            Spacer(Modifier.height(ScrollBeyondLastLine))
        }
    }
}

@Composable
private fun LineNumberGutter(
    layoutResult: TextLayoutResult?,
    lineStarts: IntArray,
    currentLineIndex: Int,
    scrollY: Int,
    viewportHeightPx: Int,
    topPadding: Dp,
    fontSp: Float,
    fontFamily: FontFamily,
    modifier: Modifier
) {
    val colors = LocalThemeColors.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = remember(fontSp, fontFamily, colors.text3) {
        TextStyle(fontFamily = fontFamily, fontSize = (fontSp * .85f).sp, color = colors.text3)
    }
    Canvas(modifier.clipToBounds()) {
        val gutterWidthPx = LineNumberGutterWidth.toPx()
        // Canvas fills the text area's height so it can track scrolling, but never paints over it.
        drawRect(colors.surface2, size = Size(gutterWidthPx, size.height))
        val layout = layoutResult ?: return@Canvas
        val topInsetPx = with(density) { topPadding.toPx() }
        val visibleTop = (scrollY - topInsetPx).coerceAtLeast(0f)
        val visibleBottom = (scrollY + viewportHeightPx - topInsetPx).coerceAtLeast(visibleTop)
        val firstVisual = layout.getLineForVerticalPosition(visibleTop)
        val lastVisual = layout.getLineForVerticalPosition(visibleBottom)
        for (visualLine in firstVisual..lastVisual) {
            val lineStart = layout.getLineStart(visualLine)
            val logicalIndex = lineStarts.binarySearch(lineStart)
            if (logicalIndex < 0) continue // wrapped continuation: no duplicate line number
            drawLineNumber(
                text = (logicalIndex + 1).toString(),
                y = topInsetPx + layout.getLineTop(visualLine),
                isCurrent = logicalIndex == currentLineIndex,
                gutterWidthPx = gutterWidthPx,
                textMeasurer = measurer,
                normalStyle = style,
                currentColor = colors.primary
            )
        }
    }
}

private fun DrawScope.drawLineNumber(
    text: String,
    y: Float,
    isCurrent: Boolean,
    gutterWidthPx: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    normalStyle: TextStyle,
    currentColor: androidx.compose.ui.graphics.Color
) {
    val style = if (isCurrent) normalStyle.copy(color = currentColor, fontWeight = FontWeight.W800) else normalStyle
    val measured = textMeasurer.measure(text, style)
    drawText(textMeasurer, text, Offset(gutterWidthPx - measured.size.width - 8.dp.toPx(), y), style)
}
