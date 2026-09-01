package com.feige.snippetstudio.ui.detail.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
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
 * 1. 组合 4 大高频核心操作：【编辑】、【分享】、【全量复制】与【删除】。
 * 2. 具备真实的物理弹簧缩放微动效（Scale 0.92x）与微光晶透边框。
 *
 * @param onEditClick 跳转编辑器点击回调
 * @param onShareClick 触发分享面板或系统分享回调
 * @param onCopyClick 复制代码到剪贴板回调
 * @param onDeleteClick 触发删除确认弹窗回调
 * @param modifier 外部布局修饰符
 */
@Composable
fun DetailActionGrid(
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
    ) {
        // 1. 编辑代码（主操作）
        DetailActionButton(
            iconRes = R.drawable.ic_edit,
            label = stringResource(R.string.act_edit),
            onClick = onEditClick,
            isPrimary = true,
            modifier = Modifier.weight(1f)
        )
        // 2. 分享代码片段
        DetailActionButton(
            iconRes = R.drawable.ic_share,
            label = "分享",
            onClick = onShareClick,
            modifier = Modifier.weight(1f)
        )
        // 3. 复制代码
        DetailActionButton(
            iconRes = R.drawable.ic_copy,
            label = stringResource(R.string.act_copy),
            onClick = onCopyClick,
            modifier = Modifier.weight(1f)
        )
        // 4. 删除（危险动作）
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
 * @param isPrimary 是否为主操作强调样式
 * @param isDanger 是否为危险敏感操作（如删除）
 */
@Composable
fun DetailActionButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isDanger: Boolean = false
) {
    val tc = LocalThemeColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.5f),
        label = "action_btn_scale"
    )

    val iconColor = when {
        isDanger -> Danger
        isPrimary -> tc.primary
        else -> tc.text
    }
    val iconBg = when {
        isDanger -> DangerSoft
        isPrimary -> tc.primarySoft
        else -> tc.surface2
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
            .border(1.dp, tc.line.copy(alpha = 0.8f), RoundedCornerShape(R_MD))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
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
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.S2))
            Text(
                text = label,
                style = CaptionStyle,
                color = if (isPrimary) tc.primary else tc.text
            )
        }
    }
}
