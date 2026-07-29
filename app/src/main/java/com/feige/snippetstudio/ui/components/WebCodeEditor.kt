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
import org.json.JSONObject

/**
 * [WebCodeEditor] 100% 本地离线自包含的 Web 虚拟化代码编辑器组件。
 *
 * 安全与性能重构亮点：
 * 1. **全量离线化加载**：使用 `file:///android_asset/editor/index.html` 本地资源，完全剥离远程 CDN 依赖，绝不黑屏卡死。
 * 2. **JSONObject 严格序列化**：使用 Android 官方 [JSONObject.quote] 对任意多行文本、换行符 `\n` 进行百分百安全的 JSON 字符串转义，杜绝 JS 语法解析异常。
 * 3. **实时双向桥接**：打字变动、字号微调与主题变动秒级同步响应。
 *
 * @param textFieldValue 代码与选区信息
 * @param onValueChange 变动回调
 * @param onCursorChange 光标位置回调
 * @param fontSp 字体大小
 * @param snippetType 片段语言类型
 * @param isWordWrap 是否开启自动换行
 * @param modifier 外部修饰符
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

    // 记录 WebView 实例引用与初始化就绪状态
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
            // 使用 Android 官方 JSONObject.quote 对文本进行绝对安全防溃的安全转义
            val safeJsonCode = JSONObject.quote(textFieldValue.text)
            val langCode = snippetType.code
            webViewRef?.evaluateJavascript("setCodeContent($safeJsonCode, '$langCode');", null)
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
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    setSupportZoom(false)
                }

                // 注入 JS 桥接接口对象
                addJavascriptInterface(jsBridge, "AndroidBridge")

                webViewClient = object : WebViewClient() {}

                // 加载 assets 本地离线编辑器主模板
                loadUrl("file:///android_asset/editor/index.html")
            }
        },
        update = { webView ->
            webViewRef = webView
        },
        modifier = modifier.fillMaxSize()
    )
}
