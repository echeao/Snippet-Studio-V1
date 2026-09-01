package com.feige.snippetstudio.ui.subpage.vm

import com.feige.snippetstudio.data.repo.ISettingsRepository
import com.feige.snippetstudio.data.repo.ISnippetRepository
import com.feige.snippetstudio.model.Snippet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * [TrashSubState] 回收站子页面的专属 UI 状态实体。
 *
 * @param trashedSnippets 当前处于软删除状态的代码片段列表
 */
data class TrashSubState(
    val trashedSnippets: List<Snippet> = emptyList()
)

/**
 * [TrashSubViewModel] 回收站软删除管理业务逻辑控制器。
 *
 * 架构职责：
 * 1. 观察并暴露回收站中已软删除的代码片段列表。
 * 2. 支持将片段从回收站还原至正式库 [restoreSnippet]。
 * 3. 支持将片段从回收站彻底永久删除 [purgeSnippet]。
 *
 * 生命周期由 [com.feige.snippetstudio.ui.subpage.SubPageViewModel] Facade 托管。
 *
 * @param scope 由 Facade 提供的协程作用域
 * @param settingsRepository 全局设置数据仓库契约接口（读取 repoTreeUri）
 * @param snippetRepository 代码片段数据仓库契约接口
 */
class TrashSubViewModel(
    private val scope: CoroutineScope,
    private val settingsRepository: ISettingsRepository,
    private val snippetRepository: ISnippetRepository
) {
    /**
     * 对外暴露的回收站响应式 UI 状态。
     * 直接映射 snippetRepository 的 trashed 观察流。
     */
    val trashState: StateFlow<TrashSubState> = snippetRepository.observeTrashed()
        .map { trashed -> TrashSubState(trashedSnippets = trashed) }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrashSubState()
        )

    /**
     * 从回收站恢复指定的代码片段至正式库。
     *
     * @param id 待还原片段的唯一标识符
     */
    fun restoreSnippet(id: String) {
        scope.launch {
            val repoUri = settingsRepository.settingsFlow.first().repoTreeUri
            snippetRepository.restore(id, repoUri)
        }
    }

    /**
     * 从回收站彻底永久删除代码片段（不可恢复）。
     *
     * @param id 待删除片段的唯一标识符
     */
    fun purgeSnippet(id: String) {
        scope.launch {
            val repoUri = settingsRepository.settingsFlow.first().repoTreeUri
            snippetRepository.purge(id, repoUri)
        }
    }

    /**
     * 销毁此子 ViewModel，取消所有正在执行的协程。
     * 由 Facade 在 onCleared() 时调用。
     */
    fun destroy() {
        // scope 由 Facade 统一管理取消，此处预留扩展点
    }
}
