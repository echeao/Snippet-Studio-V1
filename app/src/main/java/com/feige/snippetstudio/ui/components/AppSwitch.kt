package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.feige.snippetstudio.ui.theme.*

/**
 * [AppSwitch] 带标题与副标题描述的开关选项组件。
 *
 * @param checked 当前开关选中状态
 * @param onCheckedChange 开关状态变更触发回调
 * @param label 主标题文本
 * @param subLabel 副标题/补充说明文本（可选）
 * @param modifier 外部修饰符
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    subLabel: String? = null,
    modifier: Modifier = Modifier
) {

    val tc = LocalThemeColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = Spacing.S3, horizontal = Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = ListTitleStyle,
                color = tc.text
            )
            if (!subLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.S1))
                Text(
                    text = subLabel,
                    style = CaptionStyle,
                    color = tc.text2
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = tc.primary,
                checkedThumbColor = tc.surface
            ),
            modifier = Modifier.testTag("app_switch_${label}")
        )
    }
}
