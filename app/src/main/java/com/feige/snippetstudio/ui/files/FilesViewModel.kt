package com.feige.snippetstudio.ui.files

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SettingsRepository
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.components.FilterOption
import com.feige.snippetstudio.util.FuzzySearchUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * [SortMode] 列表排序模式枚举。
 *
 * 声明代码片段列表在界面展示时的排序依据规则。
 */
enum class SortMode {
    /** 按修改时间降序 (最新更新优先) */
    UPDATED_DESC, 
    /** 按片段显示名称升序 (A-Z) */
    NAME_ASC, 
    /** 按片段代码语言类型升序 */
    TYPE_ASC
}

/**
 * [ViewMode] 列表视图展现结构枚举。
 */
enum class ViewMode {
    /** 展平单级列表视图 (Flat List) */
    FLAT, 
    /** 按文件夹分层树状视图 (Folder Tree) */
    TREE
}

/**
 * [DensityMode] 列表卡片显示密度模式枚举。
 */
enum class DensityMode {
    /** 舒适大卡片模式 (包含代码片段预览、字符数/行数统计、完整标签) */
    COMFORT, 
    /** 高密度紧凑模式 (单行极简列表，高密度展示更多文件) */
    COMPACT
}

/**
 * [FilesUiState] 文件管理与全量代码库页面的 UI 响应式不可变状态模型。
 *
 * 使用 [@Immutable] 标记确保 Compose 编译器精准识别不可变性，提高 Recomposition 跳过效率。
 *
 * @param snippets 满足当前过滤/搜索/排序条件的代码片段列表
 * @param groupedFolders 按文件夹路径分组后的 Map 结构 (Folder -> List<Snippet>)
 * @param existingFolders 全局所有已存在的文件夹名称列表
 * @param searchQuery 当前输入的搜索关键词
 * @param filterOption 当前选中的类型/收藏过滤 Chip
 * @param sortMode 当前生效的排序模式 (更新时间 / 名称 / 类型)
 * @param viewMode 当前生效的视图结构模式 (平铺 / 树状)
 * @param densityMode 当前生效的显示密度模式 (舒适大卡片 / 极简高密度)
 * @param cardClickAction 点击卡片的默认跳转行为 ("detail" 或 "editor")
 * @param isLoading 异步加载状态标志
 */
@Immutable
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

/**
 * [FilterParams] 内部流组合过滤参数数据载体。
 *
 * 用于在 Flow 的 `combine` 操作符中聚合用户的多维度控制状态。
 */
private data class FilterParams(
    val query: String,
    val filter: FilterOption,
    val sort: SortMode,
    val viewMode: ViewMode,
    val densityMode: DensityMode
)

/**
 * [FilesViewModel] 文件与全量代码仓库页面的 ViewModel 业务逻辑控制器。
 *
 * 架构责任与优化设计：
 * 1. **响应式状态聚合**：通过 Flow 管道将搜索、过滤、排序、视图及设置 Flow 进行极速响应式计算。
 * 2. **O(N) 性能优化算法**：使用 Kotlin 的 `groupBy` 进行文件夹分组算子重构，替代以往的 O(N*M) 嵌套查找，大幅降低 CPU 开销。
 * 3. **完整数据生命周期**：管理星标状态、重命名、移动文件夹、创建文件夹、删除文件夹以及移入回收站与撤销恢复。
 *
 * @param repository 代码片段与文件数据仓库实例 [SnippetRepository]
 * @param settingsRepository 应用全局设置数据仓库实例（可选）[SettingsRepository]
 */
