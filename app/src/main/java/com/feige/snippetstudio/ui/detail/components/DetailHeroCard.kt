package com.feige.snippetstudio.ui.detail.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.components.TypeIcon
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.TimeUtil

/**
 * [DetailHeroCard] 代码片段详情页顶层 Hero 头部展示卡片。
 *
 * 职责：
 * 1. 展示编程语言图标与语言类型 Badge 标签。
 * 2. 展示代码片段大字标题与重命名快捷触发。
 * 3. 展示创建时间与文件名芯片。
 * 4. 展示与编辑标签（Tags）FlowRow 芯片集合。
 *
 * @param snippet 当前展示的代码片段实体对象
 * @param onRenameClick 触发标题/文件名重命名弹窗的回调函数
 * @param onTagClick 触发标签修改弹窗的回调函数
 * @param modifier 外部布局修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailHeroCard(
    snippet: Snippet,
    onRenameClick: () -> Unit,
    onTagClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(AppElevation.Sm, RoundedCornerShape(R_XL), ambientColor = AppElevation.SmColor)
            .border(1.dp, tc.line, RoundedCornerShape(R_XL)),
        shape = RoundedCornerShape(R_XL),
        color = tc.surface
    ) {
        Column(modifier = Modifier.padding(Spacing.S5)) {
            // ===== 1. 语言类型图标与 Badge 标签 =====
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TypeIcon(type = snippet.type, size = 48.dp)

                Surface(
                    color = tc.primarySoft,
                    shape = RoundedCornerShape(R_SM)
                ) {
                    Text(
                        text = snippet.type.displayName,
                        style = BadgeStyle,
                        color = tc.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.S4))

            // ===== 2. 代码片段标题与重命名操作图标 =====
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRenameClick)
            ) {
                Text(
                    text = snippet.displayTitle,
                    style = DisplayTitleStyle,
                    color = tc.text,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onRenameClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "重命名",
                        tint = tc.text2,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.S2))

            // ===== 3. 创建时间展示 =====
            Text(
                text = "创建于 ${TimeUtil.formatFullDateTime(snippet.createdAt)}",
                style = CaptionStyle,
                color = tc.text2
            )

            Spacer(modifier = Modifier.height(Spacing.S3))

            // ===== 4. 文件名 Chip 芯片 =====
            Surface(
                color = tc.surface2,
                shape = RoundedCornerShape(R_SM),
                modifier = Modifier.clickable(onClick = onRenameClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = Spacing.S3, vertical = Spacing.S2)
                ) {
                    Text(
                        text = snippet.fileName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        color = tc.text2
                    )
                    Spacer(modifier = Modifier.width(Spacing.S2))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "编辑文件名",
                        tint = tc.text2,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.S3))

            // ===== 5. 动态标签流式展示与编辑区 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (snippet.tags.isEmpty()) {
                    // 无标签时的添加空状态按钮
                    Surface(
                        color = tc.primarySoft,
                        shape = RoundedCornerShape(R_SM),
                        modifier = Modifier.clickable(onClick = onTagClick)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Spacing.S3, vertical = Spacing.S1)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_plus),
                                contentDescription = "添加标签",
                                tint = tc.primary,
                                modifier = Modifier.size(16.dp)
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
                    // 有标签时的 FlowRow 芯片流
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
                            tint = tc.text2,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
