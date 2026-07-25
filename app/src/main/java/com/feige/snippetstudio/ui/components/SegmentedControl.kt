package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.ui.theme.*

/**
 * [SegmentedControl] iOS 胶囊样式分段选择器组件。
 *
 * 用于编辑器视图模式（【编辑代码】/【效果预览】）的平滑切换。
 *
 * @param options 可选标签文本数组
 * @param selectedIndex 当前选中项索引
 * @param onSelect 选项切换闭包
 * @param modifier 外部修饰符
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    val isDark = LocalIsDarkTheme.current
    val containerBg = if (isDark) Surface2Dark else Surface2Light
    val activeBg = if (isDark) SurfaceDark else SurfaceLight
    val activeText = Primary
    val inactiveText = if (isDark) Text2Dark else Text2Light
    val borderColor = if (isDark) LineDark else LineLight

    Box(
        modifier = modifier
            .background(containerBg, RoundedCornerShape(R_SM))
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Surface(
                    modifier = Modifier
                        .height(34.dp)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .shadow(AppElevation.Sm, RoundedCornerShape(R_SM), ambientColor = AppElevation.SmColor)
                                    .border(1.dp, borderColor, RoundedCornerShape(R_SM))
                            } else Modifier
                        )
                        .clickable { onSelect(index) }
                        .testTag("segment_$index"),
                    shape = RoundedCornerShape(R_SM),
                    color = if (isSelected) activeBg else Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = Spacing.S4),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.W800 else FontWeight.W500,
                            color = if (isSelected) activeText else inactiveText
                        )
                    }
                }
            }
        }
    }
}
