package com.feige.snippetstudio.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.util.ClipboardDetector
import com.feige.snippetstudio.util.DetectedClip
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentSnippets: List<Snippet> = emptyList(),
    val totalActiveCount: Int = 0,
    val searchQuery: String = "",
    val detectedClip: DetectedClip? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(
    private val repository: SnippetRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _detectedClip = MutableStateFlow<DetectedClip?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeActive(),
        _searchQuery,
        _detectedClip
    ) { snippets, query, clip ->
        val filtered = if (query.isBlank()) {
            snippets
        } else {
            snippets.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
        }

        HomeUiState(
            recentSnippets = filtered.take(5),
            totalActiveCount = snippets.size,
            searchQuery = query,
            detectedClip = clip,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun checkClipboard(context: Context) {
        val clip = ClipboardDetector.detect(context)
        _detectedClip.value = clip
    }

    fun ignoreClip(clip: DetectedClip) {
        ClipboardDetector.ignore(clip.contentHash)
        _detectedClip.value = null
    }

    fun saveClip(clip: DetectedClip, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val snippet = repository.create(
                type = clip.inferredType,
                initialContent = clip.content,
                initialTitle = Snippet.generateDefaultTitle(clip.inferredType)
            )
            ClipboardDetector.ignore(clip.contentHash)
            _detectedClip.value = null
            onSaved(snippet.id)
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
                return HomeViewModel(repository) as T
            }
        }
    }
}
