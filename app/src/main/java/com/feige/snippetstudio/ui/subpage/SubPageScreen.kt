package com.feige.snippetstudio.ui.subpage

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.ConfirmDialog
import com.feige.snippetstudio.ui.subpage.components.*
import com.feige.snippetstudio.ui.theme.*

/**
 * [SubPageScreen] 系统二级功能通用路由子页面视图组件。
 *
 * 架构重构亮点：
 * 1. 原 60KB 超长单文件解耦拆分为 [CategorySubPage]、[TagManagementSubPage]、[TrashSubPage]、[GitSubPage] 与 [GitLogSubPage] 五大独立业务子模块。
 * 2. 基于 [SubPageUiState.key] 分派特定路由对应的二层子页面 UI。
 * 3. 集成 SAF 本地磁盘目录选取与全局主题、多语言环境动态切换能力。
 * 4. 补充全量简体中文 KDoc 注释，提升代码维护性。
 *
 * @param viewModel 子页面 ViewModel 依赖
 * @param onBack 返回上级页面闭包
 * @param onShowSnackbar 底部消息提示闭包
 * @param onNavigateToSubPage 内部二层路由跳转闭包
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubPageScreen(
    viewModel: SubPageViewModel,
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onNavigateToSubPage: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tc = LocalThemeColors.current

    var pendingPurgeId by remember { mutableStateOf<String?>(null) }

    // ===== SAF 本地磁盘目录选择器 Launcher (OpenDocumentTree) =====
    val openTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val pathName = uri.lastPathSegment ?: uri.toString()
                viewModel.updateRepoPath(context, pathName, uri.toString())
                onShowSnackbar(context.getString(R.string.toast_saved))
            } catch (e: Exception) {
                val pathName = uri.lastPathSegment ?: uri.toString()
                viewModel.updateRepoPath(context, pathName, uri.toString())
                onShowSnackbar("Updated path: ${uri.lastPathSegment}")
            }
        }
    }

    val pageTitle = when (uiState.key) {
        "repo" -> stringResource(R.string.set_repo)
        "git" -> stringResource(R.string.set_git)
        "gitlog" -> "Git Log"
        "cat" -> stringResource(R.string.set_cat)
        "tags" -> stringResource(R.string.set_tags)
        "trash" -> stringResource(R.string.set_trash)
        "lang" -> stringResource(R.string.set_lang)
        "theme" -> stringResource(R.string.set_color_theme)
        else -> stringResource(R.string.settings_title)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("subpage_back_btn")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = tc.text
                        )
                    }
                },
                title = {
                    Text(
                        text = pageTitle,
                        style = SectionTitleStyle,
                        color = tc.text
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tc.bg)
            )
        },
        containerColor = tc.bg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.key) {
                // 子页面 1: SAF 本地工作区仓库选定
                "repo" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.S4),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S4)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(AppElevation.Sm, RoundedCornerShape(R_MD))
                                .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
                            shape = RoundedCornerShape(R_MD),
                            color = tc.surface
                        ) {
                            Column(modifier = Modifier.padding(Spacing.S4)) {
                                Text(stringResource(R.string.set_repo_cur), style = CaptionStyle, color = tc.text2)
                                Spacer(modifier = Modifier.height(Spacing.S2))
                                Text(
                                    text = uiState.settings.repoPath,
                                    style = ListTitleStyle,
                                    color = tc.text,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(Spacing.S4))
                                Button(
                                    onClick = { openTreeLauncher.launch(null) },
                                    shape = AppShapes.small,
                                    colors = ButtonDefaults.buttonColors(containerColor = tc.primary)
                                ) {
                                    Text(stringResource(R.string.sub_repo_change))
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.sub_repo_hint),
                            style = BodyStyle,
                            color = tc.text2,
                            modifier = Modifier.padding(horizontal = Spacing.S2)
                        )
                    }
                }

                // 子页面 2: Git 远程配置与同步
                "git" -> {
                    GitSubPage(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateToGitLog = { onNavigateToSubPage("gitlog") },
                        onShowSnackbar = onShowSnackbar
                    )
                }

                // 子页面 2.5: Git Log 提交历史
                "gitlog" -> {
                    GitLogSubPage(uiState = uiState)
                }

                // 子页面 3: 语言分类数量统计
                "cat" -> {
                    CategorySubPage(categoryCounts = uiState.categoryCounts)
                }

                // 子页面 4: 全局预设标签管理
                "tags" -> {
                    TagManagementSubPage(
                        globalTags = uiState.tags,
                        onAddTag = { viewModel.addGlobalTag(it) },
                        onDeleteTag = { viewModel.removeGlobalTag(it) },
                        onShowSnackbar = onShowSnackbar
                    )
                }

                // 子页面 5: 回收站软删除列表
                "trash" -> {
                    TrashSubPage(
                        trashItems = uiState.trashedSnippets,
                        onRestore = { item ->
                            viewModel.restoreSnippet(item.id)
                            onShowSnackbar(context.getString(R.string.toast_restored))
                        },
                        onPurge = { id -> viewModel.purgeSnippet(id) },
                        onShowSnackbar = onShowSnackbar
                    )
                }

                // 子页面 6: 界面语言设置
                "lang" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.S4),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S3)
                    ) {
                        listOf(
                            "zh" to stringResource(R.string.lang_zh),
                            "ja" to stringResource(R.string.lang_ja),
                            "en" to stringResource(R.string.lang_en)
                        ).forEach { (code, label) ->
                            val isSelected = uiState.settings.lang == code
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (isSelected) tc.primary else tc.line, RoundedCornerShape(R_MD))
                                    .clickable {
                                        viewModel.setLanguage(context, code)
                                        onShowSnackbar(context.getString(R.string.toast_saved))
                                    },
                                shape = RoundedCornerShape(R_MD),
                                color = if (isSelected) tc.primarySoft else tc.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.S4),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = ListTitleStyle,
                                        color = if (isSelected) tc.primary else tc.text
                                    )
                                    if (isSelected) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_check),
                                            contentDescription = "Selected",
                                            tint = tc.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 子页面 7: 全局配色主题切换
                "theme" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.S4),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S3)
                    ) {
                        ColorThemeStyle.entries.forEach { style ->
                            val isSelected = uiState.settings.colorTheme == style.id
                            val palette = ColorThemeRegistry.paletteOf(style)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isSelected) palette.primary else tc.line,
                                        RoundedCornerShape(R_MD)
                                    )
                                    .clickable {
                                        viewModel.setColorTheme(style.id)
                                        onShowSnackbar(context.getString(R.string.toast_saved))
                                    },
                                shape = RoundedCornerShape(R_MD),
                                color = if (isSelected) palette.primarySoft.copy(alpha = 0.3f) else tc.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.S4),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                                                drawCircle(palette.primary)
                                            }
                                            androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                                                drawCircle(palette.primary2)
                                            }
                                            androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                                                drawCircle(palette.bgDark)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(Spacing.S3))
                                        Text(
                                            text = style.displayName,
                                            style = ListTitleStyle,
                                            color = if (isSelected) palette.primary else tc.text
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_check),
                                            contentDescription = "Selected",
                                            tint = palette.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 彻底删除确认框
        ConfirmDialog(
            show = (pendingPurgeId != null),
            title = stringResource(R.string.confirm_purge_title),
            desc = stringResource(R.string.confirm_purge_desc),
            onConfirm = {
                pendingPurgeId?.let { id ->
                    viewModel.purgeSnippet(id)
                    onShowSnackbar("Deleted forever")
                }
            },
            onDismiss = { pendingPurgeId = null },
            isDanger = true
        )
    }
}
