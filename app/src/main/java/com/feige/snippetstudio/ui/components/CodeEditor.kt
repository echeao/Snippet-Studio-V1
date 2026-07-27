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
import com.feige.snippetstudio.util.SyntaxLanguage
import com.feige.snippetstudio.util.SyntaxLanguageDetector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures

/**
 * [CodeEditor] 是专为 Snippet Studio 设计的现代 Jetpack Compose 代码编辑器核心 UI 组件。
 *
 * 核心功能与架构特性：
 * 1. **实时语法高亮**：通过 [VisualTransformation] 与 [SyntaxHighlighter] 将纯文本渲染为具有不同色彩的富文本 [AnnotatedString]。
 * 2. **左侧行号装订轨 (Gutter Line Numbers)**：独立测算行数并显示行号，高亮当前光标所在行，并且行号轨道与代码区域同步滚动。
 * 3. **当前行高亮背景层**：根据当前光标所在的 [currentLineIndex]，在编辑器背景绘制突出显示的高亮带。
 * 4. **自动换行与横向滚动控制**：通过 [isWordWrap] 参数切换软换行还是横向自由滚动。
 *
 * @param textFieldValue 当前编辑框的 [TextFieldValue]（包含文本内容与光标 Selection 选中信息）
 * @param onValueChange 文本变动回调
 * @param fontSp 字体大小 (sp)
 * @param currentLineIndex 当前光标所在的行索引 (0-based)
 * @param snippetType 代码片段类型
 * @param syntaxLanguage 语法高亮语言（可选，传入则优先使用，否则根据 snippetType 推断）
 * @param isWordWrap 是否开启自动换行
 * @param showLineNumbers 是否显示行号栏
 * @param highlightCurrentLine 是否高亮当前行背景
 * @param topContentPadding 顶部留白内边距（防遮挡）
 * @param onFontSizeChange 双指缩放调整字体大小回调（deltaSp > 0 放大，< 0 缩小）
 * @param fontFamily 编辑器字体族（默认等宽字体）
 */
@Composable
fun CodeEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontSp: Float,
    currentLineIndex: Int,
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
    val tc = LocalThemeColors.current
    val isDark = tc.isDark

    // 确定实际使用的语法语言（对 SnippetType 进行穷举匹配）
    val effectiveLanguage = syntaxLanguage ?: when (snippetType) {
        SnippetType.HTML -> SyntaxLanguage.HTML
        SnippetType.JS -> SyntaxLanguage.JS
        SnippetType.MARKDOWN -> SyntaxLanguage.MARKDOWN
        SnippetType.PROMPT -> SyntaxLanguage.PROMPT
        SnippetType.GENERAL -> SyntaxLanguage.PLAIN
    }

    // 记住并构建语法高亮转换器 VisualTransformation
    val syntaxTransformation = remember(effectiveLanguage, isDark) {
        VisualTransformation { text ->
            val highlighted = SyntaxHighlighter.highlightByLanguage(text.text, effectiveLanguage, isDark)
            TransformedText(highlighted, OffsetMapping.Identity)
        }
    }

    // 记住垂直与水平滚动状态 State
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // 统计代码总行数（通过计算换行符 '\n' 数量加 1，最小值为 1 行）
    val linesCount = remember(textFieldValue.text) {
        val count = textFieldValue.text.count { it == '\n' } + 1
        maxOf(1, count)
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(tc.surface)
            .pointerInput(onFontSizeChange) {
                // 当传入了字号调节闭包时，开启双指缩放检测
                if (onFontSizeChange != null) {
                    detectTransformGestures { _, _, zoom, _ ->
                        // 设定 3% 死区避开单指操作误触
                        if (kotlin.math.abs(zoom - 1f) > 0.03f) {
                            val delta = (zoom - 1f) * 15f
                            onFontSizeChange.invoke(delta.coerceIn(-3f, 3f))
                        }
                    }
                }
            }
    ) {
        // ===== 1. 左侧行号装订轨 (Line Number Gutter) =====
        if (showLineNumbers) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(44.dp)
                    .background(tc.surface2)
                    .verticalScroll(verticalScrollState) // 与右侧代码区绑着同一个 verticalScrollState 共用垂直滚动
                    .padding(top = Spacing.S3 + topContentPadding, bottom = Spacing.S3),
                horizontalAlignment = Alignment.End
            ) {
                // 循环渲染每一行的数字文本
                for (i in 0 until linesCount) {
                    val isCurrent = (i == currentLineIndex)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 动态指定行高为 fontSp * 1.6f，确保与右侧代码输入框的 lineHeight (fontSp * 1.6f) 绝对基线对齐
                            .height((fontSp * 1.6f).dp)
                            .padding(end = Spacing.S2),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "${i + 1}",
                            fontFamily = FontFamily.Monospace, // 等宽字体对齐
                            fontSize = (fontSp * 0.85f).sp,
                            fontWeight = if (isCurrent) FontWeight.W800 else FontWeight.W400,
                            color = if (isCurrent) tc.primary else tc.text3
                        )
                    }
                }
            }
        }

        // ===== 2. 右侧主代码编辑区域 (Code Text Area) =====
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(verticalScrollState) // 与左侧行号轨共用垂直滚动
                .padding(top = Spacing.S3 + topContentPadding, bottom = Spacing.S3, start = Spacing.S3, end = Spacing.S3)
        ) {
            // 当前焦点行高亮背景绘制层
            if (highlightCurrentLine && currentLineIndex in 0 until linesCount) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (currentLineIndex * (fontSp * 1.6f)).dp)
                        .height((fontSp * 1.6f).dp)
                        .background(tc.primarySoft.copy(alpha = if (isDark) 0.15f else 0.5f))
                )
            }

            // 基础无原生框架装饰的文本输入框 BasicTextField
            if (isWordWrap) {
                // 模式 A: 开启自动换行 (Word Wrap)
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = onValueChange,
                    visualTransformation = syntaxTransformation,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace, // 强制使用代码标准等宽字体
                        fontSize = fontSp.sp,
                        lineHeight = (fontSp * 1.6f).sp,
                        color = tc.text
                    ),
                    cursorBrush = SolidColor(tc.primary),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("code_editor_input")
                )
            } else {
                // 模式 B: 禁用换行，开启横向自由滚动 (Horizontal Scrollable Box)
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
                            color = tc.text
                        ),
                        cursorBrush = SolidColor(tc.primary),
                        modifier = Modifier
                            .widthIn(min = 1200.dp) // 给定最小 1200.dp 支撑宽文本横向拖拽
                            .fillMaxHeight()
                            .testTag("code_editor_input")
                    )
                }
            }
        }
    }
}


