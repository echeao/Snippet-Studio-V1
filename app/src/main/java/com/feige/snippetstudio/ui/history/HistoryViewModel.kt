package com.feige.snippetstudio.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.git.GitManager
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.DiffLine
import com.feige.snippetstudio.model.GitCommitInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [HistoryUiState] 单片段 Git 历史履历页面的 UI 状态模型。
 *
 * @param snippetTitle 代码片段标题
 * @param fileName 片段文件名
 * @param folder 所在的目录路径
 * @param commitList 提交历史记录集合
 * @param selectedCommitId 当前被选中的提交记录 Hash ID
 * @param fileContentAtCommit 特定提交节点下的文件快照内容
 * @param diffLines Myers / 行级差异对比结果集
 * @param showDiff 是否展示 Diff 对比视图
 * @param isLoading 是否在加载历史
 * @param isRestoring 是否正在进行版本回滚
 * @param errorMessage 错误提示消息
 */
data class HistoryUiState(
    val snippetTitle: String = "",
    val fileName: String = "",
    val folder: String = "",
    val commitList: List<GitCommitInfo> = emptyList(),
    val selectedCommitId: String? = null,
    val fileContentAtCommit: String? = null,
    val diffLines: List<DiffLine> = emptyList(),
    val showDiff: Boolean = false,
    val isLoading: Boolean = true,
    val isRestoring: Boolean = false,
    val errorMessage: String? = null
)

/**
 * [HistoryViewModel] 代码片段 Git 历史履历与版本回滚 ViewModel 控制器。
 *
 * 核心交互机制：
 * 1. 从 JGit 沙盒仓读取当前代码片段对应的历史 Git 提交节点树 [GitCommitInfo]。
 * 2. 支撑工作区当前版本与历史指定节点的实时行级 Diff 对比。
 * 3. 策略保证：在执行版本回滚 [restoreToVersion] 时，自动将当前工作区的最新代码提交快照留存，避免回滚导致当前更改丢失。
 *
 * @param snippetId 片段唯一 ID
 * @param snippetRepository 数据仓库服务
 * @param gitManager JGit 交互引擎
 */
