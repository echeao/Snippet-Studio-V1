package com.feige.snippetstudio.ui.files

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.theme.*

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.AccountTree

/**
 * [FilesScreen] 文件与全量代码片段仓库主界面。
 *
 * 功能结构：
 * 1. **TopBar 顶部控制栏**：
 *    - 切换【平铺列表 ViewMode.FLAT】与【树状文件夹 ViewMode.TREE】的视觉视图。
 *    - 循环切换排序模式 (SortMode: 修改时间降序 / 片段名称升序 / 类型升序)。
 * 2. **SearchBar 搜索输入框**：支持实时搜索正文与标签。
 * 3. **FilterChipsRow 筛选 Chip 滚动条**：按【全部 / 收藏 / HTML / JS / Markdown / Prompt】进行分类筛选。
 * 4. **双视图模式渲染**：
 *    - **FLAT 平铺视图**：单列高密度卡片列表。
 *    - **TREE 树状分组视图**：自动提取 `folder` 相对路径形成树状卡片组。
 * 5. **交互弹框集合**：涵盖重命名 [RenameDialog]、移动文件夹 [FolderMoveDialog] 与删除弹窗 [ConfirmDialog]。
 *
 * @param viewModel 文件仓库 ViewModel
 * @param onNavigateToDetail 导航至详情页
 * @param onNavigateToEditor 导航至编辑器页
 * @param onNavigateToNewEditor 导航至新建编辑器页
 * @param onShowSnackbar 底部提示弹窗回调
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light

    var pendingTrashId by remember { mutableStateOf<String?>(null) }
    var pendingRenameSnippet by remember { mutableStateOf<com.feige.snippetstudio.model.Snippet?>(null) }
    var pendingFolderSnippet by remember { mutableStateOf<com.feige.snippetstudio.model.Snippet?>(null) }

    val sortLabel = when (uiState.sortMode) {
        SortMode.UPDATED_DESC -> stringResource(R.string.sort_updated)
        SortMode.NAME_ASC -> stringResource(R.string.sort_name)
        SortMode.TYPE_ASC -> stringResource(R.string.sort_type)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.files_title),
                        style = DisplayTitleStyle,
                        color = textPrimary
                    )
                },
                actions = {
                    // ===== 按钮 1: 切换【平铺 / 树状】视图模式 =====
                    IconButton(
                        onClick = { viewModel.toggleViewMode() },
                        modifier = Modifier.testTag("files_view_mode_btn")
                    ) {
                        Icon(
                            imageVector = if (uiState.viewMode == ViewMode.FLAT) Icons.Filled.AccountTree else Icons.Filled.List,
                            contentDescription = "Toggle View Mode",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // ===== 按钮 2: 循环切换排序字段 =====
                    TextButton(
                        onClick = { viewModel.cycleSortMode() },
                        modifier = Modifier.testTag("files_sort_btn")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sort),
                            contentDescription = "Sort",
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.S1))
                        Text(text = sortLabel, style = CaptionStyle, color = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) BgDark else BgLight
                )
            )
        },
        containerColor = if (isDark) BgDark else BgLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ===== 1. 搜索框 =====
            SearchBar(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = stringResource(R.string.files_search),
                modifier = Modifier.padding(horizontal = Spacing.S4, vertical = Spacing.S2)
            )

            Spacer(modifier = Modifier.height(Spacing.S1))

            // ===== 2. 类型与状态 Filter Chips 筛选滑动条 =====
            FilterChipsRow(
                selected = uiState.filterOption,
                onSelect = { viewModel.onFilterSelect(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.S2))

            // ===== 3. 代码片段列表或空状态展示 =====
            if (uiState.isLoading) {
                LoadingState()
            } else if (uiState.snippets.isEmpty()) {
                val isFiltered = uiState.searchQuery.isNotEmpty() || uiState.filterOption != FilterOption.All
                EmptyState(
                    title = if (isFiltered) stringResource(R.string.empty_filter_title) else stringResource(R.string.empty_none_title),
                    desc = if (isFiltered) stringResource(R.string.empty_filter_desc) else stringResource(R.string.empty_none_desc),
                    actionLabel = if (!isFiltered) stringResource(R.string.sheet_new_title) else null,
                    onAction = if (!isFiltered) { { onNavigateToNewEditor(SnippetType.HTML.code) } } else null
                )
            } else {
                if (uiState.viewMode == ViewMode.FLAT) {
                    // ===== 视图 A: FLAT 平铺视图 =====
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            items = uiState.snippets,
                            key = { it.id }
                        ) { snippet ->
                            SnippetCard(
                                snippet = snippet,
                                onOpen = {
                                    if (uiState.cardClickAction == "editor") {
                                        onNavigateToEditor(snippet.id)
                                    } else {
                                        onNavigateToDetail(snippet.id)
                                    }
                                },
                                onCopySnippet = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(snippet.content))
                                    onShowSnackbar(context.getString(R.string.toast_copied))
                                },
                                onRename = { pendingRenameSnippet = snippet },
                                onMoveFolder = { pendingFolderSnippet = snippet },
                                onToggleStar = { viewModel.toggleStar(snippet.id, snippet.starred) },
                                onMore = { pendingTrashId = snippet.id },
                                showFullDateTime = true,
                                modifier = Modifier.padding(horizontal = Spacing.S4, vertical = Spacing.S2)
                            )
                        }
                    }
                } else {
                    // ===== 视图 B: TREE 目录树状视图 =====
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        uiState.groupedFolders.forEach { (folderName, folderSnippets) ->
                            item(key = "folder_$folderName") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.S4, vertical = Spacing.S2)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = "Folder Group",
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.S2))
                                    Text(
                                        text = "$folderName (${folderSnippets.size})",
                                        style = SectionTitleStyle,
                                        color = textPrimary
                                    )
                                }
                            }

                            items(
                                items = folderSnippets,
                                key = { it.id }
                            ) { snippet ->
                                SnippetCard(
                                    snippet = snippet,
                                    onOpen = {
                                        if (uiState.cardClickAction == "editor") {
                                            onNavigateToEditor(snippet.id)
                                        } else {
                                            onNavigateToDetail(snippet.id)
                                        }
                                    },
                                    onCopySnippet = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(snippet.content))
                                        onShowSnackbar(context.getString(R.string.toast_copied))
                                    },
                                    onRename = { pendingRenameSnippet = snippet },
                                    onMoveFolder = { pendingFolderSnippet = snippet },
                                    onToggleStar = { viewModel.toggleStar(snippet.id, snippet.starred) },
                                    onMore = { pendingTrashId = snippet.id },
                                    showFullDateTime = true,
                                    modifier = Modifier.padding(start = 24.dp, end = Spacing.S4, top = Spacing.S1, bottom = Spacing.S1)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ===== 弹框集合 =====
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
