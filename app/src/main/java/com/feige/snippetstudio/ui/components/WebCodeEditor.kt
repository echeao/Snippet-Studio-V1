package com.feige.snippetstudio.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.LocalThemeColors

/**
 * [WebCodeEditor] 基于 Acode / Ace Editor 内核构建的 Compose WebView 高性能代码编辑器组件。
 *
 * 架构亮点：
 * 1. **DOM 视口虚拟化**：借由 Ace 虚拟渲染机制，可轻而易举支撑数万行超大文件极速滑动，绝对消除掉帧与卡顿。
 * 2. **全量桥接交互**：通过 [EditorJsBridge] 实现 Native 与 JS 之间代码变动、光标行列号、字号与主题的实时同步。
 * 3. **无缝平替原生**：接口与 [CodeEditor] 保持高度一致，外部无感无缝升级。
 *
 * @param textFieldValue 带有选区与文本信息的 [TextFieldValue]
 * @param onValueChange 代码修改回调
 * @param onCursorChange 光标行列变动回调 (行号, 列号)
 * @param fontSp 字体字号大小 (sp)
 * @param snippetType 代码片段类型
 * @param isWordWrap 是否开启自动换行
 * @param modifier 修饰符
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebCodeEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onCursorChange: (line: Int, col: Int) -> Unit,
    fontSp: Float,
    snippetType: SnippetType = SnippetType.HTML,
    isWordWrap: Boolean = true,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    val isDark = tc.isDark

    // 记录 WebView 实例引用
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isEditorReady by remember { mutableStateOf(false) }

    // 创建 Android-JS 通信桥接类
    val jsBridge = remember {
        EditorJsBridge(
            onCodeChanged = { newCode ->
                if (newCode != textFieldValue.text) {
                    onValueChange(TextFieldValue(text = newCode))
                }
            },
            onCursorChanged = { line, col ->
                onCursorChange(line, col)
            },
            onEditorReady = {
                isEditorReady = true
            }
        )
    }

    // 当内部资源就绪或外部 textFieldValue 变更时，更新 Web 端代码内容
    LaunchedEffect(isEditorReady, textFieldValue.text, snippetType) {
        if (isEditorReady) {
            val safeCode = textFieldValue.text.replace("\\", "\\\\").replace("`", "\\`").replace("\$", "\\\$")
            val langCode = snippetType.code
            webViewRef?.evaluateJavascript("setCodeContent(`$safeCode`, '$langCode');", null)
        }
    }

    // 动态同步字号设置
    LaunchedEffect(isEditorReady, fontSp) {
        if (isEditorReady) {
            webViewRef?.evaluateJavascript("setFontSizeSp($fontSp);", null)
        }
    }

    // 动态同步自动换行设置
    LaunchedEffect(isEditorReady, isWordWrap) {
        if (isEditorReady) {
            webViewRef?.evaluateJavascript("setWordWrapEnabled($isWordWrap);", null)
        }
    }

    // 动态同步深浅色主题设置
    LaunchedEffect(isEditorReady, isDark) {
        if (isEditorReady) {
            webViewRef?.evaluateJavascript("setThemeIsDark($isDark);", null)
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewRef = this
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    setSupportZoom(false)
                }

                // 注入 JS 桥接接口对象
                addJavascriptInterface(jsBridge, "AndroidBridge")

                webViewClient = object : WebViewClient() {}

                // 加载 assets 本地编辑器主模板
                loadUrl("file:///android_asset/editor/index.html")
            }
        },
        update = { webView ->
            webViewRef = webView
        },
        modifier = modifier.fillMaxSize()
    )
}
