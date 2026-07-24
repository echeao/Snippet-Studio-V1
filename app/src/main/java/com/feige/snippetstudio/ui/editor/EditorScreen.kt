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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.SystemUiUtil
import androidx.compose.animation.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light

    var showDiscardDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    var showFloatingIsland by remember { mutableStateOf(true) }
    var showFullscreenSymbolBar by remember { mutableStateOf(true) }

    val activity = remember(context) { SystemUiUtil.findActivity(context) }
    DisposableEffect(uiState.isFullscreen) {
        if (uiState.isFullscreen) {
            SystemUiUtil.setImmersiveFullscreen(activity, true)
        }
        onDispose {
            SystemUiUtil.setImmersiveFullscreen(activity, false)
        }
    }

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
            onBack()
        }
    }

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
                .background(if (isDark) BgDark else BgLight),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
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

    val editorContent: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier) {
            // Editor Toolbar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.S3, vertical = Spacing.S2),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented Control [Code | Preview]
                SegmentedControl(
                    options = listOf(stringResource(R.string.editor_code), stringResource(R.string.editor_preview)),
                    selectedIndex = uiState.selectedTab,
                    onSelect = { viewModel.selectTab(it) }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Font Size - / +
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { viewModel.adjustFontSize(-1f) },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("A−", style = CaptionStyle, color = textSecondary)
                        }
                        TextButton(
                            onClick = { viewModel.adjustFontSize(+1f) },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("A+", style = CaptionStyle, color = textSecondary)
                        }
                    }

                    // Fullscreen Button
                    IconButton(
                        onClick = { viewModel.toggleFullscreen() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("fullscreen_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (uiState.isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = if (uiState.isFullscreen) "Exit Fullscreen" else "Fullscreen",
                            tint = if (uiState.isFullscreen) Primary else textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Main Editor / Preview View
            if (uiState.selectedTab == 0) {
                // Code Tab
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
                        isWordWrap = uiState.isWordWrap,
                        showLineNumbers = uiState.showLineNumbers,
                        highlightCurrentLine = uiState.highlightCurrentLine,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Preview Tab
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
        // True Immersive Fullscreen Container
        val safeTopPadding = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) BgDark else BgLight)
                .nestedScroll(nestedScrollConnection)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) {
                    showFloatingIsland = !showFloatingIsland
                }
        ) {
            // Main Code / Preview Layout in Fullscreen
            if (uiState.selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CodeEditor(
                        textFieldValue = uiState.textFieldValue,
                        onValueChange = { viewModel.onTextFieldValueChange(it) },
                        fontSp = uiState.fontSp,
                        currentLineIndex = uiState.currentLineIndex,
                        snippetType = uiState.type,
                        isWordWrap = uiState.isWordWrap,
                        showLineNumbers = uiState.showLineNumbers,
                        highlightCurrentLine = uiState.highlightCurrentLine,
                        topContentPadding = safeTopPadding,
                        modifier = Modifier.weight(1f)
                    )

                    // Floating SymbolBar in Fullscreen
                    AnimatedVisibility(
                        visible = showFullscreenSymbolBar && showFloatingIsland,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
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

            // Top Floating Control Island
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
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { handleBack() },
                            modifier = Modifier.testTag("editor_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary
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
                                    color = textPrimary
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(Primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_title_input"),
                                decorationBox = { innerTextField ->
                                    if (uiState.title.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.editor_rename_hint),
                                            style = TextStyle(fontSize = 16.sp, color = textSecondary)
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            Spacer(modifier = Modifier.width(Spacing.S2))

                            // Save status badge
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
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "More",
                                    tint = textPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("编辑区设置", style = CaptionStyle, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        showSettingsSheet = true
                                    },
                                    modifier = Modifier.testTag("menu_editor_settings")
                                )
                                DropdownMenuItem(
                                    text = { Text("切换语言类型", style = CaptionStyle) },
                                    leadingIcon = { Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        showTypeDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("编辑片段标签", style = CaptionStyle) },
                                    leadingIcon = { Icon(Icons.Filled.Tag, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        showTagDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("复制全部代码", style = CaptionStyle) },
                                    leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp)) },
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
                                    leadingIcon = { Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.forceSaveNow()
                                        onShowSnackbar("已保存")
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDark) BgDark else BgLight
                    )
                )
            },
            bottomBar = {
                // Status bar at bottom
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .border(1.dp, if (isDark) LineDark else LineLight),
                    color = if (isDark) Surface2Dark else Surface2Light
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
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            color = PrimarySoft,
                            shape = RoundedCornerShape(R_SM),
                            modifier = Modifier.clickable { showTypeDialog = true }
                        ) {
                            Text(
                                text = uiState.type.displayName,
                                style = BadgeStyle,
                                color = Primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            containerColor = if (isDark) BgDark else BgLight
        ) { innerPadding ->
            editorContent(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    // Editor Settings Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (isDark) SurfaceDark else SurfaceLight
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

    // Type Switcher Dialog
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
                                Text(text = type.displayName, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text(text = "扩展名: ${type.extension}", style = CaptionStyle, color = textSecondary)
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

    // Tag Edit Dialog
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

    // Discard Confirmation Dialog
    ConfirmDialog(
        show = showDiscardDialog,
        title = stringResource(R.string.confirm_discard_title),
        desc = "有未保存的更改，确定离开吗？",
        onConfirm = {
            showDiscardDialog = false
            onBack()
        },
        onDismiss = { showDiscardDialog = false },
        isDanger = true
    )
}

@Composable
private fun EditorSettingsContent(
    uiState: EditorUiState,
    viewModel: EditorViewModel,
    onClose: () -> Unit,
    onOpenTagsDialog: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light

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
                color = textPrimary
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = textSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 0: Fragment Tags
        Text("片段属性", style = CaptionStyle, color = Primary, fontWeight = FontWeight.Bold)
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
                Text(text = "管理代码标签", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                Text(
                    text = if (uiState.tags.isEmpty()) "暂无标签，点击添加" else uiState.tags.joinToString(", ") { "#$it" },
                    style = CaptionStyle,
                    color = textSecondary
                )
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = textSecondary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 1: Display & Layout
        Text("显示与排版", style = CaptionStyle, color = Primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Word Wrap
        SettingSwitchRow(
            title = "自动换行 (Word Wrap)",
            subtitle = "超出编辑器边缘时自动换行",
            checked = uiState.isWordWrap,
            onCheckedChange = { viewModel.setWordWrap(it) }
        )

        // Show Line Numbers
        SettingSwitchRow(
            title = "显示行号 (Line Numbers)",
            subtitle = "在代码左侧展示行号栏",
            checked = uiState.showLineNumbers,
            onCheckedChange = { viewModel.setShowLineNumbers(it) }
        )

        // Highlight Current Line
        SettingSwitchRow(
            title = "高亮当前行",
            subtitle = "高亮背景标记光标所在行",
            checked = uiState.highlightCurrentLine,
            onCheckedChange = { viewModel.setHighlightCurrentLine(it) }
        )

        // Auto Pair Brackets
        SettingSwitchRow(
            title = "自动括号/引号配对",
            subtitle = "输入括号与引号时自动补全闭合",
            checked = uiState.autoPairBrackets,
            onCheckedChange = { viewModel.setAutoPairBrackets(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Font Size Slider
        Text("代码字号: ${uiState.fontSp} sp", style = CaptionStyle, color = textPrimary, fontWeight = FontWeight.Medium)
        Slider(
            value = uiState.fontSp,
            onValueChange = { viewModel.adjustFontSize(it - uiState.fontSp) },
            valueRange = 11f..22f,
            steps = 11
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: File & Encoding
        Text("编码与换行符", style = CaptionStyle, color = Primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Encoding Choice
        Text("字符编码格式", style = CaptionStyle, color = textSecondary)
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

        // Line Endings
        Text("换行符格式", style = CaptionStyle, color = textSecondary)
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

        // Tab Size
        Text("Tab 缩进大小", style = CaptionStyle, color = textSecondary)
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

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textPrimary)
            Text(text = subtitle, style = CaptionStyle, color = textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
