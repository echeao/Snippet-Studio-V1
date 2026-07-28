package com.feige.snippetstudio.ui.subpage.vm

import com.feige.snippetstudio.data.repo.SettingsRepository
import com.feige.snippetstudio.data.repo.SnippetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * [TagSubState] 全局标签管理子页面的专属 UI 状态实体。
 *
 * @param tags 全局标签全集（自定义预设标签 + 片段中已使用的标签去重合并）
 */
data class TagSubState(
    val tags: List<String> = emptyList()
)

/**
 * [TagSubViewModel] 全局预设标签管理业务逻辑控制器。
 *
 * 架构职责：
 * 1. 聚合计算全局标签全集（settings.customTags + 所有活跃片段的 tags 去重）。
 * 2. 支持新增全局预设自定义标签 [addGlobalTag]。
 * 3. 支持删除全局预设自定义标签 [removeGlobalTag]。
 *
 * 生命周期由 [com.feige.snippetstudio.ui.subpage.SubPageViewModel] Facade 托管。
 *
 * @param scope 由 Facade 提供的协程作用域
 * @param settingsRepository 全局设置数据仓库
 * @param snippetRepository 代码片段数据仓库
 */
class TagSubViewModel(
    private val scope: CoroutineScope,
    private val settingsRepository: SettingsRepository,
    private val snippetRepository: SnippetRepository
) {
    /**
     * 对外暴露的标签管理响应式 UI 状态。
     * 通过 combine 合并 settings 自定义标签与活跃片段中已使用的标签。
     */
    val tagState: StateFlow<TagSubState> = combine(
        settingsRepository.settingsFlow,
        snippetRepository.observeActive()
    ) { settings, activeSnippets ->
        val allTags = (settings.customTags + activeSnippets.flatMap { it.tags }).distinct()
        TagSubState(tags = allTags)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TagSubState()
    )

    /**
     * 新增全局预设自定义标签。
     * 自动去除前后空白、移除 "#" 前缀并去重。
     *
     * @param tag 用户输入的标签文本
     */
    fun addGlobalTag(tag: String) {
        val trimmed = tag.trim().removePrefix("#").trim()
        if (trimmed.isBlank()) return
        scope.launch {
            settingsRepository.updateSettings { current ->
                val updated = (current.customTags + trimmed).distinct()
                current.copy(customTags = updated)
            }
        }
    }

    /**
     * 删除全局预设自定义标签。
     *
     * @param tag 待删除的标签文本
     */
    fun removeGlobalTag(tag: String) {
        scope.launch {
            settingsRepository.updateSettings { current ->
                val updated = current.customTags.filter { it != tag }
                current.copy(customTags = updated)
            }
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
