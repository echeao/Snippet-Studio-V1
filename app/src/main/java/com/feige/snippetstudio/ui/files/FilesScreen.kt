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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: FilesViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToNewEditor: (String) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light

    var pendingTrashId by remember { mutableStateOf<String?>(null) }

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
                            onOpen = { onNavigateToDetail(snippet.id) },
                            onToggleStar = { viewModel.toggleStar(snippet.id, snippet.starred) },
                            onMore = { pendingTrashId = snippet.id },
                            showFullDateTime = true,
                            modifier = Modifier.padding(horizontal = Spacing.S4, vertical = Spacing.S2)
                        )
                    }
                }
            }
        }

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
