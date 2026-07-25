package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.ui.theme.*

/**
 * [AppSwitch] 带矢量图标、标题、副标题描述与状态开关的设置选项组件。
 *
 * 设计特性：
 * 1. 支持可选的前置 Icon 图标，呈现与 [SettingsItem] 保持一致的品牌主色样式。
 * 2. 深入集成 [LocalThemeColors] 语义色系统，显式定制 Switch 开关在开启 (checked) 与关闭 (unchecked)
 *    状态下的动态色板，确保在浅色/深色模式及多主题切换下绝无色彩混淆异常。
 *
 * @param checked 当前开关选中状态
 * @param onCheckedChange 开关状态变更触发回调
 * @param label 主标题文本
 * @param subLabel 副标题/补充说明文本（可选）
 * @param iconRes 前置矢量图标资源 ID（可选，如 R.drawable.ic_moon）
 * @param modifier 外部修饰符
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    subLabel: String? = null,
    iconRes: Int? = null,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // 若配置了图标，则渲染品牌主色的矢量图标
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = tc.primary,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(Spacing.S3))
            }

            Column {
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
        }

        // 显式绑定 Switch 在开启与关闭状态下的主题语义色，解决浅色模式关闭状态下原生 M3 控件显示异常问题
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = tc.primary,
                checkedThumbColor = tc.surface,
                uncheckedTrackColor = tc.surface2,
                uncheckedThumbColor = tc.text3,
                uncheckedBorderColor = tc.line
            ),
            modifier = Modifier.testTag("app_switch_${label}")
        )
    }
}
