package com.feige.snippetstudio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.model.*
import com.feige.snippetstudio.ui.theme.LocalThemeColors
import com.feige.snippetstudio.ui.theme.Spacing
import com.feige.snippetstudio.ui.theme.SyncGreen
import com.feige.snippetstudio.ui.theme.SyncRed
import com.feige.snippetstudio.ui.theme.SyncBlue

/**
 * [SyncPreviewSheet] 同步预览底部面板。
 *
 * 展示同步操作前的变更清单与冲突列表，用户确认后才执行实际操作。
 *
 * @param preview 同步预览数据
 * @param syncProgress 执行进度文本（非 null 时表示正在执行）
 * @param onResolveConflict 冲突解决回调 (index, resolution)
 * @param onConfirm 确认执行回调
 * @param onCancel 取消回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncPreviewSheet(
    preview: SyncPreview,
    syncProgress: String?,
    onResolveConflict: (Int, ConflictResolution) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val tc = LocalThemeColors.current
    val isExecuting = syncProgress != null

    ModalBottomSheet(
        onDismissRequest = { if (!isExecuting) onCancel() },
        containerColor = tc.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.S4)
                .padding(bottom = Spacing.S6)
        ) {
            // ===== 顶部：数据流向指示 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val directionText = if (preview.direction == SyncDirection.INCOMING) {
                    "远端仓库  →  本地设备"
                } else {
                    "本地设备  →  远端仓库"
                }
                Text(
                    text = directionText,
                    style = MaterialTheme.typography.titleMedium,
                    color = tc.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.S3))

            // ===== 执行进度视图 =====
            if (isExecuting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.S6),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = tc.primary)
                        Spacer(modifier = Modifier.height(Spacing.S3))
                        Text(
                            text = syncProgress ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.text2
                        )
                    }
                }
                return@Column
            }

            // ===== 统计摘要 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
            ) {
                val addedCount = preview.changes.count { it.changeType == SyncChangeType.ADDED }
                val updatedCount = preview.changes.count { it.changeType == SyncChangeType.UPDATED }
                val conflictCount = preview.conflicts.size

                if (addedCount > 0) {
                    StatChip(label = "+$addedCount 新增", color = SyncGreen)
                }
                if (updatedCount > 0) {
                    StatChip(label = "~$updatedCount 更新", color = SyncBlue)
                }
                if (conflictCount > 0) {
                    StatChip(label = "!$conflictCount 冲突", color = SyncRed)
                }
                if (preview.totalCount == 0) {
                    Text(
                        text = "已是最新状态，无需同步",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tc.text2
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.S3))

            // ===== 变更列表 =====
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(Spacing.S2)
            ) {
                // 普通变更条目
                itemsIndexed(preview.changes) { _, item ->
                    ChangeItemRow(item = item)
                }

                // 冲突条目
                itemsIndexed(preview.conflicts) { index, conflict ->
                    ConflictItemRow(
                        conflict = conflict,
                        onResolve = { resolution -> onResolveConflict(index, resolution) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.S4))

            // ===== 底部操作按钮 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("取消", color = tc.text2)
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !preview.hasUnresolvedConflicts && preview.totalCount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = tc.primary)
                ) {
                    val label = if (preview.hasUnresolvedConflicts) {
                        "请先解决冲突"
                    } else {
                        "确认执行 (${preview.totalCount} 个变更)"
                    }
                    Text(label)
                }
            }
        }
    }
}

/**
 * 统计标签小组件。
 */
@Composable
private fun StatChip(label: String, color: androidx.compose.ui.graphics.Color) {
    val tc = LocalThemeColors.current
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 普通变更条目行。
 */
@Composable
private fun ChangeItemRow(item: SyncChangeItem) {
    val tc = LocalThemeColors.current

    val (icon, color) = when (item.changeType) {
        SyncChangeType.ADDED -> "+" to SyncGreen
        SyncChangeType.UPDATED -> "~" to SyncBlue
        SyncChangeType.DELETED -> "-" to SyncRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(tc.bg)
            .padding(Spacing.S3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 变更类型标记
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(Spacing.S3))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = tc.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.folder.isNotBlank()) {
                Text(
                    text = item.folder,
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 变更类型文字
        Text(
            text = when (item.changeType) {
                SyncChangeType.ADDED -> "新增"
                SyncChangeType.UPDATED -> "更新"
                SyncChangeType.DELETED -> "远端已删"
            },
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * 冲突条目行（可展开查看内容与选择解决策略）。
 */
@Composable
private fun ConflictItemRow(
    conflict: SyncConflict,
    onResolve: (ConflictResolution) -> Unit
) {
    val tc = LocalThemeColors.current
    var expanded by remember { mutableStateOf(false) }

    val resolved = conflict.resolution != ConflictResolution.PENDING
    val borderColor = if (resolved) SyncGreen.copy(alpha = 0.5f) else SyncRed.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(tc.bg)
    ) {
        // 冲突文件标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(Spacing.S3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SyncRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("!", color = SyncRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(Spacing.S3))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conflict.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (resolved) {
                        when (conflict.resolution) {
                            ConflictResolution.KEEP_LOCAL -> "已选择：保留本地"
                            ConflictResolution.KEEP_REMOTE -> "已选择：保留远端"
                            ConflictResolution.KEEP_BOTH -> "已选择：两者都保留"
                            else -> ""
                        }
                    } else {
                        "本地与远端内容冲突"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (resolved) SyncGreen else SyncRed
                )
            }

            Text(
                text = if (expanded) "▲" else "▼",
                color = tc.text2,
                fontSize = 12.sp
            )
        }

        // 展开区域：内容预览 + 解决按钮
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.S3, vertical = Spacing.S2)
            ) {
                // 本地版本摘要
                Text(
                    text = "本地版本:",
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.text2
                )
                Text(
                    text = conflict.localContent.lines().take(3).joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.text,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(Spacing.S2))

                // 远端版本摘要
                Text(
                    text = "远端版本:",
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.text2
                )
                Text(
                    text = conflict.remoteContent.lines().take(3).joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.text,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(Spacing.S3))

                // 解决策略按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
                ) {
                    ResolutionButton(
                        label = "保留本地",
                        selected = conflict.resolution == ConflictResolution.KEEP_LOCAL,
                        onClick = { onResolve(ConflictResolution.KEEP_LOCAL) },
                        modifier = Modifier.weight(1f)
                    )
                    ResolutionButton(
                        label = "保留远端",
                        selected = conflict.resolution == ConflictResolution.KEEP_REMOTE,
                        onClick = { onResolve(ConflictResolution.KEEP_REMOTE) },
                        modifier = Modifier.weight(1f)
                    )
                    ResolutionButton(
                        label = "都保留",
                        selected = conflict.resolution == ConflictResolution.KEEP_BOTH,
                        onClick = { onResolve(ConflictResolution.KEEP_BOTH) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 冲突解决策略选择按钮。
 */
@Composable
private fun ResolutionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) tc.primary else tc.bg
        ),
        border = if (!selected) androidx.compose.foundation.BorderStroke(1.dp, tc.line) else null,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) androidx.compose.ui.graphics.Color.White else tc.text,
            maxLines = 1
        )
    }
}
