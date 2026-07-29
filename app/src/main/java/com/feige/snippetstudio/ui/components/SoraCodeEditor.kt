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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 保持最新回调引用，避免 lambda 捕获过期值
    val latestOnTextChange by rememberUpdatedState(onTextChange)
    val latestOnCursorChange by rememberUpdatedState(onCursorChange)
    // 始终保持最新的外部文本引用，供事件回调与 LaunchedEffect 内部使用
    val latestText by rememberUpdatedState(text)

    // ===== 核心状态门控 =====
    // isExternalUpdate：标记当前内容变更是否由外部（Compose 侧）触发，防止循环回调
    var isExternalUpdate by remember { mutableStateOf(false) }
    // isEditorReady：编辑器就绪门控。仅当语言初始化完成且内容稳定后才开放事件传播，
    // 从根本上杜绝 TextMate 初始化/语言切换期间的异步清空事件泄漏到 ViewModel
    var isEditorReady by remember { mutableStateOf(false) }

    // TextMate 注册表初始化标记（仅在 Context 变化时执行一次）
    val tmInitialized = remember(context) { mutableStateOf(false) }

    // 创建并记忆 Sora CodeEditor 实例
    val editor = remember {
        CodeEditor(context).also { ed ->
            // 禁用自动补全弹窗（代码片段场景无需补全）
            ed.getComponent(EditorAutoCompletion::class.java).isEnabled = false
            // 初始化同步填充首屏文本
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

    // ===== 主题切换：响应 isDark 或 themeColors 变化 =====
    LaunchedEffect(isDark, themeColors) {
        if (!tmInitialized.value) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val themeName = if (isDark) "dark_default" else "light_default"
            try {
                ThemeRegistry.getInstance().setTheme(themeName)
            } catch (e: Exception) {
                // 主题切换失败时静默回退
            }
        }
        // 同步手动应用颜色方案
        editor.colorScheme = buildColorScheme(editor.colorScheme, themeColors, isDark)
    }

    // ===== 语言初始化与切换：关闭门控 → 设置语言 → 稳定后恢复内容 → 开放门控 =====
    LaunchedEffect(language, tmInitialized.value) {
        if (!tmInitialized.value) return@LaunchedEffect

        // 第一步：关闭事件传播门控，阻止初始化期间任何事件泄漏到 ViewModel
        isEditorReady = false

        // 第二步：在 IO 线程构建 TextMate 语言实例
        val lang: Language = withContext(Dispatchers.IO) {
            buildSoraLanguage(language) ?: EmptyLanguage()
        }

        // 第三步：设置语言并立即重设文本（同一 isExternalUpdate 窗口内，不触发外部回调）
        isExternalUpdate = true
        editor.setEditorLanguage(lang)
        editor.setText(latestText)
        isExternalUpdate = false

        // 第四步：等待 Sora 内部异步操作完成（语言分析器初始化 + 可能的延迟内容清空）
        kotlinx.coroutines.delay(350)

        // 第五步：再次校验并恢复内容（捕获延迟异步清空）
        if (editor.text.toString() != latestText) {
            isExternalUpdate = true
            editor.setText(latestText)
            isExternalUpdate = false
        }

        // 第六步：开放门控，此后用户编辑事件可正常传播到 ViewModel
        isEditorReady = true
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
            isExternalUpdate = false
        }
    }

    // ===== 注册编辑器内容与光标监听器（使用 Sora EventBus 订阅机制）=====
    DisposableEffect(editor) {
        // 订阅文本内容变动事件：三层防护确保仅用户真实编辑传播到 ViewModel
        val contentSub = editor.subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) { _, _ ->
            if (isEditorReady && !isExternalUpdate) {
                val newText = editor.text.toString()
                // 第三层防护：编辑器未聚焦时，内容从非空突变为空必然是 Sora 内部操作（非用户编辑），
                // 静默恢复内容并拦截事件。用户聚焦编辑时的清空操作不受影响。
                if (newText.isEmpty() && latestText.isNotEmpty() && !editor.isFocused) {
                    isExternalUpdate = true
                    editor.setText(latestText)
                    isExternalUpdate = false
                    return@subscribeEvent
                }
                latestOnTextChange(newText)
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
    try {
        val assets: AssetManager = context.assets

        // 注册 Assets 文件解析器
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(assets)
        )

        // 注册 TextMate 语法规则（语言 → 语法文件映射）
        GrammarRegistry.getInstance().loadGrammars(languages {
            language("javascript") {
                grammar = "textmate/javascript.tmLanguage.json"
                defaultScopeName()
                scopeName = "source.js"
            }
            language("html") {
                grammar = "textmate/html.tmLanguage.json"
                defaultScopeName()
                scopeName = "text.html.basic"
            }
            language("css") {
                grammar = "textmate/css.tmLanguage.json"
                defaultScopeName()
                scopeName = "source.css"
            }
            language("python") {
                grammar = "textmate/python.tmLanguage.json"
                defaultScopeName()
                scopeName = "source.python"
            }
            language("java") {
                grammar = "textmate/java.tmLanguage.json"
                defaultScopeName()
                scopeName = "source.java"
            }
        })

        // 注册深色和浅色主题
        ThemeRegistry.getInstance().loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(
                    assets.open("textmate/themes/dark_default.json"),
                    "dark_default",
                    null
                ),
                "dark_default"
            )
        )
        ThemeRegistry.getInstance().loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(
                    assets.open("textmate/themes/light_default.json"),
                    "light_default",
                    null
                ),
                "light_default"
            )
        )
    } catch (e: Exception) {
        // 语法注册失败时降级为无高亮模式，保证编辑器基本可用
        android.util.Log.w("SoraCodeEditor", "TextMate 注册表初始化失败，降级为无高亮模式: ${e.message}")
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
            SyntaxLanguage.PLAIN -> return null
        }
        TextMateLanguage.create(scopeName, true)
    } catch (e: Exception) {
        android.util.Log.w("SoraCodeEditor", "语言 $language 构建失败: ${e.message}")
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
