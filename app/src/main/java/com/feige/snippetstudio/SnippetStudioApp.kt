package com.feige.snippetstudio

import android.app.Application
import com.feige.snippetstudio.di.AppContainer

/**
 * [SnippetStudioApp] 是整个应用程序的入口基类 (Application Class)。
 *
 * 职责与作用：
 * 1. 在 Android 应用进程启动时最先被创建。
 * 2. 负责全局生命周期级别的初始化工作，例如实例化手动的依赖注入容器 [AppContainer]。
 * 3. 作为全局单例保存依赖容器实例 [container]，供 Activity、ViewModel 或组件获取所需的数据仓库 (Repository)。
 */
class SnippetStudioApp : Application() {

    /**
     * 全局依赖注入容器 (Manual Dependency Injection Container)。
     * [AppContainer] 保存了应用生命周期内唯一的数据库实例、DataStore、Git 管理对象及 Repository 仓库。
     * 使用 `lateinit` 延迟初始化，在 [onCreate] 中真正实例化。
     * setter 设为 `private` 以防止外部恶意修改容器引用。
     */
    lateinit var container: AppContainer
        private set

    /**
     * 当应用进程启动时调用此生命周期回调方法。
     * 在这里完成 [container] 的创建，确保依赖对象在后续界面或组件使用前准备就绪。
     */
    override fun onCreate() {
        super.onCreate()
        // 初始化手动依赖注入容器，传入当前 Application 上下文
        container = AppContainer(this)
    }
}

