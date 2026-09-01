package com.feige.snippetstudio.ui.subpage.vm

import android.content.Context
import com.feige.snippetstudio.data.repo.ISettingsRepository
import com.feige.snippetstudio.data.repo.ISnippetRepository
import com.feige.snippetstudio.model.AppSettings
import com.feige.snippetstudio.util.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * [SettingsSubState] 通用设置子页面（语言/主题/仓库路径）的专属 UI 状态实体。
 *
 * @param settings 全局偏好设置实体
 */
data class SettingsSubState(
    val settings: AppSettings = AppSettings()
)

/**
 * [SettingsSubViewModel] 通用设置管理业务逻辑控制器。
 *
 * 架构职责：
 * 1. 绑定/切换 SAF 本地磁盘工作区目录树 [updateRepoPath]。
 * 2. 切换系统显示语言并动态更新 Locale [setLanguage]。
 * 3. 切换全局配色风格主题 [setColorTheme]。
 *
 * 生命周期由 [com.feige.snippetstudio.ui.subpage.SubPageViewModel] Facade 托管。
 *
 * @param scope 由 Facade 提供的协程作用域
 * @param settingsRepository 全局设置数据仓库契约接口
 * @param snippetRepository 代码片段数据仓库契约接口（用于工作区同步）
 */
class SettingsSubViewModel(
    private val scope: CoroutineScope,
    private val settingsRepository: ISettingsRepository,
    private val snippetRepository: ISnippetRepository
) {
    /**
     * 对外暴露的设置响应式 UI 状态。
     * 直接映射 settingsRepository 的设置流。
     */
    val settingsState: StateFlow<SettingsSubState> = settingsRepository.settingsFlow
        .map { settings -> SettingsSubState(settings = settings) }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsSubState()
        )

    /**
     * 切换并绑定本地 SAF 磁盘目录树作为工作区。
     * 绑定后自动触发 [SnippetRepository.syncWithLocalRepository] 执行文件同步。
     *
     * @param context Android Context（用于 SAF 权限获取）
     * @param pathDisplay 用于展示的路径名称
     * @param treeUriStr SAF DocumentTree URI 字符串
     */
    fun updateRepoPath(context: Context, pathDisplay: String, treeUriStr: String) {
        scope.launch {
            settingsRepository.updateSettings {
                it.copy(
                    repoPath = pathDisplay,
                    repoTreeUri = treeUriStr
                )
            }
            snippetRepository.syncWithLocalRepository(context, treeUriStr)
        }
    }

    /**
     * 切换系统显示语言并在 Context 中动态更新 Locale。
     *
     * @param context Android Context
     * @param langCode 目标语言代码 ("zh" / "en" / "ja")
     */
    fun setLanguage(context: Context, langCode: String) {
        scope.launch {
            settingsRepository.updateSettings { it.copy(lang = langCode) }
            LocaleHelper.setLocale(context, langCode)
        }
    }

    /**
     * 切换全局配色风格主题。
     *
     * @param colorThemeId 配色主题标识符 ("forest", "ocean", "sunset", "lavender", "mono")
     */
    fun setColorTheme(colorThemeId: String) {
        scope.launch {
            settingsRepository.updateSettings { it.copy(colorTheme = colorThemeId) }
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
