package com.feige.snippetstudio.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.feige.snippetstudio.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 声明 Context 扩展属性：创建全局单例的 Jetpack Preferences DataStore 实例 (数据文件名为 "snippet_settings")。
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snippet_settings")

/**
 * [SettingsDataStore] 负责对应用全局偏好设置 [AppSettings] 进行响应式读取与持久化更新。
 *
 * 教学解析：
 * 1. 替代 SharedPreferences: SharedPreferences 的 get/put 会进行同步 I/O，可能引起 UI 卡顿；
 *    DataStore 内部完全使用 Kotlin 协程 Flow 异步读写文件，在基态背景线程调度（Dispatchers.IO）执行。
 * 2. 原子事务: 通过 `edit` 挂起闭包操作写入，确保多个字段更新时原子生效，避免中间不一致状态。
 */
class SettingsDataStore(private val context: Context) {

    /**
     * 定义 DataStore 内部使用的强类型属性键名 Keys。
     */
    private object Keys {
        val LANG = stringPreferencesKey("lang")
        val THEME = stringPreferencesKey("theme")
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val EDITOR_FONT = floatPreferencesKey("editor_font")
        val IS_WORD_WRAP = booleanPreferencesKey("is_word_wrap")
        val ENCODING = stringPreferencesKey("encoding")
        val LINE_ENDING = stringPreferencesKey("line_ending")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val HIGHLIGHT_CURRENT_LINE = booleanPreferencesKey("highlight_current_line")
        val TAB_SIZE = intPreferencesKey("tab_size")
        val AUTO_PAIR_BRACKETS = booleanPreferencesKey("auto_pair_brackets")
        val REPO_PATH = stringPreferencesKey("repo_path")
        val REPO_TREE_URI = stringPreferencesKey("repo_tree_uri")
        val GIT_URL = stringPreferencesKey("git_url")
        val GIT_BRANCH = stringPreferencesKey("git_branch")
        val GIT_PAT = stringPreferencesKey("git_pat")
        val GIT_CONNECTED = booleanPreferencesKey("git_connected")
        val LAST_SYNC_TIME = androidx.datastore.preferences.core.longPreferencesKey("last_sync_time")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val CARD_CLICK_ACTION = stringPreferencesKey("card_click_action")
        val USE_BOILERPLATE = booleanPreferencesKey("use_boilerplate")
    }

