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

import com.feige.snippetstudio.data.git.GitManager

data class SubPageUiState(
    val key: String = "",
    val settings: AppSettings = AppSettings(),
    val trashedSnippets: List<Snippet> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val tags: List<String> = emptyList(),
    val gitUrlInput: String = "",
    val gitBranchInput: String = "main",
    val gitPatInput: String = "",
    val isGitOperating: Boolean = false,
    val isLoading: Boolean = true
)

class SubPageViewModel(
    val key: String,
    private val settingsRepository: SettingsRepository,
    private val snippetRepository: SnippetRepository,
    private val gitManager: GitManager? = null
) : ViewModel() {

    private val _gitUrl = MutableStateFlow("")
    private val _gitBranch = MutableStateFlow("main")
    private val _gitPat = MutableStateFlow("")
    private val _isGitOperating = MutableStateFlow(false)

    val uiState: StateFlow<SubPageUiState> = combine(
        settingsRepository.settingsFlow,
        snippetRepository.observeTrashed(),
        snippetRepository.observeActive(),
        _gitUrl,
        _gitBranch,
        _gitPat,
        _isGitOperating
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
        val isOperating = flows[6] as Boolean

        val counts = active.groupBy { it.type.displayName }.mapValues { it.value.size }
        val allTags = (settings.customTags + active.flatMap { it.tags }).distinct()

        SubPageUiState(
            key = key,
            settings = settings,
            trashedSnippets = trashed,
            categoryCounts = counts,
            tags = allTags,
            gitUrlInput = if (gUrl.isEmpty()) settings.gitUrl else gUrl,
            gitBranchInput = if (gBranch.isEmpty()) settings.gitBranch else gBranch,
            gitPatInput = if (gPat.isEmpty()) settings.gitPat else gPat,
            isGitOperating = isOperating,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubPageUiState(key = key, isLoading = true)
    )

    fun updateRepoPath(context: Context, pathDisplay: String, treeUriStr: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(
                    repoPath = pathDisplay,
                    repoTreeUri = treeUriStr
                )
            }
            snippetRepository.syncWithLocalRepository(context, treeUriStr)
        }
    }

    fun onGitUrlChange(url: String) { _gitUrl.value = url }
    fun onGitBranchChange(branch: String) { _gitBranch.value = branch }
    fun onGitPatChange(pat: String) { _gitPat.value = pat }

    fun testGitConnection(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isGitOperating.value = true
            val url = _gitUrl.value.ifBlank { uiState.value.settings.gitUrl }
            val branch = _gitBranch.value.ifBlank { uiState.value.settings.gitBranch }
            val pat = _gitPat.value.ifBlank { uiState.value.settings.gitPat }

            if (gitManager == null) {
                _isGitOperating.value = false
                onResult(false, "GitManager 未初始化")
                return@launch
            }

            val testRes = gitManager.testConnection(url, branch, pat)
            if (testRes.isSuccess && testRes.getOrDefault(false)) {
                // 初始化或更新仓库
                val initRes = gitManager.initOrClone(url, branch, pat)
                if (initRes.isSuccess) {
                    // 同步刷盘与导入
                    snippetRepository.exportAllToGit()
                    snippetRepository.syncGitFilesToDb()

                    settingsRepository.updateSettings {
                        it.copy(
                            gitUrl = url,
                            gitBranch = branch,
                            gitPat = pat,
                            gitConnected = true
                        )
                    }
                    _isGitOperating.value = false
                    onResult(true, null)
                } else {
                    _isGitOperating.value = false
                    onResult(false, initRes.exceptionOrNull()?.localizedMessage ?: "克隆/初始化仓库失败")
                }
            } else {
                _isGitOperating.value = false
                onResult(false, testRes.exceptionOrNull()?.localizedMessage ?: "远程连接或鉴权失败，请检查 URL 和 Token")
            }
        }
    }

    fun syncGit(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isGitOperating.value = true
            val settings = uiState.value.settings
            if (gitManager == null || !settings.gitConnected) {
                _isGitOperating.value = false
                onResult(false, "Git 未连接或服务不可用")
                return@launch
            }

            // 1. 将现有的数据库片段导出至 Git 物理仓
            snippetRepository.exportAllToGit()

            // 2. Commit 并 Push 提交到远程
            val commitRes = gitManager.commitAndPush(
                commitMessage = "sync: Snippet Studio sync at ${System.currentTimeMillis()}",
                url = settings.gitUrl,
                branch = settings.gitBranch,
                pat = settings.gitPat
            )

            if (commitRes.isFailure) {
                _isGitOperating.value = false
                onResult(false, "推送提交失败: ${commitRes.exceptionOrNull()?.localizedMessage}")
                return@launch
            }

            // 3. 从远端 Pull 最新提交
            val pullRes = gitManager.pull(settings.gitUrl, settings.gitBranch, settings.gitPat)
            if (pullRes.isSuccess) {
                // 4. 将 Pull 到的新文件导入写入 Room 数据库
                snippetRepository.syncGitFilesToDb()
                settingsRepository.updateSettings { it.copy(lastSyncTime = System.currentTimeMillis()) }
                _isGitOperating.value = false
                onResult(true, "Git 同步成功！")
            } else {
                _isGitOperating.value = false
                onResult(false, "拉取最新更改失败: ${pullRes.exceptionOrNull()?.localizedMessage}")
            }
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

    fun addGlobalTag(tag: String) {
        val trimmed = tag.trim().removePrefix("#").trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            settingsRepository.updateSettings { current ->
                val updated = (current.customTags + trimmed).distinct()
                current.copy(customTags = updated)
            }
        }
    }

    fun removeGlobalTag(tag: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { current ->
                val updated = current.customTags.filter { it != tag }
                current.copy(customTags = updated)
            }
        }
    }

    companion object {
        fun factory(
            key: String,
            settingsRepository: SettingsRepository,
            snippetRepository: SnippetRepository,
            gitManager: GitManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SubPageViewModel(key, settingsRepository, snippetRepository, gitManager) as T
            }
        }
    }
}
