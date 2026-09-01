package com.feige.snippetstudio.ui.home.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

/**
 * [HomeStatsBar] 首页仪表盘轻量数据统计卡片小部件（全新精致现代 UI 版）。
 *
 * 规范对齐现代 Demo：
 * 1. 顶部 Header：大写小字标题 + 柔绿/柔蓝微胶囊指示器 (同步正常 / 存储就绪)。
 * 2. 统计卡片网格：采用多微容器块 (Micro-container boxes)，等宽大数字 + 细腻说明文字。
 * 3. 渐变背景光晕：微弱的环境主色弥散光影，提升整体界面的高级质感。
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
            .shadow(AppElevation.Sm, RoundedCornerShape(R_LG), ambientColor = AppElevation.SmColor)
            .border(1.dp, tc.line.copy(alpha = if (tc.isDark) 0.15f else 0.08f), RoundedCornerShape(R_LG)),
        shape = RoundedCornerShape(R_LG),
        color = tc.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            tc.primarySoft.copy(alpha = if (tc.isDark) 0.22f else 0.35f),
                            tc.surface
                        ),
                        radius = 450f
                    )
                )
                .padding(Spacing.S4)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ===== 顶部标题与状态指示行 =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "代码仓库概览",
                        style = CaptionStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = tc.text2
                    )

                    // 右侧状态胶囊
                    Surface(
                        color = Success.copy(alpha = if (tc.isDark) 0.2f else 0.12f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Success, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "本地就绪",
                                style = BadgeStyle.copy(fontSize = 10.sp),
                                color = Success
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.S3))

                // ===== 3 项独立微容器统计网格 =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
                ) {
                    ModernStatItem(
                        label = "总代码数",
                        value = animatedTotal,
                        valueColor = tc.text,
                        modifier = Modifier.weight(1f)
                    )

                    ModernStatItem(
                        label = "已收藏",
                        value = animatedStarred,
                        valueColor = StarOn,
                        modifier = Modifier.weight(1f)
                    )

                    ModernStatItem(
                        label = "文件夹数",
                        value = animatedFolder,
                        valueColor = tc.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * [ModernStatItem] 现代微卡片单项统计容器。
 */
@Composable
private fun ModernStatItem(
    label: String,
    value: Int,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Surface(
        modifier = modifier
            .border(1.dp, tc.line.copy(alpha = if (tc.isDark) 0.12f else 0.06f), RoundedCornerShape(R_SM)),
        shape = RoundedCornerShape(R_SM),
        color = tc.surface2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = BadgeStyle.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = tc.text3
            )
        }
    }
}

