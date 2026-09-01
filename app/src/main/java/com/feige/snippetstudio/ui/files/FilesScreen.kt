package com.feige.snippetstudio.ui.files

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.common.LocalSnackbarManager
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.files.components.*
import com.feige.snippetstudio.ui.theme.*

/**
 * [FilesScreen] 全量代码片段与文件中心仓库主界面。
 *
 * 模块化架构结构：
 * 1. **[FilesTopBar] 顶栏控制组件**：
 *    - 支持显式新建空文件夹 [FolderCreateDialog]。
 *    - 切换【大卡片预览 COMFORT】与【极嵌高密度 COMPACT】显示密度。
 *    - 切换【平铺列表 ViewMode.FLAT】与【树状文件夹 ViewMode.TREE】视图。
 *    - 下拉选择排序模式 (SortMode: 修改时间降序 / 片段名称升序 / 类型升序)。
 * 2. **[SearchBar] 实时搜索栏**：基于平滑滚动防抖算法自动收起/展开，支持搜索标题、正文及标签。
 * 3. **[FilterChipsRow] 分类 Chip 滚动条**：按【全部 / 收藏 / HTML / JS / Markdown / Prompt】进行条件过滤。
 * 4. **多模式列表组件**：
 *    - 平铺大卡片模式：[FilesComfortList]
 *    - 平铺高密度列表：[FilesCompactList]
 *    - 可折叠树状视图：[FilesTreeList]
 * 5. **交互弹框集合**：
 *    - 重命名代码片段 [RenameDialog]
 *    - 移动文件夹 [FolderMoveDialog]
 *    - 重命名文件夹 [FolderRenameDialog]
 *    - 新建文件夹 [FolderCreateDialog]
 *    - 移入回收站确认 [ConfirmDialog]
 *
 * @param viewModel 文件仓库 ViewModel 控制器
 * @param onNavigateToDetail 导航至详情页回调
 * @param onNavigateToEditor 导航至编辑器页回调
 * @param onNavigateToNewEditor 导航至新建编辑器页回调
 * @param onShowSnackbar 底部提示弹窗回调
 */
