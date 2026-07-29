package com.feige.snippetstudio.ui.components

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.feige.snippetstudio.ui.theme.ThemeColors
import com.feige.snippetstudio.util.SyntaxLanguage
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.dsl.languages
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IThemeSource

/**
 * [SoraCodeEditor] 基于 Sora-Editor 原生 Android View 的 Compose 包装组件。
 *
 * 核心优势：
 * - 原生自定义 View 渲染（非 WebView 或 BasicTextField），帧率稳定 60fps+
 * - TextMate 语法高亮引擎（与 VS Code 同款 Tree-sitter 底层）
 * - 内置行号、当前行高亮、自动缩进、括号匹配等专业编辑器功能
 * - 通过双向状态桥接与 Compose 状态系统无缝集成
 *
 * @param text 编辑器内容文本（外部状态）
 * @param onTextChange 文本变更回调，由 Sora 内部 ContentListener 触发
 * @param onCursorChange 光标位置变化回调，传入 (行号0-based, 列号0-based)
 * @param language 当前片段语言，用于选择 TextMate 语法高亮
 * @param isDark 是否为深色主题模式
 * @param themeColors 当前应用主题颜色，用于映射 Sora 编辑器颜色
 * @param fontSp 编辑器字体大小（sp 单位）
 * @param showLineNumbers 是否显示行号列
 * @param isWordWrap 是否启用自动换行
 * @param modifier Compose 布局修饰符
 */
