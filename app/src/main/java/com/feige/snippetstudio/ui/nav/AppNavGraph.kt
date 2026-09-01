package com.feige.snippetstudio.ui.nav

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.feige.snippetstudio.di.AppContainer
import com.feige.snippetstudio.ui.detail.DetailScreen
import com.feige.snippetstudio.ui.detail.DetailViewModel
import com.feige.snippetstudio.ui.editor.EditorScreen
import com.feige.snippetstudio.ui.editor.EditorViewModel
import com.feige.snippetstudio.ui.files.FilesScreen
import com.feige.snippetstudio.ui.files.FilesViewModel
import com.feige.snippetstudio.ui.history.HistoryScreen
import com.feige.snippetstudio.ui.history.HistoryViewModel
import com.feige.snippetstudio.ui.preview.TempFilePreviewScreen
import com.feige.snippetstudio.ui.home.HomeScreen
import com.feige.snippetstudio.ui.home.HomeViewModel
import com.feige.snippetstudio.ui.settings.SettingsScreen
import com.feige.snippetstudio.ui.settings.SettingsViewModel
import com.feige.snippetstudio.ui.subpage.SubPageScreen
import com.feige.snippetstudio.ui.subpage.SubPageViewModel

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing

// ===== 全局页面转场动画常量与辅助函数 =====

/** 转场动画时长（进入 300ms，层叠平缓就位） */
private const val ENTER_DURATION = 300
/** 转场动画时长（退出 220ms，干净利落退出） */
private const val EXIT_DURATION = 220

/** 默认进入转场：Material 3 物理减速淡入 + 从右侧轻柔滑入 */
private fun defaultEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(ENTER_DURATION, easing = FastOutSlowInEasing)) +
            slideInHorizontally(animationSpec = tween(ENTER_DURATION, easing = FastOutSlowInEasing)) { (it * 0.12f).toInt() }

/** 默认退出转场：Material 3 线性加速淡出 + 向左侧微幅滑出 */
private fun defaultExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(EXIT_DURATION, easing = FastOutLinearInEasing)) +
            slideOutHorizontally(animationSpec = tween(EXIT_DURATION, easing = FastOutLinearInEasing)) { -(it * 0.12f).toInt() }

/** 默认 Pop 进入转场：淡入 + 从左侧轻柔滑入 */
private fun defaultPopEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(ENTER_DURATION, easing = FastOutSlowInEasing)) +
            slideInHorizontally(animationSpec = tween(ENTER_DURATION, easing = FastOutSlowInEasing)) { -(it * 0.12f).toInt() }

/** 默认 Pop 退出转场：淡出 + 向右侧轻柔滑出 */
private fun defaultPopExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(EXIT_DURATION, easing = FastOutLinearInEasing)) +
            slideOutHorizontally(animationSpec = tween(EXIT_DURATION, easing = FastOutLinearInEasing)) { (it * 0.12f).toInt() }

