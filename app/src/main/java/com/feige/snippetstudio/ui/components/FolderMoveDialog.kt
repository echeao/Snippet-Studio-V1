package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

/**
 * [FolderMoveDialog] 移动代码片段至指定文件夹模态对话框。
 *
 * 功能：
 * 1. 允许用户手动输入新的文件夹相对路径 (如 "utils/string")，空字符串代表根目录 `/`。
 * 2. 自动列出当前已存在的文件夹列表，支持一键点击填入。
 *
 * @param show 显隐开关
 * @param currentFolder 当前代码片段原所在的文件夹路径
 * @param existingFolders 当前库中已存在的所有文件夹列表
 * @param onDismiss 关闭弹窗回调
 * @param onConfirm 确认移动到的目标文件夹路径回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderMoveDialog(
    show: Boolean,
    currentFolder: String,
    existingFolders: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (targetFolder: String) -> Unit
) {
    if (!show) return

    var folderInput by remember(currentFolder) { mutableStateOf(currentFolder) }
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "移动至文件夹", style = SectionTitleStyle)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.S2),
                verticalArrangement = Arrangement.spacedBy(Spacing.S3)
            ) {
                OutlinedTextField(
                    value = folderInput,
                    onValueChange = { folderInput = it },
                    label = { Text("文件夹路径 (留空表示根目录)", style = CaptionStyle) },
                    placeholder = { Text("如: web/components", style = CaptionStyle) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_input")
                )

                // 已存在文件夹快捷选单
                if (existingFolders.isNotEmpty()) {
                    Text(text = "已存在文件夹：", style = CaptionStyle, color = textSecondary)
                    
                    Surface(
                        color = if (isDark) Surface2Dark else Surface2Light,
                        shape = RoundedCornerShape(R_MD),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(Spacing.S2)
                        ) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { folderInput = "" }
                                        .padding(horizontal = Spacing.S2, vertical = Spacing.S2)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = "Root Folder",
                                        tint = Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.S2))
                                    Text(text = "/ (根目录)", style = BodyStyle, color = textPrimary)
                                }
                            }

                            items(existingFolders.filter { it.isNotBlank() }) { folder ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { folderInput = folder }
                                        .padding(horizontal = Spacing.S2, vertical = Spacing.S2)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = "Folder",
                                        tint = Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.S2))
                                    Text(text = folder, style = BodyStyle, color = textPrimary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(folderInput.trim())
                    onDismiss()
                },
                modifier = Modifier.testTag("folder_confirm_btn")
            ) {
                Text(text = stringResource(R.string.common_confirm), color = Primary, style = ListTitleStyle)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("folder_cancel_btn")
            ) {
                Text(text = stringResource(R.string.common_cancel), style = ListTitleStyle)
            }
        },
        shape = AppShapes.large
    )
}

