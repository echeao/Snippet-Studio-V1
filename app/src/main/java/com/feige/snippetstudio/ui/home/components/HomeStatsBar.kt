package com.feige.snippetstudio.ui.home.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.ui.theme.*

/**
 * [HomeStatsBar] 首页仪表盘轻量数据统计卡片小部件（全新精致视觉版）。
 *
 * 采用微渐变容器 + 数字变动平滑动画，提升首页的精美度与高级感。
 *
 * @param totalCount 活动代码片段总数量
 * @param starredCount 星标收藏代码片段总数
 * @param folderCount 已划分的文件夹总数量
 * @param modifier 外部 Modifier 修饰符
 */
@Composable
fun HomeStatsBar(
    totalCount: Int,
    starredCount: Int,
    folderCount: Int,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    val animatedTotal by animateIntAsState(targetValue = totalCount, animationSpec = tween(600), label = "total")
    val animatedStarred by animateIntAsState(targetValue = starredCount, animationSpec = tween(600), label = "starred")
    val animatedFolder by animateIntAsState(targetValue = folderCount, animationSpec = tween(600), label = "folder")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
            .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
        shape = RoundedCornerShape(R_MD),
        color = tc.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tc.primarySoft.copy(alpha = 0.25f),
                            tc.surface
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.S3 + 2.dp, horizontal = Spacing.S4),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ===== 1. 代码片段总数统计列 =====
                StatItem(
                    label = "总代码数",
                    value = animatedTotal,
                    valueColor = tc.text
                )

                // 垂直分割线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(tc.line)
                )

                // ===== 2. 星标收藏数统计列 =====
                StatItem(
                    label = "已收藏",
                    value = animatedStarred,
                    valueColor = tc.primary
                )

                // 垂直分割线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(tc.line)
                )

                // ===== 3. 文件夹分类数统计列 =====
                StatItem(
                    label = "文件夹数",
                    value = animatedFolder,
                    valueColor = tc.text2
                )
            }
        }
    }
}

/**
 * [StatItem] 内部单项统计数字与标签组合。
 */
@Composable
private fun StatItem(
    label: String,
    value: Int,
    valueColor: androidx.compose.ui.graphics.Color
) {
    val tc = LocalThemeColors.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = BadgeStyle,
            color = tc.text2
        )
    }
}

