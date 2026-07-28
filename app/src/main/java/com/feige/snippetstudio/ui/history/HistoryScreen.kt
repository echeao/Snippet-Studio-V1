package com.feige.snippetstudio.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.DiffType
import com.feige.snippetstudio.model.GitCommitInfo
import com.feige.snippetstudio.ui.components.ConfirmDialog
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.TimeUtil

/**
 * [HistoryScreen] 单片段 Git 历史履历页面。
 * 展示提交 Timeline、版本内容查看、Diff 对比与版本恢复功能。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tc = LocalThemeColors.current
    val context = LocalContext.current
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreTargetCommit by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.history_title),
                            style = SectionTitleStyle,
                            color = tc.text
                        )
                        if (uiState.snippetTitle.isNotBlank()) {
                            Text(
                                text = uiState.snippetTitle,
                                style = CaptionStyle,
                                color = tc.text2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = tc.text
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tc.bg)
            )
        },
        containerColor = tc.bg
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = tc.primary)
                }
            }
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = uiState.errorMessage!!, color = tc.text2, style = CaptionStyle)
                }
            }
            uiState.commitList.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.history_empty),
                        color = tc.text2,
                        style = CaptionStyle
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(Spacing.S4),
                    verticalArrangement = Arrangement.spacedBy(Spacing.S2)
                ) {
                    // Diff 视图区域
                    if (uiState.showDiff && uiState.diffLines.isNotEmpty()) {
                        item {
                            DiffView(
                                diffLines = uiState.diffLines,
                                onClose = { viewModel.closeDiff() },
                                tc = tc
                            )
                            Spacer(modifier = Modifier.height(Spacing.S4))
                        }
                    }

                    // 版本内容预览
                    if (uiState.selectedCommitId != null && uiState.fileContentAtCommit != null && !uiState.showDiff) {
                        item {
                            CommitContentView(
                                content = uiState.fileContentAtCommit!!,
                                tc = tc
                            )
                            Spacer(modifier = Modifier.height(Spacing.S4))
                        }
                    }

                    // Timeline 列表
                    items(uiState.commitList) { commit ->
                        CommitTimelineItem(
                            commit = commit,
                            isSelected = uiState.selectedCommitId == commit.commitId,
                            tc = tc,
                            timeText = TimeUtil.formatRelativeTime(context, commit.timestamp),
                            onViewContent = { viewModel.viewCommitContent(commit.commitId) },
                            onCompare = { viewModel.compareWithCurrent(commit.commitId) },
                            onRestore = {
                                restoreTargetCommit = commit.commitId
                                showRestoreDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // 恢复确认对话框
    ConfirmDialog(
        show = showRestoreDialog,
        title = stringResource(R.string.history_restore_title),
        desc = stringResource(R.string.history_restore_desc),
        onConfirm = {
            showRestoreDialog = false
            restoreTargetCommit?.let { commitId ->
                viewModel.restoreToVersion(commitId) { success ->
                    if (success) {
                        onShowSnackbar(context.getString(R.string.toast_restored_version))
                        onBack()
                    } else {
                        onShowSnackbar(context.getString(R.string.toast_restore_failed))
                    }
                }
            }
        },
        onDismiss = { showRestoreDialog = false },
        isDanger = false
    )
}

/** Timeline 单条提交记录 */
@Composable
private fun CommitTimelineItem(
    commit: GitCommitInfo,
    isSelected: Boolean,
    tc: ThemeColors,
    timeText: String,
    onViewContent: () -> Unit,
    onCompare: () -> Unit,
    onRestore: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) tc.primarySoft.copy(alpha = 0.3f) else tc.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, tc.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(Spacing.S4),
            verticalArrangement = Arrangement.spacedBy(Spacing.S2)
        ) {
            // 提交消息 + shortId
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = commit.message,
                    style = ListTitleStyle,
                    color = tc.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = tc.surface2
                ) {
                    Text(
                        text = commit.shortId,
                        style = CaptionStyle,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = tc.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 作者 + 时间
            Text(
                text = "${commit.author} · $timeText",
                style = CaptionStyle,
                color = tc.text3
            )

            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onViewContent, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(stringResource(R.string.history_view), style = CaptionStyle, color = tc.primary)
                }
                TextButton(onClick = onCompare, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(stringResource(R.string.history_compare), style = CaptionStyle, color = tc.text2)
                }
                TextButton(onClick = onRestore, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(stringResource(R.string.history_restore), style = CaptionStyle, color = tc.text2)
                }
            }
        }
    }
}

/** 版本内容只读预览 */
@Composable
private fun CommitContentView(content: String, tc: ThemeColors) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
        shape = RoundedCornerShape(12.dp),
        color = tc.surface2
    ) {
        Column(modifier = Modifier.padding(Spacing.S3)) {
            Text(
                text = stringResource(R.string.history_content_at_version),
                style = CaptionStyle,
                color = tc.text2,
                modifier = Modifier.padding(bottom = Spacing.S2)
            )
            Text(
                text = content,
                style = CaptionStyle,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = tc.text,
                maxLines = 20,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Diff 对比视图 */
@Composable
private fun DiffView(
    diffLines: List<com.feige.snippetstudio.model.DiffLine>,
    onClose: () -> Unit,
    tc: ThemeColors
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
        shape = RoundedCornerShape(12.dp),
        color = tc.surface2
    ) {
        Column(modifier = Modifier.padding(Spacing.S3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_diff_title),
                    style = CaptionStyle,
                    color = tc.text2,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClose, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(stringResource(R.string.common_close), style = CaptionStyle, color = tc.primary)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.S2))

            diffLines.take(100).forEach { line ->
                val bgColor = when (line.type) {
                    DiffType.ADD -> Color(0x204CAF50)
                    DiffType.DELETE -> Color(0x20F44336)
                    DiffType.CONTEXT -> Color.Transparent
                }
                val textColor = when (line.type) {
                    DiffType.ADD -> Color(0xFF4CAF50)
                    DiffType.DELETE -> Color(0xFFF44336)
                    DiffType.CONTEXT -> tc.text2
                }
                val prefix = when (line.type) {
                    DiffType.ADD -> "+ "
                    DiffType.DELETE -> "- "
                    DiffType.CONTEXT -> "  "
                }
                Text(
                    text = "$prefix${line.content}",
                    style = CaptionStyle,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