@Composable
fun SoraCodeEditor(
    text: String,
    onTextChange: (String) -> Unit,
    onCursorChange: (line: Int, column: Int) -> Unit = { _, _ -> },
    language: SyntaxLanguage,
    isDark: Boolean,
    themeColors: ThemeColors,
    fontSp: Float,
    showLineNumbers: Boolean = true,
    isWordWrap: Boolean = true,
    selectionOffset: Int = -1,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 保持最新回调引用，避免 lambda 捕获过期值
    val latestOnTextChange by rememberUpdatedState(onTextChange)
    val latestOnCursorChange by rememberUpdatedState(onCursorChange)
    // 始终保持最新的外部文本引用，供事件回调与 LaunchedEffect 内部使用
    val latestText by rememberUpdatedState(text)
    // 始终保持最新的光标偏移引用，供文本同步后恢复光标位置
    val latestSelectionOffset by rememberUpdatedState(selectionOffset)

   // ===== 核心状态门控 =====
    // isExternalUpdate：标记当前内容变更是否由外部（Compose 侧）触发，防止循环回调
    var isExternalUpdate by remember { mutableStateOf(false) }
    // isEditorReady：仅当语言和初始文本都已写入编辑器后才开放用户输入事件。
    // 设置 Sora 语言时会替换内部 Content；在此之前开放事件会把这次内部替换误写回 ViewModel。
    var isEditorReady by remember { mutableStateOf(false) }

    // TextMate 注册表初始化标记（仅在 Context 变化时执行一次）
    val tmInitialized = remember(context) { mutableStateOf(false) }
    // 持有当前 TextMateColorScheme 引用，供主题切换 effect 复用（避免重复创建）
    var currentTmColorScheme by remember { mutableStateOf<TextMateColorScheme?>(null) }
    // 首次初始化完成标记：主题切换 effect 以此为门控，避免与初始化 effect 竞争或循环触发
    var tmFirstInitDone by remember { mutableStateOf(false) }
    // 主题标识缓存：用于跳过主题切换 effect 的冗余触发
    var lastThemeKey by remember { mutableStateOf("") }

    // 创建并记忆 Sora CodeEditor 实例
    val editor = remember {
        CodeEditor(context).also { ed ->
            // 禁用自动补全弹窗（代码片段场景无需补全）
            ed.getComponent(EditorAutoCompletion::class.java).isEnabled = false
            // 首帧同步填充，避免等待 TextMate 注册期间以空白编辑器渲染一帧。
            // Sora 的 setEditorLanguage() 会复用既有 Content 并据此启动语法分析。
            ed.setText(text)
        }
    }

    // ===== TextMate 主题与语法注册（异步初始化，仅执行一次）=====
    LaunchedEffect(context) {
        if (!tmInitialized.value) {
            withContext(Dispatchers.IO) {
                initTextMateRegistry(context)
            }
            tmInitialized.value = true
        }
    }

    // ===== 首次初始化：注册表 → 主题 → 配色 → 语言 → 文本（严格顺序，消除竞态）=====
    // 合并原先独立的"主题配色"与"语言初始化"两个 effect 为单一顺序协程，
    // 确保 TextMateColorScheme 在 setEditorLanguage() 之前已绑定到编辑器。
    LaunchedEffect(language, tmInitialized.value) {
        if (!tmInitialized.value) return@LaunchedEffect
    
        // 第一步：关闭事件传播门控，阻止初始化期间任何事件泄漏到 ViewModel
        isEditorReady = false
    
        // 第二步：设置 TextMate 主题并创建配色方案（IO 线程）
        val textMateColorScheme = withContext(Dispatchers.IO) {
            val themeName = if (isDark) "dark_default" else "light_default"
            try {
                ThemeRegistry.getInstance().setTheme(themeName)
            } catch (e: Exception) {
                android.util.Log.w("SoraCodeEditor", "TextMate 主题设置失败: ${e.message}")
            }
            try {
                TextMateColorScheme.create(ThemeRegistry.getInstance())
            } catch (e: Exception) {
                android.util.Log.w("SoraCodeEditor", "TextMate 配色初始化失败: ${e.message}")
                null
            }
        }
    
        // 第三步：在主线程绑定配色方案到编辑器（必须先于语言设置）
        if (textMateColorScheme != null) {
            editor.colorScheme = buildColorScheme(textMateColorScheme, themeColors, isDark)
            currentTmColorScheme = textMateColorScheme
            // 诊断：检查 TextMate token 颜色槽位是否非零（非透明）
            val testColor = textMateColorScheme.getColor(256)
            android.util.Log.d("SoraCodeEditor", "[TM-Init] colorScheme 已绑定, getColor(256)=0x${Integer.toHexString(testColor)}")
        } else {
            android.util.Log.w("SoraCodeEditor", "[TM-Init] TextMateColorScheme 为 null，编辑器将使用默认配色（无 TextMate 高亮）")
        }
    
        // 第四步：在 IO 线程构建 TextMate 语言实例
        val lang: Language = withContext(Dispatchers.IO) {
            buildSoraLanguage(language) ?: EmptyLanguage()
        }
        android.util.Log.d("SoraCodeEditor", "语言初始化: $language -> ${lang.javaClass.simpleName}, colorScheme=${textMateColorScheme != null}")
    
        // 第五步：Sora 的 setEditorLanguage() 保留已有 Content，并对该 Content 调用
        // AnalyzeManager.reset()。此时配色已就绪，token 颜色可正确解析。
        isExternalUpdate = true
        editor.setEditorLanguage(lang)
        // 仅当语言创建期间外部文本确实发生变化时才补一次同步，避免不必要的闪屏。
        if (editor.text.toString() != latestText) {
            editor.setText(latestText)
        }
        isExternalUpdate = false
    
        // 第六步：只有内容与外部状态一致后才接收用户编辑事件。
        isEditorReady = true
        // 记录当前主题标识，使主题切换 effect 跳过初始化后的冗余触发
        lastThemeKey = "${isDark}_${themeColors.hashCode()}"
        // 标记首次初始化完成，解锁主题切换 effect
        tmFirstInitDone = true
    }
    
    // ===== 主题切换：仅在首次初始化完成后响应 isDark / themeColors 真实变化 =====
    // 以 tmFirstInitDone 为门控；通过 lastThemeKey 跳过初始化后的冗余触发，
    // 避免在语言分析器异步启动期间替换 colorScheme 导致渲染异常。
    LaunchedEffect(isDark, themeColors, tmFirstInitDone) {
        if (!tmFirstInitDone) return@LaunchedEffect
        // 构造当前主题标识；若与上次应用的一致则跳过（消除初始化后的冗余触发）
        val themeKey = "${isDark}_${themeColors.hashCode()}"
        if (themeKey == lastThemeKey) return@LaunchedEffect
        lastThemeKey = themeKey

        android.util.Log.d("SoraCodeEditor", "主题切换: isDark=$isDark")
        withContext(Dispatchers.IO) {
            val themeName = if (isDark) "dark_default" else "light_default"
            try {
                ThemeRegistry.getInstance().setTheme(themeName)
            } catch (e: Exception) {
                android.util.Log.w("SoraCodeEditor", "主题切换失败: ${e.message}")
            }
        }
        // ThemeRegistry 内部状态已更新，重新创建 scheme 以读取新主题的 token 颜色
        val newScheme = withContext(Dispatchers.IO) {
            try {
                TextMateColorScheme.create(ThemeRegistry.getInstance())
            } catch (e: Exception) {
                android.util.Log.w("SoraCodeEditor", "主题切换配色创建失败: ${e.message}")
                null
            }
        }
        if (newScheme != null) {
            editor.colorScheme = buildColorScheme(newScheme, themeColors, isDark)
            currentTmColorScheme = newScheme
        }
    }

    // ===== 字体大小响应 =====
    LaunchedEffect(fontSp) {
        editor.setTextSize(fontSp)
    }

    // ===== 编辑选项响应（行号、自动换行）=====
    LaunchedEffect(showLineNumbers, isWordWrap) {
        editor.isLineNumberEnabled = showLineNumbers
        editor.setWordwrap(isWordWrap)
    }

    // ===== 外部 text 变化同步到编辑器（门控开放后或门控状态变化时触发同步）=====
    LaunchedEffect(text, isEditorReady) {
        if (!isEditorReady) return@LaunchedEffect
        val currentEditorText = editor.text.toString()
        if (currentEditorText != text) {
            isExternalUpdate = true
            editor.setText(text)
            // 文本同步后恢复光标位置（符号插入等场景需要精确定位）
            if (latestSelectionOffset >= 0) {
                try {
                    val pos = editor.text.indexer.getCharPosition(
                        latestSelectionOffset.coerceIn(0, text.length)
                    )
                    editor.setSelection(pos.line, pos.column)
                } catch (_: Exception) { /* 光标定位失败时静默忽略 */ }
            }
            isExternalUpdate = false
        }
    }

    // ===== 注册编辑器内容与光标监听器（使用 Sora EventBus 订阅机制）=====
    DisposableEffect(editor) {
        // 订阅文本内容变动事件。外部同步产生的事件要么处于 isExternalUpdate
        // 窗口内，要么文本已等于外部状态；两种情况都不能再回写 ViewModel。
        val contentSub = editor.subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) { _, _ ->
            if (isEditorReady && !isExternalUpdate) {
                val newText = editor.text.toString()
                if (newText != latestText) {
                    latestOnTextChange(newText)
                }
            }
        }

        // 订阅光标位置变动事件 SelectionChangeEvent
        val cursorSub = editor.subscribeEvent(io.github.rosemoe.sora.event.SelectionChangeEvent::class.java) { event, _ ->
            val cursor = event.editor.cursor
            latestOnCursorChange(cursor.leftLine, cursor.leftColumn)
        }

        onDispose {
            contentSub.unsubscribe()
            cursorSub.unsubscribe()
            editor.release()
        }
    }

    // ===== 将 Sora CodeEditor View 嵌入 Compose 布局 =====
    AndroidView(
        factory = { editor },
        modifier = modifier,
        update = { ed ->
            // update 回调处理 View 属性
            ed.setTextSize(fontSp)
            ed.isLineNumberEnabled = showLineNumbers
            ed.setWordwrap(isWordWrap)
            ed.isHorizontalScrollBarEnabled = !isWordWrap
            ed.isVerticalScrollBarEnabled = true
        }
    )
}

