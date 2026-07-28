package com.feige.snippetstudio.ui.files.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.components.SnippetPreviewCard
import com.feige.snippetstudio.ui.theme.Spacing

/**
 * [FilesComfortList] 舒适大卡片平铺列表组件。
 *
 * 职责说明：
 * 1. 在 COMFORT 密度与 FLAT 视图下高效展示全量代码片段的大卡片组件。
 * 2. 展现前 4 行代码预览微图、字符/行数统计与完整标签。
 * 3. 关联复制、重命名、移动文件夹、切换星标及放入回收站操作。
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
 * @param onShowSnackbar 底部 Toast 消息提示
 * @param modifier 外部 Modifier 样式
 */
@Composable
fun FilesComfortList(
    snippets: List<Snippet>,
    listState: LazyListState,
    cardClickAction: String,
    searchQuery: String = "",
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onRename: (Snippet) -> Unit,
    onMoveFolder: (Snippet) -> Unit,
    onToggleStar: (Snippet) -> Unit,
    onTrash: (Snippet) -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(
            items = snippets,
            key = { it.id }
        ) { snippet ->
            val onOpen = {
                if (cardClickAction == "editor") {
                    onNavigateToEditor(snippet.id)
                } else {
                    onNavigateToDetail(snippet.id)
                }
            }
            val onCopy = {
                clipboardManager.setText(AnnotatedString(snippet.content))
                onShowSnackbar(context.getString(R.string.toast_copied))
            }

            SnippetPreviewCard(
                snippet = snippet,
                onOpen = onOpen,
                onCopySnippet = onCopy,
                onRename = { onRename(snippet) },
                onMoveFolder = { onMoveFolder(snippet) },
                onToggleStar = { onToggleStar(snippet) },
                onMore = { onTrash(snippet) },
                showFullDateTime = true,
                searchQuery = searchQuery,
                modifier = Modifier.padding(horizontal = Spacing.S4, vertical = Spacing.S2)
            )
        }
    }
}
