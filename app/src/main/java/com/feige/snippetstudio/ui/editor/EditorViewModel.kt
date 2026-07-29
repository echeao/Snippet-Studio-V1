package com.feige.snippetstudio.ui.editor

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.feige.snippetstudio.data.repo.SettingsRepository
import com.feige.snippetstudio.data.repo.SnippetRepository
import com.feige.snippetstudio.model.PromptVariable
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.util.PromptVariableParser
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * [SaveState] 代码保存状态枚举。
 */
enum class SaveState {
    /** 已保存 */
    SAVED, 
    /** 正在保存中 */
    SAVING, 
    /** 未保存 (有未提交修改) */
    UNSAVED
}

/**
 * [EditorUiState] 代码编辑器的完整 UI 响应式状态模型。
 *
 * @param id 当前编辑的片段 ID ("new" 表示新建)
 * @param snippet 当前加载的领域模型
 * @param title 标题
 * @param textFieldValue Compose 带有光标位置的光标状态结构 [TextFieldValue]
 * @param type 片段类型 (HTML, JS, Markdown, Prompt)
 * @param tags 片段拥有的标签列表
 * @param allAvailableTags 全局可用的候选标签
 * @param selectedTab 选中的 Tab 选项卡 (0: 代码编辑, 1: 实时预览)
 * @param saveState 当前保存状态
 * @param fontSp 字体字号大小 (sp)
 * @param isWordWrap 是否开启自动换行
 * @param encoding 编码格式 (如 UTF-8)
 * @param lineEnding 换行符 (LF / CRLF)
 * @param showLineNumbers 是否显示左侧行号
 * @param highlightCurrentLine 是否高亮光标所在行
 * @param tabSize Tab 缩进空格数
 * @param autoPairBrackets 是否开启括号自动成对补全
 * @param isFullscreen 是否处于全屏沉浸焦点模式
 * @param currentLineIndex 光标当前行号 (0-indexed)
 * @param currentColumnIndex 光标当前列号 (0-indexed)
 * @param lineCount 总行数
 * @param charCount 总字符数
 * @param isLoading 加载状态
 * @param promptVariables 检测到的 Prompt 变量列表
 * @param showVariablePanel 是否显示变量填充面板
 * @param variableValues 变量填充值映射
 */
data class EditorUiState(
    val id: String = "",
    val snippet: Snippet? = null,
    val title: String = "",
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val type: SnippetType = SnippetType.HTML,
    val tags: List<String> = emptyList(),
    val allAvailableTags: List<String> = emptyList(),
    val selectedTab: Int = 0, // 0 = Code, 1 = Preview
    val saveState: SaveState = SaveState.SAVED,
    val fontSp: Float = 13.5f,
    val isWordWrap: Boolean = true,
    val encoding: String = "UTF-8",
    val lineEnding: String = "LF",
    val showLineNumbers: Boolean = true,
    val highlightCurrentLine: Boolean = true,
    val tabSize: Int = 4,
    val autoPairBrackets: Boolean = true,
    val isFullscreen: Boolean = false,
    val currentLineIndex: Int = 0,
    val currentColumnIndex: Int = 0,
    val lineCount: Int = 1,
    val charCount: Int = 0,
    val isLoading: Boolean = true,
    val promptVariables: List<PromptVariable> = emptyList(),
    val showVariablePanel: Boolean = false,
    val variableValues: Map<String, String> = emptyMap(),
    /** 工作区未配置警告：文件仅存储在应用私有目录，手机文件管理器不可见 */
    val noWorkspaceConfigured: Boolean = false,
    /** 编辑器字体族 */
    val editorFontFamily: String = "monospace"
)

/**
 * [EditorViewModel] 代码编辑器的 ViewModel 控制器。
 *
 * 核心技术点：
 * 1. **Debounce 防抖自动保存**：使用 Coroutines Flow 管道，当用户连续打字时挂起保存，停止输入 800ms 后异步触发 [performSave]，避免高频写 SQLite / 文件。
 * 2. **精准光标与行列计算**：根据 [TextFieldValue] 的 selection 计算光标所在行号与列号。
 * 3. **全屏沉浸模式切换**：提供焦点模式、快捷符号插入、Tab 切换与实时偏好调整。
 */
