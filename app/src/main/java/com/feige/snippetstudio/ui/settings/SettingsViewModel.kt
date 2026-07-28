package com.feige.snippetstudio.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SettingsRepository
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.AppSettings
import com.feige.snippetstudio.util.Exporter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.io.File

/**
 * [SettingsViewModel] 系统全局设置页面的 ViewModel 控制器。
 *
 * 职责：
 * 1. 暴露 [settings] 全局偏好设置的 StateFlow 流。
 * 2. 处理外观主题切换（深色/浅色）、卡片默认点击响应策略。
 * 3. 驱动全量数据 JSON 导出与 ZIP 压缩文件导出。
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val snippetRepository: SnippetRepository
) : ViewModel() {

    /** 暴露给界面层的单向 settings 数据流 */
    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    /** 切换深色/浅色主题 */
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(theme = if (enabled) "dark" else "light")
            }
        }
    }

    /**
     * 切换配色风格主题。
     *
     * @param colorThemeId 配色主题标识符 ("forest", "ocean", "sunset", "lavender", "mono")
     */
    fun updateColorTheme(colorThemeId: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(colorTheme = colorThemeId)
            }
        }
    }

    /** 切换新建代码片段时是否注入默认样板代码 */
    fun toggleUseBoilerplate(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(useBoilerplate = enabled)
            }
        }
    }

    /** 更新片段卡片点击触发动作 ("detail": 详情页, "editor": 编辑页) */
    fun updateCardClickAction(action: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(cardClickAction = action)
            }
        }
    }

    /** 更新系统分享剪藏接收行为 ("panel": 编辑面板, "silent": 静默保存) */
    fun updateShareAction(action: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(shareAction = action)
            }
        }
    }

    /** 调整代码编辑器文本字号 (sp) */
    fun updateEditorFontSp(sp: Float) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(editorFontSp = sp.coerceIn(10f, 24f))
            }
        }
    }

    /** 切换代码编辑器自动软换行 */
    fun toggleWordWrap(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(isWordWrap = enabled)
            }
        }
    }

    /** 切换代码编辑器显示行号 */
    fun toggleShowLineNumbers(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(showLineNumbers = enabled)
            }
        }
    }

    /** 调整编辑器 Tab 缩进空格数 (2 或 4) */
    fun updateTabSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(tabSize = size)
            }
        }
    }

    /** 切换编辑器括号与引号自动配对补全 */
    fun toggleAutoPairBrackets(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(autoPairBrackets = enabled)
            }
        }
    }

    /**
     * 将选中的 SAF JSON 备份文件反序列化解析并恢复合并入本地数据库中。
     *
     * @param context 上下文对象
     * @param uri 用户选择的 JSON 文件 Uri
     * @param onResult 结果回调 (success: 是否成功, count: 恢复片段数量)
     */
    fun importBackupJson(context: Context, uri: Uri, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            val snippets = Exporter.parseJsonImport(context, uri)
            if (snippets != null) {
                var count = 0
                val currentTreeUri = settings.value.repoTreeUri
                snippets.forEach { snippet ->
                    snippetRepository.saveOrUpdate(snippet, currentTreeUri)
                    count++
                }
                onResult(true, count)
            } else {
                onResult(false, 0)
            }
        }
    }

    /** 恢复全局偏好配置为默认状态 */
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                AppSettings()
            }
        }
    }

    /**
     * 将数据库中的全量代码片段导出为选定 SAF 目录下的 JSON 备份文件。
     *
     * 教学解析：
     * 1. 异步调度: 使用 `viewModelScope.launch` 启动非阻塞协程。
     * 2. `allForExport()`: 从 Room 数据库中获取全量 Snippet 领域快照，避免主线程数据库 I/O 卡顿。
     * 3. 结果回调: 通过 `onResult` 高阶闭包将成功/失败标志传给 Compose UI 显示 Snackbar。
     */
    fun exportBackupJson(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val snippets = snippetRepository.allForExport()
            val success = Exporter.exportToJsonFile(context, snippets, uri)
            onResult(success)
        }
    }

    /**
     * 将数据库全量代码片段按文件夹归类打包并导出为 ZIP 压缩文件。
     */
    fun exportZipFile(context: Context, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val snippets = snippetRepository.allForExport()
            val zipFile = Exporter.createZipFile(context, snippets)
            onResult(zipFile)
        }
    }


    companion object {
        /** ViewModelFactory 工厂构造器 */
        fun factory(
            settingsRepository: SettingsRepository,
            snippetRepository: SnippetRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsRepository, snippetRepository) as T
            }
        }
    }
}

