package com.feige.snippetstudio.ui.subpage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.git.GitManager
import com.feige.snippetstudio.data.repo.ISettingsRepository
import com.feige.snippetstudio.data.repo.ISnippetRepository
import com.feige.snippetstudio.model.AppSettings
import com.feige.snippetstudio.ui.subpage.vm.GitSubViewModel
import com.feige.snippetstudio.ui.subpage.vm.SettingsSubViewModel
import com.feige.snippetstudio.ui.subpage.vm.TagSubViewModel
import com.feige.snippetstudio.ui.subpage.vm.TrashSubViewModel
import kotlinx.coroutines.flow.*

/**
 * [SubPageUiState] 设置子页面的轻量级路由分派 UI 状态。
 *
 * 重构说明：原 God Object 式的全量状态已拆分至各子 ViewModel 的专属 State 中，
 * 此处仅保留路由分派与跨域共享所需的最小字段集。
 *
 * @param key 当前子页面标识 ("git", "gitlog", "repo", "cat", "tags", "trash", "lang", "theme", "about")
 * @param settings 全局偏好设置实体（各子页面均需读取）
 * @param categoryCounts 按语言分类统计的片段数量集合（Category 子页面使用）
 * @param isLoading 页面初始化加载状态
 */
data class SubPageUiState(
    val key: String = "",
    val settings: AppSettings = AppSettings(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true
)

/**
 * - [GitSubViewModel]：Git 远程仓库连接/同步/预览/Diff/Log（~320 行）
 * - [TrashSubViewModel]：回收站恢复/永久删除（~30 行）
 * - [TagSubViewModel]：全局标签增删（~30 行）
 * - [SettingsSubViewModel]：语言/主题/Repo 路径设置（~50 行）
 *
 * 本 Facade 职责：
 * 1. 根据路由 [key] 惰性创建对应的子 ViewModel 实例。
 * 2. 维护最小化的 [uiState]（仅含路由 key + settings + categoryCounts）。
 * 3. 在 [onCleared] 时统一销毁所有子 ViewModel 并取消协程。
 *
 * @param key 当前子页面路由标识
 * @param settingsRepository 全局设置数据仓库
 * @param snippetRepository 代码片段数据仓库
 * @param gitManager JGit 管理器实例（可为 null）
 */
class SubPageViewModel(
    val key: String,
    private val settingsRepository: ISettingsRepository,
    private val snippetRepository: ISnippetRepository,
    private val gitManager: GitManager? = null
) : ViewModel() {

    // ===== 惰性子 ViewModel 实例（按 key 按需创建） =====

    /** Git 远程仓库管理子 ViewModel（key = "git" 或 "gitlog" 时激活） */
    val gitVm: GitSubViewModel by lazy {
        GitSubViewModel(viewModelScope, settingsRepository, snippetRepository, gitManager)
    }

    /** 回收站管理子 ViewModel（key = "trash" 时激活） */
    val trashVm: TrashSubViewModel by lazy {
        TrashSubViewModel(viewModelScope, settingsRepository, snippetRepository)
    }

    /** 全局标签管理子 ViewModel（key = "tags" 时激活） */
    val tagVm: TagSubViewModel by lazy {
        TagSubViewModel(viewModelScope, settingsRepository, snippetRepository)
    }

    /** 通用设置子 ViewModel（key = "repo" / "lang" / "theme" 时激活） */
    val settingsVm: SettingsSubViewModel by lazy {
        SettingsSubViewModel(viewModelScope, settingsRepository, snippetRepository)
    }

    init {
        // 根据路由 key 预热对应的子 ViewModel（触发 lazy 初始化）
        when (key) {
            "git" -> gitVm
            "gitlog" -> {
                gitVm
                gitVm.loadGitLog() // GitLog 页面需自动加载提交历史
            }
            "trash" -> trashVm
            "tags" -> tagVm
            "repo", "lang", "theme" -> settingsVm
        }
    }

    /**
     * 对外暴露的轻量级路由分派 UI 状态。
     * 仅 combine settings 与 activeSnippets（用于 categoryCounts 计算）。
     */
    val uiState: StateFlow<SubPageUiState> = combine(
        settingsRepository.settingsFlow,
        snippetRepository.observeActive()
    ) { settings, active ->
        val counts = active.groupBy { it.type.displayName }.mapValues { it.value.size }
        SubPageUiState(
            key = key,
            settings = settings,
            categoryCounts = counts,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubPageUiState(key = key, isLoading = true)
    )

    companion object {
        /**
         * ViewModelFactory 工厂构造器。
         * 供 [androidx.lifecycle.viewmodel.compose.viewModel] 在 NavGraph 中创建实例。
         */
        fun factory(
            key: String,
            settingsRepository: ISettingsRepository,
            snippetRepository: ISnippetRepository,
            gitManager: GitManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SubPageViewModel(key, settingsRepository, snippetRepository, gitManager) as T
            }
        }
    }
}