/**
 * [AppNavGraph] 应用程序的中心导航路由图 (Navigation Host Graph)。
 *
 * 职责：
 * 1. 声明 [NavHost]，管理 6 个主/子页面路线 (Home, Files, Settings, Editor, Detail, SubPage)。
 * 2. 在每个路线页面定义处，通过 自定义 ViewModel.factory 将 [AppContainer] 中的依赖对象注入对应的 ViewModel。
 * 3. 闭包式连接各 Screen 之间的跳转动作 (`navController.navigate(...)`) 与页面返回动作 (`navController.popBackStack()`)。
 *
 * @param navController 导航控制器
 * @param appContainer 依赖注入容器
 * @param onShowSnackbar 全局显示 Snackbar 提示函数
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    appContainer: AppContainer,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = { defaultEnterTransition() },
        exitTransition = { defaultExitTransition() },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() }
    ) {
        // ===== 1. 首页 (Home Screen) =====
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(
                    appContainer.snippetRepository,
                    appContainer.settingsRepository
                )
            )
            HomeScreen(
                viewModel = viewModel,
                onNavigateToEditor = { id -> navController.navigate(Screen.Editor.edit(id)) },
                onNavigateToNewEditor = { type -> navController.navigate(Screen.Editor.new(type)) },
                onNavigateToDetail = { id -> navController.navigate(Screen.Detail.of(id)) },
                onNavigateToFiles = { navController.navigate(Screen.Files.route) },
                onShowSnackbar = onShowSnackbar
            )
        }

        // ===== 2. 文件与仓库管理页 (Files Screen) =====
        composable(Screen.Files.route) {
            val viewModel: FilesViewModel = viewModel(
                factory = FilesViewModel.factory(
                    appContainer.snippetRepository,
                    appContainer.settingsRepository
                )
            )
            FilesScreen(
                viewModel = viewModel,
                onNavigateToDetail = { id -> navController.navigate(Screen.Detail.of(id)) },
                onNavigateToEditor = { id -> navController.navigate(Screen.Editor.edit(id)) },
                onNavigateToNewEditor = { type -> navController.navigate(Screen.Editor.new(type)) },
                onShowSnackbar = onShowSnackbar
            )
        }

        // ===== 3. 全局设置页 (Settings Screen) =====
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(
                    appContainer.settingsRepository,
                    appContainer.snippetRepository
                )
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToSubPage = { key -> navController.navigate(Screen.SubPage.of(key)) },
                onShowSnackbar = onShowSnackbar
            )
        }

        // ===== 4. 代码编辑器页面 (Editor Screen) =====
        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("type") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "html"
                },
                navArgument("shared") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: "new"
            val type = backStackEntry.arguments?.getString("type") ?: "html"
            val sharedText = backStackEntry.arguments?.getString("shared")?.let {
                try { java.net.URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { null }
            }

            val viewModel: EditorViewModel = viewModel(
                factory = EditorViewModel.factory(
                    snippetId = id,
                    initialTypeStr = type,
                    snippetRepository = appContainer.snippetRepository,
                    settingsRepository = appContainer.settingsRepository,
                    sharedText = sharedText
                )
            )
            EditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar,
                onNavigateToHistory = { id -> navController.navigate(Screen.History.of(id)) }
            )
        }

        // ===== 5. 代码片段详情页面 (Detail Screen) =====
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""

            val viewModel: DetailViewModel = viewModel(
                factory = DetailViewModel.factory(
                    snippetId = id,
                    repository = appContainer.snippetRepository,
                    settingsRepository = appContainer.settingsRepository
                )
            )
            DetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToEditor = { targetId -> navController.navigate(Screen.Editor.edit(targetId)) },
                onShowSnackbar = onShowSnackbar,
                onNavigateToHistory = { id -> navController.navigate(Screen.History.of(id)) }
            )
        }

        // ===== 6. 设置子功能页面 (SubPage Screen, 如 Git 配置页 / 关于页) =====
        composable(
            route = Screen.SubPage.route,
            arguments = listOf(navArgument("key") { type = NavType.StringType })
        ) { backStackEntry ->
            val key = backStackEntry.arguments?.getString("key") ?: ""

            val viewModel: SubPageViewModel = viewModel(
                factory = SubPageViewModel.factory(
                    key = key,
                    settingsRepository = appContainer.settingsRepository,
                    snippetRepository = appContainer.snippetRepository,
                    gitManager = appContainer.gitManager
                )
            )
            SubPageScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar,
                onNavigateToSubPage = { targetKey -> navController.navigate(Screen.SubPage.of(targetKey)) }
            )
        }

        // ===== 7. 分享文件临时预览页面 (FilePreview Screen) =====
        composable(
            route = Screen.FilePreview.route,
            arguments = listOf(
                navArgument("fileName") { type = NavType.StringType; nullable = true; defaultValue = "" },
                navArgument("type") { type = NavType.StringType; defaultValue = "general" }
            )
        ) { backStackEntry ->
            val content = remember { TempPreviewCache.content.also { TempPreviewCache.content = "" } }
            val fileName = backStackEntry.arguments?.getString("fileName")?.let {
                if (it.isBlank()) null else try { java.net.URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { null }
            }
            val type = backStackEntry.arguments?.getString("type") ?: "general"
            TempFilePreviewScreen(
                content = content,
                fileName = fileName,
                typeCode = type,
                onBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar
            )
        }

        // ===== 8. Git 历史履历页面 (History Screen) =====
        composable(
            route = Screen.History.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""

            val viewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.factory(
                    snippetId = id,
                    snippetRepository = appContainer.snippetRepository,
                    gitManager = appContainer.gitManager
                )
            )
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar
            )
        }
    }
}

