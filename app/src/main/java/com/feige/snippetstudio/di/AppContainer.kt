package com.feige.snippetstudio.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.feige.snippetstudio.data.local.AppDatabase
import com.feige.snippetstudio.data.local.SettingsDataStore
import com.feige.snippetstudio.data.repo.SettingsRepository
import com.feige.snippetstudio.data.repo.SnippetRepository

import com.feige.snippetstudio.data.git.GitManager

/**
 * [AppContainer] 是应用的手动依赖注入容器 (Manual Dependency Injection Container)。
 *
 * 架构原理：
 * 1. 控制反转 (IoC): 将对象的创建与依赖生命周期从使用方 (ViewModel/Screen) 剥离，统一在 AppContainer 中管理。
 * 2. 惰性单例 (by lazy): 采用 Kotlin 默认的线程安全双重检查锁 (Synchronized LazyThreadSafetyMode.SYNCHRONIZED)，
 *    只有在第一次访问该属性时才在内存中进行对象构建，之后直接复用，避免启动时一次性加载过多密集资源。
 *
 * @param context 应用上下文对象 Application Context (使用 applicationContext 避免内存泄漏)
 */
class AppContainer(private val context: Context) {

    /**
     * Room 数据库单例。首次调用时通过 [AppDatabase.create] 构建 SQLite 数据库句柄。
     */
    val database: AppDatabase by lazy {
        AppDatabase.create(context)
    }

    /**
     * JGit 版本控制与物理仓同步管理器。在应用程序沙盒 FilesDir 目录中操作本地 Git 库。
     */
    val gitManager: GitManager by lazy {
        GitManager(context)
    }

    /**
     * 代码片段业务仓库 (Snippet Repository)。
     * 依赖组合：注入 `database.snippetDao()` 与 `database.folderDao()` 数据访问对象、`context` 上下文及 `gitManager` 底层组件。
     */
    val snippetRepository: SnippetRepository by lazy {
        SnippetRepository(
            snippetDao = database.snippetDao(),
            folderDao = database.folderDao(),
            context = context,
            gitManager = gitManager
        )
    }

    /**
     * DataStore Preferences 异步持久化流数据源。
     */
    val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(context)
    }

    /**
     * 全局设置偏好仓库 (Settings Repository)。
     * 依赖组合：注入 `settingsDataStore` 作为唯一可信数据源。
     */
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(settingsDataStore)
    }
}

/**
 * [LocalAppContainer] 用于在 Compose 节点树中向上查找或向下共享 [AppContainer] 的静态 CompositionLocal。
 *
 * 教学解析：
 * `staticCompositionLocalOf` 与 `compositionLocalOf` 的区别：
 * - `staticCompositionLocalOf`: 适用于改变频率极低或几乎不变的值（如全局依赖容器、系统配置）。改变时会导致整棵 Compose 节点树重新读取。
 * - `compositionLocalOf`: 适用于频繁改变的值，只会使读取了该值的局部组件触发重组。
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer 未在 CompositionLocalProvider 中提供！请检查 SnippetStudioApp 初始化。")
}