class FilesViewModel(
    private val repository: SnippetRepository,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    /** 搜索框内部关键词 Flow */
    private val _searchQuery = MutableStateFlow("")
    /** 分类过滤 Chip 内部选中 Flow */
    private val _filterOption = MutableStateFlow<FilterOption>(FilterOption.All)
    /** 排序规则内部 Flow */
    private val _sortMode = MutableStateFlow(SortMode.UPDATED_DESC)
    /** 视图模式内部 Flow (平铺/树状) */
    private val _viewMode = MutableStateFlow(ViewMode.FLAT)
    /** 显示密度内部 Flow (大卡片/高密度) */
    private val _densityMode = MutableStateFlow(DensityMode.COMFORT)

    /** 聚合控制参数 Flow */
    private val _filterParams = combine(_searchQuery, _filterOption, _sortMode, _viewMode, _densityMode) { query, filter, sort, viewMode, densityMode ->
        FilterParams(query, filter, sort, viewMode, densityMode)
    }

    /** 暴露给 UI 界面观察的单向只读 StateFlow 响应式状态 */
    val uiState: StateFlow<FilesUiState> = combine(
        repository.observeActive(),
        repository.observeFolders(),
        _filterParams,
        settingsRepository?.settingsFlow ?: flowOf(com.feige.snippetstudio.model.AppSettings())
    ) { allSnippets, allDbFolders, params, settings ->
        var list = allSnippets

        // ===== 1. 按类型 / 收藏状态条件过滤 =====
        list = when {
            params.filter.isFav -> list.filter { it.starred }
            params.filter.type != null -> list.filter { it.type == params.filter.type }
            else -> list
        }

        // ===== 2. 按搜索词模糊匹配 (标题 / 正文 / 标签) =====
        if (params.query.isNotBlank()) {
            list = list.filter {
                FuzzySearchUtil.match(it.title, params.query) ||
                        FuzzySearchUtil.match(it.content, params.query) ||
                        it.tags.any { tag -> FuzzySearchUtil.match(tag, params.query) }
            }
        }

        // ===== 3. 按指定 SortMode 排序模式升降序排列 =====
        list = when (params.sort) {
            SortMode.UPDATED_DESC -> list.sortedByDescending { it.updatedAt }
            SortMode.NAME_ASC -> list.sortedBy { it.displayTitle.lowercase() }
            SortMode.TYPE_ASC -> list.sortedBy { it.type.displayName }
        }

        // ===== 4. 优化：高效 O(N) 文件夹树状分组算子 (Folder Tree Grouping) =====
        // 提取 Room 数据库中 FolderEntity 持久化的所有文件夹与 Snippet 中出现的文件夹，去重合并
        val dbFolderPaths = allDbFolders.map { it.path }
        val snippetFolderPaths = allSnippets.map { it.folder }
        val existingFoldersList = (dbFolderPaths + snippetFolderPaths)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        // 使用 O(N) 的 groupBy 单次遍历快速收集已知片段
        val snippetsByFolder = list.groupBy { it.folder }

        // 使用 LinkedHashMap 保持根目录优先及字母排序顺序
        val groupedMap = LinkedHashMap<String, List<Snippet>>()
        
        // A. 插入根目录 Snippet (folder 为空的片段)
        groupedMap["根目录"] = snippetsByFolder[""] ?: emptyList()

        // B. 遍历已知文件夹，保证即使是空文件夹也能正常占位展示
        existingFoldersList.forEach { folderName ->
            groupedMap[folderName] = snippetsByFolder[folderName] ?: emptyList()
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

    /**
     * 更新搜索关键词。
     *
     * @param query 用户输入的搜索字符串
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * 切换 Chip 类型的筛选选项。
     *
     * @param option 选中的 FilterOption 类别
     */
    fun onFilterSelect(option: FilterOption) {
        _filterOption.value = option
    }

    /**
     * 设置显式指定的排序规则。
     *
     * @param mode 目标排序模式 [SortMode]（更新时间降序 / 名称升序 / 类型升序）
     */
    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    /**
     * 循环切换排序规则 (时间降序 -> 名称升序 -> 类型升序 -> 时间降序)。
     */
    fun cycleSortMode() {
        _sortMode.value = when (_sortMode.value) {
            SortMode.UPDATED_DESC -> SortMode.NAME_ASC
            SortMode.NAME_ASC -> SortMode.TYPE_ASC
            SortMode.TYPE_ASC -> SortMode.UPDATED_DESC
        }
    }

    /**
     * 切换【平铺列表 FLAT】与【树状文件夹 TREE】视图结构模式。
     */
    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.FLAT) ViewMode.TREE else ViewMode.FLAT
    }

    /**
     * 切换【大卡片 COMFORT】与【极简高密度 COMPACT】显示密度。
     */
    fun toggleDensityMode() {
        _densityMode.value = if (_densityMode.value == DensityMode.COMFORT) DensityMode.COMPACT else DensityMode.COMFORT
    }

    /**
     * 切换指定代码片段的星标收藏状态。
     *
     * @param id 目标代码片段 ID
     * @param currentStarred 当前星标状态
     */
    fun toggleStar(id: String, currentStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStar(id, currentStarred)
        }
    }

    /**
     * 重命名指定代码片段的标题及文件名。
     *
     * @param id 目标片段 ID
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
     * 将代码片段移动至指定的目标文件夹。
     *
     * @param id 目标片段 ID
     * @param newFolder 目标文件夹相对路径
     */
    fun updateFolder(id: String, newFolder: String) {
        viewModelScope.launch {
            val repoUri = settingsRepository?.settingsFlow?.first()?.repoTreeUri ?: ""
            repository.updateFolder(id, newFolder, repoUri)
        }
    }

    /**
     * 显式创建新文件夹（同步更新 Room 数据库与物理存储）。
     *
     * @param folderName 相对文件夹路径（如 "components"）
     * @param repoTreeUriStr SAF 授权目录 URI 字符串
     */
    fun createFolder(folderName: String, repoTreeUriStr: String = "") {
        viewModelScope.launch {
            repository.createFolder(folderName, repoTreeUriStr)
        }
    }

    /**
     * 重命名文件夹，将该文件夹下的所有代码片段更新至新文件夹路径。
     *
     * @param oldFolder 旧文件夹路径
     * @param newFolder 新文件夹路径
     */
    fun renameFolder(oldFolder: String, newFolder: String) {
        viewModelScope.launch {
            val repoUri = settingsRepository?.settingsFlow?.first()?.repoTreeUri ?: ""
            val cleanOld = oldFolder.trim().trim('/')
            val cleanNew = newFolder.trim().trim('/')
            if (cleanOld.isBlank() || cleanNew.isBlank() || cleanOld == cleanNew) return@launch
            
            // 找到包含在旧文件夹里的所有代码片段并进行批量文件夹迁移
            val affectedSnippets = repository.allForExport().filter { it.folder == cleanOld }
            affectedSnippets.forEach { snippet ->
                repository.updateFolder(snippet.id, cleanNew, repoUri)
            }
            // 同时建立新文件夹记录
            repository.createFolder(cleanNew, repoUri)
        }
    }

    /**
     * 将代码片段移入回收站 (软删除)。
     *
     * @param id 代码片段 ID
     */
    fun trashSnippet(id: String) {
        viewModelScope.launch {
            val repoUri = settingsRepository?.settingsFlow?.first()?.repoTreeUri ?: ""
            repository.trash(id, repoUri)
        }
    }

    /**
     * 从回收站恢复（撤销删除）指定代码片段。
     *
     * @param id 恢复的目标代码片段 ID
     */
    fun restoreSnippet(id: String) {
        viewModelScope.launch {
            val repoUri = settingsRepository?.settingsFlow?.first()?.repoTreeUri ?: ""
            repository.restore(id, repoUri)
        }
    }

    companion object {
        /**
         * ViewModelFactory 工厂构造方法，注入 Repository 依赖。
         *
         * @param repository 数据仓库实例 [SnippetRepository]
         * @param settingsRepository 全局设置仓库 [SettingsRepository]
         */
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

