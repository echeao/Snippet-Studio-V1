package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * [SnippetCompactRow] Snippet Studio 原生风格高密度紧凑列表项组件（成组极细分割线设计）。
 *
 * 规范设计：
 * 1. 项与项之间无离散边框间隔，外层由统一圆角容器包裹，内部以 0.8dp 缩进极细线分隔。
 * 2. 消除视觉冗余边框与阴影噪点，达成极致沉浸与高密度的文件浏览体验。
 * 3. 继承应用原生 [TypeIcon] (32dp 微缩版) 与品牌色彩体系。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SnippetCompactRow(
    snippet: Snippet,
    onOpen: () -> Unit,
    onToggleStar: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    showFullDateTime: Boolean = false,
    onRename: (() -> Unit)? = null,
    onMoveFolder: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    var showMenu by remember { mutableStateOf(false) }

    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light
    val dividerColor = if (isDark) LineDark.copy(alpha = 0.6f) else LineLight.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("snippet_compact_row_${snippet.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.S3, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 复用原生 TypeIcon (微缩至 32dp，完全保持品牌一致性)
            TypeIcon(type = snippet.type, size = 32.dp)

            Spacer(modifier = Modifier.width(Spacing.S3))

            // 2. 中间主要内容：标题 + 底部元数据行 (时间、文件夹、标签)
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

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showFullDateTime) TimeUtil.formatFullDateTime(snippet.updatedAt) else TimeUtil.formatRelativeTime(context, snippet.updatedAt),
                        style = CaptionStyle,
                        color = textSecondary,
                        maxLines = 1
                    )

                    if (snippet.folder.isNotBlank()) {
                        Spacer(modifier = Modifier.width(Spacing.S2))
                        Surface(
                            color = PrimarySoft,
                            shape = RoundedCornerShape(R_SM)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = "Folder",
                                    tint = Primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = snippet.folder,
                                    style = BadgeStyle,
                                    color = Primary
                                )
                            }
                        }
                    }

                    if (snippet.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(Spacing.S2))
                        snippet.tags.take(1).forEach { tag ->
                            Surface(
                                color = C_TagBg,
                                shape = RoundedCornerShape(R_SM)
                            ) {
                                Text(
                                    text = "# $tag",
                                    style = BadgeStyle,
                                    color = C_Tag,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(Spacing.S2))

            // 3. 右侧精简操作按钮组 (星标 + 更多操作)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.size(28.dp).testTag("star_button_${snippet.id}")
                ) {
                    Icon(
                        imageVector = if (snippet.starred) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = stringResource(R.string.filter_fav),
                        tint = if (snippet.starred) StarOn else textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp).testTag("more_button_${snippet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
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

        if (showDivider) {
            HorizontalDivider(
                color = dividerColor,
                thickness = 0.8.dp,
                modifier = Modifier.padding(start = 52.dp, end = Spacing.S3)
            )
        }
    }
}
