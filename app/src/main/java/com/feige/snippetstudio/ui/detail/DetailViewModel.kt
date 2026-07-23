package com.feige.snippetstudio.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.Snippet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val snippet: Snippet? = null,
    val isSourceExpanded: Boolean = false,
    val isLoading: Boolean = true
)

class DetailViewModel(
    private val snippetId: String,
    private val repository: SnippetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState(isLoading = true))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadSnippet()
    }

    fun loadSnippet() {
        viewModelScope.launch {
            val snippet = repository.getById(snippetId)
            _uiState.update {
                it.copy(
                    snippet = snippet,
                    isLoading = false
                )
            }
        }
    }

    fun toggleSourceExpanded() {
        _uiState.update { it.copy(isSourceExpanded = !it.isSourceExpanded) }
    }

    fun trashSnippet(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.trash(snippetId)
            onDeleted()
        }
    }

    companion object {
        fun factory(snippetId: String, repository: SnippetRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DetailViewModel(snippetId, repository) as T
            }
        }
    }
}