class HistoryViewModel(
    private val snippetId: String,
    private val snippetRepository: SnippetRepository,
    private val gitManager: GitManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    /**
     * 加载片段的 Git 历史提交履历列表。
     */
    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val snippet = snippetRepository.getById(snippetId)
            if (snippet == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "片段不存在") }
                return@launch
            }

            val fileName = if (snippet.fileName.isBlank()) snippet.defaultFileName else snippet.fileName
            _uiState.update {
                it.copy(snippetTitle = snippet.title, fileName = fileName, folder = snippet.folder)
            }

            val result = gitManager.getFileHistory(fileName, snippet.folder)
            result.onSuccess { commits ->
                _uiState.update { it.copy(commitList = commits, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "加载历史失败") }
            }
        }
    }

    /**
     * 查看某次指定提交节点下的代码历史快照。
     *
     * @param commitId Git Commit 提交 Hash ID
     */
    fun viewCommitContent(commitId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val relativePath = if (state.folder.isBlank()) state.fileName else "${state.folder}/${state.fileName}"

            _uiState.update { it.copy(selectedCommitId = commitId, showDiff = false) }

            val result = gitManager.getFileContentAtCommit(commitId, relativePath)
            result.onSuccess { content ->
                _uiState.update { it.copy(fileContentAtCommit = content) }
            }.onFailure {
                _uiState.update { it.copy(fileContentAtCommit = null) }
            }
        }
    }

    /**
     * 对比任意两个历史提交版本之间的代码差异。
     *
     * @param oldCommitId 源历史 Commit ID
     * @param newCommitId 目标历史 Commit ID
     */
    fun compareVersions(oldCommitId: String, newCommitId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val relativePath = if (state.folder.isBlank()) state.fileName else "${state.folder}/${state.fileName}"

            val result = gitManager.getFileDiff(oldCommitId, newCommitId, relativePath)
            result.onSuccess { diff ->
                _uiState.update { it.copy(diffLines = diff, showDiff = true, selectedCommitId = newCommitId) }
            }
        }
    }

    /**
     * 关闭当前展开的 Diff 差异对比视图。
     */
    fun closeDiff() {
        _uiState.update { it.copy(showDiff = false, diffLines = emptyList()) }
    }

    /**
     * 将指定历史提交版本与当前工作区编辑器的代码开展行级 Diff 对比。
     *
     * @param commitId 用于对比的历史提交 Hash ID
     */
    fun compareWithCurrent(commitId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val snippet = snippetRepository.getById(snippetId) ?: return@launch
            val relativePath = if (state.folder.isBlank()) state.fileName else "${state.folder}/${state.fileName}"

            val oldContent = gitManager.getFileContentAtCommit(commitId, relativePath).getOrDefault("")
            val newContent = snippet.content

            val oldLines = oldContent.lines()
            val newLines = newContent.lines()
            val diffLines = mutableListOf<DiffLine>()

            var oldIdx = 0
            var newIdx = 0
            while (oldIdx < oldLines.size || newIdx < newLines.size) {
                when {
                    oldIdx >= oldLines.size -> {
                        diffLines.add(DiffLine(com.feige.snippetstudio.model.DiffType.ADD, newLines[newIdx], newLineNum = newIdx + 1))
                        newIdx++
                    }
                    newIdx >= newLines.size -> {
                        diffLines.add(DiffLine(com.feige.snippetstudio.model.DiffType.DELETE, oldLines[oldIdx], oldLineNum = oldIdx + 1))
                        oldIdx++
                    }
                    oldLines[oldIdx] == newLines[newIdx] -> {
                        diffLines.add(DiffLine(com.feige.snippetstudio.model.DiffType.CONTEXT, oldLines[oldIdx], oldLineNum = oldIdx + 1, newLineNum = newIdx + 1))
                        oldIdx++
                        newIdx++
                    }
                    else -> {
                        diffLines.add(DiffLine(com.feige.snippetstudio.model.DiffType.DELETE, oldLines[oldIdx], oldLineNum = oldIdx + 1))
                        diffLines.add(DiffLine(com.feige.snippetstudio.model.DiffType.ADD, newLines[newIdx], newLineNum = newIdx + 1))
                        oldIdx++
                        newIdx++
                    }
                }
            }

            _uiState.update { it.copy(diffLines = diffLines, showDiff = true, selectedCommitId = commitId) }
        }
    }

    /**
     * 恢复/回滚至指定的历史 Git 版本。
     *
     * 策略要求：回滚前自动将当前工作区代码提交留存一个【回滚前自动快照】Git 节点，确保修改可回溯防丢失。
     *
     * @param commitId 目标回滚版本的 Commit ID
     * @param onResult 执行结果闭包 (是否成功)
     */
    fun restoreToVersion(commitId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }

            val state = _uiState.value
            val relativePath = if (state.folder.isBlank()) state.fileName else "${state.folder}/${state.fileName}"

            val contentResult = gitManager.getFileContentAtCommit(commitId, relativePath)
            contentResult.onSuccess { targetContent ->
                val snippet = snippetRepository.getById(snippetId)
                if (snippet != null) {
                    // 1. 自动安全备份：将当前最新版本自动创建提交快照
                    val backupMessage = "回滚前自动快照: ${snippet.title}"
                    snippetRepository.saveOrUpdate(snippet, backupMessage)

                    // 2. 覆盖应用目标历史版本的代码内容
                    val restored = snippet.copy(
                        content = targetContent,
                        updatedAt = System.currentTimeMillis()
                    )
                    snippetRepository.saveOrUpdate(restored, "回滚至历史版本 $commitId")

                    _uiState.update { it.copy(isRestoring = false) }
                    loadHistory() // 重新加载提交列表
                    onResult(true)
                } else {
                    _uiState.update { it.copy(isRestoring = false) }
                    onResult(false)
                }
            }.onFailure {
                _uiState.update { it.copy(isRestoring = false) }
                onResult(false)
            }
        }
    }

    companion object {
        fun factory(
            snippetId: String,
            snippetRepository: SnippetRepository,
            gitManager: GitManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(snippetId, snippetRepository, gitManager) as T
            }
        }
    }
}
