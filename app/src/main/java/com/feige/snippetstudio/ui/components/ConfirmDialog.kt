package com.feige.snippetstudio.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

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
