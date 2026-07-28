package com.feige.snippetstudio.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

/**
 * [DetailActionGrid] 详情页快捷操作动作网格布局组件。
 *
 * 职责：
 * 1. 组合 4 大高频核心操作：编辑、运行/预览、全量复制与移入回收站。
 * 2. 响应相应的点击回调，提供一致的卡片阴影与触摸反馈。
 *
 * @param onEditClick 跳转编辑器点击回调
 * @param onRunClick 触发运行或定位到预览区域回调
 * @param onCopyClick 复制代码到剪贴板回调
 * @param onDeleteClick 触发删除确认弹窗回调
 * @param modifier 外部布局修饰符
 */
@Composable
fun DetailActionGrid(
    onEditClick: () -> Unit,
    onRunClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
    ) {
        // 编辑按钮
        DetailActionButton(
            iconRes = R.drawable.ic_edit,
            label = stringResource(R.string.act_edit),
            onClick = onEditClick,
            modifier = Modifier.weight(1f)
        )
        // 运行按钮
        DetailActionButton(
            iconRes = R.drawable.ic_play,
            label = stringResource(R.string.act_run),
            onClick = onRunClick,
            modifier = Modifier.weight(1f)
        )
        // 复制按钮
        DetailActionButton(
            iconRes = R.drawable.ic_copy,
            label = stringResource(R.string.act_copy),
            onClick = onCopyClick,
            modifier = Modifier.weight(1f)
        )
        // 删除危险动作按钮
        DetailActionButton(
            iconRes = R.drawable.ic_trash,
            label = stringResource(R.string.act_delete),
            onClick = onDeleteClick,
            isDanger = true,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * [DetailActionButton] 详情页快捷操作单项卡片按钮组件。
 *
 * @param iconRes 矢量图标资源 ID
 * @param label 按钮底部的说明文案
 * @param onClick 按钮点击触发闭包
 * @param modifier 外部布局修饰符
 * @param isDanger 是否为危险敏感操作（如删除，将呈现红色警示配色）
 */
@Composable
fun DetailActionButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false
) {
    val tc = LocalThemeColors.current
    val iconColor = if (isDanger) Danger else tc.primary
    val iconBg = if (isDanger) DangerSoft else tc.primarySoft

    Surface(
        modifier = modifier
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
            .border(1.dp, tc.line, RoundedCornerShape(R_MD))
            .clickable(onClick = onClick)
            .testTag("detail_act_$label"),
        shape = RoundedCornerShape(R_MD),
        color = tc.surface
    ) {
        Column(
            modifier = Modifier.padding(vertical = Spacing.S3),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, RoundedCornerShape(R_SM)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.S2))
            Text(
                text = label,
                style = CaptionStyle,
                color = tc.text
            )
        }
    }
}
