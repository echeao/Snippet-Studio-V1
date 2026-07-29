package com.feige.snippetstudio.ui.components

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * [EditorJsBridge] 是用于 Android 原生 Kotlin WebView 与内嵌 Web 代码编辑器之间的数据交互桥接器。
 *
 * 线程安全重构：
 * 1. WebView 的 [JavascriptInterface] 回调发生在 Binder 后台子线程。
 * 2. 使用 [mainHandler] 将所有代码变动、光标位置、就绪通知强制派发回 Android [Looper.getMainLooper] 主线程。
 * 3. 彻底消除非主线程更新 Compose 状态引发的时序死锁与渲染不刷新问题。
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

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * JS 调用原生的代码修改回调接口
     *
     * @param code 最新代码全文
     */
    @JavascriptInterface
    fun onCodeChanged(code: String) {
        mainHandler.post {
            onCodeChanged.invoke(code)
        }
    }

    /**
     * JS 调用原生的光标位置修改回调接口
     *
     * @param line 当前光标所在的行索引 (0-based)
     * @param col 当前光标所在的列索引 (0-based)
     */
    @JavascriptInterface
    fun onCursorChanged(line: Int, col: Int) {
        mainHandler.post {
            onCursorChanged.invoke(line, col)
        }
    }

    /**
     * JS 调用原生的编辑器资源就绪通知接口
     */
    @JavascriptInterface
    fun onEditorReady() {
        mainHandler.post {
            onEditorReady.invoke()
        }
    }
}
