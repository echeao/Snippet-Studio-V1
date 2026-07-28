package com.feige.snippetstudio.ui.subpage.vm

import com.feige.snippetstudio.data.git.GitManager
import com.feige.snippetstudio.data.git.SyncEngine
import com.feige.snippetstudio.data.repo.SettingsRepository
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.AppSettings
import com.feige.snippetstudio.model.ConflictResolution
import com.feige.snippetstudio.model.DiffLine
import com.feige.snippetstudio.model.GitCommitInfo
import com.feige.snippetstudio.model.SyncDirection
import com.feige.snippetstudio.model.SyncPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * [GitSubState] Git 远程仓库管理与同步功能的专属 UI 状态实体。
 *
 * 从原 [com.feige.snippetstudio.ui.subpage.SubPageUiState] 中拆分而来，
 * 仅保留与 Git 版本控制相关的字段，遵循单一职责原则 (SRP)。
 *
 * @param settings 全局偏好设置（含 gitUrl / gitBranch / gitPat / gitConnected 等）
 * @param gitUrlInput 用户当前输入的远程仓库地址
 * @param gitBranchInput 用户当前输入的默认分支名称
 * @param gitPatInput 用户当前输入的 Personal Access Token
 * @param isGitOperating Git 网络操作（测试/克隆/同步）是否进行中
 * @param syncPreview 当前同步预览结果（Pull/Push 变更与冲突列表）
 * @param isPreviewing 是否正在生成同步预览
 * @param syncProgress 同步进度文案（null 表示无进行中同步）
 * @param gitLogCommits Git 提交历史列表
 * @param isGitLogLoading 是否正在加载提交历史
 * @param gitLogError 加载提交历史时的错误信息
 * @param localChanges 本地未提交变更映射 (文件路径 → 变更类型)
 * @param selectedDiffPath 当前选中查看 Diff 的文件路径
 * @param currentDiff 当前文件的 Diff 行列表
 * @param isDiffLoading 是否正在加载 Diff 内容
 */
data class GitSubState(
    val settings: AppSettings = AppSettings(),
    val gitUrlInput: String = "",
    val gitBranchInput: String = "main",
    val gitPatInput: String = "",
    val isGitOperating: Boolean = false,
    val syncPreview: SyncPreview? = null,
    val isPreviewing: Boolean = false,
    val syncProgress: String? = null,
    val gitLogCommits: List<GitCommitInfo> = emptyList(),
    val isGitLogLoading: Boolean = false,
    val gitLogError: String? = null,
    val localChanges: Map<String, String> = emptyMap(),
    val selectedDiffPath: String? = null,
    val currentDiff: List<DiffLine> = emptyList(),
    val isDiffLoading: Boolean = false
)

/**
 * [GitSubViewModel] Git 远程仓库管理与双向同步业务逻辑控制器。
 *
 * 架构职责：
 * 1. 管理 Git 远程 URL、分支、PAT 的输入状态与连接校验。
 * 2. 通过 [SyncEngine] 执行细粒度 Pull/Push 预览与确认同步。
 * 3. 展示本地工作区未提交的文件变更矩阵与逐行 Diff 对比。
 * 4. 加载 Git 仓库全量提交历史 (Git Log)。
 *
 * 生命周期由 [com.feige.snippetstudio.ui.subpage.SubPageViewModel] Facade 托管，
 * Facade 在 onCleared() 时调用 [destroy] 取消协程作用域。
 *
 * @param scope 由 Facade 提供的协程作用域
 * @param settingsRepository 全局设置数据仓库
 * @param snippetRepository 代码片段数据仓库
 * @param gitManager JGit 管理器实例（可为 null 表示 Git 功能不可用）
 */
