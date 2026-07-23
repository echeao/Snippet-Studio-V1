package com.feige.snippetstudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.components.TypeIcon
import com.feige.snippetstudio.ui.nav.Screen
import com.feige.snippetstudio.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    content: @Composable (PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val showBottomBarAndFab = currentRoute in listOf(
        Screen.Home.route,
        Screen.Files.route,
        Screen.Settings.route
    )

    var showNewSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    val isDark = LocalIsDarkTheme.current
    val barBg = if (isDark) SurfaceDark else SurfaceLight
    val borderColor = if (isDark) LineDark else LineLight

    Scaffold(
        bottomBar = {
            if (showBottomBarAndFab) {
                NavigationBar(
                    containerColor = barBg,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.border(1.dp, borderColor)
                ) {
                    NavigationBarItem(
                        selected = (currentRoute == Screen.Home.route),
                        onClick = {
                            if (currentRoute != Screen.Home.route) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == Screen.Home.route) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text(stringResource(R.string.home_title)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            indicatorColor = PrimarySoft
                        ),
                        modifier = Modifier.testTag("tab_home")
                    )

                    NavigationBarItem(
                        selected = (currentRoute == Screen.Files.route),
                        onClick = {
                            if (currentRoute != Screen.Files.route) {
                                navController.navigate(Screen.Files.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == Screen.Files.route) Icons.Filled.Folder else Icons.Outlined.Folder,
                                contentDescription = "Files"
                            )
                        },
                        label = { Text(stringResource(R.string.files_title)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            indicatorColor = PrimarySoft
                        ),
                        modifier = Modifier.testTag("tab_files")
                    )

                    NavigationBarItem(
                        selected = (currentRoute == Screen.Settings.route),
                        onClick = {
                            if (currentRoute != Screen.Settings.route) {
                                navController.navigate(Screen.Settings.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == Screen.Settings.route) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text(stringResource(R.string.settings_title)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            indicatorColor = PrimarySoft
                        ),
                        modifier = Modifier.testTag("tab_settings")
                    )
                }
            }
        },
        floatingActionButton = {
            if (showBottomBarAndFab) {
                FloatingActionButton(
                    onClick = { showNewSheet = true },
                    containerColor = Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(R_MD),
                    modifier = Modifier.testTag("fab_new")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New Snippet",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isDark) SurfaceDark else TextLight,
                    contentColor = Color.White,
                    shape = AppShapes.small
                )
            }
        }
    ) { innerPadding ->
        content(innerPadding)

        // ModalBottomSheet for creating a new snippet (2x2 grid)
        if (showNewSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNewSheet = false },
                sheetState = sheetState,
                containerColor = if (isDark) SurfaceDark else SurfaceLight,
                shape = RoundedCornerShape(topStart = R_XL, topEnd = R_XL)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.S5)
                ) {
                    Text(
                        text = stringResource(R.string.sheet_new_title),
                        style = SectionTitleStyle,
                        color = if (isDark) TextDark else TextLight
                    )

                    Spacer(modifier = Modifier.height(Spacing.S4))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
                    ) {
                        NewSheetTypeItem(
                            type = SnippetType.HTML,
                            onClick = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showNewSheet = false
                                    navController.navigate(Screen.Editor.new(SnippetType.HTML.code))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        NewSheetTypeItem(
                            type = SnippetType.JS,
                            onClick = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showNewSheet = false
                                    navController.navigate(Screen.Editor.new(SnippetType.JS.code))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.S3))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
                    ) {
                        NewSheetTypeItem(
                            type = SnippetType.MARKDOWN,
                            onClick = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showNewSheet = false
                                    navController.navigate(Screen.Editor.new(SnippetType.MARKDOWN.code))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        NewSheetTypeItem(
                            type = SnippetType.PROMPT,
                            onClick = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showNewSheet = false
                                    navController.navigate(Screen.Editor.new(SnippetType.PROMPT.code))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.S4))

                    OutlinedButton(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showNewSheet = false }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.medium
                    ) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            }
        }
    }
}

@Composable
fun NewSheetTypeItem(
    type: SnippetType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Surface2Dark else Surface2Light
    val textPrimary = if (isDark) TextDark else TextLight

    Surface(
        modifier = modifier
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD))
            .clickable(onClick = onClick)
            .testTag("sheet_type_${type.code}"),
        shape = RoundedCornerShape(R_MD),
        color = cardBg
    ) {
        Row(
            modifier = Modifier.padding(Spacing.S4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TypeIcon(type = type, size = 40.dp)
            Spacer(modifier = Modifier.width(Spacing.S3))
            Text(
                text = type.displayName,
                style = ListTitleStyle,
                color = textPrimary
            )
        }
    }
}
