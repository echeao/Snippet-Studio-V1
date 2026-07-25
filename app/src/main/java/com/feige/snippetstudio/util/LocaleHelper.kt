package com.feige.snippetstudio.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * [LocaleHelper] 语言与本地化工具对象。
 *
 * 用于在运行时动态切换应用的多语言（简体中文 `zh`、英文 `en`、日语 `ja`），
 * 并动态覆盖 Context 的 [Configuration] 配置以确保资源字符串 (`strings.xml`) 能够无缝即时更新。
 */
object LocaleHelper {

    /**
     * 根据语言代码设置并更新 [Context] 的 Locale 区域语言配置。
     *
     * @param context 原始上下文 Context
     * @param langCode 语言标识符（如 "zh", "en", "ja"）
     * @return 注入新 Locale 配置后的包装上下文 Context
     */
    /**
     * 根据语言代码设置并更新 [Context] 的 Locale 区域语言配置。
     *
     * 教学解析：
     * 1. `Locale.setDefault(locale)`: 改变 JVM 进程级别的默认语言环境。
     * 2. Android 7.0 (API 24)+ 兼容: 旧版直接给 `config.locale` 赋值；API 24+ 引入 `LocaleList` 并必须调用 `context.createConfigurationContext(config)`
     *    返回一个全新封装了目标 Language 资源表的 Context 对象，从而允许在不重启 App 进程的前提下实现动态多语言无缝切换。
     *
     * @param context 原始上下文 Context
     * @param langCode 语言标识符（如 "zh", "en", "ja"）
     * @return 注入新 Locale 配置后的包装上下文 Context
     */
    fun setLocale(context: Context, langCode: String): Context {
        // 根据语言代码识别并选取 Locale 语言常量对象
        val locale = when (langCode) {
            "ja" -> Locale.JAPAN
            "en" -> Locale.ENGLISH
            else -> Locale.SIMPLIFIED_CHINESE
        }
        // 更新 JVM 级别的默认 Locale
        Locale.setDefault(locale)

        val resources = context.resources
        val config = Configuration(resources.configuration)

        // API 24+ (Android N) 兼容处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        // 更新当前 resources 资源句柄的屏幕度量与 Configuration 配置
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        // 生成并返回搭载了目标语言 Configuration 的派生 Context 上下文
        val localizedContext = context.createConfigurationContext(config)
        @Suppress("DEPRECATION")
        localizedContext.resources.updateConfiguration(config, resources.displayMetrics)

        return localizedContext
    }


    /**
     * 获取当前 Context 应用生效的 [Locale] 区域实例。
     */
    fun getLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0] ?: Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale ?: Locale.getDefault()
        }
    }
}

