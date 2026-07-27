package com.feige.snippetstudio.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.theme.*

/**
 * [HomeScreen] 应用程序首页主界面视图。
 *
 * 界面组成结构：
 * 1. **TopBar 顶部栏**：应用名称标题与实时刷新同步按钮。
 * 2. **ClipBar 剪贴板识别条**：当 Activity 处于 Resume 状态时触发扫描，展示识别到的智能代码条。
 * 3. **SearchBar 搜索输入框**：支持按标题、正文与标签实时过滤代码片段。
 * 4. **2x2 快捷新建卡片区**：一键快速创建 HTML, JS, Markdown 与 Prompt 代码片段。
 * 5. **最近代码片段列表**：使用 [LazyColumn] 展示前 5 个活动片段，集成 [SnippetCard] 的富文本交互。
 * 6. **交互模态对话框**：包含重命名 [RenameDialog]、移动文件夹 [FolderMoveDialog] 与确认移入回收站 [ConfirmDialog]。
 *
 * @param viewModel 首页 ViewModel 依赖
 * @param onNavigateToEditor 打开编辑器路由
 * @param onNavigateToNewEditor 打开新建类型编辑器路由
 * @param onNavigateToDetail 打开片段详情路由
 * @param onNavigateToFiles 跳转至文件列表管理路由
 * @param onShowSnackbar 显示底部 Snack 提示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToNewEditor: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToFiles: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    // 监听 ViewModel 中的 Flow 状态
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val tc = LocalThemeColors.current

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // 各种交互操作的临时挂起状态
    var pendingTrashId by remember { mutableStateOf<String?>(null) }
    var pendingRenameSnippet by remember { mutableStateOf<com.feige.snippetstudio.model.Snippet?>(null) }
    var pendingFolderSnippet by remember { mutableStateOf<com.feige.snippetstudio.model.Snippet?>(null) }
    var showSearchBar by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    // ===== 监听 Lifecycle 声明周期：返回前台 ON_RESUME 时自动检索系统剪贴板 =====
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

    // ===== 列表滚动时隐藏/显示搜索栏 =====
    var previousScrollIndex by remember { mutableStateOf(0) }
    var previousScrollOffset by remember { mutableStateOf(0) }
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val scrollingDown = index > previousScrollIndex || (index == previousScrollIndex && offset > previousScrollOffset)
            if (scrollingDown && showSearchBar && index > 0) {
                showSearchBar = false
            } else if (!scrollingDown && !showSearchBar) {
                showSearchBar = true
            }
            previousScrollIndex = index
            previousScrollOffset = offset
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = DisplayTitleStyle,
                            color = tc.text
                        )
                        Spacer(modifier = Modifier.width(Spacing.S2))
                        Surface(
                            color = tc.primarySoft,
                            shape = RoundedCornerShape(R_SM)
                        ) {
                            Text(
                                text = stringResource(R.string.home_subtitle),
                                style = BadgeStyle,
                                color = tc.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onShowSnackbar(context.getString(R.string.toast_sync_ok)) },
                        modifier = Modifier.testTag("home_refresh_btn")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Refresh",
                            tint = tc.text2
                        )
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
        ) {
            // ===== 1. 剪贴板识别快捷添加条 =====
            ClipBar(
                clip = uiState.detectedClip,
                onSave = { clip ->
                    viewModel.saveClip(clip) { id ->
                        onNavigateToEditor(id)
                    }
                },
                onDismiss = { clip ->
                    viewModel.ignoreClip(clip)
                }
            )

            // ===== 2. 搜索框 =====
            AnimatedVisibility(
                visible = showSearchBar,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it }
            ) {
                SearchBar(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = stringResource(R.string.home_search),
                    modifier = Modifier.padding(horizontal = Spacing.S4, vertical = Spacing.S2)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // ===== 3. 2x2 快捷新建类型入口卡片区 =====
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.S4, vertical = Spacing.S3)
                    ) {
                        Text(
                            text = stringResource(R.string.home_quick_new),
                            style = SectionTitleStyle,
                            color = tc.text
                        )

                        Spacer(modifier = Modifier.height(Spacing.S3))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
                        ) {
                            QuickNewCard(
                                type = SnippetType.HTML,
                                onClick = { onNavigateToNewEditor(SnippetType.HTML.code) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickNewCard(
                                type = SnippetType.JS,
                                onClick = { onNavigateToNewEditor(SnippetType.JS.code) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.S3))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
                        ) {
                            QuickNewCard(
                                type = SnippetType.MARKDOWN,
                                onClick = { onNavigateToNewEditor(SnippetType.MARKDOWN.code) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickNewCard(
                                type = SnippetType.PROMPT,
                                onClick = { onNavigateToNewEditor(SnippetType.PROMPT.code) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.S3))

                        QuickNewCard(
                            type = SnippetType.GENERAL,
                            onClick = { onNavigateToNewEditor(SnippetType.GENERAL.code) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ===== 4. 最近修改代码片段列表 Header =====
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.S4, vertical = Spacing.S3),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.home_recent),
                            style = SectionTitleStyle,
                            color = tc.text
                        )

                        Text(
                            text = "${stringResource(R.string.home_view_all)} (${uiState.totalActiveCount})",
                            style = ListTitleStyle,
                            color = tc.primary,
                            modifier = Modifier
                                .clickable { onNavigateToFiles() }
                                .testTag("view_all_link")
                        )
                    }
                }

                // ===== 5. 最近代码片段列表或空状态 =====
                if (uiState.isLoading) {
                    item { LoadingState() }
                } else if (uiState.recentSnippets.isEmpty()) {
                    item {
                        EmptyState(
                            title = if (uiState.searchQuery.isNotEmpty()) stringResource(R.string.empty_filter_title) else stringResource(R.string.empty_none_title),
                            desc = if (uiState.searchQuery.isNotEmpty()) stringResource(R.string.empty_filter_desc) else stringResource(R.string.empty_none_desc),
                            actionLabel = if (uiState.searchQuery.isEmpty()) stringResource(R.string.sheet_new_title) else null,
                            onAction = if (uiState.searchQuery.isEmpty()) { { onNavigateToNewEditor(SnippetType.HTML.code) } } else null
                        )
                    }
                } else {
                    // ===== 5. 最近修改代码片段列表（使用与文件中心一致的 SnippetPreviewCard 大卡片带预览模式） =====
                    items(
                        items = uiState.recentSnippets,
                        key = { it.id }
                    ) { snippet ->
                        val onOpen = {
                            if (uiState.cardClickAction == "editor") {
                                onNavigateToEditor(snippet.id)
                            } else {
                                onNavigateToDetail(snippet.id)
                            }
                        }
                        val onCopy = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(snippet.content))
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
                            modifier = Modifier.padding(horizontal = Spacing.S4, vertical = Spacing.S2)
                        )
                    }
                }
            }
        }

        // ===== 弹框 1: 重命名对话框 =====
        RenameDialog(
            show = (pendingRenameSnippet != null),
            initialTitle = pendingRenameSnippet?.title.orEmpty(),
            initialFileName = pendingRenameSnippet?.fileName.orEmpty(),
            onDismiss = { pendingRenameSnippet = null },
            onConfirm = { newTitle, newFileName ->
                pendingRenameSnippet?.let { snippet ->
                    viewModel.renameSnippet(snippet.id, newTitle, newFileName)
                    onShowSnackbar("片段已重命名")
                }
            }
        )

        // ===== 弹框 2: 移动文件夹对话框 =====
        FolderMoveDialog(
            show = (pendingFolderSnippet != null),
            currentFolder = pendingFolderSnippet?.folder.orEmpty(),
            existingFolders = uiState.existingFolders,
            onDismiss = { pendingFolderSnippet = null },
            onConfirm = { targetFolder ->
                pendingFolderSnippet?.let { snippet ->
                    viewModel.updateFolder(snippet.id, targetFolder)
                    onShowSnackbar("已移动至文件夹")
                }
            }
        )

        // ===== 弹框 3: 移入回收站二次确认对话框 =====
        ConfirmDialog(
            show = (pendingTrashId != null),
            title = stringResource(R.string.confirm_trash_title),
            desc = stringResource(R.string.confirm_trash_desc),
            onConfirm = {
                pendingTrashId?.let { id ->
                    viewModel.trashSnippet(id)
                    onShowSnackbar(context.getString(R.string.toast_trashed))
                }
            },
            onDismiss = { pendingTrashId = null },
            isDanger = true
        )
    }
}

/**
 * [QuickNewCard] 首页 2x2 网格中单个快捷创建类型的卡片按钮组件。
 */
@Composable
fun QuickNewCard(
    type: SnippetType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Surface(
        modifier = modifier
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
            .clickable(onClick = onClick)
            .testTag("quick_new_${type.code}"),
        shape = RoundedCornerShape(R_MD),
        color = tc.surface
    ) {
        Row(
            modifier = Modifier.padding(Spacing.S3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TypeIcon(type = type, size = 36.dp)
            Spacer(modifier = Modifier.width(Spacing.S3))
            Text(
                text = type.displayName,
                style = ListTitleStyle,
                color = tc.text
            )
        }
    }
}

