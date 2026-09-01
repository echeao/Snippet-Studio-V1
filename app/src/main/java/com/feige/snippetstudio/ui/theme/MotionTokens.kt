package com.feige.snippetstudio.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * [MotionTokens] 全局高级物理动效与过渡设计代币规约。
 *
 * 核心设计哲学 (基于 Apple Motion & Emil Kowalski 交互物理学):
 * 1. **自然物理反馈 (Physical Spring)**：抛弃机械生硬的匀速线性补间，优先使用带有阻尼和刚度物理属性的弹簧动画。
 * 2. **响应无延迟 (Instant Reaction)**：按压按下时快速收缩，抬起时带有微妙的回弹感，营造真实触感。
 * 3. **分级动效系统 (Tiered Dynamics)**：针对微小图标、整块卡片、大跨度页面转场分别配置专属动效强度。
 */
object MotionTokens {

    // ===== 1. 物理弹簧模型规范 (Spring Physics Specs) =====

    /**
     * 【Snappy 极速响应弹簧】
     * 适用于：小型交互控件、图标按压、开关切换、胶囊药丸等需要瞬间响应的微动效。
     * 特性：刚度高 (600f)，阻尼中高 (0.75f)，近乎无多余振荡，干净利落。
     */
    fun <T> springSnappy(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        stiffness = 600f,
        dampingRatio = 0.75f,
        visibilityThreshold = visibilityThreshold
    )

    /**
     * 【Bouncy 灵动回弹弹簧】
     * 适用于：大卡片按压、收藏星标点亮、新建卡片、底栏突起按钮等需要生动弹性质感的交互。
     * 特性：刚度中等 (380f)，阻尼较低 (0.62f)，释放时带有柔和有弹性的触感回弹。
     */
    fun <T> springBouncy(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        stiffness = 380f,
        dampingRatio = 0.62f,
        visibilityThreshold = visibilityThreshold
    )

    /**
     * 【Smooth 丝滑平稳弹簧】
     * 适用于：浮岛控制台拖拽复位、抽屉升降、折叠展开等大范围连续位移。
     * 特性：刚度适中 (300f)，高阻尼 (0.85f)，运动平滑且无视觉冲撞感。
     */
    fun <T> springSmooth(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        stiffness = 300f,
        dampingRatio = 0.85f,
        visibilityThreshold = visibilityThreshold
    )

    // ===== 2. 补间时长与高级缓动曲线 (Timing & Easing) =====

    /** 转场进入时长 (毫秒) */
    const val DURATION_ENTER = 280

    /** 转场退出时长 (毫秒) */
    const val DURATION_EXIT = 200

    /** 快速微交互时长 (毫秒) */
    const val DURATION_FAST = 150

    /** 顺滑减速曲线 (Cubic Decel - 模拟 iOS / macOS 流畅减速滑行) */
    val DecelEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** 强加速曲线 (Cubic Accel - 利落退出) */
    val AccelEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    /** 自然 Standard 曲线 */
    val StandardEasing = FastOutSlowInEasing

    /** 默认标准进入补间 */
    fun <T> tweenEnter(duration: Int = DURATION_ENTER): TweenSpec<T> = tween(
        durationMillis = duration,
        easing = StandardEasing
    )

    /** 默认标准退出补间 */
    fun <T> tweenExit(duration: Int = DURATION_EXIT): TweenSpec<T> = tween(
        durationMillis = duration,
        easing = FastOutLinearInEasing
    )

    // ===== 3. 触控按压物理缩放比率 (Press Scale Factors) =====

    /** 大型卡片/预览视图按下时的物理缩放目标值 (0.98x，微动效沉浸感) */
    const val PRESSED_SCALE_CARD = 0.98f

    /** 中型卡片/类型新建块按下时的物理缩放目标值 (0.96f，明显触控反馈) */
    const val PRESSED_SCALE_MEDIUM_CARD = 0.96f

    /** 小型按键/图标按下时的物理缩放目标值 (0.90f，紧凑弹性) */
    const val PRESSED_SCALE_BUTTON = 0.90f
}
