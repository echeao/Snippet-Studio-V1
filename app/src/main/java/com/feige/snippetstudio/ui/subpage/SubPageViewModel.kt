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

/**
 * [SubPageUiState] 设置子页面的响应式 UI 状态实体。
 *
 * @param key 当前子页面标识 ("git", "repo", "cat", "tags", "trash", "lang", "about")
 * @param settings 全局偏好设置实体
 * @param trashedSnippets 当前存放在回收站中的软删除片段列表
 * @param categoryCounts 按分类统计的片段数量集合
 * @param tags 全局定义的自定义与现有标签全集
 * @param gitUrlInput 用户在 Git 配置页输入的远程仓库地址
 * @param gitBranchInput 用户在 Git 配置页输入的默认分支 (如 main / master)
 * @param gitPatInput 用户输入的 Personal Access Token / 密码
 * @param isGitOperating 当前 Git 远程网络测试/克隆/同步操作是否在异步进行中
 * @param isLoading 页面初始化加载状态
 */
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

/**
 * [SubPageViewModel] 设置子页面的 ViewModel 控制器。
 *
 * 聚合处理各种设置子项的业务逻辑：
 * 1. **Git 版本仓库交互**：调用 [GitManager] 测试连接、克隆/初始化物理仓并执行双向 Pull / Push 同步。
 * 2. **SAF 本地物理磁盘仓库**：绑定外部本地目录树并触发 [SnippetRepository.syncWithLocalRepository]。
 * 3. **回收站维护**：还原已删除片段 `restoreSnippet` 或彻底清除 `purgeSnippet`。
 * 4. **语言与标签设置**：快捷调整应用 Language Locale 与添加/删除全局 Custom Tags。
 */
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

    /** 暴露给 SubPageScreen 调用的单向 StateFlow UI 状态 */
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

    /** 切换并绑定本地 SAF 磁盘目录树作为工作区 */
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

    /** 测试远程 Git 仓库连接，并尝试首次 clone / init 物理仓库沙盒 */
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
                // 初始化或克隆远程仓库
                val initRes = gitManager.initOrClone(url, branch, pat)
                if (initRes.isSuccess) {
                    // 刷盘导出并导入至数据库
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

    /** 触发双向完整的 Git 同步 (本地导出 -> Commit/Push -> Pull -> 数据库入库) */
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

    /** 从回收站恢复指定的代码片段 */
    fun restoreSnippet(id: String) {
        viewModelScope.launch {
            snippetRepository.restore(id)
        }
    }

    /** 从回收站彻底永久删除代码片段 */
    fun purgeSnippet(id: String) {
        viewModelScope.launch {
            snippetRepository.purge(id)
        }
    }

    /** 切换系统语言并在 Context 中更新 Locale */
    fun setLanguage(context: Context, langCode: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(lang = langCode) }
            LocaleHelper.setLocale(context, langCode)
        }
    }

    /** 新增全局预设自定义标签 */
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

    /** 删除全局预设自定义标签 */
    fun removeGlobalTag(tag: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { current ->
                val updated = current.customTags.filter { it != tag }
                current.copy(customTags = updated)
            }
        }
    }

    companion object {
        /** ViewModelFactory 工厂构造器 */
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

