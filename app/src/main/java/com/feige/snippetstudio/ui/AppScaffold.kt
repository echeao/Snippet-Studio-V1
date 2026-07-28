package com.feige.snippetstudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * [AppScaffold] 应用程序顶层主脚手架组件。
 *
 * 架构职责：
 * 1. 负责呈现应用程序的底部导航栏 [NavigationBar]（包含【首页】、【文件/仓库】与【设置】三个主页面路由）。
 * 2. 在主页面路由中呈现全局悬浮新建按钮 [FloatingActionButton] (FAB)。
 * 3. 弹出新建代码片段底栏 [ModalBottomSheet]，提供 2x2 网格让用户选择创建 HTML、JS、Markdown 或 Prompt。
 * 4. 承载全局消息提示弹框 [SnackbarHost]。
 *
 * @param navController Compose 导航控制器
 * @param snackbarHostState Snackbar 消息状态对象
 * @param content 包裹的具体 Route 页面内容视图
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    content: @Composable (PaddingValues) -> Unit
) {
    // 监听当前导航路由
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    // 控制是否在当前页面显示底部导航栏（仅在首页、文件/仓库与设置主界面中呈现）
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Files.route,
        Screen.Settings.route
    )

    var showNewSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    val tc = LocalThemeColors.current
    val barBg = tc.surface
    val borderColor = tc.line

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            // ===== 底部集成了【新建(+)】高权重操作按钮的 NavigationBar =====
            if (showBottomBar) {
                NavigationBar(
                    containerColor = barBg,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.border(1.dp, borderColor)
                ) {
                    // 1. 首页导航按钮
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
                                painter = painterResource(id = R.drawable.ic_home),
                                contentDescription = "Home"
                            )
                        },
                        label = { Text(stringResource(R.string.home_title)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = tc.primary,
                            selectedTextColor = tc.primary,
                            indicatorColor = tc.primarySoft
                        ),
                        modifier = Modifier.testTag("tab_home")
                    )

                    // 2. 文件与仓库导航按钮
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
                                painter = painterResource(id = R.drawable.ic_folder),
                                contentDescription = "Files"
                            )
                        },
                        label = { Text(stringResource(R.string.files_title)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = tc.primary,
                            selectedTextColor = tc.primary,
                            indicatorColor = tc.primarySoft
                        ),
                        modifier = Modifier.testTag("tab_files")
                    )

                    // 3. 中央高视觉权重嵌入式新建 (+) 按钮（高亮主色胶囊造型，对齐底部导航栏）
                    NavigationBarItem(
                        selected = false,
                        onClick = { showNewSheet = true },
                        icon = {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = tc.primary,
                                shadowElevation = AppElevation.Sm,
                                modifier = Modifier
                                    .size(width = 48.dp, height = 32.dp)
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_plus),
                                        contentDescription = "New Snippet",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.sheet_new_title),
                                color = tc.primary,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("tab_new")
                    )

                    // 4. 设置导航按钮
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
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text(stringResource(R.string.settings_title)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = tc.primary,
                            selectedTextColor = tc.primary,
                            indicatorColor = tc.primarySoft
                        ),
                        modifier = Modifier.testTag("tab_settings")
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (tc.isDark) tc.surface else tc.text,
                    contentColor = Color.White,
                    shape = AppShapes.small
                )
            }
        }
    ) { innerPadding ->
        content(innerPadding)

        // ===== 点击底部嵌入新建按钮弹出的代码片段类型选择 BottomSheet (2x2 网格) =====
        if (showNewSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNewSheet = false },
                sheetState = sheetState,
                containerColor = tc.surface,
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
                        color = tc.text
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



/**
 * [NewSheetTypeItem] 底栏弹出窗中单个新建类型的选择卡片项组件。
 */
@Composable
fun NewSheetTypeItem(
    type: SnippetType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Surface(
        modifier = modifier
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD))
            .clickable(onClick = onClick)
            .testTag("sheet_type_${type.code}"),
        shape = RoundedCornerShape(R_MD),
        color = tc.surface2
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
                color = tc.text
            )
        }
    }
}

