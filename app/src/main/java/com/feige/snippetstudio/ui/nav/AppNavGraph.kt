package com.feige.snippetstudio.ui.nav

import androidx.compose.runtime.Composable
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
import com.feige.snippetstudio.ui.home.HomeScreen
import com.feige.snippetstudio.ui.home.HomeViewModel
import com.feige.snippetstudio.ui.settings.SettingsScreen
import com.feige.snippetstudio.ui.settings.SettingsViewModel
import com.feige.snippetstudio.ui.subpage.SubPageScreen
import com.feige.snippetstudio.ui.subpage.SubPageViewModel

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
        modifier = modifier
    ) {
        // Home Screen
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(appContainer.snippetRepository)
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

        // Files Screen
        composable(Screen.Files.route) {
            val viewModel: FilesViewModel = viewModel(
                factory = FilesViewModel.factory(appContainer.snippetRepository)
            )
            FilesScreen(
                viewModel = viewModel,
                onNavigateToDetail = { id -> navController.navigate(Screen.Detail.of(id)) },
                onNavigateToNewEditor = { type -> navController.navigate(Screen.Editor.new(type)) },
                onShowSnackbar = onShowSnackbar
            )
        }

        // Settings Screen
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

        // Editor Screen
        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("type") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "html"
                }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: "new"
            val type = backStackEntry.arguments?.getString("type") ?: "html"

            val viewModel: EditorViewModel = viewModel(
                factory = EditorViewModel.factory(
                    snippetId = id,
                    initialTypeStr = type,
                    snippetRepository = appContainer.snippetRepository,
                    settingsRepository = appContainer.settingsRepository
                )
            )
            EditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar
            )
        }

        // Detail Screen
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""

            val viewModel: DetailViewModel = viewModel(
                factory = DetailViewModel.factory(id, appContainer.snippetRepository)
            )
            DetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToEditor = { targetId -> navController.navigate(Screen.Editor.edit(targetId)) },
                onShowSnackbar = onShowSnackbar
            )
        }

        // SubPage Screen
        composable(
            route = Screen.SubPage.route,
            arguments = listOf(navArgument("key") { type = NavType.StringType })
        ) { backStackEntry ->
            val key = backStackEntry.arguments?.getString("key") ?: ""

            val viewModel: SubPageViewModel = viewModel(
                factory = SubPageViewModel.factory(
                    key = key,
                    settingsRepository = appContainer.settingsRepository,
                    snippetRepository = appContainer.snippetRepository
                )
            )
            SubPageScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar
            )
        }
    }
}
