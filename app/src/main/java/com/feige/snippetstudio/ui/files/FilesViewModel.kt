package com.feige.snippetstudio.ui.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.components.FilterOption
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.feige.snippetstudio.data.repo.SettingsRepository

/**
 * [SortMode] 列表排序模式枚举。
 */
enum class SortMode {
    /** 按修改时间降序 (最新优先) */
    UPDATED_DESC, 
    /** 按名称升序 (A-Z) */
    NAME_ASC, 
    /** 按片段类型分组升序 */
    TYPE_ASC
}

/**
 * [ViewMode] 列表视图模式枚举。
 */
enum class ViewMode {
    /** 展平列表视图 (Flat List) */
    FLAT, 
    /** 按文件夹分层树状视图 (Folder Tree) */
    TREE
}

/**
 * [FilesUiState] 文件管理与全量代码库页面的 UI 响应式状态模型。
 *
 * @param snippets 满足过滤/排序条件的完整片段列表
 * @param groupedFolders 按文件夹路径分组后的 Map 树状结构 (Folder -> Snippets)
 * @param existingFolders 全局所有存在的文件夹列表
 * @param searchQuery 搜索关键词
 * @param filterOption 当前选中的分类过滤 Chip
 * @param sortMode 排序模式 (更新时间 / 名称 / 类型)
 * @param viewMode 视图模式 (平铺 / 树状)
 * @param cardClickAction 卡片点击默认打开策略 ("detail" 或 "editor")
 * @param isLoading 加载状态
 */
data class FilesUiState(
    val snippets: List<Snippet> = emptyList(),
    val groupedFolders: Map<String, List<Snippet>> = emptyMap(),
    val existingFolders: List<String> = emptyList(),
    val searchQuery: String = "",
    val filterOption: FilterOption = FilterOption.All,
    val sortMode: SortMode = SortMode.UPDATED_DESC,
    val viewMode: ViewMode = ViewMode.FLAT,
    val cardClickAction: String = "detail",
    val isLoading: Boolean = true
)

private data class FilterParams(
    val query: String,
    val filter: FilterOption,
    val sort: SortMode,
    val viewMode: ViewMode
)

/**
 * [FilesViewModel] 文件与仓库页面的 ViewModel 控制器。
 *
 * 核心逻辑：
 * 1. 使用 Flow 管道组合：搜索词 + 筛选选项 + 排序模式 + 视图模式 => `_filterParams`。
 * 2. 结合 `repository.observeActive()` 实时获取活动片段，计算 `groupedFolders`（按文件夹路径归类分组）。
 * 3. 驱动星标切换、重命名、移动文件夹与放入回收站。
 */
class FilesViewModel(
    private val repository: SnippetRepository,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterOption = MutableStateFlow<FilterOption>(FilterOption.All)
    private val _sortMode = MutableStateFlow(SortMode.UPDATED_DESC)
    private val _viewMode = MutableStateFlow(ViewMode.FLAT)

    private val _filterParams = combine(_searchQuery, _filterOption, _sortMode, _viewMode) { query, filter, sort, viewMode ->
        FilterParams(query, filter, sort, viewMode)
    }

    /** 暴露给 FilesScreen 调用的单向 StateFlow UI 状态 */
    val uiState: StateFlow<FilesUiState> = combine(
        repository.observeActive(),
        _filterParams,
        settingsRepository?.settingsFlow ?: flowOf(com.feige.snippetstudio.model.AppSettings())
    ) { allSnippets, params, settings ->
        var list = allSnippets

        // ===== 1. 按类型/收藏状态过滤 =====
        list = when {
            params.filter.isFav -> list.filter { it.starred }
            params.filter.type != null -> list.filter { it.type == params.filter.type }
            else -> list
        }

        // ===== 2. 按搜索词模糊匹配 (标题 / 正文 / 标签) =====
        if (params.query.isNotBlank()) {
            list = list.filter {
                it.title.contains(params.query, ignoreCase = true) ||
                        it.content.contains(params.query, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(params.query, ignoreCase = true) }
            }
        }

        // ===== 3. 按指定 SortMode 排序模式升降序排列 =====
        list = when (params.sort) {
            SortMode.UPDATED_DESC -> list.sortedByDescending { it.updatedAt }
            SortMode.NAME_ASC -> list.sortedBy { it.displayTitle.lowercase() }
            SortMode.TYPE_ASC -> list.sortedBy { it.type.displayName }
        }

        // ===== 4. 树状分组算子 (Folder Tree Grouping) =====
        // 教学解析：使用 Kotlin 集合扩展函数 `groupBy`，将平铺代码片段按 `folder` 相对路径归类，
        // 空字符串表示项目根目录 "根目录"，产出类型为 Map<String, List<Snippet>> 的两层树状数据集供 UI LazyColumn 展开渲染。
        val grouped = list.groupBy { if (it.folder.isBlank()) "根目录" else it.folder }
        // 提炼去重后的非空文件夹名字列表
        val folders = allSnippets.map { it.folder }.filter { it.isNotBlank() }.distinct()

        FilesUiState(
            snippets = list,
            groupedFolders = grouped,
            existingFolders = folders,
            searchQuery = params.query,
            filterOption = params.filter,
            sortMode = params.sort,
            viewMode = params.viewMode,
            cardClickAction = settings.cardClickAction,
            isLoading = false
        )
    }
.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilesUiState(isLoading = true)
    )

    /** 更新搜索关键词 */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /** 选中 Chip 筛选条件 */
    fun onFilterSelect(option: FilterOption) {
        _filterOption.value = option
    }

    /** 循环切换排序规则 (时间降序 -> 名称升序 -> 类型升序 -> 时间降序) */
    fun cycleSortMode() {
        _sortMode.value = when (_sortMode.value) {
            SortMode.UPDATED_DESC -> SortMode.NAME_ASC
            SortMode.NAME_ASC -> SortMode.TYPE_ASC
            SortMode.TYPE_ASC -> SortMode.UPDATED_DESC
        }
    }

    /** 切换【平铺列表 FLAT】与【树状文件夹 TREE】视图模式 */
    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.FLAT) ViewMode.TREE else ViewMode.FLAT
    }

    /** 切换星标收藏状态 */
    fun toggleStar(id: String, currentStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStar(id, currentStarred)
        }
    }

    /** 重命名片段 */
    fun renameSnippet(id: String, newTitle: String, newFileName: String) {
        viewModelScope.launch {
            repository.updateRename(id, newTitle, newFileName)
        }
    }

    /** 移动至文件夹 */
    fun updateFolder(id: String, newFolder: String) {
        viewModelScope.launch {
            repository.updateFolder(id, newFolder)
        }
    }

    /** 移入回收站 */
    fun trashSnippet(id: String) {
        viewModelScope.launch {
            repository.trash(id)
        }
    }

    companion object {
        /** ViewModelFactory 工厂构造器 */
        fun factory(
            repository: SnippetRepository,
            settingsRepository: SettingsRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FilesViewModel(repository, settingsRepository) as T
            }
        }
    }
}