    /**
     * 【只读数据流】将 Preferences 字典流转换为类型安全的 [AppSettings] 领域数据流。
     * 自动拦截 null 并填充应用预设默认值 (例如默认语言 "zh"、默认格式 UTF-8)。
     */
    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            lang = prefs[Keys.LANG] ?: "zh",
            theme = prefs[Keys.THEME] ?: "system",
            colorTheme = prefs[Keys.COLOR_THEME] ?: "forest",
            editorFontSp = prefs[Keys.EDITOR_FONT] ?: 13.5f,
            isWordWrap = prefs[Keys.IS_WORD_WRAP] ?: true,
            encoding = prefs[Keys.ENCODING] ?: "UTF-8",
            lineEnding = prefs[Keys.LINE_ENDING] ?: "LF",
            showLineNumbers = prefs[Keys.SHOW_LINE_NUMBERS] ?: true,
            highlightCurrentLine = prefs[Keys.HIGHLIGHT_CURRENT_LINE] ?: true,
            tabSize = prefs[Keys.TAB_SIZE] ?: 4,
            autoPairBrackets = prefs[Keys.AUTO_PAIR_BRACKETS] ?: true,
            repoPath = prefs[Keys.REPO_PATH] ?: "Internal App Storage",
            repoTreeUri = prefs[Keys.REPO_TREE_URI] ?: "",
            gitUrl = prefs[Keys.GIT_URL] ?: "",
            gitBranch = prefs[Keys.GIT_BRANCH] ?: "main",
            gitPat = prefs[Keys.GIT_PAT] ?: "",
            gitConnected = prefs[Keys.GIT_CONNECTED] ?: false,
            lastSyncTime = prefs[Keys.LAST_SYNC_TIME] ?: 0L,
            autoSyncEnabled = prefs[Keys.AUTO_SYNC_ENABLED] ?: true,
            cardClickAction = prefs[Keys.CARD_CLICK_ACTION] ?: "detail",
            useBoilerplate = prefs[Keys.USE_BOILERPLATE] ?: true
        )
    }

    /**
     * 【原子事务更新】高阶扩展函数，提供安全改写设置接口。
     *
     * @param transform 闭包：传入当前最新 AppSettings 实例，返回变更目标对象后自动同步至持久化磁盘
     */
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                lang = prefs[Keys.LANG] ?: "zh",
                theme = prefs[Keys.THEME] ?: "system",
                colorTheme = prefs[Keys.COLOR_THEME] ?: "forest",
                editorFontSp = prefs[Keys.EDITOR_FONT] ?: 13.5f,
                isWordWrap = prefs[Keys.IS_WORD_WRAP] ?: true,
                encoding = prefs[Keys.ENCODING] ?: "UTF-8",
                lineEnding = prefs[Keys.LINE_ENDING] ?: "LF",
                showLineNumbers = prefs[Keys.SHOW_LINE_NUMBERS] ?: true,
                highlightCurrentLine = prefs[Keys.HIGHLIGHT_CURRENT_LINE] ?: true,
                tabSize = prefs[Keys.TAB_SIZE] ?: 4,
                autoPairBrackets = prefs[Keys.AUTO_PAIR_BRACKETS] ?: true,
                repoPath = prefs[Keys.REPO_PATH] ?: "Internal App Storage",
                repoTreeUri = prefs[Keys.REPO_TREE_URI] ?: "",
                gitUrl = prefs[Keys.GIT_URL] ?: "",
                gitBranch = prefs[Keys.GIT_BRANCH] ?: "main",
                gitPat = prefs[Keys.GIT_PAT] ?: "",
                gitConnected = prefs[Keys.GIT_CONNECTED] ?: false,
                lastSyncTime = prefs[Keys.LAST_SYNC_TIME] ?: 0L,
                autoSyncEnabled = prefs[Keys.AUTO_SYNC_ENABLED] ?: true,
                cardClickAction = prefs[Keys.CARD_CLICK_ACTION] ?: "detail",
                useBoilerplate = prefs[Keys.USE_BOILERPLATE] ?: true
            )
            // 调用高阶函数拿到修改后的更新对象
            val updated = transform(current)
            
            // 将最新值写回 DataStore Preferences 内存结构并异步持久化刷盘
            prefs[Keys.LANG] = updated.lang
            prefs[Keys.THEME] = updated.theme
            prefs[Keys.COLOR_THEME] = updated.colorTheme
            prefs[Keys.EDITOR_FONT] = updated.editorFontSp
            prefs[Keys.IS_WORD_WRAP] = updated.isWordWrap
            prefs[Keys.ENCODING] = updated.encoding
            prefs[Keys.LINE_ENDING] = updated.lineEnding
            prefs[Keys.SHOW_LINE_NUMBERS] = updated.showLineNumbers
            prefs[Keys.HIGHLIGHT_CURRENT_LINE] = updated.highlightCurrentLine
            prefs[Keys.TAB_SIZE] = updated.tabSize
            prefs[Keys.AUTO_PAIR_BRACKETS] = updated.autoPairBrackets
            prefs[Keys.REPO_PATH] = updated.repoPath
            prefs[Keys.REPO_TREE_URI] = updated.repoTreeUri
            prefs[Keys.GIT_URL] = updated.gitUrl
            prefs[Keys.GIT_BRANCH] = updated.gitBranch
            prefs[Keys.GIT_PAT] = updated.gitPat
            prefs[Keys.GIT_CONNECTED] = updated.gitConnected
            prefs[Keys.LAST_SYNC_TIME] = updated.lastSyncTime
            prefs[Keys.AUTO_SYNC_ENABLED] = updated.autoSyncEnabled
            prefs[Keys.CARD_CLICK_ACTION] = updated.cardClickAction
            prefs[Keys.USE_BOILERPLATE] = updated.useBoilerplate
        }
    }
}


