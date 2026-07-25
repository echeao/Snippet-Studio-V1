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
import kotlinx.coroutines.flow.first

/**
 * [DetailUiState] 片段详情页面的 UI 响应式状态模型。
 *
 * @param snippet 当前加载展示的代码片段领域实体
 * @param isSourceExpanded 源代码折叠/展开展开控制开关
 * @param allAvailableTags 全局可用候选标签列表
 * @param existingFolders 全局所有存在的文件夹列表
 * @param isLoading 页面加载中状态
 */
data class DetailUiState(
    val snippet: Snippet? = null,
    val isSourceExpanded: Boolean = false,
    val allAvailableTags: List<String> = emptyList(),
    val existingFolders: List<String> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * [DetailViewModel] 代码片段详情查看页面的 ViewModel 控制器。
 *
 * 职责：
 * 1. 根据 [snippetId] 异步加载片段详情。
 * 2. 结合设置与数据库观察者更新标签与文件夹候选集。
 * 3. 响应用户在详情页进行的快捷更改：修改标签、重命名、移动文件夹与放入回收站。
 */
class DetailViewModel(
    private val snippetId: String,
    private val repository: SnippetRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState(isLoading = true))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadSnippet()

        // 监听系统全量候选标签与文件夹列表
        viewModelScope.launch {
            combine(
                settingsRepository.settingsFlow,
                repository.observeActive()
            ) { settings, activeSnippets ->
                val tags = (settings.customTags + activeSnippets.flatMap { it.tags }).distinct()
                val folders = activeSnippets.map { it.folder }.filter { it.isNotBlank() }.distinct()
                Pair(tags, folders)
            }.collect { (tags, folders) ->
                _uiState.update { it.copy(allAvailableTags = tags, existingFolders = folders) }
            }
        }
    }

    /** 从 Repository 异步获取指定 ID 的代码片段数据 */
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

    /** 切换源代码卡片的展开与折叠状态 */
    fun toggleSourceExpanded() {
        _uiState.update { it.copy(isSourceExpanded = !it.isSourceExpanded) }
    }

    /** 更新此代码片段的标签 */
    fun updateTags(tags: List<String>) {
        val currentSnippet = _uiState.value.snippet ?: return
        viewModelScope.launch {
            val updated = currentSnippet.copy(tags = tags)
            repository.saveOrUpdate(updated)
            _uiState.update { it.copy(snippet = updated) }
        }
    }

    /** 在详情页中快捷重命名 */
    fun renameSnippet(newTitle: String, newFileName: String) {
        val currentSnippet = _uiState.value.snippet ?: return
        viewModelScope.launch {
            val updated = currentSnippet.copy(
                title = newTitle,
                fileName = newFileName
            )
            repository.saveOrUpdate(updated)
            _uiState.update { it.copy(snippet = updated) }
        }
    }

    /** 在详情页中快捷移动归属文件夹 */
    fun updateFolder(newFolder: String) {
        val currentSnippet = _uiState.value.snippet ?: return
        viewModelScope.launch {
            val updated = currentSnippet.copy(folder = newFolder)
            repository.saveOrUpdate(updated)
            _uiState.update { it.copy(snippet = updated) }
        }
    }

    /** 将片段移入回收站并触发删除完成回调 [onDeleted] */
    fun trashSnippet(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val repoUri = settingsRepository.settingsFlow.first().repoTreeUri
            repository.trash(snippetId, repoUri)
            onDeleted()
        }
    }

    companion object {
        /** ViewModelFactory 工厂构造器 */
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

