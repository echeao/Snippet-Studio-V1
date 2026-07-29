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
import androidx.compose.ui.layout.onSizeChanged
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
 * 性能优化说明（v1.1.1）：
 * - 行号虚拟化窗口补全了底部占位 Spacer，修复末尾行号被截断的 Bug。
 * - 使用 [rememberUpdatedState] 将 linesCount 与 derivedStateOf 解耦，
 *   避免每次打字时重建虚拟化计算 lambda。
 * - currentLineTopDp 的 remember key 移除了冗余的 currentLineIndex 项。
 * - 语法高亮防抖延迟从 120ms 调整至 150ms，减少频繁打字时的高亮重算压力。
 *
 * @param textFieldValue 当前编辑框的 [TextFieldValue]（包含文本内容与光标 Selection 选中信息）
 * @param onValueChange 文本变动回调
 * @param fontSp 字体大小 (sp)
 * @param currentLineIndex 当前光标所在的行索引 (0-based)
 * @param lineCount 总行数（由 ViewModel 后台异步计算传入，避免在组合阶段扫描字符）
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
    val tc = LocalThemeColors.current
    val isDark = tc.isDark

    // 确定实际使用的语法语言（对 SnippetType 进行穷举匹配）
    val effectiveLanguage = syntaxLanguage ?: when (snippetType) {
        SnippetType.HTML     -> SyntaxLanguage.HTML
        SnippetType.JS       -> SyntaxLanguage.JS
        SnippetType.MARKDOWN -> SyntaxLanguage.MARKDOWN
        SnippetType.PROMPT   -> SyntaxLanguage.PROMPT
        SnippetType.JAVA     -> SyntaxLanguage.JAVA
        SnippetType.GENERAL  -> SyntaxLanguage.PLAIN
    }

    // 记住语法高亮富文本结果，初始填充纯文本避免组件首次渲染空白
    var highlightedAnnotated by remember(effectiveLanguage) {
        mutableStateOf(AnnotatedString(textFieldValue.text))
    }

    // 异步计算语法高亮：在 Dispatchers.Default 协程后台线程处理正则匹配，
    // 配合 150ms 防抖避免打字频繁阻塞 UI 线程（从 120ms 微调至 150ms）
    LaunchedEffect(textFieldValue.text, effectiveLanguage, isDark) {
        kotlinx.coroutines.delay(150)
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            SyntaxHighlighter.highlightByLanguage(textFieldValue.text, effectiveLanguage, isDark)
        }
        highlightedAnnotated = result
    }

    // 构建 VisualTransformation 转换器：在防抖计算的 150ms 极短过渡期内，
    // 若高亮结果长度与文本长度不一致，安全降级使用原 text 文本，绝对防止选区与光标索引越界闪退
    val syntaxTransformation = remember(highlightedAnnotated) {
        VisualTransformation { text ->
            val safeTransformed = if (highlightedAnnotated.length == text.length) {
                highlightedAnnotated
            } else {
                text
            }
            TransformedText(safeTransformed, OffsetMapping.Identity)
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

    // 直接使用外部 ViewModel 计算好传进来的总行数，避免在 Compose 组合阶段频繁做字符扫描
    val linesCount = lineCount.coerceAtLeast(1)

    val density = androidx.compose.ui.platform.LocalDensity.current

    // 计算当前光标在视觉屏幕上的真实 Y 轴 Top 偏移量（dp），解决软自动换行折行时高亮背景横条与光标错位的问题。
    // 优化：移除了冗余的 currentLineIndex key，selection 变化已足够触发重算，避免双重触发。
    val currentLineTopDp = remember(textLayoutResult, internalTfv.selection, fontSp) {
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
            // 每行逻辑行高（与右侧代码区 lineHeight 保持一致）
            val lineHeightDp = (fontSp * 1.6f).dp

            // Gutter 视口高度（px），用于计算可见行窗口
            var gutterViewportHeightPx by remember { mutableStateOf(0) }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(44.dp)
                    .background(tc.surface2)
                    // 与右侧代码区绑着同一个 verticalScrollState 共用垂直滚动
                    .verticalScroll(verticalScrollState)
                    .onSizeChanged { gutterViewportHeightPx = it.height }
                    .padding(top = Spacing.S3 + topContentPadding),
                horizontalAlignment = Alignment.End
            ) {
                val lineHeightPx = with(density) { lineHeightDp.toPx() }

                // ── WordWrap 模式 vs 无换行模式 分支渲染 ──────────────────────────────
                // WordWrap 开启时，每条逻辑行可能折成多个视觉行，行高不固定，
                // 虚拟化窗口无法精确预估总高度，直接全量渲染所有行号（行号 Text 轻量无性能问题）。
                // WordWrap 关闭时，每行视觉高度固定等于 lineHeightDp，保留虚拟化窗口提升大文件性能。
                if (isWordWrap) {
                    // ── 模式 A：WordWrap 全量渲染所有行号 ──────────────────────────
                    for (i in 0 until linesCount) {
                        val isCurrent = (i == currentLineIndex)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(lineHeightDp)
                                .padding(end = Spacing.S2),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "${i + 1}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = (fontSp * 0.85f).sp,
                                fontWeight = if (isCurrent) FontWeight.W800 else FontWeight.W400,
                                color = if (isCurrent) tc.primary else tc.text3
                            )
                        }
                    }
                } else {
                    // ── 模式 B：无换行虚拟化窗口渲染 ──────────────────────────────────
                    // 使用 rememberUpdatedState 将 linesCount 封装为 Compose 可追踪状态，
                    // 解除与 remember key 的耦合：derivedStateOf 内部追踪 linesCountState.value
                    // 的读取，linesCount 变化时自动重算，无需重建整个 lambda，消除每次打字的重建开销。
                    val linesCountState = rememberUpdatedState(linesCount)

                    val visibleLineRange by remember(lineHeightPx, gutterViewportHeightPx) {
                        derivedStateOf {
                            // 注：linesCountState.value 被 derivedStateOf 自动追踪
                            val currentLinesCount = linesCountState.value
                            if (lineHeightPx > 0f && gutterViewportHeightPx > 0) {
                                val scrollPx = verticalScrollState.value.toFloat()
                                val start = (scrollPx / lineHeightPx)
                                    .toInt()
                                    .coerceIn(0, (currentLinesCount - 1).coerceAtLeast(0))
                                // 多渲染 2 行缓冲区，防止快速滚动时行号一帧空白闪烁
                                val end = ((scrollPx + gutterViewportHeightPx) / lineHeightPx + 2)
                                    .toInt()
                                    .coerceAtMost(currentLinesCount)
                                start until end
                            } else {
                                0 until 0
                            }
                        }
                    }

                    if (visibleLineRange.first < visibleLineRange.last) {
                        val visibleStart = visibleLineRange.first
                        val visibleEnd   = visibleLineRange.last

                        // 顶部非可见区域：用 Spacer 占位，保持正确的滚动总高度
                        if (visibleStart > 0) {
                            Spacer(modifier = Modifier.height(lineHeightDp * visibleStart))
                        }

                        // 渲染可见行号
                        for (i in visibleStart until visibleEnd) {
                            val isCurrent = (i == currentLineIndex)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(lineHeightDp)
                                    .padding(end = Spacing.S2),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "${i + 1}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (fontSp * 0.85f).sp,
                                    fontWeight = if (isCurrent) FontWeight.W800 else FontWeight.W400,
                                    color = if (isCurrent) tc.primary else tc.text3
                                )
                            }
                        }

                        // ★ Bug Fix：底部非可见区域补足占位 Spacer。
                        // 原版缺少此 Spacer 导致 Gutter Column 总高度 < 代码区高度，
                        // 用户滚动到文档末尾时行号已耗尽，产生末尾行号截断的视觉 Bug。
                        val remainingLines = linesCount - visibleEnd
                        if (remainingLines > 0) {
                            Spacer(modifier = Modifier.height(lineHeightDp * remainingLines))
                        }
                    }
                }

                // 行号轨道末尾缓冲留白，与右侧代码区 Spacer 高度保持一致
                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        // ===== 2. 右侧主代码编辑区域 (Code Text Area) =====
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(verticalScrollState) // 与左侧行号轨共用垂直滚动
                .padding(top = Spacing.S3 + topContentPadding, start = Spacing.S3, end = Spacing.S3)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
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

            // 代码区域末尾 120.dp 越过留白缓冲层 (Scroll Beyond Last Line)，确保最后一行代码能轻轻松松向上滑动到屏幕中央
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
