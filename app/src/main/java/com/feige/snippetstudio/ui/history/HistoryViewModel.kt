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
 * [HistoryUiState] Git 历史履历页面的 UI 状态。
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
 * [HistoryViewModel] 单片段 Git 历史履历的 ViewModel 控制器。
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

    /** 加载片段的 Git 提交历史 */
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

    /** 查看某次提交的文件内容 */
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

    /** 对比两个版本之间的差异 */
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

    /** 关闭 Diff 视图 */
    fun closeDiff() {
        _uiState.update { it.copy(showDiff = false, diffLines = emptyList()) }
    }

    /** 与当前工作区版本对比 */
    fun compareWithCurrent(commitId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val snippet = snippetRepository.getById(snippetId) ?: return@launch
            val relativePath = if (state.folder.isBlank()) state.fileName else "${state.folder}/${state.fileName}"

            val oldContent = gitManager.getFileContentAtCommit(commitId, relativePath).getOrDefault("")
            val newContent = snippet.content

            // 简单行级 diff
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

    /** 恢复到指定历史版本 */
    fun restoreToVersion(commitId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }

            val state = _uiState.value
            val relativePath = if (state.folder.isBlank()) state.fileName else "${state.folder}/${state.fileName}"

            val contentResult = gitManager.getFileContentAtCommit(commitId, relativePath)
            contentResult.onSuccess { content ->
                val snippet = snippetRepository.getById(snippetId)
                if (snippet != null) {
                    val restored = snippet.copy(
                        content = content,
                        updatedAt = System.currentTimeMillis()
                    )
                    snippetRepository.saveOrUpdate(restored, "")
                    _uiState.update { it.copy(isRestoring = false) }
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
