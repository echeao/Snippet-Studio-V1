package com.feige.snippetstudio.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.common.LocalSnackbarManager
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.home.components.*
import com.feige.snippetstudio.ui.theme.*

/**
 * [HomeScreen] 应用程序首页主界面视图。
 *
 * 架构设计与界面组成：
 * 1. **TopBar 顶部栏 ([HomeTopBar])**：呈现应用 Logo、状态指示与仓库同步按钮。
 * 2. **ClipBar 剪贴板识别卡片 ([ClipBar])**：顶部 Overlay 悬浮弹窗，监听 Lifecycle [Lifecycle.Event.ON_RESUME] 事件实现动态浮现。
 * 3. **StatsBar 统计小部件 ([HomeStatsBar])**：仪表盘全景展现（代码片段总数、已收藏数、文件夹总数）。
 * 4. **QuickNewSection 快捷新建区**：2x2 快捷类型网格，支持物理按压微动效与品牌微光边框。
 * 5. **RecentSnippetsSection 最近片段列表**：展示最新编辑的代码片段大卡片及其管理上下文菜单。
 * 6. **模态交互对话框**：包含重命名 [RenameDialog]、移动文件夹 [FolderMoveDialog] 与回收站确认 [ConfirmDialog]。
 *
 * @param viewModel 首页 ViewModel 逻辑依赖
 * @param onNavigateToEditor 导航跳转至代码编辑器的回调函数 (传入片段 ID)
 * @param onNavigateToNewEditor 导航跳转至新建指定类型代码编辑器的回调函数 (传入语言 Code)
 * @param onNavigateToDetail 导航跳转至代码片段详情页面的回调函数 (传入片段 ID)
 * @param onNavigateToFiles 导航跳转至全量文件列表管理页面的回调函数
 * @param onShowSnackbar 底部消息 Snackbar 提示回调函数
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToNewEditor: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToFiles: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    // 监听 ViewModel 中的响应式 UI 状态流
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tc = LocalThemeColors.current
    val clipboardManager = LocalClipboardManager.current

    // 交互操作的挂起临时状态
    var pendingTrashId by remember { mutableStateOf<String?>(null) }
    var pendingRenameSnippet by remember { mutableStateOf<Snippet?>(null) }
    var pendingFolderSnippet by remember { mutableStateOf<Snippet?>(null) }

    val listState = rememberLazyListState()

    // ===== 1. 监听 Lifecycle 生命周期：APP 返回前台 ON_RESUME 时自动检测剪贴板 =====
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkClipboard(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(
                onRefreshClick = { onShowSnackbar(context.getString(R.string.toast_sync_ok)) }
            )
        },
        containerColor = tc.bg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ===== A. 主内容可滚动区域 =====
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // ===== A-1. Dashboard 概览数据统计小部件 =====
                item {
                    HomeStatsBar(
                        totalCount = uiState.totalActiveCount,
                        starredCount = uiState.starredCount,
                        folderCount = uiState.existingFolders.size,
                        modifier = Modifier.padding(horizontal = Spacing.S4, vertical = Spacing.S2)
                    )
                }

                // ===== A-2. 2x2 快捷新建类型入口卡片区 =====
                item {
                    QuickNewSection(
                        onNavigateToNewEditor = onNavigateToNewEditor
                    )
                }

                // ===== A-2.5. 收藏片段横向滑动快捷区（仅当有收藏时显示） =====
                if (uiState.starredSnippets.isNotEmpty()) {
                    item {
                        StarredSection(
                            starredSnippets = uiState.starredSnippets,
                            onSnippetClick = { id ->
                                if (uiState.cardClickAction == "editor") onNavigateToEditor(id)
                                else onNavigateToDetail(id)
                            },
                            modifier = Modifier.padding(vertical = Spacing.S2)
                        )
                    }
                }

                // ===== A-3. 最近修改代码片段列表 Header =====
                item {
                    RecentHeader(
                        totalActiveCount = uiState.totalActiveCount,
                        onNavigateToFiles = onNavigateToFiles
                    )
                }

                // ===== A-4. 最近代码片段列表或空状态处理 =====
                if (uiState.isLoading) {
                    item { LoadingState() }
                } else if (uiState.recentSnippets.isEmpty()) {
                    item {
                        HomeEmptyState(
                            searchQuery = uiState.searchQuery,
                            onClearSearch = { viewModel.onSearchQueryChange("") },
                            onNewSnippet = { onNavigateToNewEditor(SnippetType.HTML.code) }
                        )
                    }
                } else {
                    // ===== 最近 5 条代码片段（利用 key 与 contentType 保障 LazyColumn 节点精准复用与过渡效率）=====
                    items(
                        items = uiState.recentSnippets,
                        key = { it.id },
                        contentType = { "recent_snippet_preview" }
                    ) { snippet ->
                        val onOpen = {
                            if (uiState.cardClickAction == "editor") {
                                onNavigateToEditor(snippet.id)
                            } else {
                                onNavigateToDetail(snippet.id)
                            }
                        }
                        val onCopy = {
                            clipboardManager.setText(AnnotatedString(snippet.content))
                            onShowSnackbar(context.getString(R.string.toast_copied))
                        }

                        SnippetPreviewCard(
                            snippet = snippet,
                            onOpen = onOpen,
                            onCopySnippet = onCopy,
                            onRename = { pendingRenameSnippet = snippet },
                            onMoveFolder = { pendingFolderSnippet = snippet },
                            onToggleStar = { viewModel.toggleStar(snippet.id, snippet.starred) },
                            onMore = { pendingTrashId = snippet.id },
                            showFullDateTime = true,
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = Spacing.S4, vertical = Spacing.S2)
                        )
                    }
                }
            }

            // ===== B. 剪贴板识别智能快捷悬浮弹窗（Overlay 绝对定位在顶部）=====
            ClipBar(
                clip = uiState.detectedClip,
                onSave = { clip ->
                    viewModel.saveClip(clip) { id ->
                        onNavigateToEditor(id)
                    }
                },
                onDismiss = { clip ->
                    viewModel.ignoreClip(clip)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
            )
        }

        // ===== 弹框 1: 代码片段重命名对话框 =====
        RenameDialog(
            show = (pendingRenameSnippet != null),
            initialTitle = pendingRenameSnippet?.title.orEmpty(),
            initialFileName = pendingRenameSnippet?.fileName.orEmpty(),
            onDismiss = { pendingRenameSnippet = null },
            onConfirm = { newTitle, newFileName ->
                pendingRenameSnippet?.let { snippet ->
                    viewModel.renameSnippet(snippet.id, newTitle, newFileName)
                    onShowSnackbar(context.getString(R.string.toast_renamed))
                }
            }
        )

        // ===== 弹框 2: 移动归属文件夹对话框 =====
        FolderMoveDialog(
            show = (pendingFolderSnippet != null),
            currentFolder = pendingFolderSnippet?.folder.orEmpty(),
            existingFolders = uiState.existingFolders,
            onDismiss = { pendingFolderSnippet = null },
            onConfirm = { targetFolder ->
                pendingFolderSnippet?.let { snippet ->
                    viewModel.updateFolder(snippet.id, targetFolder)
                    onShowSnackbar(context.getString(R.string.toast_moved_folder))
                }
            }
        )

        // ===== 弹框 3: 移入回收站二次确认对话框（带撤销 Action） =====
        val snackbarManager = LocalSnackbarManager.current
        ConfirmDialog(
            show = (pendingTrashId != null),
            title = stringResource(R.string.confirm_trash_title),
            desc = stringResource(R.string.confirm_trash_desc),
            onConfirm = {
                pendingTrashId?.let { id ->
                    viewModel.trashSnippet(id)
                    snackbarManager.showSnackbar(
                        message = context.getString(R.string.toast_trashed),
                        actionLabel = context.getString(R.string.toast_undo),
                        onAction = { viewModel.restoreSnippet(id) }
                    )
                }
            },
            onDismiss = { pendingTrashId = null },
            isDanger = true
        )
    }
}
