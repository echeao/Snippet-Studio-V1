package com.feige.snippetstudio.ui.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.components.FilterOption
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.feige.snippetstudio.data.repo.SettingsRepository

enum class SortMode {
    UPDATED_DESC, NAME_ASC, TYPE_ASC
}

enum class ViewMode {
    FLAT, TREE
}

data class FilesUiState(
    val snippets: List<Snippet> = emptyList(),
    val groupedFolders: Map<String, List<Snippet>> = emptyMap(),
    val existingFolders: List<String> = emptyList(),
    val searchQuery: String = "",
    val filterOption: FilterOption = FilterOption.All,
    val sortMode: SortMode = SortMode.UPDATED_DESC,
    val viewMode: ViewMode = ViewMode.FLAT,
    val cardClickAction: String = "detail",
    val isLoading: Boolean = true
)

private data class FilterParams(
    val query: String,
    val filter: FilterOption,
    val sort: SortMode,
    val viewMode: ViewMode
)

class FilesViewModel(
    private val repository: SnippetRepository,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterOption = MutableStateFlow<FilterOption>(FilterOption.All)
    private val _sortMode = MutableStateFlow(SortMode.UPDATED_DESC)
    private val _viewMode = MutableStateFlow(ViewMode.FLAT)

    private val _filterParams = combine(_searchQuery, _filterOption, _sortMode, _viewMode) { query, filter, sort, viewMode ->
        FilterParams(query, filter, sort, viewMode)
    }

    val uiState: StateFlow<FilesUiState> = combine(
        repository.observeActive(),
        _filterParams,
        settingsRepository?.settingsFlow ?: flowOf(com.feige.snippetstudio.model.AppSettings())
    ) { allSnippets, params, settings ->
        var list = allSnippets

        // Filter by option
        list = when {
            params.filter.isFav -> list.filter { it.starred }
            params.filter.type != null -> list.filter { it.type == params.filter.type }
            else -> list
        }

        // Filter by search query
        if (params.query.isNotBlank()) {
            list = list.filter {
                it.title.contains(params.query, ignoreCase = true) ||
                        it.content.contains(params.query, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(params.query, ignoreCase = true) }
            }
        }

        // Sort
        list = when (params.sort) {
            SortMode.UPDATED_DESC -> list.sortedByDescending { it.updatedAt }
            SortMode.NAME_ASC -> list.sortedBy { it.displayTitle.lowercase() }
            SortMode.TYPE_ASC -> list.sortedBy { it.type.displayName }
        }

        val grouped = list.groupBy { if (it.folder.isBlank()) "根目录" else it.folder }
        val folders = allSnippets.map { it.folder }.filter { it.isNotBlank() }.distinct()

        FilesUiState(
            snippets = list,
            groupedFolders = grouped,
            existingFolders = folders,
            searchQuery = params.query,
            filterOption = params.filter,
            sortMode = params.sort,
            viewMode = params.viewMode,
            cardClickAction = settings.cardClickAction,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilesUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelect(option: FilterOption) {
        _filterOption.value = option
    }

    fun cycleSortMode() {
        _sortMode.value = when (_sortMode.value) {
            SortMode.UPDATED_DESC -> SortMode.NAME_ASC
            SortMode.NAME_ASC -> SortMode.TYPE_ASC
            SortMode.TYPE_ASC -> SortMode.UPDATED_DESC
        }
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.FLAT) ViewMode.TREE else ViewMode.FLAT
    }

    fun toggleStar(id: String, currentStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStar(id, currentStarred)
        }
    }

    fun renameSnippet(id: String, newTitle: String, newFileName: String) {
        viewModelScope.launch {
            repository.updateRename(id, newTitle, newFileName)
        }
    }

    fun updateFolder(id: String, newFolder: String) {
        viewModelScope.launch {
            repository.updateFolder(id, newFolder)
        }
    }

    fun trashSnippet(id: String) {
        viewModelScope.launch {
            repository.trash(id)
        }
    }

    companion object {
        fun factory(
            repository: SnippetRepository,
            settingsRepository: SettingsRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FilesViewModel(repository, settingsRepository) as T
            }
        }
    }
}
