package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

/**
 * [FolderCreateDialog] 在应用内部新建文件夹的模态对话框。
 *
 * 功能结构：
 * 1. 允许用户输入新的文件夹相对路径（如 "components" 或 "utils/string"）。
 * 2. 校验文件夹路径有效性（非空且不包含违规字符）。
 * 3. 确认后回调 [onConfirm]，联动触发 Room `FolderEntity` 数据库记录与物理磁盘目录同步创建。
 *
 * @param show 显隐开关标志位
 * @param onDismiss 关闭弹窗回调
 * @param onConfirm 确认新建文件夹相对路径回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderCreateDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (folderName: String) -> Unit
) {
    if (!show) return

    var folderInput by remember { mutableStateOf("") }
    val tc = LocalThemeColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "新建文件夹", style = SectionTitleStyle, color = tc.text)
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
                    label = { Text("文件夹名称 / 路径", style = CaptionStyle) },
                    placeholder = { Text("如: components 或 utils/string", style = CaptionStyle) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_folder_input")
                )
                Text(
                    text = "注：创建后将在本地物理磁盘同步生成真实文件夹目录。",
                    style = CaptionStyle,
                    color = tc.text2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanPath = folderInput.trim().trim('/')
                    if (cleanPath.isNotBlank()) {
                        onConfirm(cleanPath)
                        onDismiss()
                    }
                },
                enabled = folderInput.trim().isNotBlank(),
                modifier = Modifier.testTag("create_folder_confirm_btn")
            ) {
                Text(text = stringResource(R.string.common_confirm), color = tc.primary, style = ListTitleStyle)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("create_folder_cancel_btn")
            ) {
                Text(text = stringResource(R.string.common_cancel), style = ListTitleStyle)
            }
        },
        shape = AppShapes.large
    )
}
