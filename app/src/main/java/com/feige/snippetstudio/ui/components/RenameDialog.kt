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
 * [RenameDialog] 重命名代码片段标题与文件名模态对话框。
 *
 * @param show 显隐开关
 * @param initialTitle 初始标题
 * @param initialFileName 初始文件名
 * @param onDismiss 关闭弹窗回调
 * @param onConfirm 保存新标题与新文件名回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameDialog(
    show: Boolean,
    initialTitle: String,
    initialFileName: String,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, newFileName: String) -> Unit
) {
    val tc = LocalThemeColors.current

    if (!show) return

    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var fileName by remember(initialFileName) { mutableStateOf(initialFileName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "重命名片段", style = SectionTitleStyle)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.S2),
                verticalArrangement = Arrangement.spacedBy(Spacing.S3)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("片段标题", style = CaptionStyle) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_title_input")
                )

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("文件名称", style = CaptionStyle) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_filename_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(title.trim(), fileName.trim())
                    onDismiss()
                },
                enabled = title.isNotBlank() || fileName.isNotBlank(),
                modifier = Modifier.testTag("rename_confirm_btn")
            ) {
                Text(text = stringResource(R.string.common_confirm), color = tc.primary, style = ListTitleStyle)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("rename_cancel_btn")
            ) {
                Text(text = stringResource(R.string.common_cancel), style = ListTitleStyle)
            }
        },
        shape = AppShapes.large
    )
}

