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

    // 维护编辑器内部的即时 TextFieldValue 状态，防止 ViewModel 异步 StateFlow 重绘延迟导致输入法 (IME) 选区归零
    var internalTfv by remember { mutableStateOf(textFieldValue) }

    // 监听 Compose 实际测量排版产生的 TextLayoutResult，用于精准捕捉光标在视觉屏上的真实 Y 轴像素坐标
    var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    // 当外部传入的 textFieldValue 发生文本变更（如加载新文件、强制重命名或恢复）时，同步更新内部状态
    LaunchedEffect(textFieldValue.text) {
        if (internalTfv.text != textFieldValue.text) {
            internalTfv = textFieldValue
        }
    }

    // 统一变动处理器：当前帧内优先同步更新本地 internalTfv 保持光标位置连续，再通知外部 ViewModel
    val handleValueChange: (TextFieldValue) -> Unit = { newTfv ->
        internalTfv = newTfv
        onValueChange(newTfv)
    }

    // 统计代码总行数（通过计算换行符 '\n' 数量加 1，最小值为 1 行）
    val linesCount = remember(internalTfv.text) {
        val count = internalTfv.text.count { it == '\n' } + 1
        maxOf(1, count)
    }

    val density = androidx.compose.ui.platform.LocalDensity.current

    // 计算当前光标在视觉屏幕上的真实 Y 轴 Top 偏移量（dp），解决软自动换行折行时高亮背景横条与光标错位的问题
    val currentLineTopDp = remember(textLayoutResult, internalTfv.selection, currentLineIndex, fontSp) {
        val layout = textLayoutResult
        if (layout != null && internalTfv.text.isNotEmpty()) {
            val caret = internalTfv.selection.start.coerceIn(0, layout.layoutInput.text.length)
            val visualLine = layout.getLineForOffset(caret)
            val topPx = layout.getLineTop(visualLine)
            with(density) { topPx.toDp() }
        } else {
            (currentLineIndex * fontSp * 1.6f).dp
        }
    }

    // 计算当前光标所在视觉行的真实高度（dp）
    val currentLineHeightDp = remember(textLayoutResult, internalTfv.selection, fontSp) {
        val layout = textLayoutResult
        if (layout != null && internalTfv.text.isNotEmpty()) {
            val caret = internalTfv.selection.start.coerceIn(0, layout.layoutInput.text.length)
            val visualLine = layout.getLineForOffset(caret)
            val bottomPx = layout.getLineBottom(visualLine)
            val topPx = layout.getLineTop(visualLine)
            with(density) { (bottomPx - topPx).toDp() }
        } else {
            (fontSp * 1.6f).dp
        }
    }

    // 监听当前光标所在行坐标，当输入回车换行或移动光标时自动平滑滚动视口，确保当前编辑行处于视口内部
    LaunchedEffect(currentLineTopDp) {
        val targetScrollPx = with(density) { currentLineTopDp.toPx() }.toInt()
        val maxScroll = verticalScrollState.maxValue
        if (targetScrollPx > verticalScrollState.value + 300 || targetScrollPx < verticalScrollState.value) {
            verticalScrollState.animateScrollTo(targetScrollPx.coerceIn(0, maxScroll))
        }
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
            // 当前焦点行高亮背景绘制层（使用 TextLayoutResult 计算的真实物理坐标，解决 WordWrap 自动换行下的对齐错位）
            if (highlightCurrentLine && currentLineIndex in 0 until linesCount) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = currentLineTopDp)
                        .height(currentLineHeightDp)
                        .background(tc.primarySoft.copy(alpha = if (isDark) 0.15f else 0.5f))
                )
            }

            // 基础无原生框架装饰的文本输入框 BasicTextField
            if (isWordWrap) {
                // 模式 A: 开启自动换行 (Word Wrap)
                BasicTextField(
                    value = internalTfv,
                    onValueChange = handleValueChange,
                    onTextLayout = { textLayoutResult = it },
                    visualTransformation = syntaxTransformation,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace, // 强制使用代码标准等宽字体
                        fontSize = fontSp.sp,
                        lineHeight = (fontSp * 1.6f).sp,
                        color = tc.text
                    ),
                    cursorBrush = SolidColor(tc.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 300.dp)
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
                        value = internalTfv,
                        onValueChange = handleValueChange,
                        onTextLayout = { textLayoutResult = it },
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


