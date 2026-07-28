package com.feige.snippetstudio.ui.subpage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.DiffViewer
import com.feige.snippetstudio.ui.components.SyncPreviewSheet
import com.feige.snippetstudio.ui.subpage.vm.GitSubState
import com.feige.snippetstudio.ui.subpage.vm.GitSubViewModel
import com.feige.snippetstudio.ui.theme.*

/**
 * [GitSubPage] Git 远程仓库管理与双向同步子页面组件。
 *
 * 架构职责：
 * 1. 提供 Git 远程 URL、分支名、Personal Access Token (PAT) 的绑定与脱敏展示。
 * 2. 支持一键连通性校验与智能克隆/初始化。
 * 3. 提供方向明确的 Pull (↓) 与 Push (↑) 手动双向同步预览与冲突解决 [SyncPreviewSheet]。
 * 4. 展示本地工作区未提交的文件变更矩阵，并集成 [DiffViewer] 开展逐行差异对比。
 * 5. 提供快捷跳转至 [GitLogSubPage] 提交历史轨迹的路由入口。
 *
 * @param gitState Git 子页面专属 UI 状态
 * @param gitViewModel Git 业务逻辑子 ViewModel
 * @param onNavigateToGitLog 跳转 Git Log 页面闭包
 * @param onShowSnackbar 提示闭包
 * @param modifier 外部 Modifier
 */
