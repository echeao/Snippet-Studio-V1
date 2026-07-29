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
 * [WebCodeEditor] 100% 本地离线自包含的高性能代码编辑器组件。
 *
 * 时序与线程安全重构：
 * 1. **PageFinished 双重判定**：在 WebViewClient 的 [WebViewClient.onPageFinished] 中确保页面 DOM 完全加载后再注入初始代码。
 * 2. **Post 线程安全执行**：所有 [WebView.evaluateJavascript] 指令统一包裹在 [WebView.post] 中执行，100% 确保处于 UI 主线程与就绪上下文。
 * 3. **JSONObject.quote 安全转义**：彻底解决复杂文本、换行符 `\n` 引发的 SyntaxError 问题。
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

    // 记录 WebView 实例引用与页面加载完毕标志
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageLoaded by remember { mutableStateOf(false) }

    // 封装主线程 safePost 方法，确保 evaluateJavascript 不被提前丢弃
    val runJs: (String) -> Unit = remember(webViewRef) {
        { jsScript ->
            webViewRef?.post {
                webViewRef?.evaluateJavascript(jsScript, null)
            }
        }
    }

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
                // JS 引擎就绪
            }
        )
    }

    // 当页面完成加载或 textFieldValue / snippetType 变更时，更新 Web 端内容
    LaunchedEffect(isPageLoaded, textFieldValue.text, snippetType) {
        if (isPageLoaded) {
            val safeJsonCode = JSONObject.quote(textFieldValue.text)
            val langCode = snippetType.code
            runJs("setCodeContent($safeJsonCode, '$langCode');")
        }
    }

    // 动态同步字号设置
    LaunchedEffect(isPageLoaded, fontSp) {
        if (isPageLoaded) {
            runJs("setFontSizeSp($fontSp);")
        }
    }

    // 动态同步自动换行设置
    LaunchedEffect(isPageLoaded, isWordWrap) {
        if (isPageLoaded) {
            runJs("setWordWrapEnabled($isWordWrap);")
        }
    }

    // 动态同步深浅色主题设置
    LaunchedEffect(isPageLoaded, isDark) {
        if (isPageLoaded) {
            runJs("setThemeIsDark($isDark);")
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

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isPageLoaded = true

                        // 页面完成 DOM 绘制，立即主线程安全灌入初始代码与配置
                        val safeJsonCode = JSONObject.quote(textFieldValue.text)
                        val langCode = snippetType.code
                        view?.post {
                            view.evaluateJavascript("setCodeContent($safeJsonCode, '$langCode');", null)
                            view.evaluateJavascript("setFontSizeSp($fontSp);", null)
                            view.evaluateJavascript("setWordWrapEnabled($isWordWrap);", null)
                            view.evaluateJavascript("setThemeIsDark($isDark);", null)
                        }
                    }
                }

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