class GitSubViewModel(
    private val scope: CoroutineScope,
    private val settingsRepository: SettingsRepository,
    private val snippetRepository: SnippetRepository,
    private val gitManager: GitManager?
) {
    // ===== 内部可变状态流 =====
    private val _gitUrl = MutableStateFlow("")
    private val _gitBranch = MutableStateFlow("main")
    private val _gitPat = MutableStateFlow("")
    private val _isGitOperating = MutableStateFlow(false)
    private val _syncPreview = MutableStateFlow<SyncPreview?>(null)
    private val _isPreviewing = MutableStateFlow(false)
    private val _syncProgress = MutableStateFlow<String?>(null)
    private val _gitLogCommits = MutableStateFlow<List<GitCommitInfo>>(emptyList())
    private val _isGitLogLoading = MutableStateFlow(false)
    private val _gitLogError = MutableStateFlow<String?>(null)
    private val _localChanges = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _selectedDiffPath = MutableStateFlow<String?>(null)
    private val _currentDiff = MutableStateFlow<List<DiffLine>>(emptyList())
    private val _isDiffLoading = MutableStateFlow(false)

    /** SyncEngine 细粒度同步引擎（延迟初始化，依赖 GitManager 可用性） */
    private val syncEngine: SyncEngine? by lazy {
        gitManager?.let { SyncEngine(it, snippetRepository, snippetRepository.getSnippetDao()) }
    }

    init {
        // 进入 Git 页面时自动扫描本地未提交变更
        loadLocalChanges()
    }

    /**
     * 对外暴露的 Git 子页面响应式 UI 状态。
     * 通过 combine 合并 settings 流与所有内部 Git 状态流。
     */
    val gitState: StateFlow<GitSubState> = combine(
        settingsRepository.settingsFlow,
        _gitUrl,
        _gitBranch,
        _gitPat,
        _isGitOperating,
        _syncPreview,
        _isPreviewing,
        _syncProgress,
        _gitLogCommits,
        _isGitLogLoading,
        _gitLogError,
        _localChanges,
        _selectedDiffPath,
        _currentDiff,
        _isDiffLoading
    ) { flows ->
        val settings = flows[0] as AppSettings
        val gUrl = flows[1] as String
        val gBranch = flows[2] as String
        val gPat = flows[3] as String
        val isOperating = flows[4] as Boolean
        val preview = flows[5] as SyncPreview?
        val previewing = flows[6] as Boolean
        val progress = flows[7] as String?
        @Suppress("UNCHECKED_CAST")
        val logCommits = flows[8] as List<GitCommitInfo>
        val logLoading = flows[9] as Boolean
        val logError = flows[10] as String?
        @Suppress("UNCHECKED_CAST")
        val localChanges = flows[11] as Map<String, String>
        val diffPath = flows[12] as String?
        @Suppress("UNCHECKED_CAST")
        val diff = flows[13] as List<DiffLine>
        val diffLoading = flows[14] as Boolean

        GitSubState(
            settings = settings,
            gitUrlInput = gUrl.ifEmpty { settings.gitUrl },
            gitBranchInput = gBranch.ifEmpty { settings.gitBranch },
            gitPatInput = gPat.ifEmpty { settings.gitPat },
            isGitOperating = isOperating,
            syncPreview = preview,
            isPreviewing = previewing,
            syncProgress = progress,
            gitLogCommits = logCommits,
            isGitLogLoading = logLoading,
            gitLogError = logError,
            localChanges = localChanges,
            selectedDiffPath = diffPath,
            currentDiff = diff,
            isDiffLoading = diffLoading
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GitSubState()
    )

    // ===== 输入状态变更 API =====

    /** 更新用户输入的 Git 远程仓库 URL */
    fun onGitUrlChange(url: String) { _gitUrl.value = url }

    /** 更新用户输入的 Git 分支名称 */
    fun onGitBranchChange(branch: String) { _gitBranch.value = branch }

    /** 更新用户输入的 Personal Access Token */
    fun onGitPatChange(pat: String) { _gitPat.value = pat }

    // ===== Git 连接与同步操作 API =====

    /**
     * 测试远程 Git 仓库连接，并尝试首次 clone / init 物理仓库沙盒。
     *
     * @param onResult 操作结果回调 (成功/失败, 错误信息)
     */
    fun testGitConnection(onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            _isGitOperating.value = true
            val url = _gitUrl.value.ifBlank { gitState.value.settings.gitUrl }
            val branch = _gitBranch.value.ifBlank { gitState.value.settings.gitBranch }
            val pat = _gitPat.value.ifBlank { gitState.value.settings.gitPat }

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
                    // 【入库优先原则】：先将 Clone 下来的 Git 沙盒文件解析导入数据库
                    snippetRepository.syncGitFilesToDb()

                    // 将导入后的内容全量写回用户物理工作区 (SAF / 内部存储)
                    snippetRepository.syncAllToPhysicalStorage(gitState.value.settings.repoTreeUri)

                    // 最后把 Room 中已建立索引的内容对齐刷盘导出到 Git
                    snippetRepository.exportAllToGit()

                    settingsRepository.updateSettings {
                        it.copy(
                            gitUrl = url,
                            gitBranch = branch,
                            gitPat = pat,
                            gitConnected = true
                        )
                    }
                    loadLocalChanges()
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

    /**
     * 生成 Pull 预览：fetch 远端并对比差异，不修改任何数据。
     *
     * @param onResult 操作结果回调
     */
    fun previewPull(onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            _isPreviewing.value = true
            val settings = gitState.value.settings
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

    /**
     * 生成 Push 预览：对比本地 DB 与沙盒差异，透传 repoTreeUri 执行磁盘前置校准。
     *
     * @param onResult 操作结果回调
     */
    fun previewPush(onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            _isPreviewing.value = true
            val settings = gitState.value.settings
            val engine = syncEngine
            if (engine == null || !settings.gitConnected) {
                _isPreviewing.value = false
                onResult(false, "Git 未连接")
                return@launch
            }

            // 透传 repoTreeUri，确保预览前先扫描外部磁盘变动
            val result = engine.previewPush(settings.repoTreeUri)
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

    /**
     * 解决指定索引的同步冲突。
     *
     * @param index 冲突在列表中的索引位置
     * @param resolution 用户选择的冲突解决策略
     */
    fun resolveConflict(index: Int, resolution: ConflictResolution) {
        val preview = _syncPreview.value ?: return
        val updatedConflicts = preview.conflicts.mapIndexed { i, conflict ->
            if (i == index) conflict.copy(resolution = resolution) else conflict
        }
        _syncPreview.value = preview.copy(conflicts = updatedConflicts)
    }

    /**
     * 确认执行当前预览中的同步操作（Pull 或 Push）。
     *
     * @param onResult 操作结果回调
     */
    fun confirmSync(onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            val preview = _syncPreview.value ?: return@launch
            val settings = gitState.value.settings
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
                        repoTreeUri = settings.repoTreeUri,
                        onProgress = { _syncProgress.value = it }
                    )
                }
            }

            if (result.isSuccess) {
                settingsRepository.updateSettings { it.copy(lastSyncTime = System.currentTimeMillis()) }
                _syncPreview.value = null
                _syncProgress.value = null
                loadLocalChanges()
                _isGitOperating.value = false
                onResult(true, if (preview.direction == SyncDirection.INCOMING) "拉取完成" else "推送完成")
            } else {
                _syncProgress.value = null
                _isGitOperating.value = false
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "同步失败")
            }
        }
    }

    /** 取消当前同步预览，清空预览状态 */
    fun cancelSync() {
        _syncPreview.value = null
        _syncProgress.value = null
    }

    /** 加载本地未提交的变更文件列表（包含暂存未提交） */
    fun loadLocalChanges() {
        scope.launch {
            if (gitManager == null) return@launch
            val changes = gitManager.stageAndGetUncommittedChanges()
            _localChanges.value = changes
        }
    }

    /**
     * 加载指定文件的 Diff 对比内容。
     *
     * @param relativePath 文件相对于仓库根目录的路径
     */
    fun loadFileDiff(relativePath: String) {
        scope.launch {
            _selectedDiffPath.value = relativePath
            _isDiffLoading.value = true
            _currentDiff.value = emptyList()
            if (gitManager == null) {
                _isDiffLoading.value = false
                return@launch
            }
            val result = gitManager.getWorkingTreeDiff(relativePath)
            result.onSuccess { diff ->
                _currentDiff.value = diff
            }.onFailure {
                _currentDiff.value = emptyList()
            }
            _isDiffLoading.value = false
        }
    }

    /** 关闭当前 Diff 视图，重置选中状态 */
    fun closeDiff() {
        _selectedDiffPath.value = null
        _currentDiff.value = emptyList()
    }

    /** 加载 Git 仓库全量提交历史 (Git Log)，最多 50 条 */
    fun loadGitLog() {
        scope.launch {
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

    /**
     * 触发双向完整 Git 同步：本地导出 → Commit/Push → Pull → 数据库入库。
     * 支持远端删除精准清理与入库优先原则。
     *
     * @param onResult 操作结果回调
     */
    fun syncGit(onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            _isGitOperating.value = true
            val settings = gitState.value.settings
            if (gitManager == null || !settings.gitConnected) {
                _isGitOperating.value = false
                onResult(false, "Git 未连接或服务不可用")
                return@launch
            }

            // 1. 若本地 DB 已有变更片段，导出至 Git 物理仓（带防全空熔断）
            snippetRepository.exportAllToGit()

            // 2. Commit 并 Push 本地提交到远程
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

            // 3. 记录 Pull 前沙盒文件集合，用于计算远端删除项目
            val beforeSandboxFiles = gitManager.getSandboxFileContents().keys

            // 从远端 Pull 最新提交
            val pullRes = gitManager.pull(settings.gitUrl, settings.gitBranch, settings.gitPat)
            if (pullRes.isSuccess) {
                val afterSandboxFiles = gitManager.getSandboxFileContents().keys
                val remoteDeletedPaths = beforeSandboxFiles - afterSandboxFiles

                // 4. 【入库优先原则】：先将 Pull 到的新文件导入写入 Room 数据库
                snippetRepository.syncGitFilesToDb()

                // 5. 将拉取到的内容回写到用户物理工作区，并精准注销远端删除的文件与 DB 记录
                snippetRepository.syncAllToPhysicalStorage(settings.repoTreeUri, remoteDeletedPaths)

                // 6. 最后对齐沙盒镜像状态
                snippetRepository.exportAllToGit()

                settingsRepository.updateSettings { it.copy(lastSyncTime = System.currentTimeMillis()) }
                _isGitOperating.value = false
                onResult(true, "Git 同步成功！")
            } else {
                _isGitOperating.value = false
                onResult(false, "拉取最新更改失败: ${pullRes.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    /**
     * 销毁此子 ViewModel，取消所有正在执行的协程。
     * 由 Facade ([com.feige.snippetstudio.ui.subpage.SubPageViewModel]) 在 onCleared() 时调用。
     */
    fun destroy() {
        // scope 由 Facade 统一管理取消，此处预留扩展点
    }
}
