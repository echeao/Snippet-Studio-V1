package com.feige.snippetstudio.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

/**
 * [ConfirmDialog] 全局高危/确认操作通用对话框组件。
 *
 * 视觉与交互特性：
 * 1. 动态感知 [LocalThemeColors]，确保在深色/浅色主题下背景与字体对比度和谐。
 * 2. 支持语义化 [isDanger] 参数，高危/删除动作自动高亮 WarningDanger 红色。
 *
 * @param show 是否显示对话框
 * @param title 弹窗标题
 * @param desc 详细描述文本
 * @param onConfirm 确认回调
 * @param onDismiss 取消/关闭回调
 * @param confirmText 确认按钮文案
 * @param dismissText 取消按钮文案
 * @param isDanger 是否为破坏性高危动作（警示红）
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
    val tc = LocalThemeColors.current

    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = title, style = SectionTitleStyle, color = tc.text)
            },
            text = {
                Text(text = desc, style = BodyStyle, color = tc.text2)
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
                        color = if (isDanger) Danger else tc.primary,
                        style = ListTitleStyle
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dialog_dismiss_btn")
                ) {
                    Text(text = dismissText, style = ListTitleStyle, color = tc.text2)
                }
            },
            shape = AppShapes.large,
            containerColor = tc.surface
        )
    }
}
