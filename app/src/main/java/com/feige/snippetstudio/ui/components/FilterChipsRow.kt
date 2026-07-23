package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.*

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

@Composable
fun FilterChipsRow(
    selected: FilterOption,
    onSelect: (FilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
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
                label = { Text(stringResource(id = option.labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = AppShapes.small,
                modifier = Modifier.testTag("filter_chip_${option.labelRes}")
            )
        }
    }
}
