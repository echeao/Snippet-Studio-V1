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

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val snippetRepository: SnippetRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(theme = if (enabled) "dark" else "light")
            }
        }
    }

    fun exportBackupJson(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val snippets = snippetRepository.allForExport()
            val success = Exporter.exportToJsonFile(context, snippets, uri)
            onResult(success)
        }
    }

    fun exportZipFile(context: Context, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val snippets = snippetRepository.allForExport()
            val zipFile = Exporter.createZipFile(context, snippets)
            onResult(zipFile)
        }
    }

    companion object {
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
