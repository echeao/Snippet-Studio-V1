package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.TimeUtil

/**
 * [SnippetPreviewCard] 大卡片预览模式组件（舒适低密度视图）。
 *
 * 特色：
 * 1. 块状大卡片布局，头部包含完整标题与识别图标。
 * 2. 具备【代码片段微型预览框】：截取前 4 行正文内容，采用 monospace 格式展现代码风貌。
 * 3. 底部包含文件统计元数据（字符数、行数）、文件夹路径胶囊以及完整 Tag 标签。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SnippetPreviewCard(
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
    val tc = LocalThemeColors.current
    var showMenu by remember { mutableStateOf(false) }

    val codeBgColor = if (tc.isDark) Color(0xFF1B1D22) else Color(0xFFF4F6F9)
    val codeTextColor = if (tc.isDark) Color(0xFFC5C8D4) else Color(0xFF333745)

    val previewCode = remember(snippet.content) {
        snippet.content.lines().take(4).joinToString("\n")
    }
    val totalLines = remember(snippet.content) {
        snippet.content.lines().size
    }
    val byteSize = remember(snippet.content) {
        snippet.content.toByteArray().size
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
            .border(1.dp, tc.line, RoundedCornerShape(R_MD))
            .clickable(onClick = onOpen)
            .testTag("snippet_preview_card_${snippet.id}"),
        shape = RoundedCornerShape(R_MD),
        color = tc.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.S4)
        ) {
            // ===== 1. 头部行：类型图标 + 标题 + 右侧按钮组 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeIcon(type = snippet.type, size = 38.dp)

                Spacer(modifier = Modifier.width(Spacing.S3))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = snippet.displayTitle,
                        style = ListTitleStyle,
                        color = tc.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (showFullDateTime) TimeUtil.formatFullDateTime(snippet.updatedAt) else TimeUtil.formatRelativeTime(context, snippet.updatedAt),
                        style = CaptionStyle,
                        color = tc.text2
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.S1))

                // 右侧快捷按钮
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onCopySnippet != null) {
                        IconButton(
                            onClick = onCopySnippet,
                            modifier = Modifier.size(32.dp).testTag("copy_button_${snippet.id}")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = "Copy Code",
                                tint = tc.text2,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleStar,
                        modifier = Modifier.size(32.dp).testTag("star_button_${snippet.id}")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = stringResource(R.string.filter_fav),
                            tint = if (snippet.starred) StarOn else tc.text2,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp).testTag("more_button_${snippet.id}")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more_vert),
                                contentDescription = "More",
                                tint = tc.text2,
                                modifier = Modifier.size(18.dp)
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

            Spacer(modifier = Modifier.height(Spacing.S3))

            // ===== 2. 代码片段微型预览框 =====
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(R_SM),
                color = codeBgColor
            ) {
                Text(
                    text = previewCode.ifBlank { "// 空内容" },
                    style = CodeTextStyle,
                    color = codeTextColor,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = Spacing.S3, vertical = Spacing.S2)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.S3))

            // ===== 3. 底部元数据行：标签、文件夹与代码行数/字节数统计 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标签与文件夹
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (snippet.folder.isNotBlank()) {
                        Surface(
                            color = tc.primarySoft,
                            shape = RoundedCornerShape(R_SM)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_folder),
                                    contentDescription = "Folder",
                                    tint = tc.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = snippet.folder,
                                    style = BadgeStyle,
                                    color = tc.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(Spacing.S2))
                    }

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
                    }
                }

                // 右下角：行数与字节数
                Text(
                    text = "$totalLines 行 · ${byteSize} B",
                    style = CaptionStyle,
                    color = tc.text2
                )
            }
        }
    }
}
