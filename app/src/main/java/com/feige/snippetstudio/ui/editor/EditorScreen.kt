package com.feige.snippetstudio.ui.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.SyntaxLanguageDetector
import com.feige.snippetstudio.util.SystemUiUtil
import androidx.compose.animation.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * [EditorScreen] 全功能专业代码编辑器主视图。
 *
 * 核心交互特性：
 * 1. **标题与保存状态 Badge**：顶部 TopBar 可直接修改片段标题，并实时反馈【已保存/正在保存/未保存】指示徽章。
 * 2. **全屏沉浸沉浸模式 (Immersive Mode)**：点击全屏按钮后调用 [SystemUiUtil.setImmersiveFullscreen] 隐藏系统状态栏与导航栏，支持 [FloatingControlIsland] 控制岛和动态手势唤出。
 * 3. **快捷键盘工具栏 [SymbolBar]**：包含智能符号快速点击插入。
 * 4. **底部专业状态栏**：展示当前光标行/列号、总行数、字符数、文件编码格式与换行符。
 * 5. **设置弹窗 [EditorSettingsContent]**：支持调节字号 (Slider)、控制 WordWrap 换行、开闭行号、缩进空格与编码切换。
 *
 * @param viewModel 编辑器 ViewModel 依赖
 * @param onBack 页面返回闭包
 * @param onShowSnackbar 全局 Snackbar 提示闭包
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

    val editorFont = remember(uiState.editorFontFamily) {
        when (uiState.editorFontFamily) {
            "sans-serif" -> FontFamily.SansSerif
            "serif" -> FontFamily.Serif
            else -> FontFamily.Monospace
        }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
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

    val handleBack = {
        if (uiState.saveState == SaveState.UNSAVED) {
            showDiscardDialog = true
        } else {
            viewModel.handleBackWithCleanup(onBack)
        }
    }

    // 拦截物理/系统返回按键
    BackHandler(enabled = true) {
        if (uiState.isFullscreen) {
            viewModel.setFullscreen(false)
        } else {
            handleBack()
        }
    }

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

    val saveBadgeText = when (uiState.saveState) {
        SaveState.SAVED -> stringResource(R.string.state_saved)
        SaveState.SAVING -> stringResource(R.string.state_saving)
        SaveState.UNSAVED -> stringResource(R.string.state_unsaved)
    }

    val saveBadgeBg = when (uiState.saveState) {
        SaveState.SAVED -> SuccessSoft
        SaveState.SAVING -> WarningSoft
        SaveState.UNSAVED -> DangerSoft
    }

    val saveBadgeFg = when (uiState.saveState) {
        SaveState.SAVED -> Success
        SaveState.SAVING -> Warning
        SaveState.UNSAVED -> Danger
    }

    // 渲染通用编辑/预览区域的核心 Composable Lambda
    val editorContent: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier) {
            // ===== 工作区未配置警告横幅 =====
            if (uiState.noWorkspaceConfigured) {
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
                    selectedIndex = uiState.selectedTab,
                    onSelect = { viewModel.selectTab(it) }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { viewModel.adjustFontSize(-1f) },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("A−", style = CaptionStyle, color = tc.text2)
                        }
                        TextButton(
                            onClick = { viewModel.adjustFontSize(+1f) },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("A+", style = CaptionStyle, color = tc.text2)
                        }
                    }

                    IconButton(
                        onClick = { viewModel.toggleFullscreen() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("fullscreen_toggle_btn")
                    ) {
                        Icon(
                            painter = painterResource(id = if (uiState.isFullscreen) R.drawable.ic_minimize else R.drawable.ic_maximize),
                            contentDescription = if (uiState.isFullscreen) "Exit Fullscreen" else "Fullscreen",
                            tint = if (uiState.isFullscreen) tc.primary else tc.text2,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ===== 主编辑视图 / 预览视图切换 =====
            if (uiState.selectedTab == 0) {
                // 代码编辑 Tab
                Column(modifier = Modifier.weight(1f)) {
                    SymbolBar(
                        snippetType = uiState.type,
                        onInsertSymbol = { symbol -> viewModel.insertSymbol(symbol) }
                    )

                    CodeEditor(
                        textFieldValue = uiState.textFieldValue,
                        onValueChange = { viewModel.onTextFieldValueChange(it) },
                        fontSp = uiState.fontSp,
                        currentLineIndex = uiState.currentLineIndex,
                        snippetType = uiState.type,
                        syntaxLanguage = SyntaxLanguageDetector.detect(uiState.snippet?.fileName ?: "", uiState.type),
                        isWordWrap = uiState.isWordWrap,
                        showLineNumbers = uiState.showLineNumbers,
                        highlightCurrentLine = uiState.highlightCurrentLine,
                        onFontSizeChange = { viewModel.adjustFontSize(it) },
                        fontFamily = editorFont,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // 实时预览 Tab
                RunPreview(
                    type = uiState.type,
                    content = uiState.textFieldValue.text,
                    modifier = Modifier.weight(1f),
                    onToast = onShowSnackbar
                )
            }
        }
    }

    if (uiState.isFullscreen) {
        // ===== 模式 A：全屏沉浸沉浸视图容器 =====
        val safeTopPadding = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tc.bg)
                .nestedScroll(nestedScrollConnection)
                .imePadding()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) {
                    showFloatingIsland = !showFloatingIsland
                }
        ) {
            if (uiState.selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CodeEditor(
                        textFieldValue = uiState.textFieldValue,
                        onValueChange = { viewModel.onTextFieldValueChange(it) },
                        fontSp = uiState.fontSp,
                        currentLineIndex = uiState.currentLineIndex,
                        snippetType = uiState.type,
                        syntaxLanguage = SyntaxLanguageDetector.detect(uiState.snippet?.fileName ?: "", uiState.type),
                        isWordWrap = uiState.isWordWrap,
                        showLineNumbers = uiState.showLineNumbers,
                        highlightCurrentLine = uiState.highlightCurrentLine,
                        topContentPadding = safeTopPadding,
                        onFontSizeChange = { viewModel.adjustFontSize(it) },
                        fontFamily = editorFont,
                        modifier = Modifier.weight(1f)
                    )

                    AnimatedVisibility(
                        visible = showFullscreenSymbolBar && showFloatingIsland,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier
                            // 动态吸收软键盘 IME 高度与底部系统安全区，防止键盘弹出后遮挡快捷符号栏
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
                com.feige.snippetstudio.ui.components.FloatingControlIsland(
                    selectedTab = uiState.selectedTab,
                    onSelectTab = { viewModel.selectTab(it) },
                    showSymbolBar = showFullscreenSymbolBar,
                    onToggleSymbolBar = { showFullscreenSymbolBar = !showFullscreenSymbolBar },
                    onExitFullscreen = { viewModel.setFullscreen(false) }
                )
            }
        }
    } else {
        // ===== 模式 B：标准带有 Scaffold TopBar / BottomBar 的编辑视图 =====
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { handleBack() },
                            modifier = Modifier.testTag("editor_back_btn")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back),
                                contentDescription = "Back",
                                tint = tc.text
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = uiState.title,
                                onValueChange = { viewModel.onTitleChange(it) },
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.W800,
                                    color = tc.text
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(tc.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_title_input"),
                                decorationBox = { innerTextField ->
                                    if (uiState.title.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.editor_rename_hint),
                                            style = TextStyle(fontSize = 16.sp, color = tc.text2)
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            Spacer(modifier = Modifier.width(Spacing.S2))

                            // 保存状态指示徽章
                            Surface(
                                color = saveBadgeBg,
                                shape = RoundedCornerShape(R_SM)
                            ) {
                                Text(
                                    text = saveBadgeText,
                                    style = BadgeStyle,
                                    color = saveBadgeFg,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.testTag("editor_more_menu_btn")
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more_vert),
                                    contentDescription = "More",
                                    tint = tc.text
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("编辑区设置", style = CaptionStyle, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_settings), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        showSettingsSheet = true
                                    },
                                    modifier = Modifier.testTag("menu_editor_settings")
                                )
                                DropdownMenuItem(
                                    text = { Text("编辑片段标签", style = CaptionStyle) },
                                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_tag), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        showTagDialog = true
                                    }
                                )
                                // Prompt 变量填充快捷入口
                                if (uiState.type == SnippetType.PROMPT && uiState.promptVariables.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.var_menu_fill), style = CaptionStyle) },
                                        leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_spark), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.toggleVariablePanel()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("复制全部代码", style = CaptionStyle) },
                                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_copy), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("snippet", uiState.textFieldValue.text)
                                        clipboard.setPrimaryClip(clip)
                                        onShowSnackbar(context.getString(R.string.toast_copied))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("立即保存", style = CaptionStyle) },
                                    leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_save), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.forceSaveNow()
                                        onShowSnackbar("已保存")
                                    }
                                )
                                // Git 历史履历入口
                                if (uiState.id != "new" && onNavigateToHistory != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_git_history), style = CaptionStyle) },
                                        leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_git), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                        onClick = {
                                            menuExpanded = false
                                            onNavigateToHistory(uiState.id)
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = tc.bg
                    )
                )
            },
            bottomBar = {
                // 底部专业代码编辑器状态栏 (行列号 / 编码格式 / 换行符 / 语言)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
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
                            text = "${uiState.currentLineIndex + 1}:${uiState.currentColumnIndex + 1}  ·  ${uiState.lineCount} 行  ·  ${uiState.charCount} 字符  ·  ${uiState.encoding}  ·  ${uiState.lineEnding}",
                            style = CaptionStyle,
                            color = tc.text2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            color = tc.primarySoft,
                            shape = RoundedCornerShape(R_SM),
                            modifier = Modifier.clickable { showTypeDialog = true }
                        ) {
                            Text(
                                text = uiState.type.displayName,
                                style = BadgeStyle,
                                color = tc.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            containerColor = tc.bg
        ) { innerPadding ->
            editorContent(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            )
        }
    }

    // ===== 弹框 A: 编辑器个性化设置 ModalBottomSheet =====
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = tc.surface
        ) {
            EditorSettingsContent(
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

    // ===== 弹框 B: 切换语言类型对话框 =====
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
            onShowSnackbar("标签已更新")
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
 * [EditorSettingsContent] 编辑器选项设置弹层正文面板组件。
 */
@Composable
private fun EditorSettingsContent(
    uiState: EditorUiState,
    viewModel: EditorViewModel,
    onClose: () -> Unit,
    onOpenTagsDialog: () -> Unit
) {
    val tc = LocalThemeColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "编辑器选项",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = tc.text
            )
            IconButton(onClick = onClose) {
                Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = "Close", tint = tc.text2)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== 属性分组 1: 片段标签管理 =====
        Text("片段属性", style = CaptionStyle, color = tc.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenTagsDialog() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "管理代码标签", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = tc.text)
                Text(
                    text = if (uiState.tags.isEmpty()) "暂无标签，点击添加" else uiState.tags.joinToString(", ") { "#$it" },
                    style = CaptionStyle,
                    color = tc.text2
                )
            }
            Icon(painter = painterResource(id = R.drawable.ic_chevron_right), contentDescription = null, tint = tc.text2)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== 属性分组 2: 显示与排版开关 (WordWrap / 行号 / 高亮) =====
        Text("显示与排版", style = CaptionStyle, color = tc.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        SettingSwitchRow(
            title = "自动换行 (Word Wrap)",
            subtitle = "超出编辑器边缘时自动换行",
            checked = uiState.isWordWrap,
            onCheckedChange = { viewModel.setWordWrap(it) }
        )

        SettingSwitchRow(
            title = "显示行号 (Line Numbers)",
            subtitle = "在代码左侧展示行号栏",
            checked = uiState.showLineNumbers,
            onCheckedChange = { viewModel.setShowLineNumbers(it) }
        )

        SettingSwitchRow(
            title = "高亮当前行",
            subtitle = "高亮背景标记光标所在行",
            checked = uiState.highlightCurrentLine,
            onCheckedChange = { viewModel.setHighlightCurrentLine(it) }
        )

        SettingSwitchRow(
            title = "自动括号/引号配对",
            subtitle = "输入括号与引号时自动补全闭合",
            checked = uiState.autoPairBrackets,
            onCheckedChange = { viewModel.setAutoPairBrackets(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 字号调整 Slider =====
        Text("代码字号: ${uiState.fontSp} sp", style = CaptionStyle, color = tc.text, fontWeight = FontWeight.Medium)
        Slider(
            value = uiState.fontSp,
            onValueChange = { viewModel.adjustFontSize(it - uiState.fontSp) },
            valueRange = 11f..22f,
            steps = 11
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 字体族选择 =====
        Text("编辑器字体", style = CaptionStyle, color = tc.text, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        val fontOptions = listOf("monospace" to "等宽", "sans-serif" to "无衬线", "serif" to "衬线")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fontOptions.forEach { (code, label) ->
                FilterChip(
                    selected = (uiState.editorFontFamily == code),
                    onClick = { viewModel.setEditorFontFamily(code) },
                    label = { Text(label, style = CaptionStyle) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 属性分组 3: 编码与换行符 (UTF-8, LF / CRLF) =====
        Text("编码与换行符", style = CaptionStyle, color = tc.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Text("字符编码格式", style = CaptionStyle, color = tc.text2)
        Spacer(modifier = Modifier.height(4.dp))
        val encodings = listOf("UTF-8", "GBK", "UTF-16", "ISO-8859-1", "US-ASCII")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            encodings.take(3).forEach { enc ->
                FilterChip(
                    selected = (uiState.encoding == enc),
                    onClick = { viewModel.setEncoding(enc) },
                    label = { Text(enc, style = CaptionStyle) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            encodings.drop(3).forEach { enc ->
                FilterChip(
                    selected = (uiState.encoding == enc),
                    onClick = { viewModel.setEncoding(enc) },
                    label = { Text(enc, style = CaptionStyle) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("换行符格式", style = CaptionStyle, color = tc.text2)
        Spacer(modifier = Modifier.height(4.dp))
        val lineEndings = listOf("LF" to "LF (Unix/Mac)", "CRLF" to "CRLF (Windows)", "CR" to "CR (Classic)")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            lineEndings.forEach { (code, label) ->
                FilterChip(
                    selected = (uiState.lineEnding == code),
                    onClick = { viewModel.setLineEnding(code) },
                    label = { Text(label, style = CaptionStyle) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Tab 缩进大小", style = CaptionStyle, color = tc.text2)
        Spacer(modifier = Modifier.height(4.dp))
        val tabSizes = listOf(2, 4, 8)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabSizes.forEach { size ->
                FilterChip(
                    selected = (uiState.tabSize == size),
                    onClick = { viewModel.setTabSize(size) },
                    label = { Text("$size 空格", style = CaptionStyle) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * [SettingSwitchRow] 开关型设置项组件。
 */
@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val tc = LocalThemeColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = tc.text)
            Text(text = subtitle, style = CaptionStyle, color = tc.text2)
        }
        // 绑定主题语义色，保障在浅色/深色模式及不同配色风格下开启与关闭状态均美观协调
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = tc.primary,
                checkedThumbColor = tc.surface,
                uncheckedTrackColor = tc.surface2,
                uncheckedThumbColor = tc.text3,
                uncheckedBorderColor = tc.line
            )
        )
    }
}

