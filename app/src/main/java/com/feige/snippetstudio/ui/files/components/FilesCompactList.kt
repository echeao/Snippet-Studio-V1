package com.feige.snippetstudio.ui.files.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.components.SnippetCompactRow
import com.feige.snippetstudio.ui.theme.AppElevation
import com.feige.snippetstudio.ui.theme.LocalThemeColors
import com.feige.snippetstudio.ui.theme.R_MD
import com.feige.snippetstudio.ui.theme.Spacing

/**
 * [FilesCompactList] 极简高密度平铺列表组件。
 *
 * 职责说明：
 * 1. 在 COMPACT 密度与 FLAT 视图下高效展示全量代码片段高密度列表。
 * 2. 采用精美的整块卡片容器包裹，行与行之间自动匹配细阴影与高品质分隔线。
 *
 * @param snippets 代码片段数据集合
 * @param listState LazyListState 滚动状态
 * @param cardClickAction 默认点击跳转策略 ("editor" 或 "detail")
 * @param onNavigateToDetail 跳转详情页回调
 * @param onNavigateToEditor 跳转编辑器页回调
 * @param onRename 触发重命名回调
 * @param onMoveFolder 触发移动文件夹回调
 * @param onToggleStar 切换星标回调
 * @param onTrash 移入回收站回调
 * @param modifier 外部 Modifier 样式
 */
@Composable
fun FilesCompactList(
    snippets: List<Snippet>,
    listState: LazyListState,
    cardClickAction: String,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onRename: (Snippet) -> Unit,
    onMoveFolder: (Snippet) -> Unit,
    onToggleStar: (Snippet) -> Unit,
    onTrash: (Snippet) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(snippets, key = { it.id }) { snippet ->
            val onOpen = {
                if (cardClickAction == "editor") {
                    onNavigateToEditor(snippet.id)
                } else {
                    onNavigateToDetail(snippet.id)
                }
            }
            Surface(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.S4, vertical = Spacing.S1)
                    .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
                    .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
                shape = RoundedCornerShape(R_MD),
                color = tc.surface
            ) {
                SnippetCompactRow(
                    snippet = snippet,
                    onOpen = onOpen,
                    onRename = { onRename(snippet) },
                    onMoveFolder = { onMoveFolder(snippet) },
                    onToggleStar = { onToggleStar(snippet) },
                    onMore = { onTrash(snippet) },
                    showDivider = false,
                    showFullDateTime = false
                )
            }
        }
    }
}
