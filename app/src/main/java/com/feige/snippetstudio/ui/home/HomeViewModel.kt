package com.feige.snippetstudio.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SettingsRepository
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.util.ClipboardDetector
import com.feige.snippetstudio.util.DetectedClip
import com.feige.snippetstudio.util.FuzzySearchUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [HomeUiState] 首页的完整响应式 UI 状态数据类。
 *
 * @param recentSnippets 过滤或最新修改的前 5 个代码片段列表
 * @param totalActiveCount 数据库中所有活动代码片段的总数量
 * @param starredCount 已设为星标收藏的代码片段数量
 * @param searchQuery 当前在搜索栏中输入的过滤关键字
 * @param cardClickAction 点击代码片段卡片的默认响应动作 ("detail" 查看详情 或 "editor" 直接编辑)
 * @param existingFolders 当前所有活动代码片段归属的非空文件夹名称列表
 * @param detectedClip 自动检索系统剪贴板识别到的代码片段数据快照（为 null 表示无最新待处理项）
 * @param isLoading 界面数据加载中状态标识
 * @param error 异常错误提示信息文本
 */
data class HomeUiState(
    val recentSnippets: List<Snippet> = emptyList(),
    val totalActiveCount: Int = 0,
    val starredCount: Int = 0,
    val searchQuery: String = "",
    val cardClickAction: String = "detail",
    val existingFolders: List<String> = emptyList(),
    val detectedClip: DetectedClip? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * [HomeViewModel] 首页视图对应的状态管理与业务逻辑 ViewModel。
 *
 * 核心功能：
 * 1. 使用 Coroutines Flow 的 [combine] 操作符结合 [Dispatchers.Default] 线程调度器，将数据库观察流、
 *    内存搜索流、剪贴板识别流与全局设置流合并为高效不可变的 [uiState]。
 * 2. 剪贴板识别与智能转换代码片段逻辑。
 * 3. 处理代码片段的星标状态切换、重命名、归属文件夹变更及移入回收站等写操作。
 *
 * @param repository 代码片段数据仓储依赖接口
 * @param settingsRepository 应用全局配置仓储依赖接口（可选）
 */
class HomeViewModel(
    private val repository: SnippetRepository,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    /** 内存搜索文本关键字数据流 */
    private val _searchQuery = MutableStateFlow("")

    /** 剪贴板最新识别结果数据流 */
    private val _detectedClip = MutableStateFlow<DetectedClip?>(null)

    /**
     * 暴露给 HomeScreen 订阅的响应式 UI 状态流 [StateFlow]。
     *
     * 优化亮点：
     * 1. 使用 `withContext(Dispatchers.Default)` 将大量数据的模糊匹配过滤与分类统计推入 CPU 后台线程计算，避免阻塞主线程。
     * 2. `SharingStarted.WhileSubscribed(5000)` 在屏幕旋转或短时间离开时保持缓存，后台超过 5s 自动停止订阅，节省电力开销。
     */
    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeActive(),
        _searchQuery,
        _detectedClip,
        settingsRepository?.settingsFlow ?: flowOf(com.feige.snippetstudio.model.AppSettings())
    ) { snippets, query, clip, settings ->
        withContext(Dispatchers.Default) {
            // ===== 步骤 1: 异步后台执行模糊搜索过滤 =====
            val filtered = if (query.isBlank()) {
                snippets
            } else {
                snippets.filter {
                    FuzzySearchUtil.match(it.title, query) ||
                            FuzzySearchUtil.match(it.content, query) ||
                            it.tags.any { tag -> FuzzySearchUtil.match(tag, query) }
                }
            }

            // ===== 步骤 2: 提取当前所有已创建的独立文件夹列表 =====
            val folders = snippets.map { it.folder }.filter { it.isNotBlank() }.distinct()

            // ===== 步骤 3: 计算星标收藏代码片段总数 =====
            val starredCount = snippets.count { it.starred }

            // ===== 步骤 4: 封装并产出全新的不可变 HomeUiState 状态 =====
            HomeUiState(
                recentSnippets = filtered.take(5), // 首页只截取展示最新 5 条记录
                totalActiveCount = snippets.size,
                starredCount = starredCount,
                searchQuery = query,
                cardClickAction = settings.cardClickAction,
                existingFolders = folders,
                detectedClip = clip,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    /**
     * 更新搜索关键字。
     *
     * @param query 用户输入的搜索字符串
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * 检查系统剪贴板中是否有符合要求的最新代码文本。
     *
     * @param context Android 应用上下文 Context
     */
    fun checkClipboard(context: Context) {
        val clip = ClipboardDetector.detect(context)
        _detectedClip.value = clip
    }

    /**
     * 忽略当前识别到的剪贴板片段，并计入已忽略哈希集合。
     *
     * @param clip 待忽略的剪贴板识别实体
     */
    fun ignoreClip(clip: DetectedClip) {
        ClipboardDetector.ignore(clip.contentHash)
        _detectedClip.value = null
    }

    /**
     * 一键保存剪贴板识别结果为新的代码片段并跳转编辑器。
     *
     * @param clip 待保存的剪贴板识别实体
     * @param onSaved 保存成功后的回调，传入新建代码片段的 ID
     */
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

    /**
     * 切换指定代码片段的星标收藏状态。
     *
     * @param id 代码片段唯一标识符 ID
     * @param currentStarred 当前星标状态
     */
    fun toggleStar(id: String, currentStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStar(id, currentStarred)
        }
    }

    /**
     * 重命名指定的代码片段。
     *
     * @param id 代码片段 ID
     * @param newTitle 新标题
     * @param newFileName 新文件名
     */
    fun renameSnippet(id: String, newTitle: String, newFileName: String) {
        viewModelScope.launch {
            val repoUri = settingsRepository?.settingsFlow?.first()?.repoTreeUri ?: ""
            repository.updateRename(id, newTitle, newFileName, repoUri)
        }
    }

    /**
     * 变更代码片段归属的文件夹。
     *
     * @param id 代码片段 ID
     * @param newFolder 目标文件夹名称
     */
    fun updateFolder(id: String, newFolder: String) {
        viewModelScope.launch {
            val repoUri = settingsRepository?.settingsFlow?.first()?.repoTreeUri ?: ""
            repository.updateFolder(id, newFolder, repoUri)
        }
    }

    /**
     * 将代码片段移入回收站。
     *
     * @param id 代码片段 ID
     */
    fun trashSnippet(id: String) {
        viewModelScope.launch {
            val repoUri = settingsRepository?.settingsFlow?.first()?.repoTreeUri ?: ""
            repository.trash(id, repoUri)
        }
    }

    companion object {
        /**
         * 创建包含 [HomeViewModel] 依赖项的 [ViewModelProvider.Factory] 工厂对象。
         *
         * @param repository 数据仓储依赖
         * @param settingsRepository 设置仓储依赖（可选）
         * @return 初始化 ViewModel 的工厂实例
         */
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
