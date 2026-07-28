package com.feige.snippetstudio.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

/**
 * [AppSettingGroup] 设置分组外框卡片组件。
 *
 * 职责：为一组功能相近的设置项提供带有层次感的分组标题、阴影圆角背景容器。
 *
 * @param title 分组分类标题（如 "工作区与存储", "外观与主题"）
 * @param modifier 布局修饰符
 * @param content 分组卡片内部放置的子 Composable 列表
 */
@Composable
fun AppSettingGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val tc = LocalThemeColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = CaptionStyle,
            color = tc.text2,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = Spacing.S2, bottom = Spacing.S2)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
                .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
            shape = RoundedCornerShape(R_MD),
            color = tc.surface
        ) {
            Column(content = content)
        }
    }
}

/**
 * [AppSettingTile] 带有物理按压缩放反馈与动态图标 Badge 的通用设置条目组件。
 *
 * 职责：渲染单个跳转或操作设置项，支持自定义左侧图标色系背景、右侧自定义 Tag / Badge 状态显示。
 *
 * @param iconRes 左侧图标资源 ID
 * @param title 主标题文本
 * @param subTitle 副标题描述文本（可选）
 * @param iconColor 图标前景色 tint (默认使用主题主色 tc.primary)
 * @param iconBgColor 图标背景 Badge 柔和色（可选，若提供则渲染精致圆角背景容器）
 * @param trailingContent 右侧自定义 Slot 内容（如当前状态 Tag 或 Switch）
 * @param onClick 点击事件回调
 */
@Composable
fun AppSettingTile(
    iconRes: Int,
    title: String,
    subTitle: String? = null,
    iconColor: Color = LocalThemeColors.current.primary,
    iconBgColor: Color? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val tc = LocalThemeColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 点击时的物理弹簧微缩动效动画 (按压时轻微缩小至 98%)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "setting_tile_press_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = tc.primary.copy(alpha = 0.12f)),
                onClick = onClick
            )
            .padding(horizontal = Spacing.S4, vertical = Spacing.S3 + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // 左侧图标 Badge 容器
            if (iconBgColor != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(R_SM))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.S3))

            Column {
                Text(
                    text = title,
                    style = ListTitleStyle,
                    color = tc.text
                )
                if (!subTitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subTitle,
                        style = CaptionStyle,
                        color = tc.text2,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(Spacing.S2))

        // 右侧状态内容 Slot 渲染
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = "Navigate",
                    tint = tc.text2.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * [AppSettingSwitchTile] 包含状态开关控制的设置条目组件。
 *
 * @param iconRes 左侧图标 ID
 * @param title 标题
 * @param subTitle 描述说明
 * @param checked 当前开关选中状态
 * @param onCheckedChange 状态切换回调
 * @param iconColor 图标主色
 * @param iconBgColor 图标背景 Badge 颜色
 */
@Composable
fun AppSettingSwitchTile(
    iconRes: Int,
    title: String,
    subTitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconColor: Color = LocalThemeColors.current.primary,
    iconBgColor: Color? = null
) {
    AppSettingTile(
        iconRes = iconRes,
        title = title,
        subTitle = subTitle,
        iconColor = iconColor,
        iconBgColor = iconBgColor,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = LocalThemeColors.current.primary
                )
            )
        },
        onClick = { onCheckedChange(!checked) }
    )
}

/**
 * [SettingChoiceOption] 单选弹窗中的可选项数据结构。
 *
 * @param key 候选值的唯一 Key
 * @param label 可视显示的名称文本
 * @param description 辅助解释说明
 */
data class SettingChoiceOption(
    val key: String,
    val label: String,
    val description: String? = null
)

/**
 * [SettingChoiceDialog] 通用设置单选弹窗对话框组件。
 *
 * 职责：替代循环点击切换体验差的问题，弹出标准的列表单选框供用户明确选择目标模式。
 *
 * @param show 是否开启展示
 * @param title 对话框标题（如 "选择卡片默认点击行为"）
 * @param options 候选项列表
 * @param selectedKey 当前选中的选项 Key
 * @param onSelect 选中目标 Key 的回调闭包
 * @param onDismiss 关闭对话框回调
 */
@Composable
fun SettingChoiceDialog(
    show: Boolean,
    title: String,
    options: List<SettingChoiceOption>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tc = LocalThemeColors.current

    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    style = SectionTitleStyle,
                    color = tc.text
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                ) {
                    options.forEach { option ->
                        val isSelected = option.key == selectedKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(R_SM))
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        onSelect(option.key)
                                        onDismiss()
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = Spacing.S3, horizontal = Spacing.S2),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = tc.primary)
                            )
                            Spacer(modifier = Modifier.width(Spacing.S3))
                            Column {
                                Text(
                                    text = option.label,
                                    style = ListTitleStyle,
                                    color = if (isSelected) tc.primary else tc.text,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (!option.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.description,
                                        style = CaptionStyle,
                                        color = tc.text2
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.common_cancel), color = tc.text2, style = ListTitleStyle)
                }
            },
            shape = AppShapes.large,
            containerColor = tc.surface
        )
    }
}
