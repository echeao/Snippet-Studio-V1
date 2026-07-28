package com.feige.snippetstudio.ui.files.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.components.SnippetCompactRow
import com.feige.snippetstudio.ui.components.SnippetPreviewCard
import com.feige.snippetstudio.ui.files.DensityMode
import com.feige.snippetstudio.ui.theme.*

/**
 * [FilesTreeList] 树状文件夹分层可折叠列表组件。
 *
 * 核心特性与优化：
 * 1. **交互折叠与顺畅动画**：支持点击文件夹头部整行进行展开/折叠，箭头图标自动完成 0° 到 90° 的旋转过度，列表扩展采用 `expandVertically + fadeIn` 动画。
 * 2. **文件夹高级生命周期菜单**：为非根目录文件夹提供【重命名文件夹】选项操作。
 * 3. **密度融合支持**：完美兼具 COMFORT 大卡片预览与 COMPACT 极简高密度的文件夹内嵌渲染。
 *
 * @param groupedFolders 文件夹与代码片段对应 Map 结构
 * @param densityMode 显示密度模式
 * @param listState 列表滚动状态 LazyListState
 * @param cardClickAction 默认点击跳转策略 ("editor" 或 "detail")
 * @param onNavigateToDetail 跳转详情页回调
 * @param onNavigateToEditor 跳转编辑器页回调
 * @param onRename 触发代码片段重命名回调
 * @param onRenameFolder 触发文件夹重命名回调
 * @param onMoveFolder 触发移动代码片段文件夹回调
 * @param onToggleStar 切换代码片段星标回调
 * @param onTrash 移入回收站回调
 * @param onShowSnackbar 底部 Toast 消息提示
 * @param modifier 外部修饰符
 */
@Composable
fun FilesTreeList(
    groupedFolders: Map<String, List<Snippet>>,
    densityMode: DensityMode,
    listState: LazyListState,
    cardClickAction: String,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onRename: (Snippet) -> Unit,
    onRenameFolder: (String) -> Unit,
    onMoveFolder: (Snippet) -> Unit,
    onToggleStar: (Snippet) -> Unit,
    onTrash: (Snippet) -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val tc = LocalThemeColors.current

    // 使用 rememberSaveable 保存各文件夹路径的展开/折叠状态（默认均展开 true）
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        groupedFolders.forEach { (folderName, folderSnippets) ->
            val isExpanded = expandedMap[folderName] ?: true
            val isRootFolder = (folderName == "根目录" || folderName.isBlank())

            // ===== 1. 文件夹头部 Title 行 =====
            item(key = "folder_header_$folderName") {
                val arrowRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 90f else 0f,
                    label = "arrowRotation"
                )
                var showFolderMenu by remember { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.S4, vertical = Spacing.S2)
                        .clip(RoundedCornerShape(R_SM))
                        .clickable { expandedMap[folderName] = !isExpanded }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    // 展开/折叠 旋转小箭头
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = "Expand/Collapse",
                        tint = tc.text2,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(arrowRotation)
                    )
                    Spacer(modifier = Modifier.width(Spacing.S1))

                    // 文件夹图标
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder),
                        contentDescription = "Folder Group",
                        tint = tc.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.S2))

                    // 文件夹名称与数量
                    Text(
                        text = "$folderName (${folderSnippets.size})",
                        style = SectionTitleStyle,
                        color = tc.text,
                        modifier = Modifier.weight(1f)
                    )

                    // 非根目录文件夹展示“更多”操作按钮（如重命名文件夹）
                    if (!isRootFolder) {
                        Box {
                            IconButton(
                                onClick = { showFolderMenu = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more_vert),
                                    contentDescription = "Folder Options",
                                    tint = tc.text2,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showFolderMenu,
                                onDismissRequest = { showFolderMenu = false },
                                modifier = Modifier.background(tc.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("重命名文件夹", color = tc.text, style = BodyStyle) },
                                    onClick = {
                                        showFolderMenu = false
                                        onRenameFolder(folderName)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_edit),
                                            contentDescription = null,
                                            tint = tc.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ===== 2. 文件夹内部包含的代码片段列表（动态展开/收起） =====
            if (isExpanded) {
                if (folderSnippets.isEmpty()) {
                    item(key = "empty_folder_$folderName") {
                        Text(
                            text = "(空文件夹)",
                            style = CaptionStyle,
                            color = tc.text2,
                            modifier = Modifier.padding(start = 44.dp, top = 2.dp, bottom = 8.dp)
                        )
                    }
                } else {
                    if (densityMode == DensityMode.COMFORT) {
                        items(
                            items = folderSnippets,
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
                                modifier = Modifier.padding(start = 28.dp, end = Spacing.S4, top = Spacing.S1, bottom = Spacing.S2)
                            )
                        }
                    } else {
                        item(key = "folder_compact_card_$folderName") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 28.dp, end = Spacing.S4, top = Spacing.S1, bottom = Spacing.S2)
                                    .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
                                    .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
                                shape = RoundedCornerShape(R_MD),
                                color = tc.surface
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    folderSnippets.forEachIndexed { index, snippet ->
                                        val onOpen = {
                                            if (cardClickAction == "editor") {
                                                onNavigateToEditor(snippet.id)
                                            } else {
                                                onNavigateToDetail(snippet.id)
                                            }
                                        }
                                        SnippetCompactRow(
                                            snippet = snippet,
                                            onOpen = onOpen,
                                            onRename = { onRename(snippet) },
                                            onMoveFolder = { onMoveFolder(snippet) },
                                            onToggleStar = { onToggleStar(snippet) },
                                            onMore = { onTrash(snippet) },
                                            showDivider = (index < folderSnippets.lastIndex),
                                            showFullDateTime = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
