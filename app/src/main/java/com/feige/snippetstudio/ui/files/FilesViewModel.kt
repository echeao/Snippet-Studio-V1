package com.feige.snippetstudio.ui.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.components.FilterOption
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortMode {
    UPDATED_DESC, NAME_ASC, TYPE_ASC
}

data class FilesUiState(
    val snippets: List<Snippet> = emptyList(),
    val searchQuery: String = "",
    val filterOption: FilterOption = FilterOption.All,
    val sortMode: SortMode = SortMode.UPDATED_DESC,
    val isLoading: Boolean = true
)

class FilesViewModel(
    private val repository: SnippetRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterOption = MutableStateFlow<FilterOption>(FilterOption.All)
    private val _sortMode = MutableStateFlow(SortMode.UPDATED_DESC)

    val uiState: StateFlow<FilesUiState> = combine(
        repository.observeActive(),
        _searchQuery,
        _filterOption,
        _sortMode
    ) { allSnippets, query, filter, sort ->
        var list = allSnippets

        // Filter by option
        list = when {
            filter.isFav -> list.filter { it.starred }
            filter.type != null -> list.filter { it.type == filter.type }
            else -> list
        }

        // Filter by search query
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
        }

        // Sort
        list = when (sort) {
            SortMode.UPDATED_DESC -> list.sortedByDescending { it.updatedAt }
            SortMode.NAME_ASC -> list.sortedBy { it.displayTitle.lowercase() }
            SortMode.TYPE_ASC -> list.sortedBy { it.type.displayName }
        }

        FilesUiState(
            snippets = list,
            searchQuery = query,
            filterOption = filter,
            sortMode = sort,
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

    fun toggleStar(id: String, currentStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStar(id, currentStarred)
        }
    }

    fun trashSnippet(id: String) {
        viewModelScope.launch {
            repository.trash(id)
        }
    }

    companion object {
        fun factory(repository: SnippetRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FilesViewModel(repository) as T
            }
        }
    }
}
