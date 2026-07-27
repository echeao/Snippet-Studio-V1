package com.feige.snippetstudio.ui.files

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow

/**
 * [FilesScreen] 文件与全量代码片段仓库主界面。
 *
 * 功能结构：
 * 1. **TopBar 顶部控制栏**：
 *    - 支持显式新建空文件夹 [FolderCreateDialog]。
 *    - 切换【大卡片预览 COMFORT】与【极嵌高密度 COMPACT】切换显示密度。
 *    - 切换【平铺列表 ViewMode.FLAT】与【树状文件夹 ViewMode.TREE】的视觉视图。
 *    - 循环切换排序模式 (SortMode: 修改时间降序 / 片段名称升序 / 类型升序)。
 * 2. **SearchBar 搜索输入框**：支持实时搜索正叠与标签。
 * 3. **FilterChipsRow 筛选 Chip 滚动条**：按【全部 / 收藏 / HTML / JS / Markdown / Prompt】进行分类筛选。
 * 4. **多视图模式渲染**：
 *    - **COMFORT 预览大卡片**：显示前 4 行代码微型预览、字符与行数统计、完整标签。
 *    - **COMPACT 高密度列表**：参照效果参考图，使用独立圆角整块卡片装合高密度列表与分割线。
 * 5. **交互弹框集合**：涵盖重命名 [RenameDialog]、移动文件夹 [FolderMoveDialog]、新建文件夹 [FolderCreateDialog] 与删除弹窗 [ConfirmDialog]。
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
    val tc = LocalThemeColors.current

    var pendingTrashId by remember { mutableStateOf<String?>(null) }
    var pendingRenameSnippet by remember { mutableStateOf<com.feige.snippetstudio.model.Snippet?>(null) }
    var pendingFolderSnippet by remember { mutableStateOf<com.feige.snippetstudio.model.Snippet?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(true) }
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

    LaunchedEffect(activeListState) {
        // 视图模式/密度模式发生切换时，重置记录的滚动偏移量，防止新视图下判定方向异常
        previousScrollIndex = 0
        previousScrollOffset = 0
        snapshotFlow {
            activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val delta = if (index != previousScrollIndex) {
                (index - previousScrollIndex) * 500 + (offset - previousScrollOffset)
            } else {
                offset - previousScrollOffset
            }

            // 滚动死区防抖：下滑超过 15px 时收起搜索栏，上滑超过 15px 时展开搜索栏
            if (delta > 15 && showSearchBar && (index > 0 || offset > 10)) {
                showSearchBar = false
            } else if (delta < -15 && !showSearchBar) {
                showSearchBar = true
            }

            // 滚动回到最顶部时重置为显示状态
            if (index == 0 && offset == 0) {
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
                    Text(
                        text = stringResource(R.string.files_title),
                        style = DisplayTitleStyle,
                        color = tc.text
                    )
                },
                actions = {
                    // ===== 按钮 0: 新建文件夹按钮 =====
                    IconButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier.testTag("files_create_folder_btn")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder_plus),
                            contentDescription = "Create Folder",
                            tint = tc.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // ===== 按钮 1: 切换【大卡片 / 高密度】显示密度模式 =====
                    IconButton(
                        onClick = { viewModel.toggleDensityMode() },
                        modifier = Modifier.testTag("files_density_mode_btn")
                    ) {
                        Icon(
                            painter = painterResource(id = if (uiState.densityMode == DensityMode.COMFORT) R.drawable.ic_list else R.drawable.ic_grid),
                            contentDescription = "Toggle Density Mode",
                            tint = tc.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // ===== 按钮 2: 切换【平铺 / 树状】视图结构 =====
                    IconButton(
                        onClick = { viewModel.toggleViewMode() },
                        modifier = Modifier.testTag("files_view_mode_btn")
                    ) {
                        Icon(
                            // 处于 FLAT 平铺视图时显示 ic_tree (点击可切至树状)；处于 TREE 树状视图时显示 ic_treetolist (点击可切至平铺)
                            painter = painterResource(id = if (uiState.viewMode == ViewMode.FLAT) R.drawable.ic_tree else R.drawable.ic_treetolist),
                            contentDescription = "Toggle View Mode",
                            tint = tc.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // ===== 按钮 3: 排序方式（点击弹出下拉选择列表，图标为 Format-Line-Spacing 样式） =====
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("files_sort_btn")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_sort),
                                contentDescription = "Sort Options",
                                tint = tc.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 下拉排序选项列表菜单
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            // 选项 1: 最近更新
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.sort_updated),
                                        color = if (uiState.sortMode == SortMode.UPDATED_DESC) tc.primary else tc.text,
                                        style = BodyStyle
                                    )
                                },
                                onClick = {
                                    viewModel.setSortMode(SortMode.UPDATED_DESC)
                                    showSortMenu = false
                                },
                                leadingIcon = if (uiState.sortMode == SortMode.UPDATED_DESC) {
                                    {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_check),
                                            contentDescription = null,
                                            tint = tc.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )

                            // 选项 2: 片段名称
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.sort_name),
                                        color = if (uiState.sortMode == SortMode.NAME_ASC) tc.primary else tc.text,
                                        style = BodyStyle
                                    )
                                },
                                onClick = {
                                    viewModel.setSortMode(SortMode.NAME_ASC)
                                    showSortMenu = false
                                },
                                leadingIcon = if (uiState.sortMode == SortMode.NAME_ASC) {
                                    {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_check),
                                            contentDescription = null,
                                            tint = tc.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )

                            // 选项 3: 片段类型
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.sort_type),
                                        color = if (uiState.sortMode == SortMode.TYPE_ASC) tc.primary else tc.text,
                                        style = BodyStyle
                                    )
                                },
                                onClick = {
                                    viewModel.setSortMode(SortMode.TYPE_ASC)
                                    showSortMenu = false
                                },
                                leadingIcon = if (uiState.sortMode == SortMode.TYPE_ASC) {
                                    {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_check),
                                            contentDescription = null,
                                            tint = tc.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
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
        ) {
            // ===== 1. 搜索框（使用 expandVertically/shrinkVertically 垂直折叠动画）=====
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
            } else if (uiState.snippets.isEmpty() && uiState.groupedFolders.isEmpty()) {
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
                    if (uiState.densityMode == DensityMode.COMFORT) {
                        LazyColumn(
                            state = flatComfortListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 0.dp)
                        ) {
                            items(
                                items = uiState.snippets,
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
                    } else {
                        // Snippet Studio 原生高密度列表：使用整块圆角容器 + 极细缩进分割线包裹
                        LazyColumn(
                            state = flatCompactListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 0.dp)
                        ) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.S4, vertical = Spacing.S1)
                                        .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
                                        .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
                                    shape = RoundedCornerShape(R_MD),
                                    color = tc.surface
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        uiState.snippets.forEachIndexed { index, snippet ->
                                            val onOpen = {
                                                if (uiState.cardClickAction == "editor") {
                                                    onNavigateToEditor(snippet.id)
                                                } else {
                                                    onNavigateToDetail(snippet.id)
                                                }
                                            }
                                            SnippetCompactRow(
                                                snippet = snippet,
                                                onOpen = onOpen,
                                                onRename = { pendingRenameSnippet = snippet },
                                                onMoveFolder = { pendingFolderSnippet = snippet },
                                                onToggleStar = { viewModel.toggleStar(snippet.id, snippet.starred) },
                                                onMore = { pendingTrashId = snippet.id },
                                                showDivider = (index < uiState.snippets.lastIndex),
                                                showFullDateTime = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ===== 视图 B: TREE 目录树状视图 =====
                    LazyColumn(
                        state = treeListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 0.dp)
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
                                        painter = painterResource(id = R.drawable.ic_folder),
                                        contentDescription = "Folder Group",
                                        tint = tc.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.S2))
                                    Text(
                                        text = "$folderName (${folderSnippets.size})",
                                        style = SectionTitleStyle,
                                        color = tc.text
                                    )
                                }
                            }

                            if (folderSnippets.isEmpty()) {
                                item(key = "empty_folder_$folderName") {
                                    Text(
                                        text = "(空文件夹)",
                                        style = CaptionStyle,
                                        color = tc.text2,
                                        modifier = Modifier.padding(start = 44.dp, top = 2.dp, bottom = 8.dp)
                                    )
                                }
                            } else {
                                if (uiState.densityMode == DensityMode.COMFORT) {
                                    items(
                                        items = folderSnippets,
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
                                            modifier = Modifier.padding(start = 24.dp, end = Spacing.S4, top = Spacing.S1, bottom = Spacing.S2)
                                        )
                                    }
                                } else {
                                    item(key = "folder_compact_card_$folderName") {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 24.dp, end = Spacing.S4, top = Spacing.S1, bottom = Spacing.S2)
                                                .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
                                                .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
                                            shape = RoundedCornerShape(R_MD),
                                            color = tc.surface
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                folderSnippets.forEachIndexed { index, snippet ->
                                                    val onOpen = {
                                                        if (uiState.cardClickAction == "editor") {
                                                            onNavigateToEditor(snippet.id)
                                                        } else {
                                                            onNavigateToDetail(snippet.id)
                                                        }
                                                    }
                                                    SnippetCompactRow(
                                                        snippet = snippet,
                                                        onOpen = onOpen,
                                                        onRename = { pendingRenameSnippet = snippet },
                                                        onMoveFolder = { pendingFolderSnippet = snippet },
                                                        onToggleStar = { viewModel.toggleStar(snippet.id, snippet.starred) },
                                                        onMore = { pendingTrashId = snippet.id },
                                                        showDivider = (index < uiState.snippets.lastIndex),
                                                        showFullDateTime = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== 弹框集合 =====
        FolderCreateDialog(
            show = showCreateFolderDialog,
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { folderName ->
                viewModel.createFolder(folderName)
                onShowSnackbar("已创建文件夹 $folderName")
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
