package com.feige.snippetstudio.ui.editor

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SettingsRepository
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SaveState {
    SAVED, SAVING, UNSAVED
}

data class EditorUiState(
    val id: String = "",
    val snippet: Snippet? = null,
    val title: String = "",
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val type: SnippetType = SnippetType.HTML,
    val selectedTab: Int = 0, // 0 = Code, 1 = Preview
    val saveState: SaveState = SaveState.SAVED,
    val fontSp: Float = 13.5f,
    val currentLineIndex: Int = 0,
    val currentColumnIndex: Int = 0,
    val lineCount: Int = 1,
    val charCount: Int = 0,
    val isLoading: Boolean = true
)

@OptIn(FlowPreview::class)
class EditorViewModel(
    private val snippetId: String,
    private val initialTypeStr: String?,
    private val snippetRepository: SnippetRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState(isLoading = true))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _autoSaveTrigger = MutableSharedFlow<Unit>(replay = 1)

    init {
        // Observe settings font size
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(fontSp = settings.editorFontSp) }
            }
        }

        // Load or create snippet
        viewModelScope.launch {
            if (snippetId == "new") {
                val type = SnippetType.fromCode(initialTypeStr ?: "html")
                val snippet = snippetRepository.create(type)
                initFromSnippet(snippet)
            } else {
                val snippet = snippetRepository.getById(snippetId)
                if (snippet != null) {
                    initFromSnippet(snippet)
                } else {
                    val fallback = snippetRepository.create(SnippetType.HTML)
                    initFromSnippet(fallback)
                }
            }
        }

        // Auto-save debounce pipeline (800ms)
        viewModelScope.launch {
            _autoSaveTrigger
                .debounce(800)
                .collect {
                    performSave()
                }
        }
    }

    private fun initFromSnippet(snippet: Snippet) {
        val tfv = TextFieldValue(text = snippet.content)
        val lines = snippet.content.count { it == '\n' } + 1
        _uiState.update {
            it.copy(
                id = snippet.id,
                snippet = snippet,
                title = snippet.title,
                textFieldValue = tfv,
                type = snippet.type,
                lineCount = lines,
                charCount = snippet.content.length,
                saveState = SaveState.SAVED,
                isLoading = false
            )
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update {
            it.copy(
                title = newTitle,
                saveState = SaveState.UNSAVED
            )
        }
        triggerAutoSave()
    }

    fun onTextFieldValueChange(tfv: TextFieldValue) {
        val text = tfv.text
        val caret = tfv.selection.start.coerceIn(0, text.length)

        // Calculate current line and column index
        val textBeforeCaret = text.take(caret)
        val currentLine = textBeforeCaret.count { it == '\n' }
        val lastNewlinePos = textBeforeCaret.lastIndexOf('\n')
        val currentCol = if (lastNewlinePos == -1) caret else caret - lastNewlinePos - 1
        val lines = text.count { it == '\n' } + 1

        _uiState.update {
            it.copy(
                textFieldValue = tfv,
                lineCount = lines,
                charCount = text.length,
                currentLineIndex = currentLine,
                currentColumnIndex = currentCol,
                saveState = SaveState.UNSAVED
            )
        }
        triggerAutoSave()
    }

    fun insertSymbol(symbol: String) {
        val currentTfv = _uiState.value.textFieldValue
        val text = currentTfv.text
        val start = currentTfv.selection.start.coerceIn(0, text.length)
        val end = currentTfv.selection.end.coerceIn(0, text.length)

        val newText = text.replaceRange(start, end, symbol)
        val newSelection = start + symbol.length

        onTextFieldValueChange(TextFieldValue(newText, androidx.compose.ui.text.TextRange(newSelection)))
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun adjustFontSize(deltaSp: Float) {
        val current = _uiState.value.fontSp
        val next = (current + deltaSp).coerceIn(11f, 22f)
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(editorFontSp = next) }
        }
    }

    private fun triggerAutoSave() {
        _uiState.update { it.copy(saveState = SaveState.SAVING) }
        viewModelScope.launch {
            _autoSaveTrigger.emit(Unit)
        }
    }

    private suspend fun performSave() {
        val state = _uiState.value
        val snippet = state.snippet ?: return
        val updated = snippet.copy(
            title = state.title,
            content = state.textFieldValue.text,
            type = state.type
        )
        snippetRepository.saveOrUpdate(updated)
        _uiState.update {
            it.copy(
                snippet = updated,
                saveState = SaveState.SAVED
            )
        }
    }

    companion object {
        fun factory(
            snippetId: String,
            initialTypeStr: String?,
            snippetRepository: SnippetRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditorViewModel(snippetId, initialTypeStr, snippetRepository, settingsRepository) as T
            }
        }
    }
}
