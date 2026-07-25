package com.feige.snippetstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.feige.snippetstudio.ui.AppScaffold
import com.feige.snippetstudio.ui.nav.AppNavGraph
import com.feige.snippetstudio.ui.theme.SnippetStudioTheme
import com.feige.snippetstudio.util.LocaleHelper
import kotlinx.coroutines.launch

/**
 * [MainActivity] 是应用程序的主 Activity，继承自 [ComponentActivity]。
 *
 * 架构职责：
 * 1. 单 Activity 架构 (Single-Activity Architecture) 入口。
 * 2. 启用 Android 全屏无边框 / 沉浸式边缘到边缘 (Edge-to-Edge) 布局。
 * 3. 响应全局应用设置变动 (语言 Locale、主题样式 Theme、本地代码仓库路径 Repo Tree URI)。
 * 4. 建立顶层 Compose 关联（提供 Context、ActivityResultRegistryOwner、Theme、Scaffold 及 Navigation 路由图）。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 调用 ComponentActivity 父类的 onCreate 生命周期回调方法，传入系统保存的状态 Bundle 实例
        super.onCreate(savedInstanceState)

        // 启用 Android 13+ / 现代全屏边到边体验 (Edge-to-Edge)，使内容延伸到状态栏和导航栏下方
        enableEdgeToEdge()

        // 强转 Application 为 SnippetStudioApp 实例，并获取全局单例依赖容器 container (包含了 Repository、Database 等)
        val appContainer = (application as SnippetStudioApp).container

        // setContent 是 Jetpack Compose 的入口扩展函数，用于将 Compose 组件节点树绑定渲染到 Activity 窗口中
        setContent {
            // 【数据流监听】通过 collectAsState 将 Flow 转为 Compose 响应式 State。
            // 当 DataStore 中存取设置改动时（如深色模式或语言），此处 settings 会自动重组 (Recomposition)
            val settings by appContainer.settingsRepository.settingsFlow.collectAsState(
                initial = com.feige.snippetstudio.model.AppSettings()
            )

            // 获取当前 Compose 组合环境中的 Context 实例对象
            val context = LocalContext.current

            // 【副效应挂起】LaunchedEffect 会在其 key (settings.repoTreeUri) 发生变化时在协程作用域中自动执行内部闭包。
            // 当用户在设置中选定了新的本地物理磁盘文件夹路径时，自动触发后台线程的本地文件同步
            LaunchedEffect(settings.repoTreeUri) {
                appContainer.snippetRepository.syncWithLocalRepository(context, settings.repoTreeUri)
            }

            // 【上下文重塑】remember 用于缓存计算结果。当 settings.lang 改变时重新生成并应用新的 Locale 上下文对象
            val localeContext = remember(settings.lang) {
                LocaleHelper.setLocale(this@MainActivity, settings.lang)
            }

            // 【全局环境隐式传递】CompositionLocalProvider 允许将全局状态（如重载语言后的 LocalContext 与 Activity 注册句柄）
            // 隐式向下传递给整棵 Compose UI 树中所有的子组件，无需逐层显式传递参数
            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                // 【主题包裹】根据设置中的 theme 属性（"light" / "dark" / "system"）应用系统配色方案与 Typography
                SnippetStudioTheme(themeSetting = settings.theme) {
                    // 创建并记住 Jetpack Navigation 路由控制器
                    val navController = rememberNavController()
                    // 状态对象：管理底层浮动 Snackbar 消息提示弹窗
                    val snackbarHostState = remember { SnackbarHostState() }
                    // 获取结合 Compose 重组生命周期的协程作用域 (CoroutineScope)
                    val scope = rememberCoroutineScope()

                    // 【全局高阶函数闭包】传递给任意子页面的消息弹窗触发闭包，使用 scope.launch 发起非阻塞异步调用
                    val showSnackbar: (String) -> Unit = { message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }

                    // 【应用主脚手架 Layout】包含 BottomBar 底部导航栏与 2x2 ModalBottomSheet 动态新建模态框
                    AppScaffold(
                        navController = navController,
                        snackbarHostState = snackbarHostState
                    ) { innerPadding ->
                        // 【全局路由图 Host】接管页面导航与 ViewModelFactory 实例映射，并传入系统 SafeDrawing 边距
                        AppNavGraph(
                            navController = navController,
                            appContainer = appContainer,
                            onShowSnackbar = showSnackbar,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

}

