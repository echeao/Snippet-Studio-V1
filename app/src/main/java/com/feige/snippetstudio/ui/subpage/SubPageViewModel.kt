package com.feige.snippetstudio.ui.subpage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SettingsRepository
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.AppSettings
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.util.LocaleHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SubPageUiState(
    val key: String = "",
    val settings: AppSettings = AppSettings(),
    val trashedSnippets: List<Snippet> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val tags: List<String> = emptyList(),
    val gitUrlInput: String = "",
    val gitBranchInput: String = "main",
    val gitPatInput: String = "",
    val isLoading: Boolean = true
)

class SubPageViewModel(
    val key: String,
    private val settingsRepository: SettingsRepository,
    private val snippetRepository: SnippetRepository
) : ViewModel() {

    private val _gitUrl = MutableStateFlow("")
    private val _gitBranch = MutableStateFlow("main")
    private val _gitPat = MutableStateFlow("")

    val uiState: StateFlow<SubPageUiState> = combine(
        settingsRepository.settingsFlow,
        snippetRepository.observeTrashed(),
        snippetRepository.observeActive(),
        _gitUrl,
        _gitBranch,
        _gitPat
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val settings = flows[0] as AppSettings
        @Suppress("UNCHECKED_CAST")
        val trashed = flows[1] as List<Snippet>
        @Suppress("UNCHECKED_CAST")
        val active = flows[2] as List<Snippet>
        val gUrl = flows[3] as String
        val gBranch = flows[4] as String
        val gPat = flows[5] as String

        val counts = active.groupBy { it.type.displayName }.mapValues { it.value.size }
        val allTags = active.flatMap { it.tags }.distinct()

        SubPageUiState(
            key = key,
            settings = settings,
            trashedSnippets = trashed,
            categoryCounts = counts,
            tags = allTags,
            gitUrlInput = if (gUrl.isEmpty()) settings.gitUrl else gUrl,
            gitBranchInput = if (gBranch.isEmpty()) settings.gitBranch else gBranch,
            gitPatInput = if (gPat.isEmpty()) settings.gitPat else gPat,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubPageUiState(key = key, isLoading = true)
    )

    fun updateRepoPath(path: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(repoPath = path) }
        }
    }

    fun onGitUrlChange(url: String) { _gitUrl.value = url }
    fun onGitBranchChange(branch: String) { _gitBranch.value = branch }
    fun onGitPatChange(pat: String) { _gitPat.value = pat }

    fun testGitConnection(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val connected = !uiState.value.settings.gitConnected
            settingsRepository.updateSettings {
                it.copy(
                    gitUrl = _gitUrl.value,
                    gitBranch = _gitBranch.value,
                    gitPat = _gitPat.value,
                    gitConnected = connected
                )
            }
            onResult(connected)
        }
    }

    fun restoreSnippet(id: String) {
        viewModelScope.launch {
            snippetRepository.restore(id)
        }
    }

    fun purgeSnippet(id: String) {
        viewModelScope.launch {
            snippetRepository.purge(id)
        }
    }

    fun setLanguage(context: Context, langCode: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(lang = langCode) }
            LocaleHelper.setLocale(context, langCode)
        }
    }

    companion object {
        fun factory(
            key: String,
            settingsRepository: SettingsRepository,
            snippetRepository: SnippetRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SubPageViewModel(key, settingsRepository, snippetRepository) as T
            }
        }
    }
}
