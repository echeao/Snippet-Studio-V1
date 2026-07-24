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
            // SearchBar
            SearchBar(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = stringResource(R.string.files_search),
                modifier = Modifier.padding(horizontal = Spacing.S4, vertical = Spacing.S2)
            )

            Spacer(modifier = Modifier.height(Spacing.S1))

            // Filter Chips
            FilterChipsRow(
                selected = uiState.filterOption,
                onSelect = { viewModel.onFilterSelect(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.S2))

            // Content List / States
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
                    // 平铺视图 (Flat View)
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
                    // 目录树视图 (Folder Tree View)
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

        // Rename Dialog
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

        // Folder Move Dialog
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

        // Trash Confirm Dialog
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
