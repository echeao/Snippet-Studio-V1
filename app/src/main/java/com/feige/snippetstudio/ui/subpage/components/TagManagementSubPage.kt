package com.feige.snippetstudio.ui.subpage.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

/**
 * [TagManagementSubPage] 全局预设标签管理子页面组件。
 *
 * 架构职责：
 * 1. 允许用户新建全局常用预设标签。
 * 2. 呈现现存全局标签 Chip 集合，并支持一键删除。
 *
 * @param globalTags 当前全局标签列表
 * @param onAddTag 添加标签回调
 * @param onDeleteTag 删除标签回调
 * @param onShowSnackbar 消息提示闭包
 * @param modifier 外部 Modifier 修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagManagementSubPage(
    globalTags: List<String>,
    onAddTag: (String) -> Unit,
    onDeleteTag: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    val context = LocalContext.current
    var newTagInput by remember { mutableStateOf("") }

    val handleAdd = {
        if (newTagInput.trim().isNotEmpty()) {
            onAddTag(newTagInput.trim())
            newTagInput = ""
            onShowSnackbar(context.getString(R.string.toast_tag_added))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.S4),
        verticalArrangement = Arrangement.spacedBy(Spacing.S4)
    ) {
        // 新建标签输入面板
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(AppElevation.Sm, RoundedCornerShape(R_MD))
                .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
            shape = RoundedCornerShape(R_MD),
            color = tc.surface
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
                    onClick = handleAdd,
                    shape = AppShapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = tc.primary)
                ) {
                    Text("添加")
                }
            }
        }

        Text(
            text = "已创建的全局预设标签 (${globalTags.size})",
            style = CaptionStyle,
            color = tc.text2,
            modifier = Modifier.padding(horizontal = Spacing.S2)
        )

        // 标签集合 Chip 展示
        if (globalTags.isEmpty()) {
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
                    Text(text = "暂无全局预设标签", style = BodyStyle, color = tc.text2)
                }
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.S2),
                verticalArrangement = Arrangement.spacedBy(Spacing.S2),
                modifier = Modifier.fillMaxWidth()
            ) {
                globalTags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { },
                        label = { Text("#$tag") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onDeleteTag(tag)
                                    onShowSnackbar(context.getString(R.string.toast_tag_deleted, tag))
                                },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Delete tag",
                                    tint = tc.text2
                                )
                            }
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = tc.primarySoft,
                            selectedLabelColor = tc.primary
                        )
                    )
                }
            }
        }
    }
}
