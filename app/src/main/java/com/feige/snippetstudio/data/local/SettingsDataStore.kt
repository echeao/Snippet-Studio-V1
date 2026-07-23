package com.feige.snippetstudio.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
        val REPO_PATH = stringPreferencesKey("repo_path")
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
            repoPath = prefs[Keys.REPO_PATH] ?: "Internal App Storage",
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
                repoPath = prefs[Keys.REPO_PATH] ?: "Internal App Storage",
                gitUrl = prefs[Keys.GIT_URL] ?: "",
                gitBranch = prefs[Keys.GIT_BRANCH] ?: "main",
                gitPat = prefs[Keys.GIT_PAT] ?: "",
                gitConnected = prefs[Keys.GIT_CONNECTED] ?: false
            )
            val updated = transform(current)
            prefs[Keys.LANG] = updated.lang
            prefs[Keys.THEME] = updated.theme
            prefs[Keys.EDITOR_FONT] = updated.editorFontSp
            prefs[Keys.REPO_PATH] = updated.repoPath
            prefs[Keys.GIT_URL] = updated.gitUrl
            prefs[Keys.GIT_BRANCH] = updated.gitBranch
            prefs[Keys.GIT_PAT] = updated.gitPat
            prefs[Keys.GIT_CONNECTED] = updated.gitConnected
        }
    }
}
