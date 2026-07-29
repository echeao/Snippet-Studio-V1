package com.feige.snippetstudio.ui.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.components.CodeEditor
import com.feige.snippetstudio.ui.components.RunPreview
import com.feige.snippetstudio.ui.components.SegmentedControl
import com.feige.snippetstudio.ui.components.SymbolBar
import com.feige.snippetstudio.ui.theme.CaptionStyle
import com.feige.snippetstudio.ui.theme.LocalThemeColors
import com.feige.snippetstudio.ui.theme.Spacing
import com.feige.snippetstudio.util.SyntaxLanguageDetector

/**
 * [EditorMainContent] 编辑器主核心区域展示组件。
 *
 * 架构职责：
 * 1. 展示未绑定工作区目录的黄色提醒横幅。
 * 2. 承载【代码编辑】与【实时预览】Tab 分段切换控制器及字号快速微调按键（A- / A+）、全屏模式切换按键。
 * 3. 渲染 [CodeEditor] 编辑器与其顶部的 [SymbolBar] 快捷输入栏，或渲染 [RunPreview] 实时网页/Markdown 渲染器。
 *
 * @param selectedTab 当前选中的 Tab (0: 代码编辑, 1: 实时预览)
 * @param textFieldValue TextField 文本输入框值状态
 * @param snippetType 片段语言类型
 * @param fontSp 代码字号 (sp)
 * @param editorFont 代码字体族 [FontFamily]
 * @param currentLineIndex 光标当前行号
 * @param isWordWrap 是否开启自动换行
 * @param showLineNumbers 是否显示行号栏
 * @param highlightCurrentLine 是否高亮当前行
 * @param isFullscreen 当前是否处于全屏沉浸模式
 * @param noWorkspaceConfigured 是否未绑定工作区目录
 * @param onTabSelect 切换 Tab 回调
 * @param onValueChange 文本修改回调
 * @param onInsertSymbol 插入快捷符号回调
 * @param onAdjustFontSize 调整字号回调
 * @param onToggleFullscreen 切换全屏回调
 * @param onShowToast 显示 Toast 消息闭包
 * @param modifier 外部 Modifier 修饰符
 */
@Composable
fun EditorMainContent(
    selectedTab: Int,
    textFieldValue: TextFieldValue,
    snippetType: SnippetType,
    fontSp: Float,
    editorFont: FontFamily,
    currentLineIndex: Int,
    lineCount: Int = 1,
    isWordWrap: Boolean,
    showLineNumbers: Boolean,
    highlightCurrentLine: Boolean,
    isFullscreen: Boolean,
    noWorkspaceConfigured: Boolean,
    onTabSelect: (Int) -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    onInsertSymbol: (String) -> Unit,
    onAdjustFontSize: (Float) -> Unit,
    onToggleFullscreen: () -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Column(modifier = modifier) {
        // ===== 工作区未配置警告横幅 =====
        if (noWorkspaceConfigured) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFF3CD),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.S3, vertical = Spacing.S2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder),
                        contentDescription = null,
                        tint = Color(0xFF856404),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "未绑定工作区目录，文件仅存储在应用私有空间，手机文件管理器不可见。请前往 设置 → 工作区仓库 绑定目录。",
                        style = CaptionStyle,
                        color = Color(0xFF856404),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ===== 编辑器顶部控制条 (Code/Preview 分段切, 字体 A-/A+ 调节, 全屏按钮) =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.S3, vertical = Spacing.S2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentedControl(
                options = listOf(stringResource(R.string.editor_code), stringResource(R.string.editor_preview)),
                selectedIndex = selectedTab,
                onSelect = onTabSelect
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onAdjustFontSize(-1f) },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("A−", style = CaptionStyle, color = tc.text2)
                    }
                    TextButton(
                        onClick = { onAdjustFontSize(+1f) },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("A+", style = CaptionStyle, color = tc.text2)
                    }
                }

                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("fullscreen_toggle_btn")
                ) {
                    Icon(
                        painter = painterResource(id = if (isFullscreen) R.drawable.ic_minimize else R.drawable.ic_maximize),
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                        tint = if (isFullscreen) tc.primary else tc.text2,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ===== 主编辑视图 / 预览视图切换 =====
        if (selectedTab == 0) {
            // 代码编辑 Tab
            Column(modifier = Modifier.weight(1f)) {
                SymbolBar(
                    snippetType = snippetType,
                    onInsertSymbol = onInsertSymbol
                )

                CodeEditor(
                    textFieldValue = textFieldValue,
                    onValueChange = onValueChange,
                    fontSp = fontSp,
                    currentLineIndex = currentLineIndex,
                    lineCount = lineCount,
                    snippetType = snippetType,
                    syntaxLanguage = SyntaxLanguageDetector.fromSnippetType(snippetType),
                    isWordWrap = isWordWrap,
                    showLineNumbers = showLineNumbers,
                    highlightCurrentLine = highlightCurrentLine,
                    onFontSizeChange = onAdjustFontSize,
                    fontFamily = editorFont,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // 实时预览 Tab
            RunPreview(
                type = snippetType,
                content = textFieldValue.text,
                modifier = Modifier.weight(1f),
                onToast = onShowToast
            )
        }
    }
}
