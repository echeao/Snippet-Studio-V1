package com.feige.snippetstudio.ui.subpage.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.components.ConfirmDialog
import com.feige.snippetstudio.ui.components.TypeIcon
import com.feige.snippetstudio.ui.theme.*

/**
 * [TrashSubPage] 回收站软删除管理子页面组件。
 *
 * 架构职责：
 * 1. 展现处于已软删除状态的代码片段列表。
 * 2. 支持【一键还原】代码片段至正式库。
 * 3. 支持【彻底清除】永久彻底删除代码片段（触发安全确认弹框）。
 *
 * @param trashItems 已删除的代码片段列表 [Snippet]
 * @param onRestore 还原片段回调
 * @param onPurge 彻底删除片段回调
 * @param onShowSnackbar 提示闭包
 * @param modifier 外部 Modifier 修饰符
 */
@Composable
fun TrashSubPage(
    trashItems: List<Snippet>,
    onRestore: (Snippet) -> Unit,
    onPurge: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    val context = LocalContext.current
    var pendingPurgeId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.S4),
        verticalArrangement = Arrangement.spacedBy(Spacing.S3)
    ) {
        if (trashItems.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
                shape = RoundedCornerShape(R_MD),
                color = tc.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.S5),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "回收站为空", style = BodyStyle, color = tc.text2)
                }
            }
        } else {
            Text(
                text = "已删除代码片段 (${trashItems.size})",
                style = CaptionStyle,
                color = tc.text2,
                modifier = Modifier.padding(horizontal = Spacing.S2)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.S3),
                modifier = Modifier.fillMaxSize()
            ) {
                items(trashItems, key = { it.id }) { item ->
                    Surface(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD))
                            .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
                        shape = RoundedCornerShape(R_MD),
                        color = tc.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.S3),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TypeIcon(type = item.type, size = 36.dp)
                            Spacer(modifier = Modifier.width(Spacing.S3))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.displayTitle,
                                    style = ListTitleStyle,
                                    color = tc.text,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "更新时间: ${com.feige.snippetstudio.util.TimeUtil.formatFullDateTime(item.updatedAt)}",
                                    style = CaptionStyle,
                                    color = tc.text2
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S1)) {
                                TextButton(onClick = {
                                    onRestore(item)
                                    onShowSnackbar(context.getString(R.string.toast_snippet_restored))
                                }) {
                                    Text("还原", color = tc.primary)
                                }

                                TextButton(onClick = { pendingPurgeId = item.id }) {
                                    Text("彻底清除", color = Danger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 彻底清除二次确认弹框
    ConfirmDialog(
        show = (pendingPurgeId != null),
        title = "彻底删除代码片段",
        desc = "确定彻底删除该代码片段吗？删除后将无法通过回收站找回！",
        onConfirm = {
            pendingPurgeId?.let { id ->
                onPurge(id)
                onShowSnackbar(context.getString(R.string.toast_snippet_purged))
            }
            pendingPurgeId = null
        },
        onDismiss = { pendingPurgeId = null },
        isDanger = true
    )
}
