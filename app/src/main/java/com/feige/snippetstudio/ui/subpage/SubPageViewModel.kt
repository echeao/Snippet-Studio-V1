package com.feige.snippetstudio.ui.subpage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SettingsRepository
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.AppSettings
import com.feige.snippetstudio.model.ConflictResolution
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SyncConflict
import com.feige.snippetstudio.model.SyncDirection
import com.feige.snippetstudio.model.SyncPreview
import com.feige.snippetstudio.util.LocaleHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.feige.snippetstudio.data.git.GitManager
import com.feige.snippetstudio.data.git.SyncEngine

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
    val isLoading: Boolean = true,
    val syncPreview: SyncPreview? = null,
    val isPreviewing: Boolean = false,
    val syncProgress: String? = null,
    /** Git Log 提交历史列表 */
    val gitLogCommits: List<com.feige.snippetstudio.model.GitCommitInfo> = emptyList(),
    val isGitLogLoading: Boolean = false,
    val gitLogError: String? = null
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
    private val _syncPreview = MutableStateFlow<SyncPreview?>(null)
    private val _isPreviewing = MutableStateFlow(false)
    private val _syncProgress = MutableStateFlow<String?>(null)
    private val _gitLogCommits = MutableStateFlow<List<com.feige.snippetstudio.model.GitCommitInfo>>(emptyList())
    private val _isGitLogLoading = MutableStateFlow(false)
    private val _gitLogError = MutableStateFlow<String?>(null)

    /** SyncEngine 细粒度同步引擎（延迟初始化） */
    private val syncEngine: SyncEngine? by lazy {
        gitManager?.let { SyncEngine(it, snippetRepository, snippetRepository.getSnippetDao()) }
    }

    init {
        // 若当前子页面为 Git Log，自动加载提交历史
        if (key == "gitlog") {
            loadGitLog()
        }
    }

    /** 暴露给 SubPageScreen 调用的单向 StateFlow UI 状态 */
    val uiState: StateFlow<SubPageUiState> = combine(
        settingsRepository.settingsFlow,
        snippetRepository.observeTrashed(),
        snippetRepository.observeActive(),
        _gitUrl,
        _gitBranch,
        _gitPat,
        _isGitOperating,
        _syncPreview,
        _isPreviewing,
        _syncProgress,
        _gitLogCommits,
        _isGitLogLoading,
        _gitLogError
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
        val preview = flows[7] as SyncPreview?
        val previewing = flows[8] as Boolean
        val progress = flows[9] as String?
        @Suppress("UNCHECKED_CAST")
        val logCommits = flows[10] as List<com.feige.snippetstudio.model.GitCommitInfo>
        val logLoading = flows[11] as Boolean
        val logError = flows[12] as String?

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
            isLoading = false,
            syncPreview = preview,
            isPreviewing = previewing,
            syncProgress = progress,
            gitLogCommits = logCommits,
            isGitLogLoading = logLoading,
            gitLogError = logError
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

                    // 回写到用户物理工作区
                    snippetRepository.syncAllToPhysicalStorage(uiState.value.settings.repoTreeUri)

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

    // ===== 细粒度同步控制 API =====

    /** 生成 Pull 预览：fetch 远端并对比差异，不修改任何数据 */
    fun previewPull(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isPreviewing.value = true
            val settings = uiState.value.settings
            val engine = syncEngine
            if (engine == null || !settings.gitConnected) {
                _isPreviewing.value = false
                onResult(false, "Git 未连接")
                return@launch
            }

            val result = engine.previewPull(settings.gitBranch, settings.gitPat)
            _isPreviewing.value = false
            if (result.isSuccess) {
                val preview = result.getOrThrow()
                if (preview.changes.isEmpty() && preview.conflicts.isEmpty()) {
                    onResult(false, "远端没有新的变更，已是最新状态")
                } else {
                    _syncPreview.value = preview
                    onResult(true, null)
                }
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "预览失败")
            }
        }
    }

    /** 生成 Push 预览：对比本地 DB 与沙盒差异 */
    fun previewPush(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isPreviewing.value = true
            val settings = uiState.value.settings
            val engine = syncEngine
            if (engine == null || !settings.gitConnected) {
                _isPreviewing.value = false
                onResult(false, "Git 未连接")
                return@launch
            }

            val result = engine.previewPush()
            _isPreviewing.value = false
            if (result.isSuccess) {
                val preview = result.getOrThrow()
                if (preview.changes.isEmpty()) {
                    onResult(false, "本地没有需要推送的变更，已与仓库一致")
                } else {
                    _syncPreview.value = preview
                    onResult(true, null)
                }
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "预览失败")
            }
        }
    }

    /** 解决指定索引的冲突 */
    fun resolveConflict(index: Int, resolution: ConflictResolution) {
        val preview = _syncPreview.value ?: return
        val updatedConflicts = preview.conflicts.mapIndexed { i, conflict ->
            if (i == index) conflict.copy(resolution = resolution) else conflict
        }
        _syncPreview.value = preview.copy(conflicts = updatedConflicts)
    }

    /** 确认执行当前预览中的同步操作 */
    fun confirmSync(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val preview = _syncPreview.value ?: return@launch
            val settings = uiState.value.settings
            val engine = syncEngine ?: return@launch

            _isGitOperating.value = true
            _syncProgress.value = "准备中..."

            val result = when (preview.direction) {
                SyncDirection.INCOMING -> {
                    engine.executePull(
                        conflicts = preview.conflicts,
                        branch = settings.gitBranch,
                        pat = settings.gitPat,
                        repoTreeUri = settings.repoTreeUri,
                        onProgress = { _syncProgress.value = it }
                    )
                }
                SyncDirection.OUTGOING -> {
                    engine.executePush(
                        url = settings.gitUrl,
                        branch = settings.gitBranch,
                        pat = settings.gitPat,
                        onProgress = { _syncProgress.value = it }
                    )
                }
            }

            if (result.isSuccess) {
                settingsRepository.updateSettings { it.copy(lastSyncTime = System.currentTimeMillis()) }
                _syncPreview.value = null
                _syncProgress.value = null
                _isGitOperating.value = false
                onResult(true, if (preview.direction == SyncDirection.INCOMING) "拉取完成" else "推送完成")
            } else {
                _syncProgress.value = null
                _isGitOperating.value = false
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "同步失败")
            }
        }
    }

    /** 取消当前预览 */
    fun cancelSync() {
        _syncPreview.value = null
        _syncProgress.value = null
    }

    /** 加载 Git 仓库全量提交历史 (Git Log) */
    fun loadGitLog() {
        viewModelScope.launch {
            _isGitLogLoading.value = true
            _gitLogError.value = null

            if (gitManager == null) {
                _isGitLogLoading.value = false
                _gitLogError.value = "GitManager 未初始化"
                return@launch
            }

            val result = gitManager.getRepoLog(50)
            result.onSuccess { commits ->
                _gitLogCommits.value = commits
                _isGitLogLoading.value = false
            }.onFailure { e ->
                _gitLogError.value = e.localizedMessage ?: "加载提交历史失败"
                _isGitLogLoading.value = false
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

                // 5. 将拉取到的内容回写到用户物理工作区（SAF 或内部存储）
                snippetRepository.syncAllToPhysicalStorage(settings.repoTreeUri)

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
            val repoUri = settingsRepository.settingsFlow.first().repoTreeUri
            snippetRepository.restore(id, repoUri)
        }
    }

    /** 从回收站彻底永久删除代码片段 */
    fun purgeSnippet(id: String) {
        viewModelScope.launch {
            val repoUri = settingsRepository.settingsFlow.first().repoTreeUri
            snippetRepository.purge(id, repoUri)
        }
    }

    /** 切换系统语言并在 Context 中更新 Locale */
    fun setLanguage(context: Context, langCode: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(lang = langCode) }
            LocaleHelper.setLocale(context, langCode)
        }
    }

    /**
     * 切换配色风格主题。
     *
     * @param colorThemeId 配色主题标识符 ("forest", "ocean", "sunset", "lavender", "mono")
     */
    fun setColorTheme(colorThemeId: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(colorTheme = colorThemeId) }
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

