package com.feige.snippetstudio.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.MarkdownRenderer

/**
 * [ConsoleLog] 控制台日志实体数据模型。
 */
data class ConsoleLog(val message: String, val level: String)

/**
 * [WebConsoleBridge] 网页 JavaScript 原生控制台拦截桥接器。
 *
 * 通过 `@JavascriptInterface` 将 JavaScript 的 `console.log` / `console.error` 等输出捕获并重定向回 Android 原生界面展示。
 */
class WebConsoleBridge(private val onLog: (String, String) -> Unit) {
    @JavascriptInterface
    fun postMessage(msg: String, level: String) {
        onLog(msg, level)
    }
}

/**
 * [RunPreview] 实时运行预览面板组件。
 *
 * 根据片段类型 [SnippetType] 提供针对性的交互式运行与效果渲染：
 * 1. **HTML**：内置 WebView 动态渲染 HTML/CSS 样式。
 * 2. **JS**：内置 WebView 执行 JavaScript 代码，结合 [WebConsoleBridge] 在下方控制台面板实能拦截输出日志。
 * 3. **Markdown**：调用 [MarkdownRenderer] 转换为 HTML 并通过 WebView 展示富文本样式。
 * 4. **Prompt**：格式化展示 AI 提示词文本，并提供“一键复制”快捷操作。
 *
 * @param type 代码片段类型
 * @param content 片段代码正文
 * @param onToast Toast 提示消息回调闭包
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RunPreview(
    type: SnippetType,
    content: String,
    modifier: Modifier = Modifier,
    onToast: ((String) -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // 保存 JavaScript 控制台日志的响应式列表
    val consoleLogs = remember { mutableStateListOf<ConsoleLog>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) SurfaceDark else SurfaceLight)
    ) {
        when (type) {
            // ===== 1. HTML 渲染预览模式 =====
            SnippetType.HTML -> {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                        }
                    },
                    update = { webView ->
                        val htmlContent = if (content.contains("<html", ignoreCase = true)) {
                            content
                        } else {
                            """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="utf-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <style>
                                    body { font-family: sans-serif; padding: 16px; margin: 0; background-color: ${if (isDark) "#181B22" else "#FFFFFF"}; color: ${if (isDark) "#EDEFF4" else "#16181F"}; }
                                </style>
                            </head>
                            <body>$content</body>
                            </html>
                            """.trimIndent()
                        }
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("webview_html_preview")
                )
            }

            // ===== 2. JavaScript 执行与 Console 日志拦截模式 =====
            SnippetType.JS -> {
                Box(modifier = Modifier.weight(1f)) {
                    // AndroidView 用于在 Jetpack Compose 中嵌入原生 Android View 组件 (如 WebView)
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                // 实例化 JS <-> Native 桥接通信通道，拦截控制台消息并更新 consoleLogs 列表
                                val bridge = WebConsoleBridge { msg, level ->
                                    post { consoleLogs.add(ConsoleLog(msg, level)) }
                                }
                                // 将 bridge 绑定给 WebView 中的全局 window.AndroidConsole 对象
                                addJavascriptInterface(bridge, "AndroidConsole")
                                webViewClient = WebViewClient()
                            }
                        },
                        update = { webView ->
                            consoleLogs.clear()
                            // 构造 JS 包装沙盒代码：重写 console.log / error / warn，将其转发给 AndroidConsole.postMessage
                            val wrappedJs = """
                                <!DOCTYPE html>
                                <html>
                                <head><meta charset="utf-8"></head>
                                <body>
                                <script>
                                    (function() {
                                        function send(msg, lvl) {
                                            try {
                                                if (window.AndroidConsole) {
                                                    window.AndroidConsole.postMessage(typeof msg === 'object' ? JSON.stringify(msg) : String(msg), lvl);
                                                }
                                            } catch(e){}
                                        }
                                        console.log = function() { Array.from(arguments).forEach(m => send(m, 'log')); };
                                        console.error = function() { Array.from(arguments).forEach(m => send(m, 'error')); };
                                        console.warn = function() { Array.from(arguments).forEach(m => send(m, 'warn')); };
                                        console.info = function() { Array.from(arguments).forEach(m => send(m, 'info')); };

                                        try {
                                            $content
                                        } catch (err) {
                                            console.error('Uncaught ' + err.toString());
                                        }
                                    })();
                                </script>
                                </body>
                                </html>
                            """.trimIndent()
                            webView.loadDataWithBaseURL(null, wrappedJs, "text/html", "UTF-8", null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }


                // 下方 JS 控制台日志面板 (Console Log Panel)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, if (isDark) LineDark else LineLight),
                    color = if (isDark) Surface2Dark else Surface2Light
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDark) LineDark else LineLight)
                                .padding(horizontal = Spacing.S3, vertical = Spacing.S1),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.editor_console),
                                style = CaptionStyle,
                                color = if (isDark) Text2Dark else Text2Light
                            )
                            TextButton(
                                onClick = { consoleLogs.clear() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(stringResource(R.string.common_close), fontSize = 11.sp)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(Spacing.S2)
                        ) {
                            items(consoleLogs) { log ->
                                val color = when (log.level) {
                                    "error" -> Danger
                                    "warn" -> Warning
                                    else -> if (isDark) TextDark else TextLight
                                }
                                Text(
                                    text = "> ${log.message}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = color,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ===== 3. Markdown 富文本渲染模式 =====
            SnippetType.MARKDOWN -> {
                val renderedHtml = remember(content) { MarkdownRenderer.toHtml(content) }
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            webViewClient = WebViewClient()
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, renderedHtml, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("webview_md_preview")
                )
            }

            // ===== 4. Prompt AI 提示词视图 =====
            SnippetType.PROMPT -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.S4)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(content))
                                onToast?.invoke(context.getString(R.string.toast_copied))
                            },
                            shape = AppShapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Prompt",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.S1))
                            Text(stringResource(R.string.act_copy))
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.S3))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, if (isDark) LineDark else LineLight, AppShapes.medium),
                        shape = AppShapes.medium,
                        color = if (isDark) Surface2Dark else Surface2Light
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(Spacing.S4)
                        ) {
                            Text(
                                text = content,
                                style = BodyStyle,
                                color = if (isDark) TextDark else TextLight,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

