package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Immutable
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.*

/**
 * [FilterOption] 代码片段筛选选项密封类 (Sealed Class)。
 *
 * @param labelRes 对应 string 资源 ID
 * @param type 指定匹配的 [SnippetType] 类型（为空表示全部类型）
 * @param isFav 是否仅过滤标记为收藏的片段
 */
@Immutable
sealed class FilterOption(val labelRes: Int, val type: SnippetType? = null, val isFav: Boolean = false) {
    object All : FilterOption(R.string.filter_all)
    object Html : FilterOption(R.string.type_html, SnippetType.HTML)
    object Js : FilterOption(R.string.type_js, SnippetType.JS)
    object Markdown : FilterOption(R.string.type_md, SnippetType.MARKDOWN)
    object Prompt : FilterOption(R.string.type_prompt, SnippetType.PROMPT)
    object Favorites : FilterOption(R.string.filter_fav, isFav = true)

    companion object {
        val list: List<FilterOption>
            get() = listOf(All, Html, Js, Markdown, Prompt, Favorites)
    }
}

/**
 * [FilterChipsRow] 代码片段分类/状态水平可滚动 FilterChip 选项栏组件。
 *
 * @param selected 当前高亮选中的筛选选项
 * @param onSelect 选项选中切换闭包
 * @param modifier 外部修饰符
 */
@Composable
fun FilterChipsRow(
    selected: FilterOption,
    onSelect: (FilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.S2),
        contentPadding = PaddingValues(horizontal = Spacing.S4)
    ) {
        items(FilterOption.list) { option ->
            val isSelected = selected == option
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        text = stringResource(id = option.labelRes),
                        style = CaptionStyle.copy(fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tc.primary,
                    selectedLabelColor = Color.White,
                    containerColor = tc.surface2,
                    labelColor = tc.text2
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = tc.line.copy(alpha = 0.6f),
                    selectedBorderColor = tc.primary,
                    borderWidth = 1.dp
                ),
                shape = RoundedCornerShape(R_SM),
                modifier = Modifier.testTag("filter_chip_${option.labelRes}")
            )
        }
    }
}
