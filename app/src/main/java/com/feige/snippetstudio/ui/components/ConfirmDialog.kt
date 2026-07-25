package com.feige.snippetstudio.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

/**
 * [ConfirmDialog] 通用确认对话框组件。
 *
 * 用于高危操作确认（如放入回收站、彻底清空删除等）。
 *
 * @param show 是否显示对话框
 * @param title 标题
 * @param desc 描述文本
 * @param onConfirm 确认触发闭包
 * @param onDismiss 取消/关闭闭包
 * @param confirmText 确认按钮显示文本（默认“确认”）
 * @param dismissText 取消按钮显示文本（默认“取消”）
 * @param isDanger 是否为破坏性高危操作（使用红色突出警示）
 */
@Composable
fun ConfirmDialog(
    show: Boolean,
    title: String,
    desc: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(R.string.common_confirm),
    dismissText: String = stringResource(R.string.common_cancel),
    isDanger: Boolean = false
) {

    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = title, style = SectionTitleStyle)
            },
            text = {
                Text(text = desc, style = BodyStyle)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("dialog_confirm_btn")
                ) {
                    Text(
                        text = confirmText,
                        color = if (isDanger) Danger else Primary,
                        style = ListTitleStyle
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dialog_dismiss_btn")
                ) {
                    Text(text = dismissText, style = ListTitleStyle)
                }
            },
            shape = AppShapes.large
        )
    }
}
