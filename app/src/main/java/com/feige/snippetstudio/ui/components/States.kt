package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

/**
 * [EmptyState] 页面或列表内容为空时的统一缺省占位 UI 组件。
 *
 * @param title 标题
 * @param desc 描述信息
 * @param modifier 外部修饰符
 * @param iconRes 图标 drawable 资源 ID (可选)
 * @param actionLabel 底部操作按钮文案 (可选)
 * @param onAction 底部操作按钮点击闭包 (可选)
 */
@Composable
fun EmptyState(
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val tc = LocalThemeColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.S6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(tc.surface2, RoundedCornerShape(R_XL)),
            contentAlignment = Alignment.Center
        ) {
            when {
                iconRes != null -> {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Empty State Icon",
                        tint = tc.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                else -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_code),
                        contentDescription = "Empty State Icon",
                        tint = tc.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.S4))

        Text(
            text = title,
            style = SectionTitleStyle,
            color = tc.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.S2))

        Text(
            text = desc,
            style = BodyStyle,
            color = tc.text2,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(Spacing.S5))
            Button(
                onClick = onAction,
                shape = AppShapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = tc.primary),
                modifier = Modifier.testTag("empty_state_action_btn")
            ) {
                Text(text = actionLabel, style = ListTitleStyle)
            }
        }
    }
}

/**
 * [LoadingState] 骨架屏 (Skeleton) 加载动画占位视图。
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.S4),
        verticalArrangement = Arrangement.spacedBy(Spacing.S3)
    ) {
        repeat(3) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(R_MD),
                color = tc.surface2
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.S4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(tc.line, RoundedCornerShape(R_SM))
                    )
                    Spacer(modifier = Modifier.width(Spacing.S3))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(16.dp)
                                .background(tc.line, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(12.dp)
                                .background(tc.line, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

/**
 * [ErrorState] 加载失败与重试界面。
 *
 * @param title 错误信息标题
 * @param onRetry 重试回调函数
 * @param modifier 外部修饰符
 */
@Composable
fun ErrorState(
    title: String = stringResource(R.string.error_title),
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.S6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_warning),
            contentDescription = "Error",
            tint = Danger,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(Spacing.S3))

        Text(
            text = title,
            style = SectionTitleStyle,
            color = tc.text
        )

        Spacer(modifier = Modifier.height(Spacing.S4))

        Button(
            onClick = onRetry,
            shape = AppShapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = tc.primary)
        ) {
            Text(stringResource(R.string.common_retry))
        }
    }
}
