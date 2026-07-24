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

import com.feige.snippetstudio.data.repo.SettingsRepository
import kotlinx.coroutines.flow.combine

data class DetailUiState(
    val snippet: Snippet? = null,
    val isSourceExpanded: Boolean = false,
    val allAvailableTags: List<String> = emptyList(),
    val isLoading: Boolean = true
)

class DetailViewModel(
    private val snippetId: String,
    private val repository: SnippetRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState(isLoading = true))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadSnippet()

        // Observe all available tags
        viewModelScope.launch {
            combine(
                settingsRepository.settingsFlow,
                repository.observeActive()
            ) { settings, activeSnippets ->
                (settings.customTags + activeSnippets.flatMap { it.tags }).distinct()
            }.collect { tags ->
                _uiState.update { it.copy(allAvailableTags = tags) }
            }
        }
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

    fun updateTags(tags: List<String>) {
        val currentSnippet = _uiState.value.snippet ?: return
        viewModelScope.launch {
            val updated = currentSnippet.copy(tags = tags)
            repository.saveOrUpdate(updated)
            _uiState.update { it.copy(snippet = updated) }
        }
    }

    fun trashSnippet(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.trash(snippetId)
            onDeleted()
        }
    }

    companion object {
        fun factory(
            snippetId: String,
            repository: SnippetRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DetailViewModel(snippetId, repository, settingsRepository) as T
            }
        }
    }
}
