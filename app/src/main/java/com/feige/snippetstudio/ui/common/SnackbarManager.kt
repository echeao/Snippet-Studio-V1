package com.feige.snippetstudio.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [SnackbarManager] 全局 Snackbar 消息提示管理器。
 *
 * 架构职责：
 * 1. 统一管理应用内所有 Snackbar 消息的发射与 Action 按钮回调。
 * 2. 支持简单文本消息 [showSnackbar] 与带操作按钮的消息 [showSnackbar]（含 actionLabel + onAction）。
 * 3. 通过 [LocalSnackbarManager] CompositionLocal 向 Compose UI 树提供全局访问点，
 *    避免在每个 Screen 参数中逐层传递回调闭包。
 *
 * 典型使用场景：
 * - 删除操作后显示"已删除"+ "撤销"按钮，用户点击撤销时执行恢复逻辑。
 * - 普通操作反馈（保存成功、导出完成等）仅显示文本。
 *
 * @param scope 协程作用域（通常来自 rememberCoroutineScope()）
 * @param hostState Material3 SnackbarHostState 实例
 */
class SnackbarManager(
    private val scope: CoroutineScope,
    val hostState: SnackbarHostState
) {
    /** 当前待执行的 Action 回调（用户点击 Snackbar Action 按钮时触发） */
    @Volatile
    var pendingAction: (() -> Unit)? = null
        private set

    /**
     * 显示简单文本 Snackbar 消息（无 Action 按钮）。
     *
     * @param message 提示文本内容
     */
    fun showSnackbar(message: String) {
        pendingAction = null
        scope.launch {
            hostState.showSnackbar(message = message)
        }
    }

    /**
     * 显示带 Action 操作按钮的 Snackbar 消息。
     * 用户点击 Action 按钮后自动执行 [onAction] 回调。
     *
     * @param message 提示文本内容
     * @param actionLabel Action 按钮显示文本（如"撤销"）
     * @param onAction 用户点击 Action 按钮时执行的回调闭包
     * @param duration Snackbar 显示时长（默认 Short ≈ 4 秒）
     */
    fun showSnackbar(
        message: String,
        actionLabel: String,
        onAction: () -> Unit,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        pendingAction = onAction
        scope.launch {
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = duration
            )
            if (result == SnackbarResult.ActionPerformed) {
                pendingAction?.invoke()
            }
            pendingAction = null
        }
    }
}

/**
 * [LocalSnackbarManager] 全局 Snackbar 管理器 CompositionLocal 提供器。
 * UI 组件可通过 `LocalSnackbarManager.current` 直接访问，无需逐层参数传递。
 */
val LocalSnackbarManager = staticCompositionLocalOf<SnackbarManager> {
    error("LocalSnackbarManager 未被提供，请确保在根节点通过 CompositionLocalProvider 注入")
}
