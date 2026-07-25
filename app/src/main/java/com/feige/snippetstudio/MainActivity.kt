package com.feige.snippetstudio

import android.content.Intent
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
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.AppScaffold
import com.feige.snippetstudio.ui.components.SharePanel
import com.feige.snippetstudio.ui.nav.AppNavGraph
import com.feige.snippetstudio.ui.nav.Screen
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

    /** 从系统分享接收到的文本内容（待导航消费后清空） */
    private var pendingSharedText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 检测系统分享意图 (ACTION_SEND)
        handleShareIntent(intent)

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

            // 【副效应挂起】监听工作区 URI 变化，执行本地文件同步与反向清理。
            // 当 DataStore 加载完成发射真实 URI 时会自动触发，
            // 从而检测到用户在外部文件管理器中删除的文件并清理数据库中的幽灵记录。
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
                // 【主题包裹】根据设置中的 theme 属性（"light" / "dark" / "system"）与 colorTheme 风格应用系统配色方案与 Typography
                SnippetStudioTheme(
                    themeSetting = settings.theme,
                    colorThemeId = settings.colorTheme
                ) {
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

                    // 消费系统分享意图：根据 shareAction 设置决定静默保存或弹出编辑面板
                    var showSharePanel by remember { mutableStateOf(false) }
                    var sharePanelText by remember { mutableStateOf("") }
                    var sharePanelType by remember { mutableStateOf(SnippetType.PROMPT) }

                    LaunchedEffect(pendingSharedText) {
                        val text = pendingSharedText
                        if (!text.isNullOrBlank()) {
                            val detectedType = SnippetType.fromCode(detectShareType(text))
                            if (settings.shareAction == "silent") {
                                // 静默模式：直接保存并提示
                                val snippet = Snippet(
                                    id = "share_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(4)}",
                                    type = detectedType,
                                    title = Snippet.generateDefaultTitle(detectedType),
                                    fileName = "",
                                    content = text,
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis(),
                                    sizeBytes = text.toByteArray().size
                                )
                                appContainer.snippetRepository.saveOrUpdate(snippet, settings.repoTreeUri)
                                showSnackbar(context.getString(R.string.share_saved_silent))
                                pendingSharedText = null
                            } else {
                                // 面板模式：弹出快速编辑面板
                                sharePanelText = text
                                sharePanelType = detectedType
                                showSharePanel = true
                                pendingSharedText = null
                            }
                        }
                    }

                    // 分享快速编辑面板
                    SharePanel(
                        show = showSharePanel,
                        sharedText = sharePanelText,
                        detectedType = sharePanelType,
                        onDismiss = { showSharePanel = false },
                        onConfirm = { title, type ->
                            showSharePanel = false
                            scope.launch {
                                val snippet = Snippet(
                                    id = "share_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(4)}",
                                    type = type,
                                    title = title,
                                    fileName = "",
                                    content = sharePanelText,
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis(),
                                    sizeBytes = sharePanelText.toByteArray().size
                                )
                                appContainer.snippetRepository.saveOrUpdate(snippet, settings.repoTreeUri)
                                showSnackbar(context.getString(R.string.share_saved_silent))
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    /** 解析 ACTION_SEND 意图，提取分享文本 */
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            pendingSharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
    }

    /** 根据分享文本内容特征自动推断片段类型 */
    private fun detectShareType(text: String): String {
        val trimmed = text.trim()
        return when {
            trimmed.contains(Regex("</?[a-zA-Z][\\s\\S]*?>")) && trimmed.contains(Regex("<(html|div|span|p|body|head|a |img )")) -> "html"
            trimmed.contains(Regex("\\b(const|let|var|function|import|export|=>|console\\.log)\\b")) -> "js"
            trimmed.contains(Regex("(?m)^#{1,6}\\s")) || trimmed.contains(Regex("\\*\\*.*\\*\\*")) -> "markdown"
            else -> "prompt"
        }
    }
}

