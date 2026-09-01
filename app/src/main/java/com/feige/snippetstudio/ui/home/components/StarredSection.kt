package com.feige.snippetstudio.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.*

/**
 * [StarredSection] 首页"收藏片段"横向滑动快捷入口区组件。
 *
 * 架构职责：
 * 1. 以 LazyRow 横向滑动卡片列表展示用户已星标收藏的代码片段（最多 8 条）。
 * 2. 具备 contentType 节点复用与高效重组优化。
 * 3. 每张紧凑小卡片展示：语言类型色条 + 标题 + 类型图标 + 物理弹簧按压微动效。
 * 4. 点击卡片直接跳转至对应片段详情/编辑页面。
 *
 * @param starredSnippets 已收藏的代码片段列表
 * @param onSnippetClick 点击收藏卡片的回调（传入片段 ID）
 * @param modifier 外部 Modifier 修饰符
 */
@Composable
fun StarredSection(
    starredSnippets: List<Snippet>,
    onSnippetClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 区域标题行
        Row(
            modifier = Modifier.padding(horizontal = Spacing.S4, vertical = Spacing.S2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_star),
                contentDescription = null,
                tint = tc.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "收藏片段",
                style = SectionTitleStyle,
                color = tc.text
            )
        }

        // 横向滑动卡片区（添加 contentType 保障 Compose 节点精准复用）
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.S4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.S3),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = starredSnippets,
                key = { it.id },
                contentType = { "starred_card" }
            ) { snippet ->
                StarredCard(
                    snippet = snippet,
                    onClick = { onSnippetClick(snippet.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

/**
 * [StarredCard] 收藏片段紧凑小卡片。
 * 展示语言类型色条 + 标题文本 + 类型图标，并提供基于 [MotionTokens] 的高级物理弹性触感。
 *
 * @param snippet 代码片段数据
 * @param onClick 点击回调
 * @param modifier 外部修饰符
 */
@Composable
private fun StarredCard(
    snippet: Snippet,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    val style = LocalColorThemeStyle.current
    val typeColor = remember(style, snippet.type) {
        val palette = ColorThemeRegistry.paletteOf(style)
        when (snippet.type) {
            SnippetType.HTML -> palette.typeIcons.html
            SnippetType.JS -> palette.typeIcons.js
            SnippetType.MARKDOWN -> palette.typeIcons.md
            SnippetType.PROMPT -> palette.typeIcons.prompt
            SnippetType.JAVA -> palette.typeIcons.html
            SnippetType.GENERAL -> palette.typeIcons.prompt
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 接入全局物理弹簧缩放微动效 (纯 Draw 阶段渲染)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) MotionTokens.PRESSED_SCALE_MEDIUM_CARD else 1.0f,
        animationSpec = MotionTokens.springBouncy(),
        label = "starred_card_scale"
    )

    Surface(
        modifier = modifier
            .width(140.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
            .border(1.dp, tc.line.copy(alpha = 0.8f), RoundedCornerShape(R_MD))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // 自定义物理弹簧动效代替普通水波纹
                onClick = onClick
            ),
        shape = RoundedCornerShape(R_MD),
        color = tc.surface
    ) {
        Column(
            modifier = Modifier.padding(Spacing.S3)
        ) {
            // 顶部：类型色条 + 类型图标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(4.dp)
                        .background(typeColor, RoundedCornerShape(2.dp))
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_star),
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.S2))

            // 标题文本（最多 2 行溢出省略，使用 displayTitle 回退逻辑）
            Text(
                text = snippet.displayTitle,
                style = ListTitleStyle,
                color = tc.text,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

