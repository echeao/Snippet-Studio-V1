package com.feige.snippetstudio.ui.subpage

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.ui.theme.SyncGreen
import com.feige.snippetstudio.ui.theme.SyncRed
import com.feige.snippetstudio.ui.theme.SyncBlue

/**
 * [SubPageScreen] 系统设置二级通用功能子页面视图。
 *
 * 依据路由中的 [SubPageUiState.key] 分发展示针对性界面：
 * 1. `"repo"`: SAF 本地工作区磁盘目录切换。
 * 2. `"git"`: Git 远程 URL/PAT 设置、初始化与手动双向 Push/Pull 同步。
 * 3. `"cat"`: 代码分类数量统计情况。
 * 4. `"tags"`: 全局常用预设标签管理器（支持实时新增与删除）。
 * 5. `"trash"`: 回收站软删除列表（支持“一键还原”与“永久彻底清除”）。
 * 6. `"lang"`: 多语言环境 (简体中文 / English / 日本語) 切换。
 *
 * @param viewModel 子页面 ViewModel
 * @param onBack 返回上级页面
 * @param onShowSnackbar 底部提示闭包
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    val textPrimary = tc.text
    val textSecondary = tc.text2
    val cardBg = tc.surface
    val borderColor = tc.line
    val Primary = tc.primary
    val PrimarySoft = tc.primarySoft

    var pendingPurgeId by remember { mutableStateOf<String?>(null) }

    // ===== SAF 目录选择器 Launcher (OpenDocumentTree) =====
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.bg
                )
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
                // ===== 子页面 1: SAF 本地磁盘工作区仓库配置 =====
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
                                .border(1.dp, borderColor, RoundedCornerShape(R_MD)),
                            shape = RoundedCornerShape(R_MD),
                            color = cardBg
                        ) {
                            Column(modifier = Modifier.padding(Spacing.S4)) {
                                Text(stringResource(R.string.set_repo_cur), style = CaptionStyle, color = textSecondary)
                                Spacer(modifier = Modifier.height(Spacing.S2))
                                Text(
                                    text = uiState.settings.repoPath,
                                    style = ListTitleStyle,
                                    color = textPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(Spacing.S4))
                                Button(
                                    onClick = { openTreeLauncher.launch(null) },
                                    shape = AppShapes.small,
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    Text(stringResource(R.string.sub_repo_change))
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.sub_repo_hint),
                            style = BodyStyle,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = Spacing.S2)
                        )
                    }
                }

                // ===== 子页面 2: Git 远程通信与版本管理 =====
                "git" -> {
                    // 记忆当前 Git 页面的纵向滚动状态，确保在展开“本地变更”或“代码差异对比”时页面可顺畅滑屏
                    val gitScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(gitScrollState)
                            .padding(Spacing.S4),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S4)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(AppElevation.Sm, RoundedCornerShape(R_MD))
                                .border(1.dp, borderColor, RoundedCornerShape(R_MD)),
                            shape = RoundedCornerShape(R_MD),
                            color = cardBg
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.S4),
                                verticalArrangement = Arrangement.spacedBy(Spacing.S3)
                            ) {
                                OutlinedTextField(
                                    value = uiState.gitUrlInput,
                                    onValueChange = { viewModel.onGitUrlChange(it) },
                                    label = { Text(stringResource(R.string.sub_git_url)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isGitOperating
                                )
                                OutlinedTextField(
                                    value = uiState.gitBranchInput,
                                    onValueChange = { viewModel.onGitBranchChange(it) },
                                    label = { Text(stringResource(R.string.sub_git_branch)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isGitOperating
                                )
                                OutlinedTextField(
                                    value = uiState.gitPatInput,
                                    onValueChange = { viewModel.onGitPatChange(it) },
                                    label = { Text(stringResource(R.string.sub_git_pat)) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isGitOperating
                                )

                                Spacer(modifier = Modifier.height(Spacing.S2))

                                if (uiState.isGitOperating && uiState.syncPreview == null) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Primary)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            viewModel.testGitConnection { success, errorMsg ->
                                                if (success) {
                                                    onShowSnackbar("Git 远程验证通过，本地仓库已初始化！")
                                                } else {
                                                    onShowSnackbar(errorMsg ?: "操作失败")
                                                }
                                            }
                                        },
                                        shape = AppShapes.small,
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !uiState.isGitOperating
                                    ) {
                                        Text("校验连接并初始化/克隆")
                                    }

                                    if (uiState.settings.gitConnected) {
                                        Spacer(modifier = Modifier.height(Spacing.S2))

                                        // 方向分离按钮：Pull / Push（设置紧凑内边距 contentPadding 与单行 maxLines = 1，防止极端屏宽下触发自动折行）
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.previewPull { success, msg ->
                                                        if (!success) onShowSnackbar(msg ?: "预览失败")
                                                    }
                                                },
                                                shape = AppShapes.small,
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                                modifier = Modifier.weight(1f),
                                                enabled = !uiState.isGitOperating && !uiState.isPreviewing
                                            ) {
                                                Text(
                                                    text = "拉取远端 (Pull ↓)",
                                                    fontSize = 13.sp,
                                                    maxLines = 1
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.previewPush { success, msg ->
                                                        if (!success) onShowSnackbar(msg ?: "预览失败")
                                                    }
                                                },
                                                shape = AppShapes.small,
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                                modifier = Modifier.weight(1f),
                                                enabled = !uiState.isGitOperating && !uiState.isPreviewing
                                            ) {
                                                Text(
                                                    text = "推送本地 (Push ↑)",
                                                    fontSize = 13.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        if (uiState.isPreviewing) {
                                            Spacer(modifier = Modifier.height(Spacing.S2))
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    color = Primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Git Log 入口按钮
                                if (uiState.settings.gitConnected) {
                                    Spacer(modifier = Modifier.height(Spacing.S3))
                                    // 加载本地变更按钮
                                    OutlinedButton(
                                        onClick = { viewModel.loadLocalChanges() },
                                        shape = AppShapes.small,
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !uiState.isGitOperating
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_code),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.S2))
                                        Text("查看本地变更 (${uiState.localChanges.size})")
                                    }

                                    // 本地变更文件列表
                                    if (uiState.localChanges.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(Spacing.S2))
                                        uiState.localChanges.forEach { (path, changeType) ->
                                            val isSelected = uiState.selectedDiffPath == path
                                            val (icon, chgColor) = when (changeType) {
                                                "ADDED" -> "+" to SyncGreen
                                                "MODIFIED" -> "~" to SyncBlue
                                                "DELETED" -> "-" to SyncRed
                                                else -> "?" to textSecondary
                                            }
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            if (isSelected) {
                                                                viewModel.closeDiff()
                                                            } else {
                                                                viewModel.loadFileDiff(path)
                                                            }
                                                        },
                                                    color = if (isSelected) PrimarySoft.copy(alpha = 0.3f) else cardBg,
                                                    shape = RoundedCornerShape(R_SM)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = Spacing.S3, vertical = Spacing.S2),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(20.dp)
                                                                .background(chgColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(icon, color = chgColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Spacer(modifier = Modifier.width(Spacing.S2))
                                                        Text(
                                                            text = path,
                                                            style = CaptionStyle,
                                                            color = textPrimary,
                                                            modifier = Modifier.weight(1f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = when (changeType) {
                                                                "ADDED" -> "新增"
                                                                "MODIFIED" -> "修改"
                                                                "DELETED" -> "删除"
                                                                else -> ""
                                                            },
                                                            fontSize = 11.sp,
                                                            color = chgColor
                                                        )
                                                    }
                                                }

                                                // Diff 展开区域
                                                if (isSelected) {
                                                    if (uiState.isDiffLoading) {
                                                        Box(
                                                            modifier = Modifier.fillMaxWidth().padding(Spacing.S3),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            CircularProgressIndicator(
                                                                color = Primary,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    } else if (uiState.currentDiff.isNotEmpty()) {
                                                        DiffViewer(
                                                            diffLines = uiState.currentDiff,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .heightIn(max = 300.dp)
                                                                .padding(start = Spacing.S3, top = Spacing.S1, bottom = Spacing.S1)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "无差异内容",
                                                            style = CaptionStyle,
                                                            color = textSecondary,
                                                            modifier = Modifier.padding(start = Spacing.S3, top = Spacing.S1, bottom = Spacing.S1)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(Spacing.S3))
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.loadGitLog()
                                            onNavigateToSubPage("gitlog")
                                        },
                                        shape = AppShapes.small,
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !uiState.isGitOperating
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_git),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.S2))
                                        Text("查看提交记录 (Git Log)")
                                    }
                                }

                                // 同步预览面板
                                if (uiState.syncPreview != null) {
                                    SyncPreviewSheet(
                                        preview = uiState.syncPreview!!,
                                        syncProgress = uiState.syncProgress,
                                        onResolveConflict = { index, resolution ->
                                            viewModel.resolveConflict(index, resolution)
                                        },
                                        onConfirm = {
                                            viewModel.confirmSync { success, msg ->
                                                onShowSnackbar(msg ?: (if (success) "同步完成" else "同步失败"))
                                            }
                                        },
                                        onCancel = { viewModel.cancelSync() }
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (uiState.settings.gitConnected)
                                "Git 状态: 已连接。修改代码片段时将自动同步至本地 Git 仓并可推送远端。"
                            else
                                "请输入有效的远程仓库 URL 和 Personal Access Token (PAT)，点击“校验连接”即可完成准备。",
                            style = BodyStyle,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = Spacing.S2)
                        )
                    }
                }

                // ===== 子页面 2.5: Git Log 提交历史 =====
                "gitlog" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.S4),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S3)
                    ) {
                        if (uiState.isGitLogLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(Spacing.S5),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Primary)
                            }
                        } else if (uiState.gitLogError != null) {
                            Text(
                                text = uiState.gitLogError!!,
                                style = BodyStyle,
                                color = Color(0xFFE53935),
                                modifier = Modifier.padding(Spacing.S3)
                            )
                        } else if (uiState.gitLogCommits.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(Spacing.S5),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无提交记录", style = BodyStyle, color = textSecondary)
                            }
                        } else {
                            Text(
                                text = "共 ${uiState.gitLogCommits.size} 条提交",
                                style = CaptionStyle,
                                color = textSecondary,
                                modifier = Modifier.padding(horizontal = Spacing.S2)
                            )
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(Spacing.S2)
                            ) {
                                items(uiState.gitLogCommits) { commit ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(R_SM),
                                        color = cardBg,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(Spacing.S3),
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
                                        ) {
                                            // 左侧时间线圆点
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(Primary, CircleShape)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = commit.message,
                                                    style = ListTitleStyle,
                                                    color = textPrimary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
                                                ) {
                                                    Text(
                                                        text = commit.shortId,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        color = Primary
                                                    )
                                                    Text(
                                                        text = commit.author,
                                                        fontSize = 11.sp,
                                                        color = textSecondary
                                                    )
                                                }
                                                Text(
                                                    text = com.feige.snippetstudio.util.TimeUtil.formatFullDateTime(commit.timestamp),
                                                    fontSize = 10.5.sp,
                                                    color = textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ===== 子页面 3: 分类片段统计 =====
                "cat" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.S4),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S3)
                    ) {
                        listOf("HTML", "JavaScript", "Markdown", "Prompt").forEach { cat ->
                            val count = uiState.categoryCounts[cat] ?: 0
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, borderColor, RoundedCornerShape(R_MD)),
                                shape = RoundedCornerShape(R_MD),
                                color = cardBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.S4),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = cat, style = ListTitleStyle, color = textPrimary)
                                    Surface(
                                        color = PrimarySoft,
                                        shape = RoundedCornerShape(R_SM)
                                    ) {
                                        Text(
                                            text = "$count 项",
                                            style = BadgeStyle,
                                            color = Primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ===== 子页面 4: 全局预设标签管理 =====
                "tags" -> {
                    var newTagInput by remember { mutableStateOf("") }
                    val addAction = {
                        if (newTagInput.trim().isNotEmpty()) {
                            viewModel.addGlobalTag(newTagInput)
                            newTagInput = ""
                            onShowSnackbar("已添加全局预设标签")
                        }
                    }

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
                                .border(1.dp, borderColor, RoundedCornerShape(R_MD)),
                            shape = RoundedCornerShape(R_MD),
                            color = cardBg
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.S3),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
                            ) {
                                OutlinedTextField(
                                    value = newTagInput,
                                    onValueChange = { newTagInput = it },
                                    label = { Text("新建全局常用标签") },
                                    placeholder = { Text("例如: UI, API, 工具") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = addAction,
                                    enabled = newTagInput.trim().isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    shape = AppShapes.small
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_plus), contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("添加")
                                }
                            }
                        }

                        if (uiState.tags.isEmpty()) {
                            EmptyState(
                                title = "尚无自定义标签",
                                desc = "可在此提前添加全局常备标签，或在代码片段中打上标签"
                            )
                        } else {
                            Text("已积累与预设的标签列表", style = CaptionStyle, color = textSecondary)

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                uiState.tags.forEach { tag ->
                                    Surface(
                                        color = C_TagBg,
                                        shape = RoundedCornerShape(R_SM)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "# $tag",
                                                style = ListTitleStyle,
                                                color = C_Tag,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_close),
                                                contentDescription = "Remove Tag",
                                                tint = C_Tag,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        viewModel.removeGlobalTag(tag)
                                                        onShowSnackbar("已移除预设标签: $tag")
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ===== 子页面 5: 回收站与彻底清空管理 =====
                "trash" -> {
                    if (uiState.trashedSnippets.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.empty_trash_title),
                            desc = stringResource(R.string.confirm_trash_desc)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.S4),
                            verticalArrangement = Arrangement.spacedBy(Spacing.S3)
                        ) {
                            items(
                                items = uiState.trashedSnippets,
                                key = { it.id }
                            ) { snippet ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, borderColor, RoundedCornerShape(R_MD)),
                                    shape = RoundedCornerShape(R_MD),
                                    color = cardBg
                                ) {
                                    Row(
                                        modifier = Modifier.padding(Spacing.S4),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = snippet.displayTitle, style = ListTitleStyle, color = textPrimary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = snippet.type.displayName, style = CaptionStyle, color = textSecondary)
                                        }

                                        Row {
                                            TextButton(
                                                onClick = {
                                                    viewModel.restoreSnippet(snippet.id)
                                                    onShowSnackbar(context.getString(R.string.toast_restored))
                                                }
                                            ) {
                                                Icon(painter = painterResource(id = R.drawable.ic_restore), contentDescription = "Restore", tint = Success)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(stringResource(R.string.act_restore), color = Success)
                                            }

                                            TextButton(
                                                onClick = { pendingPurgeId = snippet.id }
                                            ) {
                                                Icon(painter = painterResource(id = R.drawable.ic_trash), contentDescription = "Purge", tint = Danger)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(stringResource(R.string.act_purge), color = Danger)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ===== 子页面 6: 系统语言 (简体中文 / English / 日本語) 切换 =====
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

                // ===== 子页面 7: 配色风格主题选择 =====
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
                                        // 色板预览圆点（展示品牌主色 primary + 辅助主色 primary2 + 深色背景 bgDark）
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            androidx.compose.foundation.Canvas(
                                                modifier = Modifier.size(20.dp)
                                            ) { drawCircle(palette.primary) }
                                            androidx.compose.foundation.Canvas(
                                                modifier = Modifier.size(20.dp)
                                            ) { drawCircle(palette.primary2) }
                                            androidx.compose.foundation.Canvas(
                                                modifier = Modifier.size(20.dp)
                                            ) { drawCircle(palette.bgDark) }
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

        // 彻底永久删除二次确认弹窗
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

