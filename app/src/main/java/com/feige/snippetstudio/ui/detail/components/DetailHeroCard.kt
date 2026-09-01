package com.feige.snippetstudio.ui.detail.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.components.TypeIcon
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.SizeUtil
import com.feige.snippetstudio.util.TimeUtil

/**
 * [DetailHeroCard] 代码片段详情页一体化 Hero 头部大卡片。
 *
 * 现代化集成设计：
 * 1. 顶部：语言图标 + 语言类型 Badge + 右上角一键【星标收藏】按钮（带弹性微动效）。
 * 2. 中部：大字标题（点击重命名）+ `# 标签` 药丸胶囊流。
 * 3. 底部：内嵌 4 大紧凑快捷动作行（【编辑】、【分享】、【复制】、【删除】），大幅压缩页面高度。
 *
 * @param snippet 当前展示的代码片段实体对象
 * @param onRenameClick 触发标题/文件名重命名弹窗的回调
 * @param onTagClick 触发标签修改弹窗的回调
 * @param onToggleStar 触发星标收藏切换的回调
 * @param onEditClick 点击编辑代码回调
 * @param onShareClick 点击系统分享回调
 * @param onCopyClick 点击复制代码回调
 * @param onDeleteClick 点击删除确认回调
 * @param modifier 外部布局修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailHeroCard(
    snippet: Snippet,
    onRenameClick: () -> Unit,
    onTagClick: () -> Unit,
    onToggleStar: () -> Unit,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    val starInteraction = remember { MutableInteractionSource() }
    val isStarPressed by starInteraction.collectIsPressedAsState()
    val starScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isStarPressed) 0.85f else 1.0f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.5f),
        label = "star_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(AppElevation.Sm, RoundedCornerShape(R_XL), ambientColor = AppElevation.SmColor)
            .border(1.dp, tc.line.copy(alpha = 0.85f), RoundedCornerShape(R_XL)),
        shape = RoundedCornerShape(R_XL),
        color = tc.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tc.primarySoft.copy(alpha = if (tc.isDark) 0.12f else 0.28f),
                            tc.surface
                        )
                    )
                )
                .padding(Spacing.S4 + 2.dp)
        ) {
            Column {
                // ===== 1. 顶部行：语言图标 + 语言 Badge + 创建时间 + 右侧星标收藏 =====
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TypeIcon(type = snippet.type, size = 42.dp)
                        Spacer(modifier = Modifier.width(Spacing.S3))
                        Column {
                            Surface(
                                color = tc.primarySoft,
                                shape = RoundedCornerShape(R_SM)
                            ) {
                                Text(
                                    text = snippet.type.displayName,
                                    style = BadgeStyle,
                                    color = tc.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "创建于 ${TimeUtil.formatFullDateTime(snippet.createdAt)}",
                                style = CaptionStyle.copy(fontWeight = FontWeight.Normal),
                                color = tc.text3
                            )
                        }
                    }

                    // 右上角：星标收藏按钮
                    IconButton(
                        onClick = onToggleStar,
                        interactionSource = starInteraction,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = starScale
                                scaleY = starScale
                            }
                            .size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = "收藏",
                            tint = if (snippet.starred) StarOn else tc.text3,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.S3))

                // ===== 2. 代码片段大字标题 (点击触发重命名) =====
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRenameClick)
                ) {
                    Text(
                        text = snippet.displayTitle,
                        style = DisplayTitleStyle.copy(fontSize = 22.sp),
                        color = tc.text,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(Spacing.S2))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "重命名",
                        tint = tc.text3,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.S3))

                // ===== 3. 动态标签流式展示与管理区 =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (snippet.tags.isEmpty()) {
                        Surface(
                            color = tc.primarySoft.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(R_SM),
                            modifier = Modifier.clickable(onClick = onTagClick)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_plus),
                                    contentDescription = "添加标签",
                                    tint = tc.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "添加标签",
                                    style = CaptionStyle,
                                    color = tc.primary
                                )
                            }
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onTagClick)
                        ) {
                            snippet.tags.forEach { tag ->
                                Surface(
                                    color = C_TagBg,
                                    shape = RoundedCornerShape(R_SM)
                                ) {
                                    Text(
                                        text = "# $tag",
                                        style = BadgeStyle,
                                        color = C_Tag,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onTagClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = "编辑标签",
                                tint = tc.text3,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.S3))
                HorizontalDivider(color = tc.line.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(Spacing.S3))

                // ===== 4. 底部紧凑内嵌 4 快捷动作行（编辑、分享、复制、删除） =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
                ) {
                    // 编辑代码（主操作）
                    HeroActionButton(
                        iconRes = R.drawable.ic_edit,
                        label = stringResource(R.string.act_edit),
                        onClick = onEditClick,
                        isPrimary = true,
                        modifier = Modifier.weight(1.1f)
                    )
                    // 分享代码
                    HeroActionButton(
                        iconRes = R.drawable.ic_share,
                        label = "分享",
                        onClick = onShareClick,
                        modifier = Modifier.weight(1f)
                    )
                    // 复制代码
                    HeroActionButton(
                        iconRes = R.drawable.ic_copy,
                        label = stringResource(R.string.act_copy),
                        onClick = onCopyClick,
                        modifier = Modifier.weight(1f)
                    )
                    // 删除
                    HeroActionButton(
                        iconRes = R.drawable.ic_trash,
                        label = stringResource(R.string.act_delete),
                        onClick = onDeleteClick,
                        isDanger = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * [HeroActionButton] 内嵌在 Hero 卡片底部的紧凑动作胶囊组件。
 */
@Composable
private fun HeroActionButton(
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
        label = "hero_action_scale"
    )

    val iconColor = when {
        isDanger -> Danger
        isPrimary -> tc.primary
        else -> tc.text2
    }
    val containerBg = when {
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
            .height(40.dp)
            .border(1.dp, tc.line.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(10.dp),
        color = containerBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                color = if (isPrimary) tc.primary else if (isDanger) Danger else tc.text
            )
        }
    }
}
