package com.feige.snippetstudio.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.detail.components.*
import com.feige.snippetstudio.ui.theme.*
import kotlinx.coroutines.launch

/**
 * [DetailScreen] 代码片段详情查看与交互管理主界面。
 *
 * 模块化装配设计：
 * 1. **TopAppBar**：返回导航按钮、标题与 Git 版本历史入口。
 * 2. **Hero 顶层头部卡片 [DetailHeroCard]**：展示语言图标、大字标题、创建时间、文件名与标签流。
 * 3. **4 大快捷动作网格 [DetailActionGrid]**：【编辑】、【运行定位】、【全量复制】与【删除】。
 * 4. **实时运行预览面板 [DetailPanel] + [RunPreview]**：交互式代码运行嵌入与结果展现。
 * 5. **语法高亮源码面板 [DetailSourcePanel]**：集成 [SyntaxHighlighter] 富文本与代码行号阅读器，支持平滑折叠/展开。
 * 6. **元数据面板 [DetailInfoPanel]**：展示文件真实大小、包含文件夹、修改时间与 Git 仓状态。
 *
 * @param viewModel 详情页状态与数据控制器
 * @param onBack 页面返回闭包
 * @param onNavigateToEditor 跳转至代码编辑器路由闭包
 * @param onShowSnackbar 底部全局 Snackbar 消息提示闭包
 * @param onNavigateToHistory 跳转至 Git 版本历史闭包 (可选)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
    onNavigateToHistory: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val tc = LocalThemeColors.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 交互弹窗状态管理
    var showTrashDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }

    val snippet = uiState.snippet

    // 加载中状态反馈
    if (uiState.isLoading || snippet == null) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_btn")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "返回",
                            tint = tc.text
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.detail_title),
                        style = SectionTitleStyle,
                        color = tc.text
                    )
                },
                actions = {
                    // Git 历史记录入口
                    if (onNavigateToHistory != null) {
                        IconButton(onClick = { onNavigateToHistory(snippet.id) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_git),
                                contentDescription = stringResource(R.string.menu_git_history),
                                tint = tc.text
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.bg
                )
            )
        },
        containerColor = tc.bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(Spacing.S4),
            verticalArrangement = Arrangement.spacedBy(Spacing.S4)
        ) {
            // ===== 1. Hero 顶层头部卡片 =====
            DetailHeroCard(
                snippet = snippet,
                onRenameClick = { showRenameDialog = true },
                onTagClick = { showTagDialog = true }
            )

            // ===== 2. 4 大快捷动作按钮网格 =====
            DetailActionGrid(
                onEditClick = { onNavigateToEditor(snippet.id) },
                onRunClick = {
                    // 平滑滚动定位至预览卡片或启动编辑
                    coroutineScope.launch {
                        scrollState.animateScrollTo(400)
                    }
                },
                onCopyClick = {
                    clipboardManager.setText(AnnotatedString(snippet.content))
                    onShowSnackbar(context.getString(R.string.toast_copied))
                },
                onDeleteClick = { showTrashDialog = true }
            )

            // ===== 3. "运行预览" 嵌入式面板 =====
            DetailPanel(
                title = stringResource(R.string.detail_preview)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    RunPreview(
                        type = snippet.type,
                        content = snippet.content,
                        onToast = onShowSnackbar
                    )
                }
            }

            // ===== 4. "源码片段" 语法高亮与带行号可折叠面板 =====
            DetailSourcePanel(
                snippet = snippet,
                isExpanded = uiState.isSourceExpanded,
                onToggleExpanded = { viewModel.toggleSourceExpanded() },
                onShowSnackbar = onShowSnackbar
            )

            // ===== 5. "详细信息与元数据" 面板 =====
            DetailInfoPanel(
                snippet = snippet,
                onFolderClick = { showFolderDialog = true }
            )
        }

        // ===== 交互弹窗集合 =====

        // 1. 重命名弹窗
        RenameDialog(
            show = showRenameDialog,
            initialTitle = snippet.title,
            initialFileName = snippet.fileName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newTitle, newFileName ->
                viewModel.renameSnippet(newTitle, newFileName)
                onShowSnackbar(context.getString(R.string.toast_renamed))
            }
        )

        // 2. 文件夹移动弹窗
        FolderMoveDialog(
            show = showFolderDialog,
            currentFolder = snippet.folder,
            existingFolders = uiState.existingFolders,
            onDismiss = { showFolderDialog = false },
            onConfirm = { targetFolder ->
                viewModel.updateFolder(targetFolder)
                onShowSnackbar(context.getString(R.string.toast_moved_folder))
            }
        )

        // 3. 标签编辑弹窗
        TagEditDialog(
            show = showTagDialog,
            initialTags = snippet.tags,
            allAvailableTags = uiState.allAvailableTags,
            onDismiss = { showTagDialog = false },
            onSave = { updatedTags ->
                viewModel.updateTags(updatedTags)
                onShowSnackbar(context.getString(R.string.toast_tags_updated))
            }
        )

        // 4. 放入回收站确认弹窗
        ConfirmDialog(
            show = showTrashDialog,
            title = stringResource(R.string.confirm_trash_title),
            desc = stringResource(R.string.confirm_trash_desc),
            onConfirm = {
                viewModel.trashSnippet {
                    onShowSnackbar(context.getString(R.string.toast_trashed))
                    onBack()
                }
            },
            onDismiss = { showTrashDialog = false },
            isDanger = true
        )
    }
}
