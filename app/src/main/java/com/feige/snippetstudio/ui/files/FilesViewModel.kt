package com.feige.snippetstudio.ui.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SettingsRepository
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.components.FilterOption
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
 * [DensityMode] 列表显示密度模式枚举。
 */
enum class DensityMode {
    /** 舒适大卡片模式 (包含代码片段预览、字符数/行数统计、完整标签) */
    COMFORT, 
    /** 高密度紧凑模式 (单行极简列表，高密度展示更多文件) */
    COMPACT
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
 * @param densityMode 密度模式 (舒适大卡片 / 极简高密度)
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
    val densityMode: DensityMode = DensityMode.COMFORT,
    val cardClickAction: String = "detail",
    val isLoading: Boolean = true
)

private data class FilterParams(
    val query: String,
    val filter: FilterOption,
    val sort: SortMode,
    val viewMode: ViewMode,
    val densityMode: DensityMode
)

/**
 * [FilesViewModel] 文件与仓库页面的 ViewModel 控制器。
 *
 * 核心逻辑：
 * 1. 使用 Flow 管道组合：搜索词 + 筛选选项 + 排序模式 + 视图模式 + 密度模式 => `_filterParams`。
 * 2. 结合 `repository.observeActive()` 与 `repository.observeFolders()` 实时获取活动片段与文件夹数据库记录。
 * 3. 驱动星标切换、重命名、移动文件夹、显式新建文件夹与放入回收站。
 */
class FilesViewModel(
    private val repository: SnippetRepository,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterOption = MutableStateFlow<FilterOption>(FilterOption.All)
    private val _sortMode = MutableStateFlow(SortMode.UPDATED_DESC)
    private val _viewMode = MutableStateFlow(ViewMode.FLAT)
    private val _densityMode = MutableStateFlow(DensityMode.COMFORT)

    private val _filterParams = combine(_searchQuery, _filterOption, _sortMode, _viewMode, _densityMode) { query, filter, sort, viewMode, densityMode ->
        FilterParams(query, filter, sort, viewMode, densityMode)
    }

    /** 暴露给 FilesScreen 调用的单向 StateFlow UI 状态 */
    val uiState: StateFlow<FilesUiState> = combine(
        repository.observeActive(),
        repository.observeFolders(),
        _filterParams,
        settingsRepository?.settingsFlow ?: flowOf(com.feige.snippetstudio.model.AppSettings())
    ) { allSnippets, allDbFolders, params, settings ->
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

        // ===== 4. 融合文件夹与树状分组算子 (Folder Tree Grouping) =====
        // 提取 Room 数据库中 FolderEntity 持久化的所有文件夹与所有 Snippet 中出现的文件夹，去重合并
        val dbFolderPaths = allDbFolders.map { it.path }
        val snippetFolderPaths = allSnippets.map { it.folder }
        val existingFoldersList = (dbFolderPaths + snippetFolderPaths)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        // 使用 LinkedHashMap 保持有序展示
        val groupedMap = LinkedHashMap<String, List<Snippet>>()
        
        // A. 放入根目录 Snippet
        val rootSnippets = list.filter { it.folder.isBlank() }
        groupedMap["根目录"] = rootSnippets

        // B. 遍历所有的已存在文件夹（包含没有文件包含的物理/数据库空文件夹）
        existingFoldersList.forEach { folderName ->
            val folderSnippets = list.filter { it.folder == folderName }
            groupedMap[folderName] = folderSnippets
        }

        FilesUiState(
            snippets = list,
            groupedFolders = groupedMap,
            existingFolders = existingFoldersList,
            searchQuery = params.query,
            filterOption = params.filter,
            sortMode = params.sort,
            viewMode = params.viewMode,
            densityMode = params.densityMode,
            cardClickAction = settings.cardClickAction,
            isLoading = false
        )
    }.stateIn(
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

    /** 切换【大卡片 COMFORT】与【高密度 COMPACT】显示密度 */
    fun toggleDensityMode() {
        _densityMode.value = if (_densityMode.value == DensityMode.COMFORT) DensityMode.COMPACT else DensityMode.COMFORT
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

    /**
     * 显式创建新文件夹（同步更新 Room SQLite 数据库与物理磁盘）。
     *
     * @param folderName 相对文件夹路径（如 "components" 或 "utils/string"）
     * @param repoTreeUriStr SAF 目录 URI 字符串
     */
    fun createFolder(folderName: String, repoTreeUriStr: String = "") {
        viewModelScope.launch {
            repository.createFolder(folderName, repoTreeUriStr)
        }
    }

    /** 移入回收站 */
    fun trashSnippet(id: String) {
        viewModelScope.launch {
            val repoUri = settingsRepository?.settingsFlow?.first()?.repoTreeUri ?: ""
            repository.trash(id, repoUri)
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