@OptIn(FlowPreview::class)
class EditorViewModel(
    private val snippetId: String,
    private val initialTypeStr: String?,
    private val snippetRepository: SnippetRepository,
    private val settingsRepository: SettingsRepository,
    private val sharedText: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState(isLoading = true))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // 触发防抖自动保存的管道
    private val _autoSaveTrigger = MutableSharedFlow<Unit>(replay = 1)
    // 触发 Prompt 变量解析的防抖管道
    private val _varParseTrigger = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    /** 标记当前是否为新建片段（用于返回时判断是否需要清理空文件） */
    private var isNewSnippet = false
    /** 新建时的初始标题（用于判断用户是否修改过标题） */
    private var initialTitle: String = ""
    /** 新建时的初始内容（用于判断用户是否修改过内容） */
    private var initialContent: String = ""

    init {
        // ===== 1. 订阅偏好设置流变更 =====
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        fontSp = settings.editorFontSp,
                        isWordWrap = settings.isWordWrap,
                        encoding = settings.encoding,
                        lineEnding = settings.lineEnding,
                        showLineNumbers = settings.showLineNumbers,
                        highlightCurrentLine = settings.highlightCurrentLine,
                        tabSize = settings.tabSize,
                        autoPairBrackets = settings.autoPairBrackets,
                        editorFontFamily = settings.editorFontFamily
                    )
                }
            }
        }

        // ===== 2. 收集全局候选标签库 =====
        viewModelScope.launch {
            combine(
                settingsRepository.settingsFlow,
                snippetRepository.observeActive()
            ) { settings, activeSnippets ->
                (settings.customTags + activeSnippets.flatMap { it.tags }).distinct()
            }.collect { tags ->
                _uiState.update { it.copy(allAvailableTags = tags) }
            }
        }

        // ===== 3. 加载已有片段或初始化新建片段 =====
        viewModelScope.launch {
            val currentSettings = settingsRepository.settingsFlow.first()

            // 检测工作区是否已配置：若 repoTreeUri 为空，文件将存储在应用私有目录，手机文件管理器不可见
            if (currentSettings.repoTreeUri.isBlank()) {
                _uiState.update { it.copy(noWorkspaceConfigured = true) }
            }

            if (snippetId == "new") {
                val type = SnippetType.fromCode(initialTypeStr ?: "html")
                val snippet = snippetRepository.create(type, repoTreeUriStr = currentSettings.repoTreeUri, useBoilerplate = currentSettings.useBoilerplate)
                isNewSnippet = true
                initialTitle = snippet.title
                initialContent = snippet.content
                // 如果是从系统分享接收的文本，直接覆盖内容
                if (!sharedText.isNullOrBlank()) {
                    val sharedSnippet = snippet.copy(content = sharedText)
                    snippetRepository.saveOrUpdate(sharedSnippet, currentSettings.repoTreeUri)
                    initialContent = sharedText
                    initFromSnippet(sharedSnippet)
                } else {
                    initFromSnippet(snippet)
                }
            } else {
                val snippet = snippetRepository.getById(snippetId)
                if (snippet != null) {
                    initFromSnippet(snippet)
                } else {
                    val fallback = snippetRepository.create(SnippetType.HTML, repoTreeUriStr = currentSettings.repoTreeUri, useBoilerplate = currentSettings.useBoilerplate)
                    initFromSnippet(fallback)
                }
            }
        }

        // ===== 4. 800ms debounce 防抖自动保存管线 =====
        // 教学解析：在实时代码编辑器中，如果用户每打一个字就调用一次磁盘写入/SQLite 事务，
        // 会极大地消耗 IO 性能并可能造成 UI 卡顿。
        // 通过 MutableSharedFlow 结合 `debounce(800)` 算子，只有在用户停止输入持续满 800 毫秒后，才会真正触发 `performSave()` 落盘！
        viewModelScope.launch {
            _autoSaveTrigger
                .debounce(800)
                .collect {
                    performSave()
                }
        }

        // ===== 5. Prompt 变量 500ms 防抖解析管线 =====
        viewModelScope.launch {
            _varParseTrigger
                .debounce(500)
                .collect { text ->
                    val state = _uiState.value
                    if (state.type == SnippetType.PROMPT) {
                        val variables = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                            PromptVariableParser.parse(text)
                        }
                        _uiState.update { it.copy(promptVariables = variables) }
                    } else {
                        if (_uiState.value.promptVariables.isNotEmpty()) {
                            _uiState.update { it.copy(promptVariables = emptyList()) }
                        }
                    }
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
                tags = snippet.tags,
                lineCount = lines,
                charCount = snippet.content.length,
                saveState = SaveState.SAVED,
                isLoading = false
            )
        }
    }

    /** 修改片段标题并触发防抖保存 */
    fun onTitleChange(newTitle: String) {
        _uiState.update {
            it.copy(
                title = newTitle,
                saveState = SaveState.UNSAVED
            )
        }
        triggerAutoSave()
    }

    /**
     * 文本编辑区输入内容变动：同步更新 TextFieldValue 保证输入流畅，并将耗时的行列号与字符数统计移至后台协程执行。
     *
     * 算位逻辑解析：
     * 1. 主线程同步更新选区 TextFieldValue 与 UNSAVED 状态，消除输入法软键盘与打字时延。
     * 2. 在 [Dispatchers.Default] 线程池遍历 `text` 字符数组，统计换行符 `\n` 并计算当前行、列与总行数。
     * 3. 计算完毕切回主线程更新 `_uiState` 中的行列位置状态。
     * 4. 触发异步防抖 Prompt 变量解析与防抖落盘保存。
     *
     * @param tfv 编辑框新的 TextFieldValue
     */
    fun onTextFieldValueChange(tfv: TextFieldValue) {
        // 步骤 1：主线程同步更新 TextFieldValue 与保存状态，保证输入框光标无延迟
        _uiState.update {
            it.copy(
                textFieldValue = tfv,
                saveState = SaveState.UNSAVED
            )
        }

        // 步骤 2：后台协程异步计算行列号与字符统计
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val text = tfv.text
            val caret = tfv.selection.start.coerceIn(0, text.length)

            var currentLine = 0
            var lastNewlinePos = -1
            var totalNewlines = 0
            for (i in text.indices) {
                if (text[i] == '\n') {
                    totalNewlines++
                    if (i < caret) {
                        currentLine++
                        lastNewlinePos = i
                    }
                }
            }
            val currentCol = if (lastNewlinePos == -1) caret else caret - lastNewlinePos - 1
            val lines = totalNewlines + 1

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        lineCount = lines,
                        charCount = text.length,
                        currentLineIndex = currentLine,
                        currentColumnIndex = currentCol
                    )
                }
            }
        }

        triggerPromptVariableParse(tfv.text)
        triggerAutoSave()
    }

    /**
     * Sora-Editor 文本变更回调（适配层）。
     * Sora-Editor 返回纯 String 而非 TextFieldValue，此方法适配到现有接口。
     * 注意：保留当前 TextFieldValue 的 selection 不覆盖，光标位置由 onSoraCursorChange 独立维护。
     * @param newText Sora-Editor 发布的最新文本内容
     */
    fun onSoraTextChange(newText: String) {
        val currentTfv = _uiState.value.textFieldValue
        if (currentTfv.text != newText) {
            _uiState.update {
                it.copy(
                    textFieldValue = currentTfv.copy(text = newText),
                    saveState = SaveState.UNSAVED,
                    charCount = newText.length
                )
            }
            triggerPromptVariableParse(newText)
            triggerAutoSave()
        }
    }

    /**
     * Sora-Editor 光标位置变更回调（高效版）。
     * 直接提供行列号，同步更新状态栏显示与 TextFieldValue.selection（供符号插入使用）。
     * @param line 光标所在行号 (0-indexed)
     * @param column 光标所在列号 (0-indexed)
     */
    fun onSoraCursorChange(line: Int, column: Int) {
        val currentText = _uiState.value.textFieldValue.text
        // 将行列号转换为绝对偏移量，保持 TextFieldValue.selection 与实际光标同步
        var offset = 0
        var curLine = 0
        for (i in currentText.indices) {
            if (curLine == line) {
                offset = (i + column).coerceAtMost(currentText.length)
                break
            }
            if (currentText[i] == '\n') curLine++
        }
        if (curLine < line) offset = currentText.length

        _uiState.update {
            it.copy(
                currentLineIndex = line,
                currentColumnIndex = column,
                lineCount = currentText.count { c -> c == '\n' } + 1,
                textFieldValue = it.textFieldValue.copy(
                    selection = androidx.compose.ui.text.TextRange(offset.coerceIn(0, currentText.length))
                )
            )
        }
    }


    /** 从快捷符号栏插入特定代码符号到当前光标处 */
    fun insertSymbol(symbol: String) {
        val currentTfv = _uiState.value.textFieldValue
        val text = currentTfv.text
        // 使用 TextFieldValue.selection（已由 onSoraCursorChange 同步为实际光标位置）
        val start = currentTfv.selection.start.coerceIn(0, text.length)
        val end = currentTfv.selection.end.coerceIn(0, text.length)

        val newText = text.replaceRange(start, end, symbol)
        val newSelection = start + symbol.length

        _uiState.update {
            it.copy(
                textFieldValue = TextFieldValue(newText, androidx.compose.ui.text.TextRange(newSelection)),
                saveState = SaveState.UNSAVED,
                charCount = newText.length
            )
        }
        triggerPromptVariableParse(newText)
        triggerAutoSave()
    }

    /** 切换代码编辑 / 实时预览选项卡 */
    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    /** 设置全屏焦点沉浸模式 */
    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreen = fullscreen) }
    }

    /** 切换全屏沉浸模式开/关 */
    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    /** 更新偏好设置：自动换行 */
    fun setWordWrap(enabled: Boolean) {
        if (_uiState.value.isWordWrap == enabled) return
        _uiState.update { it.copy(isWordWrap = enabled) }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateSettings { it.copy(isWordWrap = enabled) }
        }
    }

    /** 更新偏好设置：编码格式 */
    fun setEncoding(encoding: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(encoding = encoding) }
        }
    }

    /** 更新偏好设置：换行符 */
    fun setLineEnding(lineEnding: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(lineEnding = lineEnding) }
        }
    }

    /** 更新偏好设置：显示行号 */
    fun setShowLineNumbers(show: Boolean) {
        if (_uiState.value.showLineNumbers == show) return
        _uiState.update { it.copy(showLineNumbers = show) }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateSettings { it.copy(showLineNumbers = show) }
        }
    }

    /** 更新偏好设置：高亮当前行 */
    fun setHighlightCurrentLine(highlight: Boolean) {
        if (_uiState.value.highlightCurrentLine == highlight) return
        _uiState.update { it.copy(highlightCurrentLine = highlight) }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateSettings { it.copy(highlightCurrentLine = highlight) }
        }
    }

    /** 更新偏好设置：Tab 缩进长度 */
    fun setTabSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(tabSize = size) }
        }
    }

    /** 更新偏好设置：自动成对补全括号 */
    fun setAutoPairBrackets(autoPair: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(autoPairBrackets = autoPair) }
        }
    }

    /** 修改当前代码片段语言类型 (如从 HTML 更改为 JS) */
    fun setSnippetType(type: SnippetType) {
        _uiState.update {
            it.copy(type = type, saveState = SaveState.UNSAVED)
        }
        triggerAutoSave()
    }

    /** 更新片段标签列表 */
    fun updateTags(tags: List<String>) {
        _uiState.update {
            it.copy(tags = tags, saveState = SaveState.UNSAVED)
        }
        triggerAutoSave()
    }

    /** 设置编辑器字体族 */
    fun setEditorFontFamily(fontFamily: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(editorFontFamily = fontFamily) }
        }
    }

    /** 快捷缩放编辑器字体大小 */
    fun adjustFontSize(deltaSp: Float) {
        val current = _uiState.value.fontSp
        val next = (current + deltaSp).coerceIn(11f, 22f)
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(editorFontSp = next) }
        }
    }

    // ===== Prompt 变量填空功能 =====

    /** 触发 Prompt 变量的异步防抖解析管道 */
    private fun triggerPromptVariableParse(text: String) {
        viewModelScope.launch {
            _varParseTrigger.emit(text)
        }
    }

    /** 切换变量填充面板显隐 */
    fun toggleVariablePanel() {
        _uiState.update { it.copy(showVariablePanel = !it.showVariablePanel) }
    }

    /** 更新单个变量的填充值 */
    fun onVariableValueChange(name: String, value: String) {
        _uiState.update {
            it.copy(variableValues = it.variableValues + (name to value))
        }
    }

    /** 应用变量填充：将所有 {{name}} 替换为对应值，生成新内容 */
    fun applyVariableFill() {
        val state = _uiState.value
        val filledText = PromptVariableParser.fill(state.textFieldValue.text, state.variableValues)
        val newTfv = TextFieldValue(filledText)
        onTextFieldValueChange(newTfv)
        _uiState.update { it.copy(showVariablePanel = false) }
    }

    /** 获取变量填充后的预览文本（不修改原文） */
    fun getFilledPreview(): String {
        val state = _uiState.value
        return PromptVariableParser.fill(state.textFieldValue.text, state.variableValues)
    }

    /** 手动强制立即执行保存 (忽略防抖) */
    fun forceSaveNow() {
        viewModelScope.launch {
            performSave()
        }
    }

    /**
     * 处理返回导航：若为新建片段且用户未修改标题或内容，则删除该空片段（视为误触/撤销）。
     *
     * @param onBack 实际执行返回的回调
     */
    fun handleBackWithCleanup(onBack: () -> Unit) {
        val state = _uiState.value
        if (isNewSnippet && state.title == initialTitle && state.textFieldValue.text == initialContent) {
            // 用户未做任何有意义修改，删除这个空片段
            viewModelScope.launch {
                val currentSettings = settingsRepository.settingsFlow.first()
                snippetRepository.purge(state.id, currentSettings.repoTreeUri)
                onBack()
            }
        } else {
            onBack()
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
        val currentSettings = settingsRepository.settingsFlow.first()

        // 检测标题是否变更：若变更则同步更新 fileName，避免标题与物理文件名脱节
        val titleChanged = state.title != snippet.title
        val newFileName = if (titleChanged) {
            if (state.title.isBlank()) "snippet${state.type.extension}"
            else "${state.title.take(20).replace("\\s+".toRegex(), "_")}${state.type.extension}"
        } else {
            snippet.fileName
        }

        if (titleChanged && newFileName != snippet.fileName) {
            // 标题变更且文件名随之变化：走 updateRename 路径（自动清理旧文件残留）
            snippetRepository.updateRename(snippet.id, state.title, newFileName, currentSettings.repoTreeUri)
            val updated = snippet.copy(
                title = state.title,
                fileName = newFileName,
                content = state.textFieldValue.text,
                type = state.type,
                tags = state.tags
            )
            // 内容也需要同步落盘
            snippetRepository.saveOrUpdate(updated, currentSettings.repoTreeUri)
            _uiState.update {
                it.copy(
                    snippet = updated,
                    saveState = SaveState.SAVED
                )
            }
        } else {
            val updated = snippet.copy(
                title = state.title,
                content = state.textFieldValue.text,
                type = state.type,
                tags = state.tags
            )
            snippetRepository.saveOrUpdate(updated, currentSettings.repoTreeUri)
            _uiState.update {
                it.copy(
                    snippet = updated,
                    saveState = SaveState.SAVED
                )
            }
        }
    }

    companion object {
        /** 创建带参数依赖的 ViewModelProvider.Factory 工厂对象 */
        fun factory(
            snippetId: String,
            initialTypeStr: String?,
            snippetRepository: SnippetRepository,
            settingsRepository: SettingsRepository,
            sharedText: String? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditorViewModel(snippetId, initialTypeStr, snippetRepository, settingsRepository, sharedText) as T
            }
        }
    }
}

