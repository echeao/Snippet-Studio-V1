package com.feige.snippetstudio.data.repo

import com.feige.snippetstudio.data.local.SettingsDataStore
import com.feige.snippetstudio.model.AppSettings
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val settingsDataStore: SettingsDataStore) {
    val settingsFlow: Flow<AppSettings> = settingsDataStore.settingsFlow

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsDataStore.update(transform)
    }
}
