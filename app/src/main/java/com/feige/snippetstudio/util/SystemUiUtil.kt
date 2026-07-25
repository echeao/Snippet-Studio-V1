package com.feige.snippetstudio.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import android.os.Build
import android.view.WindowManager

/**
 * [SystemUiUtil] 系统 UI 与沉浸式全屏控制工具类。
 */
object SystemUiUtil {
    /**
     * 沿着 ContextWrapper 树递归查找最靠近的 [Activity] 对象。
     */
    fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * 开启或关闭沉浸式全屏模式（隐藏/显示系统状态栏和导航栏）。
     *
     * @param activity 目标 Activity 实例
     * @param enabled true 表示开启全屏沉浸，false 表示恢复显示系统栏
     */
    /**
     * 开启或关闭沉浸式全屏模式（隐藏/显示系统状态栏 StatusBar 与导航栏 NavigationBar）。
     *
     * 教学解析：
     * 1. WindowCompat.getInsetsController: Android 11+ 推荐的现代窗口控制 API，向上向下兼容低版本系统。
     * 2. BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE: 手势防抖模式。在全屏状态下从边缘内滑时，
     *    系统栏以半透明状态临时显现，数秒无操作后自动再次隐去，提供沉浸的代码编辑体验。
     * 3. LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES: 允许内容延伸到挖孔屏/刘海屏凹槽区域下方。
     *
     * @param activity 目标 Activity 实例
     * @param enabled true 表示开启全屏沉浸，false 表示恢复显示系统栏
     */
    fun setImmersiveFullscreen(activity: Activity?, enabled: Boolean) {
        val window = activity?.window ?: return
        val controller = WindowCompat.getInsetsController(window, window.decorView)

        if (enabled) {
            // Android 9.0 (API 28)+ 挖孔屏适配：允许窗口内容延伸到短边刘海屏下方
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            // 边缘滑动触发临时半透明系统栏显示
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // 隐藏状态栏 + 导航栏 (systemBars)
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            // 恢复默认刘海屏避让模式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                }
            }
            // 恢复显示状态栏与导航栏
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}


