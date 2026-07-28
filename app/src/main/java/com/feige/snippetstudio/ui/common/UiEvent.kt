package com.feige.snippetstudio.ui.common

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * [UiEvent] 一次性 UI 事件密封类。
 *
 * 架构设计说明：
 * 在 MVVM 架构中，State（状态）用于描述 UI 的持续状态（如列表数据、加载标志），
 * 而 Event（事件）用于描述一次性、不可重放的副作用操作（如弹出提示、导航返回）。
 *
 * 使用 [Channel] + [receiveAsFlow] 确保每个事件仅被消费一次，
 * 避免配置变更（屏幕旋转）时重复触发。
 *
 * 典型使用模式：
 * ```kotlin
 * // ViewModel 中发射事件
 * _events.send(UiEvent.ShowSnackbar("已保存"))
 *
 * // Screen 中收集事件
 * LaunchedEffect(Unit) {
 *     viewModel.events.collect { event ->
 *         when (event) {
 *             is UiEvent.ShowSnackbar -> snackbarManager.showSnackbar(event.message)
 *             is UiEvent.NavigateBack -> onBack()
 *         }
 *     }
 * }
 * ```
 */
sealed class UiEvent {

    /**
     * 显示 Snackbar 消息提示事件。
     *
     * @param message 提示文本内容
     * @param actionLabel 可选的 Action 按钮文本（如"撤销"）
     * @param onAction 可选的 Action 按钮点击回调
     */
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : UiEvent()

    /** 导航返回事件：触发页面 popBackStack */
    data object NavigateBack : UiEvent()

    /**
     * 导航跳转事件：携带目标路由标识。
     *
     * @param route 目标路由路径
     */
    data class NavigateTo(val route: String) : UiEvent()
}

/**
 * [EventEmitter] 一次性事件发射器混入接口。
 *
 * ViewModel 可实现此接口快速获得事件发射能力：
 * ```kotlin
 * class MyViewModel : ViewModel(), EventEmitter {
 *     override val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
 *
 *     fun doSomething() {
 *         viewModelScope.launch {
 *             emitEvent(UiEvent.ShowSnackbar("操作完成"))
 *         }
 *     }
 * }
 * ```
 */
interface EventEmitter {

    /** 内部事件通道（BUFFERED 模式防止发射阻塞） */
    val eventChannel: Channel<UiEvent>

    /** 对外暴露的事件流（Screen 层通过 collect 消费） */
    val events: Flow<UiEvent>
        get() = eventChannel.receiveAsFlow()

    /**
     * 发射一次性 UI 事件。
     * 此方法为挂起函数，需在协程中调用。
     *
     * @param event 待发射的 UI 事件实例
     */
    suspend fun emitEvent(event: UiEvent) {
        eventChannel.send(event)
    }

    /**
     * 发射一次性 UI 事件（非挂起版本，内部使用 trySend）。
     * 适用于非协程上下文的快捷调用。
     *
     * @param event 待发射的 UI 事件实例
     */
    fun tryEmitEvent(event: UiEvent) {
        eventChannel.trySend(event)
    }
}