/**
 * 初始化 TextMate 语法注册表。
 * 注册 assets/textmate/ 目录下的语法文件和主题文件。
 * 此操作是线程安全的，应在 IO Dispatcher 中调用。
 *
 * @param context Android 上下文，用于访问 assets
 */
private fun initTextMateRegistry(context: Context) {
    val assets: AssetManager = context.assets

    // === 第 1 步：注册 Assets 文件解析器 ===
    try {
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(assets)
        )
        android.util.Log.d("SoraCodeEditor", "[TM-Init] FileProviderRegistry OK")
    } catch (e: Exception) {
        android.util.Log.e("SoraCodeEditor", "[TM-Init] FileProviderRegistry 失败", e)
        return
    }

    // === 第 2 步：加载语法 ===
    try {
        GrammarRegistry.getInstance().loadGrammars(languages {
            language("javascript") {
                grammar = "textmate/javascript.tmLanguage.json"
                scopeName = "source.js"
            }
            language("html") {
                grammar = "textmate/html.tmLanguage.json"
                scopeName = "text.html.basic"
            }
            language("css") {
                grammar = "textmate/css.tmLanguage.json"
                scopeName = "source.css"
            }
            language("python") {
                grammar = "textmate/python.tmLanguage.json"
                scopeName = "source.python"
            }
            language("java") {
                grammar = "textmate/java.tmLanguage.json"
                scopeName = "source.java"
            }
        })
        // 验证语法是否真正注册成功
        val testGrammar = GrammarRegistry.getInstance().findGrammar("source.js")
        android.util.Log.d("SoraCodeEditor", "[TM-Init] 语法加载完成, findGrammar(source.js)=${testGrammar != null}")
        if (testGrammar != null) {
            // 直接 tokenize 测试：验证 grammar 能否实际工作
            try {
                val result = testGrammar.tokenizeLine("const x = 1;", null, null)
                android.util.Log.d("SoraCodeEditor", "[TM-Init] tokenize 测试: ${result?.tokens?.size ?: 0} tokens")
            } catch (e: Exception) {
                android.util.Log.e("SoraCodeEditor", "[TM-Init] tokenize 测试失败", e)
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("SoraCodeEditor", "[TM-Init] 语法加载失败", e)
    }

    // === 第 3 步：加载主题（独立于语法，互不影响） ===
    try {
        // 使用标准 API 加载主题（ThemeRegistry.loadTheme 内部调用 themeModel.load(null)，
        // tm4e 的 Theme.createFromRawTheme(rawTheme, null) 会自动创建内部 colorMap）
        ThemeRegistry.getInstance().loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(
                    assets.open("textmate/themes/dark_default.json"),
                    "dark_default.json",
                    null
                ),
                "dark_default"
            )
        )
        ThemeRegistry.getInstance().loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(
                    assets.open("textmate/themes/light_default.json"),
                    "light_default.json",
                    null
                ),
                "light_default"
            )
        )
        val currentTheme = ThemeRegistry.getInstance().currentThemeModel
        android.util.Log.d("SoraCodeEditor", "[TM-Init] 主题加载完成, current=${currentTheme?.name}, loaded=${currentTheme?.isLoaded}")
    } catch (e: Exception) {
        android.util.Log.e("SoraCodeEditor", "[TM-Init] 主题加载失败", e)
    }
}