@Composable
fun FilesScreen(
    viewModel: FilesViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToNewEditor: (String) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tc = LocalThemeColors.current

    // 挂起的操作状态记录
    var pendingTrashId by remember { mutableStateOf<String?>(null) }
    var pendingRenameSnippet by remember { mutableStateOf<Snippet?>(null) }
    var pendingFolderSnippet by remember { mutableStateOf<Snippet?>(null) }
    var pendingRenameFolderName by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(true) }

    // 列表滚动状态
    val flatComfortListState = rememberLazyListState()
    val flatCompactListState = rememberLazyListState()
    val treeListState = rememberLazyListState()
    val activeListState = when {
        uiState.viewMode == ViewMode.TREE -> treeListState
        uiState.densityMode == DensityMode.COMFORT -> flatComfortListState
        else -> flatCompactListState
    }

    var previousScrollIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    // 优化：更加平滑与稳定的滚动防抖判定，结合 distinctUntilChanged 避免无意义重绘
    LaunchedEffect(activeListState) {
        previousScrollIndex = 0
        previousScrollOffset = 0
        snapshotFlow {
            activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset
        }
        .distinctUntilChanged()
        .collect { (index, offset) ->
            val delta = if (index != previousScrollIndex) {
                (index - previousScrollIndex) * 400 + (offset - previousScrollOffset)
            } else {
                offset - previousScrollOffset
            }

            // 平滑防抖判定：下滑位移超过 60px 且不在顶端时收起，上滑位移超过 50px 时展开，消除小幅度颤动
            if (delta > 60 && showSearchBar && (index > 0 || offset > 30)) {
                showSearchBar = false
            } else if (delta < -50 && !showSearchBar) {
                showSearchBar = true
            }

            // 滚动回到最顶部附近时自动展开搜索栏
            if (index == 0 && offset < 10) {
                showSearchBar = true
            }

            previousScrollIndex = index
            previousScrollOffset = offset
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            FilesTopBar(
                sortMode = uiState.sortMode,
                viewMode = uiState.viewMode,
                densityMode = uiState.densityMode,
                onCreateFolderClick = { showCreateFolderDialog = true },
                onToggleDensityClick = { viewModel.toggleDensityMode() },
                onToggleViewModeClick = { viewModel.toggleViewMode() },
                onSelectSortMode = { viewModel.setSortMode(it) }
            )
        },
        containerColor = tc.bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ===== 1. 搜索框（使用 expandVertically/shrinkVertically 柔缓垂直折叠动画）=====
            AnimatedVisibility(
                visible = showSearchBar,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SearchBar(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = stringResource(R.string.files_search),
                    modifier = Modifier.padding(horizontal = Spacing.S4, vertical = Spacing.S2)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.S1))

            // ===== 2. 分类 Filter Chips 条件筛选滑动条 =====
            FilterChipsRow(
                selected = uiState.filterOption,
                onSelect = { viewModel.onFilterSelect(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.S2))

            // ===== 3. 代码片段列表或空状态展示 =====
            if (uiState.isLoading) {
                LoadingState()
            } else if (uiState.snippets.isEmpty() && uiState.groupedFolders.isEmpty()) {
                val isFiltered = uiState.searchQuery.isNotEmpty() || uiState.filterOption != FilterOption.All
                EmptyState(
                    title = if (isFiltered) stringResource(R.string.empty_filter_title) else stringResource(R.string.empty_none_title),
                    desc = if (isFiltered) stringResource(R.string.empty_filter_desc) else stringResource(R.string.empty_none_desc),
                    actionLabel = if (isFiltered) "重置筛选条件" else stringResource(R.string.sheet_new_title),
                    onAction = if (isFiltered) {
                        {
                            viewModel.onSearchQueryChange("")
                            viewModel.onFilterSelect(FilterOption.All)
                        }
                    } else {
                        { onNavigateToNewEditor(SnippetType.HTML.code) }
                    }
                )
            } else {
                if (uiState.viewMode == ViewMode.FLAT) {
                    // ===== 视图 A: FLAT 平铺视图 =====
                    if (uiState.densityMode == DensityMode.COMFORT) {
                        FilesComfortList(
                            snippets = uiState.snippets,
                            listState = flatComfortListState,
                            cardClickAction = uiState.cardClickAction,
                            searchQuery = uiState.searchQuery,
                            onNavigateToDetail = onNavigateToDetail,
                            onNavigateToEditor = onNavigateToEditor,
                            onRename = { pendingRenameSnippet = it },
                            onMoveFolder = { pendingFolderSnippet = it },
                            onToggleStar = { viewModel.toggleStar(it.id, it.starred) },
                            onTrash = { pendingTrashId = it.id },
                            onShowSnackbar = onShowSnackbar
                        )
                    } else {
                        FilesCompactList(
                            snippets = uiState.snippets,
                            listState = flatCompactListState,
                            cardClickAction = uiState.cardClickAction,
                            onNavigateToDetail = onNavigateToDetail,
                            onNavigateToEditor = onNavigateToEditor,
                            onRename = { pendingRenameSnippet = it },
                            onMoveFolder = { pendingFolderSnippet = it },
                            onToggleStar = { viewModel.toggleStar(it.id, it.starred) },
                            onTrash = { pendingTrashId = it.id }
                        )
                    }
                } else {
                    // ===== 视图 B: TREE 可折叠树状视图 =====
                    FilesTreeList(
                        groupedFolders = uiState.groupedFolders,
                        densityMode = uiState.densityMode,
                        listState = treeListState,
                        cardClickAction = uiState.cardClickAction,
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToEditor = onNavigateToEditor,
                        onRename = { pendingRenameSnippet = it },
                        onRenameFolder = { pendingRenameFolderName = it },
                        onMoveFolder = { pendingFolderSnippet = it },
                        onToggleStar = { viewModel.toggleStar(it.id, it.starred) },
                        onTrash = { pendingTrashId = it.id },
                        onShowSnackbar = onShowSnackbar
                    )
                }
            }
        }

        // ===== 弹框集合 =====
        FolderCreateDialog(
            show = showCreateFolderDialog,
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { folderName ->
                viewModel.createFolder(folderName)
                onShowSnackbar(context.getString(R.string.toast_folder_created, folderName))
            }
        )

        FolderRenameDialog(
            show = (pendingRenameFolderName != null),
            initialFolderName = pendingRenameFolderName.orEmpty(),
            onDismiss = { pendingRenameFolderName = null },
            onConfirm = { newFolderName ->
                pendingRenameFolderName?.let { oldFolder ->
                    viewModel.renameFolder(oldFolder, newFolderName)
                    onShowSnackbar(context.getString(R.string.toast_folder_renamed, newFolderName))
                }
            }
        )

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
