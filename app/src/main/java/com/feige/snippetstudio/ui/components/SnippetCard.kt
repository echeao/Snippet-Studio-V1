package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.TimeUtil

/**
 * [SnippetCard] 是列表展示单个代码片段的主体 UI 卡片组件。
 *
 * 布局结构：
 * - 左侧：[TypeIcon] 识别图标 (如 HTML/JS/MD/Prompt 标识)。
 * - 中间：代码片段标题、自定义标签 Chips、文件夹路径胶囊 Badge、修改时间。
 * - 右侧：一键复制代码按钮、星标收藏 Toggle 按钮、更多操作下拉菜单 DropdownMenu（编辑/重命名/移动文件夹/删除）。
 *
 * @param snippet 关联的代码片段领域模型 [Snippet]
 * @param onOpen 点击卡片触发的打开/编辑事件回调
 * @param onToggleStar 点击收藏按钮事件回调
 * @param onMore 点击删除/更多主要操作事件回调
 * @param showFullDateTime 是否显示完整标准时间 (true: yyyy-MM-dd HH:mm:ss, false: 相对时间)
 * @param onCopySnippet 一键复制片段正文到剪贴板的回调
 * @param onRename 重命名按钮回调
 * @param onMoveFolder 移动文件夹按钮回调
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SnippetCard(
    snippet: Snippet,
    onOpen: () -> Unit,
    onToggleStar: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    showFullDateTime: Boolean = false,
    onCopySnippet: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onMoveFolder: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    var showMenu by remember { mutableStateOf(false) }

    val borderColor = if (isDark) LineDark else LineLight
    val surfaceColor = if (isDark) SurfaceDark else SurfaceLight
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
            .border(1.dp, borderColor, RoundedCornerShape(R_MD))
            .clickable(onClick = onOpen)
            .testTag("snippet_card_${snippet.id}"),
        shape = RoundedCornerShape(R_MD),
        color = surfaceColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.S4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类型图标
            TypeIcon(type = snippet.type, size = 44.dp)

            Spacer(modifier = Modifier.width(Spacing.S3))

            // 中间文本描述与元数据
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = snippet.displayTitle,
                    style = ListTitleStyle,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 标签、文件夹胶囊与修改时间
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (snippet.tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            maxItemsInEachRow = 2
                        ) {
                            snippet.tags.take(2).forEach { tag ->
                                Surface(
                                    color = C_TagBg,
                                    shape = RoundedCornerShape(R_SM)
                                ) {
                                    Text(
                                        text = "# $tag",
                                        style = BadgeStyle,
                                        color = C_Tag,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(Spacing.S2))
                    }

                    if (snippet.folder.isNotBlank()) {
                        Surface(
                            color = PrimarySoft,
                            shape = RoundedCornerShape(R_SM)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = "Folder",
                                    tint = Primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = snippet.folder,
                                    style = BadgeStyle,
                                    color = Primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(Spacing.S2))
                    }

                    Text(
                        text = if (showFullDateTime) TimeUtil.formatFullDateTime(snippet.updatedAt) else TimeUtil.formatRelativeTime(context, snippet.updatedAt),
                        style = CaptionStyle,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.S2))

            // 右侧操作按钮组
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 快捷复制按钮
                if (onCopySnippet != null) {
                    IconButton(
                        onClick = onCopySnippet,
                        modifier = Modifier.testTag("copy_button_${snippet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 收藏星标按钮
                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.testTag("star_button_${snippet.id}")
                ) {
                    Icon(
                        imageVector = if (snippet.starred) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = stringResource(R.string.filter_fav),
                        tint = if (snippet.starred) StarOn else textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 更多选项下拉菜单按钮
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("more_button_${snippet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.act_edit)) },
                            onClick = {
                                showMenu = false
                                onOpen()
                            }
                        )
                        if (onRename != null) {
                            DropdownMenuItem(
                                text = { Text("重命名") },
                                onClick = {
                                    showMenu = false
                                    onRename()
                                }
                            )
                        }
                        if (onMoveFolder != null) {
                            DropdownMenuItem(
                                text = { Text("移动至文件夹") },
                                onClick = {
                                    showMenu = false
                                    onMoveFolder()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.act_delete), color = Danger) },
                            onClick = {
                                showMenu = false
                                onMore()
                            }
                        )
                    }
                }
            }
        }
    }
}

