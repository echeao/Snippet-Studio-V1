package com.feige.snippetstudio.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.components.TypeIcon
import com.feige.snippetstudio.ui.theme.*

/**
 * [QuickNewCard] 首页快捷创建指定语言类型代码片段的交互卡片组件。
 *
 * 交互与视效增强：
 * 1. 按压物理反馈：使用 [animateFloatAsState] 实现真实的弹性按压（Scale 0.97x）微动效。
 * 2. 材质与光影：采用柔和表面环境光阴影与 Subtle 微光边框 [tc.line.copy(alpha = 0.7f)]。
 * 3. 信息层次：提供标题 + 浅色一句话副标题，强化工具专业感。
 *
 * @param type 待创建的代码片段类型 [SnippetType]
 * @param subtitle 辅助副标题描述文字
 * @param onClick 点击触发的回调函数
 * @param modifier 外部修饰符 Modifier
 */
@Composable
fun QuickNewCard(
    type: SnippetType,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 点击按压时的物理弹簧缩放动画 (接入全局 Bouncy 弹簧规约)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) MotionTokens.PRESSED_SCALE_MEDIUM_CARD else 1.0f,
        animationSpec = MotionTokens.springBouncy(),
        label = "quick_card_scale"
    )

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
                indication = null, // 自定义弹性缩放代替标准水波纹
                onClick = onClick
            )
            .testTag("quick_new_${type.code}"),
        shape = RoundedCornerShape(R_MD),
        color = tc.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.S3 + 2.dp, vertical = Spacing.S3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TypeIcon(type = type, size = 36.dp)
            Spacer(modifier = Modifier.width(Spacing.S3))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.displayName,
                    style = ListTitleStyle,
                    color = tc.text
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = CaptionStyle.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
                        color = tc.text3,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * [QuickNewSection] 首页快捷新建卡片组容器。
 *
 * 呈现 2x2 常用类型网格（HTML, JS, Markdown, Prompt）及通用类型创建快捷按键。
 *
 * @param onNavigateToNewEditor 路由导航至对应类型新建编辑器的回调
 * @param modifier 外部修饰符 Modifier
 */
@Composable
fun QuickNewSection(
    onNavigateToNewEditor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.S4, vertical = Spacing.S2)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = Spacing.S3)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = null,
                tint = tc.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.S2))
            Text(
                text = stringResource(R.string.home_quick_new),
                style = SectionTitleStyle,
                color = tc.text
            )
        }

        // 2x2 网格第 1 行: HTML & JS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
        ) {
            QuickNewCard(
                type = SnippetType.HTML,
                subtitle = "网页与模版",
                onClick = { onNavigateToNewEditor(SnippetType.HTML.code) },
                modifier = Modifier.weight(1f)
            )
            QuickNewCard(
                type = SnippetType.JS,
                subtitle = "脚本与逻辑",
                onClick = { onNavigateToNewEditor(SnippetType.JS.code) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.S3))

        // 2x2 网格第 2 行: Markdown & Prompt
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
        ) {
            QuickNewCard(
                type = SnippetType.MARKDOWN,
                subtitle = "排版与文档",
                onClick = { onNavigateToNewEditor(SnippetType.MARKDOWN.code) },
                modifier = Modifier.weight(1f)
            )
            QuickNewCard(
                type = SnippetType.PROMPT,
                subtitle = "AI 提示词",
                onClick = { onNavigateToNewEditor(SnippetType.PROMPT.code) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.S3))

        // 底部通栏：常规通用片段快捷卡片
        QuickNewCard(
            type = SnippetType.GENERAL,
            subtitle = "自由备忘与纯代码片段",
            onClick = { onNavigateToNewEditor(SnippetType.GENERAL.code) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
