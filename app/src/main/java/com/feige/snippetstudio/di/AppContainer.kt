package com.feige.snippetstudio.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.feige.snippetstudio.data.local.AppDatabase
import com.feige.snippetstudio.data.local.SettingsDataStore
import com.feige.snippetstudio.data.repo.SettingsRepository
import com.feige.snippetstudio.data.repo.SnippetRepository

import com.feige.snippetstudio.data.git.GitManager

class AppContainer(private val context: Context) {
    val database: AppDatabase by lazy {
        AppDatabase.create(context)
    }

    val gitManager: GitManager by lazy {
        GitManager(context)
    }

    val snippetRepository: SnippetRepository by lazy {
        SnippetRepository(database.snippetDao(), context, gitManager)
    }

    val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(context)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(settingsDataStore)
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
