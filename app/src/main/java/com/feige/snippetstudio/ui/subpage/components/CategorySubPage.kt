package com.feige.snippetstudio.ui.subpage.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.ui.theme.*

/**
 * [CategorySubPage] 分类统计子页面视图组件。
 *
 * 架构职责：
 * 1. 展现 HTML, JavaScript, Markdown, Prompt 等不同语言分类的代码片段数量。
 * 2. 提供视觉一致的分类统计卡片与 Badge 数字展示。
 *
 * @param categoryCounts 分类数量映射关系 (CategoryName -> Count)
 * @param modifier 外部 Modifier 修饰符
 */
@Composable
fun CategorySubPage(
    categoryCounts: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current
    val categories = listOf("HTML", "JavaScript", "Markdown", "Prompt")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.S4),
        verticalArrangement = Arrangement.spacedBy(Spacing.S3)
    ) {
        categories.forEach { cat ->
            val count = categoryCounts[cat] ?: 0
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
                shape = RoundedCornerShape(R_MD),
                color = tc.surface
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.S4),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = cat, style = ListTitleStyle, color = tc.text)
                    Surface(
                        color = tc.primarySoft,
                        shape = RoundedCornerShape(R_SM)
                    ) {
                        Text(
                            text = "$count 项",
                            style = BadgeStyle,
                            color = tc.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