/**
 * 根据 [SyntaxLanguage] 枚举构建对应的 TextMate 语言实例。
 * 语言不支持时返回 null，调用方应降级为 [EmptyLanguage]。
 *
 * @param language 目标语言枚举
 * @return [TextMateLanguage] 实例，或 null（不支持的语言）
 */
private fun buildSoraLanguage(language: SyntaxLanguage): Language? {
    return try {
        val scopeName = when (language) {
            SyntaxLanguage.JS -> "source.js"
            SyntaxLanguage.HTML, SyntaxLanguage.XML -> "text.html.basic"
            SyntaxLanguage.CSS -> "source.css"
            SyntaxLanguage.PYTHON -> "source.python"
            SyntaxLanguage.JAVA -> "source.java"
            // 以下语言暂无 TextMate 语法文件，降级为无高亮
            SyntaxLanguage.JSON,
            SyntaxLanguage.MARKDOWN,
            SyntaxLanguage.YAML,
            SyntaxLanguage.SHELL,
            SyntaxLanguage.CPP,
            SyntaxLanguage.GO,
            SyntaxLanguage.RUST,
            SyntaxLanguage.PROMPT,
            SyntaxLanguage.PLAIN -> {
                android.util.Log.d("SoraCodeEditor", "[TM-Lang] $language 无对应 grammar，降级为 EmptyLanguage")
                return null
            }
        }
        val lang = TextMateLanguage.create(scopeName, true)
        android.util.Log.d("SoraCodeEditor", "[TM-Lang] 成功创建 TextMateLanguage: scope=$scopeName, class=${lang.javaClass.simpleName}")
        lang
    } catch (e: Exception) {
        android.util.Log.e("SoraCodeEditor", "[TM-Lang] 语言 $language 构建失败", e)
        null
    }
}