@Composable
fun GitSubPage(
    gitState: GitSubState,
    gitViewModel: GitSubViewModel,
    onNavigateToGitLog: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    val context = LocalContext.current
    val gitScrollState = rememberScrollState()
    var isEditingGitConfig by remember { mutableStateOf(false) }
    val isGitConnected = gitState.settings.gitConnected

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(gitScrollState)
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
            Column(
                modifier = Modifier.padding(Spacing.S4),
                verticalArrangement = Arrangement.spacedBy(Spacing.S2)
            ) {
                if (isGitConnected && !isEditingGitConfig) {
                    // ===== A. 已连接状态 =====
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(SyncGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(Spacing.S2))
                            Text(
                                text = "已连接远程仓库",
                                style = ListTitleStyle,
                                color = tc.text
                            )
                            Spacer(modifier = Modifier.width(Spacing.S2))
                            Surface(
                                color = tc.primarySoft,
                                shape = RoundedCornerShape(R_SM)
                            ) {
                                Text(
                                    text = gitState.settings.gitBranch,
                                    style = BadgeStyle,
                                    color = tc.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        TextButton(
                            onClick = { isEditingGitConfig = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = "Edit Config",
                                tint = tc.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "修改配置", style = CaptionStyle, color = tc.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // 脱敏地址与密钥卡片
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(tc.bg.copy(alpha = 0.5f), RoundedCornerShape(R_SM))
                            .border(1.dp, tc.line.copy(alpha = 0.5f), RoundedCornerShape(R_SM))
                            .padding(Spacing.S3)
                    ) {
                        Text(text = stringResource(R.string.sub_git_url), style = CaptionStyle, color = tc.text2)
                        Text(
                            text = gitState.settings.gitUrl,
                            style = BodyStyle,
                            color = tc.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(Spacing.S1))
                        Text(text = "Access Token / 凭证", style = CaptionStyle, color = tc.text2)
                        Text(
                            text = if (gitState.settings.gitPat.isNotEmpty()) "••••••••••••••••" else "(无)",
                            style = BodyStyle,
                            color = tc.text2
                        )
                    }
                } else {
                    // ===== B. 编辑与配置状态 =====
                    if (isGitConnected) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "修改 Git 仓库配置", style = ListTitleStyle, color = tc.text)
                            TextButton(onClick = { isEditingGitConfig = false }) {
                                Text("取消", color = tc.text2)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = gitState.gitUrlInput,
                        onValueChange = { gitViewModel.onGitUrlChange(it) },
                        label = { Text(stringResource(R.string.sub_git_url)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !gitState.isGitOperating
                    )
                    OutlinedTextField(
                        value = gitState.gitBranchInput,
                        onValueChange = { gitViewModel.onGitBranchChange(it) },
                        label = { Text(stringResource(R.string.sub_git_branch)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !gitState.isGitOperating
                    )
                    OutlinedTextField(
                        value = gitState.gitPatInput,
                        onValueChange = { gitViewModel.onGitPatChange(it) },
                        label = { Text(stringResource(R.string.sub_git_pat)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !gitState.isGitOperating
                    )

                    if (gitState.isGitOperating && gitState.syncPreview == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.S2),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = tc.primary)
                        }
                    } else {
                        Button(
                            onClick = {
                                gitViewModel.testGitConnection { success, errorMsg ->
                                    if (success) {
                                        isEditingGitConfig = false
                                        onShowSnackbar(context.getString(R.string.toast_git_verified))
                                    } else {
                                        onShowSnackbar(errorMsg ?: "操作失败")
                                    }
                                }
                            },
                            shape = AppShapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = tc.primary),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !gitState.isGitOperating
                        ) {
                            Text(if (isGitConnected) "保存并重新验证" else "校验连接并初始化/克隆")
                        }
                    }
                }

                // 方向明确的拉取 (Pull ↓) 与推送 (Push ↑) 按键
                if (isGitConnected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
                    ) {
                        OutlinedButton(
                            onClick = {
                                gitViewModel.previewPull { success, msg ->
                                    if (!success) onShowSnackbar(msg ?: "预览失败")
                                }
                            },
                            shape = AppShapes.small,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f),
                            enabled = !gitState.isGitOperating && !gitState.isPreviewing
                        ) {
                            Text(text = "拉取远端 (Pull ↓)", fontSize = 13.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                gitViewModel.previewPush { success, msg ->
                                    if (!success) onShowSnackbar(msg ?: "预览失败")
                                }
                            },
                            shape = AppShapes.small,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f),
                            enabled = !gitState.isGitOperating && !gitState.isPreviewing
                        ) {
                            Text(text = "推送本地 (Push ↑)", fontSize = 13.sp, maxLines = 1)
                        }
                    }

                    if (gitState.isPreviewing) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = tc.primary, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                // 查看本地变更与 Git Log
                if (gitState.settings.gitConnected) {
                    OutlinedButton(
                        onClick = { gitViewModel.loadLocalChanges() },
                        shape = AppShapes.small,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !gitState.isGitOperating
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_code),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.S2))
                        Text("查看本地变更 (${gitState.localChanges.size})")
                    }

                    // 本地变更列表与 Diff 对比
                    if (gitState.localChanges.isNotEmpty()) {
                        gitState.localChanges.forEach { (path, changeType) ->
                            val isSelected = gitState.selectedDiffPath == path
                            val (icon, chgColor) = when (changeType) {
                                "ADDED" -> "+" to SyncGreen
                                "MODIFIED" -> "~" to SyncBlue
                                "DELETED" -> "-" to SyncRed
                                else -> "?" to tc.text2
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) gitViewModel.closeDiff() else gitViewModel.loadFileDiff(path)
                                        },
                                    color = if (isSelected) tc.primarySoft.copy(alpha = 0.3f) else tc.surface,
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
                                            Text(icon, color = chgColor, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(Spacing.S2))
                                        Text(
                                            text = path,
                                            style = CaptionStyle,
                                            color = tc.text,
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

                                if (isSelected) {
                                    if (gitState.isDiffLoading) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(Spacing.S3),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = tc.primary, modifier = Modifier.size(20.dp))
                                        }
                                    } else if (gitState.currentDiff.isNotEmpty()) {
                                        DiffViewer(
                                            diffLines = gitState.currentDiff,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 300.dp)
                                                .padding(start = Spacing.S3, top = Spacing.S1, bottom = Spacing.S1)
                                        )
                                    } else {
                                        Text(
                                            text = "无差异内容",
                                            style = CaptionStyle,
                                            color = tc.text2,
                                            modifier = Modifier.padding(start = Spacing.S3, top = Spacing.S1, bottom = Spacing.S1)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            gitViewModel.loadGitLog()
                            onNavigateToGitLog()
                        },
                        shape = AppShapes.small,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !gitState.isGitOperating
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

                // 同步预览底栏 Sheet
                if (gitState.syncPreview != null) {
                    SyncPreviewSheet(
                        preview = gitState.syncPreview!!,
                        syncProgress = gitState.syncProgress,
                        onResolveConflict = { index, resolution -> gitViewModel.resolveConflict(index, resolution) },
                        onConfirm = {
                            gitViewModel.confirmSync { success, msg ->
                                onShowSnackbar(msg ?: (if (success) "同步完成" else "同步失败"))
                            }
                        },
                        onCancel = { gitViewModel.cancelSync() }
                    )
                }
            }
        }

        Text(
            text = if (gitState.settings.gitConnected)
                "Git 状态: 已连接。修改代码片段时将自动同步至本地 Git 仓并可推送远端。"
            else
                "请输入有效的远程仓库 URL 和 Personal Access Token (PAT)，点击“校验连接”即可完成准备。",
            style = BodyStyle,
            color = tc.text2,
            modifier = Modifier.padding(horizontal = Spacing.S2)
        )
    }
}
