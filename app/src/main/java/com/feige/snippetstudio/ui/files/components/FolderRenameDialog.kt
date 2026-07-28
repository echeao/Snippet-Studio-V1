package com.feige.snippetstudio.ui.files.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.BodyStyle
import com.feige.snippetstudio.ui.theme.LocalThemeColors
import com.feige.snippetstudio.ui.theme.SectionTitleStyle

/**
 * [FolderRenameDialog] 文件夹重命名交互弹窗组件。
 *
 * 职责说明：
 * 接收旧文件夹路径，允许用户修改并保存为新文件夹路径。
 *
 * @param show 是否显示弹窗
 * @param initialFolderName 初始旧文件夹名称
 * @param onDismiss 弹窗关闭回调
 * @param onConfirm 确认修改回调 (newFolderName)
 */
@Composable
fun FolderRenameDialog(
    show: Boolean,
    initialFolderName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (!show) return

    val tc = LocalThemeColors.current
    var folderName by remember(initialFolderName) { mutableStateOf(initialFolderName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "重命名文件夹",
                style = SectionTitleStyle,
                color = tc.text
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "请输入新的文件夹名称：",
                    style = BodyStyle,
                    color = tc.text2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    singleLine = true,
                    placeholder = { Text("文件夹名称", color = tc.text2) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (folderName.isNotBlank() && folderName != initialFolderName) {
                        onConfirm(folderName)
                    }
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.common_confirm), color = tc.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel), color = tc.text2)
            }
        },
        containerColor = tc.surface
    )
}
