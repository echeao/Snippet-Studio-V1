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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snippet_settings")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val LANG = stringPreferencesKey("lang")
        val THEME = stringPreferencesKey("theme")
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
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            lang = prefs[Keys.LANG] ?: "zh",
            theme = prefs[Keys.THEME] ?: "system",
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
            gitConnected = prefs[Keys.GIT_CONNECTED] ?: false
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                lang = prefs[Keys.LANG] ?: "zh",
                theme = prefs[Keys.THEME] ?: "system",
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
                gitConnected = prefs[Keys.GIT_CONNECTED] ?: false
            )
            val updated = transform(current)
            prefs[Keys.LANG] = updated.lang
            prefs[Keys.THEME] = updated.theme
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
        }
    }
}
