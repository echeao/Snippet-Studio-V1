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
import com.feige.snippetstudio.util.FuzzySearchUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.feige.snippetstudio.data.repo.SettingsRepository

/**
 * [HomeUiState] 首页的完整响应式 UI 状态实体。
 *
 * @param recentSnippets 过滤或最新修改的前 5 个代码片段
 * @param totalActiveCount 数据库活动片段总数量
 * @param searchQuery 当前输入的搜索关键字
 * @param cardClickAction 点击卡片默认动作 ("detail" 或 "editor")
 * @param existingFolders 全局已存在的所有文件夹列表
 * @param detectedClip 从剪贴板检测到的未处理文本快照 (若为 null 表示无新文本)
 * @param isLoading 加载状态
 * @param error 错误消息提示
 */
data class HomeUiState(
    val recentSnippets: List<Snippet> = emptyList(),
    val totalActiveCount: Int = 0,
    val searchQuery: String = "",
    val cardClickAction: String = "detail",
    val existingFolders: List<String> = emptyList(),
    val detectedClip: DetectedClip? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * [HomeViewModel] 首页界面对应的架构 ViewModel 业务控制层。
 *
 * 核心逻辑：
 * 1. 使用 Kotlin Coroutines Flow 的 [combine] 操作符，将代码片段数据库 Flow、搜索词 Flow、剪贴板 Flow 与系统设置 Flow 组合成单向数据流 [uiState]。
 * 2. 处理剪贴板智能代码识别（检查剪贴板 -> 保存为新代码片段 / 忽略）。
 * 3. 驱动收藏、删除、重命名与移动文件夹等数据改写动作。
 */
class HomeViewModel(
    private val repository: SnippetRepository,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _detectedClip = MutableStateFlow<DetectedClip?>(null)

    /**
     * 暴露给 HomeScreen 调用的单向 StateFlow UI 状态。
     *
     * 教学解析：
     * 1. `combine`: 响应式多流组合算子。将 4 个独立的源数据流合并：
     *    - 数据库片段 Flow
     *    - 内存搜索关键字 Flow
     *    - 剪贴板检测 Flow
     *    - DataStore 全局设置 Flow
     *    任何一个源 Flow 发生改变时，闭包都会被重新触发计算，自动产出全新的 `HomeUiState`。
     * 2. `stateIn`: 将冷流 (Cold Flow) 转化为热流 StateFlow。
     *    `SharingStarted.WhileSubscribed(5000)`: 当界面销毁或退入后台超过 5 秒（如屏幕旋转时保持 5000ms 缓存），
     *    上游数据流会自动暂停收集，极大地节省电池电量与 CPU 资源开销。
     */
    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeActive(),
        _searchQuery,
        _detectedClip,
        settingsRepository?.settingsFlow ?: flowOf(com.feige.snippetstudio.model.AppSettings())
    ) { snippets, query, clip, settings ->
        // ===== 步骤 1: 根据搜索关键词匹配过滤代码片段 =====
        val filtered = if (query.isBlank()) {
            snippets
        } else {
            snippets.filter {
                FuzzySearchUtil.match(it.title, query) ||
                        FuzzySearchUtil.match(it.content, query) ||
                        it.tags.any { tag -> FuzzySearchUtil.match(tag, query) }
            }
        }

        // ===== 步骤 2: 提取当前所有已创建的独立文件夹名称列表 =====
        val folders = snippets.map { it.folder }.filter { it.isNotBlank() }.distinct()

        // ===== 步骤 3: 封装并产出不可变 HomeUiState 状态 =====
        HomeUiState(
            recentSnippets = filtered.take(5), // 首页只截取展示最新 5 条记录
            totalActiveCount = snippets.size,
            searchQuery = query,
            cardClickAction = settings.cardClickAction,
            existingFolders = folders,
            detectedClip = clip,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope, // 绑定 ViewModel 的生命周期作用域
        started = SharingStarted.WhileSubscribed(5000), // 5 秒防退后台中断策略
        initialValue = HomeUiState(isLoading = true)
    )


    /** 更新搜索文本关键词 */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /** 检查系统剪贴板是否有最新复制代码 */
    fun checkClipboard(context: Context) {
        val clip = ClipboardDetector.detect(context)
        _detectedClip.value = clip
    }

    /** 忽略当前检测到的剪贴板内容 */
    fun ignoreClip(clip: DetectedClip) {
        ClipboardDetector.ignore(clip.contentHash)
        _detectedClip.value = null
    }

    /** 将检测到的剪贴板内容一键保存为新代码片段 */
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

    /** 切换代码片段的星标收藏状态 */
    fun toggleStar(id: String, currentStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStar(id, currentStarred)
        }
    }

    /** 重命名代码片段 */
    fun renameSnippet(id: String, newTitle: String, newFileName: String) {
        viewModelScope.launch {
            val repoUri = settingsRepository?.settingsFlow?.first()?.repoTreeUri ?: ""
            repository.updateRename(id, newTitle, newFileName, repoUri)
        }
    }

    /** 移动代码片段归属文件夹 */
    fun updateFolder(id: String, newFolder: String) {
        viewModelScope.launch {
            val repoUri = settingsRepository?.settingsFlow?.first()?.repoTreeUri ?: ""
            repository.updateFolder(id, newFolder, repoUri)
        }
    }

    /** 将代码片段移入回收站 */
    fun trashSnippet(id: String) {
        viewModelScope.launch {
            val repoUri = settingsRepository?.settingsFlow?.first()?.repoTreeUri ?: ""
            repository.trash(id, repoUri)
        }
    }

    companion object {
        /** 创建包含 ViewModel 依赖项的 ViewModelProvider.Factory 工厂对象 */
        fun factory(
            repository: SnippetRepository,
            settingsRepository: SettingsRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(repository, settingsRepository) as T
            }
        }
    }
}

