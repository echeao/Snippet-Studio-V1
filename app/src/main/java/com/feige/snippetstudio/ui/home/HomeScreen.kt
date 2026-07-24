package com.feige.snippetstudio.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light
    val cardBg = if (isDark) SurfaceDark else SurfaceLight

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    var pendingTrashId by remember { mutableStateOf<String?>(null) }
    var pendingRenameSnippet by remember { mutableStateOf<com.feige.snippetstudio.model.Snippet?>(null) }
    var pendingFolderSnippet by remember { mutableStateOf<com.feige.snippetstudio.model.Snippet?>(null) }

    // Clipboard detection on ON_RESUME
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
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = DisplayTitleStyle,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.width(Spacing.S2))
                        Surface(
                            color = PrimarySoft,
                            shape = RoundedCornerShape(R_SM)
                        ) {
                            Text(
                                text = stringResource(R.string.home_subtitle),
                                style = BadgeStyle,
                                color = Primary,
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
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = textSecondary
                        )
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
            // ClipBar
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

            // SearchBar
            SearchBar(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = stringResource(R.string.home_search),
                modifier = Modifier.padding(horizontal = Spacing.S4, vertical = Spacing.S2)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Quick New 2x2 Grid
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.S4, vertical = Spacing.S3)
                    ) {
                        Text(
                            text = stringResource(R.string.home_quick_new),
                            style = SectionTitleStyle,
                            color = textPrimary
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
                    }
                }

                // Recent Snippets Section Header
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
                            color = textPrimary
                        )

                        Text(
                            text = "${stringResource(R.string.home_view_all)} (${uiState.totalActiveCount})",
                            style = ListTitleStyle,
                            color = Primary,
                            modifier = Modifier
                                .clickable { onNavigateToFiles() }
                                .testTag("view_all_link")
                        )
                    }
                }

                // List / States
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
                    items(
                        items = uiState.recentSnippets,
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
                            modifier = Modifier.padding(horizontal = Spacing.S4, vertical = Spacing.S2)
                        )
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

        // Confirmation dialog for moving to trash
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

@Composable
fun QuickNewCard(
    type: SnippetType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val cardBg = if (isDark) SurfaceDark else SurfaceLight

    Surface(
        modifier = modifier
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
            .clickable(onClick = onClick)
            .testTag("quick_new_${type.code}"),
        shape = RoundedCornerShape(R_MD),
        color = cardBg
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
                color = textPrimary
            )
        }
    }
}