/**
 * 将应用的 [ThemeColors] 映射到 Sora-Editor 的 [EditorColorScheme]。
 * 补充 TextMate 主题未覆盖的 Sora 专有 UI 颜色（行号栏背景、选中背景、光标颜色等）。
 *
 * @param base 当前 Sora 颜色方案（作为基础，避免重置 TextMate 设置的颜色）
 * @param tc 应用主题颜色
 * @param isDark 是否为深色模式
 * @return 融合了应用主题色的 [EditorColorScheme]
 */
private fun buildColorScheme(
    base: EditorColorScheme,
    tc: ThemeColors,
    isDark: Boolean
): EditorColorScheme {
    return base.also { scheme ->
        // 编辑器主背景色
        scheme.setColor(
            EditorColorScheme.WHOLE_BACKGROUND,
            tc.surface.toArgb()
        )
        // 行号栏背景
        scheme.setColor(
            EditorColorScheme.LINE_NUMBER_BACKGROUND,
            tc.surface2.toArgb()
        )
        // 行号文字颜色
        scheme.setColor(
            EditorColorScheme.LINE_NUMBER,
            tc.text3.toArgb()
        )
        // 当前行行号颜色（高亮）
        scheme.setColor(
            EditorColorScheme.LINE_NUMBER_CURRENT,
            tc.primary.toArgb()
        )
        // 当前行背景高亮
        scheme.setColor(
            EditorColorScheme.CURRENT_LINE,
            tc.primarySoft.copy(alpha = if (isDark) 0.15f else 0.5f).toArgb()
        )
        // 光标颜色
        scheme.setColor(
            EditorColorScheme.SELECTION_INSERT,
            tc.primary.toArgb()
        )
        // 文本选中背景色
        scheme.setColor(
            EditorColorScheme.SELECTED_TEXT_BACKGROUND,
            tc.primary.copy(alpha = 0.3f).toArgb()
        )
        // 选中把手颜色
        scheme.setColor(
            EditorColorScheme.SELECTION_HANDLE,
            tc.primary.toArgb()
        )
        // 普通文本颜色（未被 TextMate 覆盖的部分）
        scheme.setColor(
            EditorColorScheme.TEXT_NORMAL,
            tc.text.toArgb()
        )
        // 缩进参考线颜色
        scheme.setColor(
            EditorColorScheme.BLOCK_LINE,
            tc.line.copy(alpha = 0.5f).toArgb()
        )
        // 搜索高亮颜色
        scheme.setColor(
            EditorColorScheme.MATCHED_TEXT_BACKGROUND,
            tc.primary.copy(alpha = 0.25f).toArgb()
        )
    }
}

/**
 * [androidx.compose.ui.graphics.Color] → Android ARGB Int 转换扩展函数。
 * 将 Compose 颜色空间中的颜色值转换为 Android 框架使用的 ARGB 整数格式。
 *
 * @return Android ARGB 格式的颜色整数
 */
private fun androidx.compose.ui.graphics.Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
