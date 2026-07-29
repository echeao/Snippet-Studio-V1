package com.feige.snippetstudio.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.editor.components.EditorMainContent
import com.feige.snippetstudio.ui.editor.components.EditorSettingsSheet
import com.feige.snippetstudio.ui.editor.components.EditorTopAppBar
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.SyntaxLanguageDetector
import com.feige.snippetstudio.util.SystemUiUtil

/**
 * [EditorScreen] 全功能专业代码编辑器主视图。
 *
 * 架构重构与组件设计亮点：
 * 1. **代码解耦模块化**：拆分为 [EditorTopAppBar]（顶部标题与操作栏）、[EditorMainContent]（编辑与预览核心容器）、[EditorSettingsSheet]（设置面板）。
 * 2. **全屏沉浸模式**：调用 [SystemUiUtil.setImmersiveFullscreen]，配合 [FloatingControlIsland] 控制岛与滑动手势自动隐显。
 * 3. **底栏状态防护**：联合 IME 软键盘与 NavigationBars 安全区避让，防止键盘弹出后遮挡底部状态栏。
 * 4. **全量简体中文注释**：补充详细的架构职责与交互说明。
 *
 * @param viewModel 编辑器 ViewModel 实例
 * @param onBack 页面返回闭包
 * @param onShowSnackbar 显示全局提示消息闭包
 * @param onNavigateToHistory 跳转 Git 版本历史记录页面闭包
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onNavigateToHistory: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tc = LocalThemeColors.current

    // 计算编辑器使用字体族
    val editorFont = remember(uiState.editorFontFamily) {
        when (uiState.editorFontFamily) {
            "sans-serif" -> FontFamily.SansSerif
            "serif" -> FontFamily.Serif
            else -> FontFamily.Monospace
        }
    }

    // 状态弹窗控制
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    var showFloatingIsland by remember { mutableStateOf(true) }
    var showFullscreenSymbolBar by remember { mutableStateOf(true) }

    // ===== 全屏沉浸模式与系统 WindowInsets 的交互处理 =====
    val activity = remember(context) { SystemUiUtil.findActivity(context) }
    DisposableEffect(uiState.isFullscreen) {
        if (uiState.isFullscreen) {
            SystemUiUtil.setImmersiveFullscreen(activity, true)
        }
        onDispose {
            SystemUiUtil.setImmersiveFullscreen(activity, false)
        }
    }

    // 全屏模式下滚动手势隐显控制岛
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15f) {
                    showFloatingIsland = false
                } else if (available.y > 15f) {
                    showFloatingIsland = true
                }
                return Offset.Zero
            }
        }
    }

    // 返回拦截机制
    val handleBack = {
        if (uiState.saveState == SaveState.UNSAVED) {
            showDiscardDialog = true
        } else {
            viewModel.handleBackWithCleanup(onBack)
        }
    }

    BackHandler(enabled = true) {
        if (uiState.isFullscreen) {
            viewModel.setFullscreen(false)
        } else {
            handleBack()
        }
    }

    // 加载等待指示器
    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tc.bg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = tc.primary)
        }
        return
    }

    if (uiState.isFullscreen) {
        // ===== 模式 A：全屏沉浸沉浸视图容器 =====
        val safeTopPadding = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tc.bg)
                .nestedScroll(nestedScrollConnection)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    showFloatingIsland = !showFloatingIsland
                }
        ) {
            if (uiState.selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SoraCodeEditor(
                        text = uiState.textFieldValue.text,
                        onTextChange = { viewModel.onSoraTextChange(it) },
                        onCursorChange = { line, col -> viewModel.onSoraCursorChange(line, col) },
                        language = SyntaxLanguageDetector.fromSnippetType(uiState.type),
                        isDark = tc.isDark,
                        themeColors = tc,
                        fontSp = uiState.fontSp,
                        showLineNumbers = uiState.showLineNumbers,
                        isWordWrap = uiState.isWordWrap,
                        selectionOffset = uiState.textFieldValue.selection.start,
                        modifier = Modifier.weight(1f)
                    )

                    AnimatedVisibility(
                        visible = showFullscreenSymbolBar && showFloatingIsland,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)))
                            .padding(bottom = 2.dp)
                    ) {
                        SymbolBar(
                            snippetType = uiState.type,
                            onInsertSymbol = { symbol -> viewModel.insertSymbol(symbol) }
                        )
                    }
                }
            } else {
                RunPreview(
                    type = uiState.type,
                    content = uiState.textFieldValue.text,
                    modifier = Modifier.fillMaxSize(),
                    onToast = onShowSnackbar
                )
            }

            // 悬浮全屏控制岛 (顶部岛状按钮)
            AnimatedVisibility(
                visible = showFloatingIsland,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = 8.dp)
            ) {
                FloatingControlIsland(
                    selectedTab = uiState.selectedTab,
                    onSelectTab = { viewModel.selectTab(it) },
                    showSymbolBar = showFullscreenSymbolBar,
                    onToggleSymbolBar = { showFullscreenSymbolBar = !showFullscreenSymbolBar },
                    onExitFullscreen = { viewModel.setFullscreen(false) }
                )
            }
        }
    } else {
        // ===== 模式 B：标准带有 TopBar 与 Status BottomBar 的编辑视图 =====
        Scaffold(
            topBar = {
                EditorTopAppBar(
                    title = uiState.title,
                    saveState = uiState.saveState,
                    snippetType = uiState.type,
                    hasPromptVariables = uiState.promptVariables.isNotEmpty(),
                    snippetId = uiState.id,
                    codeContent = uiState.textFieldValue.text,
                    onTitleChange = { viewModel.onTitleChange(it) },
                    onBack = { handleBack() },
                    onOpenSettingsSheet = { showSettingsSheet = true },
                    onOpenTagDialog = { showTagDialog = true },
                    onToggleVariablePanel = { viewModel.toggleVariablePanel() },
                    onForceSave = { viewModel.forceSaveNow() },
                    onShowSnackbar = onShowSnackbar,
                    onNavigateToHistory = onNavigateToHistory
                )
            },
            bottomBar = {
                // 底部专业代码编辑器状态栏 (独立的组合节点，隔离频繁变化的行列号重组)
                EditorBottomStatusBar(
                    currentLineIndex = uiState.currentLineIndex,
                    currentColumnIndex = uiState.currentColumnIndex,
                    lineCount = uiState.lineCount,
                    charCount = uiState.charCount,
                    encoding = uiState.encoding,
                    lineEnding = uiState.lineEnding,
                    snippetType = uiState.type,
                    onOpenTypeDialog = { showTypeDialog = true }
                )
            },
            containerColor = tc.bg
        ) { innerPadding ->
            EditorMainContent(
                selectedTab = uiState.selectedTab,
                textFieldValue = uiState.textFieldValue,
                snippetType = uiState.type,
                fontSp = uiState.fontSp,
                editorFont = editorFont,
                currentLineIndex = uiState.currentLineIndex,
                lineCount = uiState.lineCount,
                isWordWrap = uiState.isWordWrap,
                showLineNumbers = uiState.showLineNumbers,
                highlightCurrentLine = uiState.highlightCurrentLine,
                isFullscreen = uiState.isFullscreen,
                noWorkspaceConfigured = uiState.noWorkspaceConfigured,
                onTabSelect = { viewModel.selectTab(it) },
                onValueChange = { viewModel.onTextFieldValueChange(it) },
                onSoraTextChange = { viewModel.onSoraTextChange(it) },
                onSoraCursorChange = { line, col -> viewModel.onSoraCursorChange(line, col) },
                onInsertSymbol = { viewModel.insertSymbol(it) },
                onAdjustFontSize = { viewModel.adjustFontSize(it) },
                onToggleFullscreen = { viewModel.toggleFullscreen() },
                onShowToast = onShowSnackbar,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    // ===== 弹框 A: 编辑器选项 ModalBottomSheet =====
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = tc.surface
        ) {
            EditorSettingsSheet(
                uiState = uiState,
                viewModel = viewModel,
                onClose = { showSettingsSheet = false },
                onOpenTagsDialog = {
                    showSettingsSheet = false
                    showTagDialog = true
                }
            )
        }
    }

    // ===== 弹框 B: 选择片段语言类型对话框 =====
    if (showTypeDialog) {
        AlertDialog(
            onDismissRequest = { showTypeDialog = false },
            title = { Text("选择片段语言类型", fontWeight = FontWeight.Bold, style = SectionTitleStyle) },
            text = {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SnippetType.entries.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSnippetType(type)
                                    showTypeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            RadioButton(
                                selected = (uiState.type == type),
                                onClick = {
                                    viewModel.setSnippetType(type)
                                    showTypeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = type.displayName, fontWeight = FontWeight.Bold, color = tc.text)
                                Text(text = "扩展名: ${type.extension}", style = CaptionStyle, color = tc.text2)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTypeDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }

    // ===== 弹框 C: 标签编辑对话框 =====
    TagEditDialog(
        show = showTagDialog,
        initialTags = uiState.tags,
        allAvailableTags = uiState.allAvailableTags,
        onDismiss = { showTagDialog = false },
        onSave = { updatedTags ->
            viewModel.updateTags(updatedTags)
            onShowSnackbar(context.getString(R.string.toast_tags_updated))
        }
    )

    // ===== 弹框 D: 未保存离开二次确认弹窗 =====
    ConfirmDialog(
        show = showDiscardDialog,
        title = stringResource(R.string.confirm_discard_title),
        desc = "有未保存的更改，确定离开吗？",
        onConfirm = {
            showDiscardDialog = false
            viewModel.handleBackWithCleanup(onBack)
        },
        onDismiss = { showDiscardDialog = false },
        isDanger = true
    )

    // ===== 弹框 E: Prompt 变量填充面板 =====
    VariableFillPanel(
        show = uiState.showVariablePanel,
        variables = uiState.promptVariables,
        variableValues = uiState.variableValues,
        onValueChange = { name, value -> viewModel.onVariableValueChange(name, value) },
        onApply = { viewModel.applyVariableFill() },
        onDismiss = { viewModel.toggleVariablePanel() }
    )
}

/**
 * [EditorBottomStatusBar] 独立封装的底部代码编辑器状态栏组件。
 *
 * 架构优化与重组隔离说明：
 * 1. 单独接收频繁变动的行列号 (currentLineIndex, currentColumnIndex)、总行数与字符数参数。
 * 2. 在 Compose 组合树中作为叶子节点隔离，光标移动或文本长短变化时只有该小组件发生 Recomposition，避免引发上层大界面重绘。
 *
 * @param currentLineIndex 当前光标行号 (0-based)
 * @param currentColumnIndex 当前光标列号 (0-based)
 * @param lineCount 片段总行数
 * @param charCount 片段总字符数
 * @param encoding 文件编码格式字符串 (如 "UTF-8")
 * @param lineEnding 换行符类型 (如 "LF")
 * @param snippetType 当前片段语言类型
 * @param onOpenTypeDialog 点击切换类型 Badge 回调
 */
@Composable
private fun EditorBottomStatusBar(
    currentLineIndex: Int,
    currentColumnIndex: Int,
    lineCount: Int,
    charCount: Int,
    encoding: String,
    lineEnding: String,
    snippetType: SnippetType,
    onOpenTypeDialog: () -> Unit
) {
    val tc = LocalThemeColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)))
            .height(36.dp)
            .border(1.dp, tc.line),
        color = tc.surface2
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.S3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentLineIndex + 1}:${currentColumnIndex + 1}  ·  ${lineCount} 行  ·  ${charCount} 字符  ·  ${encoding}  ·  ${lineEnding}",
                style = CaptionStyle,
                color = tc.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Surface(
                color = tc.primarySoft,
                shape = RoundedCornerShape(R_SM),
                modifier = Modifier.clickable { onOpenTypeDialog() }
            ) {
                Text(
                    text = snippetType.displayName,
                    style = BadgeStyle,
                    color = tc.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
