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
import androidx.compose.ui.unit.sp
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
        Box(modifier = Modifier.fillMaxSize()) {
            // ===== 1. 全量页面主内容全屏呈现（背景贯穿屏幕最底部，消灭白块断层） =====
            content(PaddingValues(0.dp))

            // ===== 2. 方案 A 悬浮胶囊底栏 (Overlay 绝对定位在最顶层底部 Alignment.BottomCenter) =====
            if (showBottomBar) {
                FloatingDock(
                    currentRoute = currentRoute,
                    onNavigate = { targetRoute ->
                        if (currentRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onNewClick = { showNewSheet = true },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

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

/**
 * [FloatingDock] 方案 A：现代纯图标悬浮胶囊底栏组件 (Modern Icon-only Floating Dock)。
 *
 * 视觉与交互重构亮点：
 * 1. 外层 Box 配合 [navigationBarsPadding] 沉浸避开 Android 底部导航条手势区。
 * 2. 悬浮容器采用高圆角 32.dp、95% 半透明磨砂背景 [tc.surface] 与 10.dp 柔和弥散阴影。
 * 3. 底栏高度提升至 64.dp，触控胶囊按键加高至 44.dp（符合 48dp 盲操标准），极大地提升手势点击舒适度。
 * 4. 中央“新建(+)”按钮采用立体突显高亮胶囊造型，内嵌按压缩放微动效 (Scale Transition)。
 *
 * @param currentRoute 当前活跃路由路径字符串
 * @param onNavigate 导航路由切换回调
 * @param onNewClick 点击中央新建 (+) 按钮回调
 * @param modifier 外部 Modifier 修饰符
 */
@Composable
fun FloatingDock(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onNewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = tc.surface.copy(alpha = 0.95f),
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, tc.line.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 首页 Tab 导航项
                DockNavItem(
                    selected = (currentRoute == Screen.Home.route),
                    iconRes = R.drawable.ic_home,
                    label = stringResource(R.string.home_title),
                    onClick = { onNavigate(Screen.Home.route) },
                    modifier = Modifier.weight(1f).testTag("tab_home")
                )

                // 2. 文件中心/仓库 Tab 导航项
                DockNavItem(
                    selected = (currentRoute == Screen.Files.route),
                    iconRes = R.drawable.ic_folder,
                    label = stringResource(R.string.files_title),
                    onClick = { onNavigate(Screen.Files.route) },
                    modifier = Modifier.weight(1f).testTag("tab_files")
                )

                // 3. 中央高权重高亮按压新建按键 (+)
                DockNewButton(
                    onClick = onNewClick,
                    modifier = Modifier.weight(1f).testTag("tab_new")
                )

                // 4. 设置 Tab 导航项
                DockNavItem(
                    selected = (currentRoute == Screen.Settings.route),
                    iconRes = R.drawable.ic_settings,
                    label = stringResource(R.string.settings_title),
                    onClick = { onNavigate(Screen.Settings.route) },
                    modifier = Modifier.weight(1f).testTag("tab_settings")
                )
            }
        }
    }
}

/**
 * [DockNavItem] 悬浮胶囊底栏单个纯图标 Tab 按钮组件（加高触控区至 44.dp）。
 *
 * @param selected 是否选中当前路由
 * @param iconRes 矢量图标资源 ID
 * @param label 按钮无障碍描述标签
 * @param onClick 点击交互回调
 * @param modifier 外部 Modifier 修饰符
 */
@Composable
fun DockNavItem(
    selected: Boolean,
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    val contentColor = if (selected) tc.primary else tc.text2.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (selected) tc.primarySoft else Color.Transparent,
            modifier = Modifier.size(width = 52.dp, height = 44.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * [DockNewButton] 悬浮胶囊底栏中央立体凸起【新建(+)】微动效交互按键（加高至 44.dp）。
 *
 * @param onClick 点击弹出 2x2 选择面板回调
 * @param modifier 外部 Modifier 修饰符
 */
@Composable
fun DockNewButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 点击按压时的微缩物理弹性动画 (0.92f 缩放)
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        label = "dockNewPressScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = tc.primary,
            shadowElevation = AppElevation.Sm,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .size(width = 52.dp, height = 44.dp)
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "New Snippet",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


