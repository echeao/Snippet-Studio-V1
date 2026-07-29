package com.feige.snippetstudio.ui.components

import android.webkit.JavascriptInterface

/**
 * [EditorJsBridge] 是用于 Android 原生 Kotlin WebView 与内嵌 Web 代码编辑器之间的数据交互桥接器。
 *
 * 核心职责：
 * 1. 接收 Web 编辑器打字事件回调 [onCodeChanged]，通知外部 ViewModel 同步最新代码文本。
 * 2. 接收 Web 编辑器光标与选区变动回调 [onCursorChanged]，通知外部 UI 更新底部状态栏行列号。
 * 3. 接收网页就绪通知 [onEditorReady]，触发初始化数据灌入。
 *
 * @param onCodeChanged 网页代码文本发生变化时的回调闭包
 * @param onCursorChanged 网页光标位置发生变化时的回调闭包 (行号, 列号)
 * @param onEditorReady 网页编辑器加载完毕后的回调闭包
 */
class EditorJsBridge(
    private val onCodeChanged: (String) -> Unit,
    private val onCursorChanged: (line: Int, col: Int) -> Unit,
    private val onEditorReady: () -> Unit
) {

    /**
     * JS 调用原生的代码修改回调接口
     *
     * @param code 最新代码全文
     */
    @JavascriptInterface
    fun onCodeChanged(code: String) {
        onCodeChanged.invoke(code)
    }

    /**
     * JS 调用原生的光标位置修改回调接口
     *
     * @param line 当前光标所在的行索引 (0-based)
     * @param col 当前光标所在的列索引 (0-based)
     */
    @JavascriptInterface
    fun onCursorChanged(line: Int, col: Int) {
        onCursorChanged.invoke(line, col)
    }

    /**
     * JS 调用原生的编辑器资源就绪通知接口
     */
    @JavascriptInterface
    fun onEditorReady() {
        onEditorReady.invoke()
    }
}
