package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

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
    val tc = LocalThemeColors.current
    var showMenu by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 接入全局物理弹簧缩放微动效 (纯 Draw 阶段渲染)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) MotionTokens.PRESSED_SCALE_CARD else 1.0f,
        animationSpec = MotionTokens.springSnappy(),
        label = "snippet_card_press_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(AppElevation.Sm, RoundedCornerShape(R_LG), ambientColor = AppElevation.SmColor)
            .border(1.dp, tc.line.copy(alpha = if (tc.isDark) 0.15f else 0.08f), RoundedCornerShape(R_LG))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // 自定义物理弹簧动效
                onClick = onOpen
            )
            .testTag("snippet_card_${snippet.id}"),
        shape = RoundedCornerShape(R_LG),
        color = tc.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.S4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TypeIcon(type = snippet.type, size = 44.dp)

            Spacer(modifier = Modifier.width(Spacing.S3))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = snippet.displayTitle,
                    style = ListTitleStyle,
                    color = tc.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

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

                    Text(
                        text = if (showFullDateTime) TimeUtil.formatFullDateTime(snippet.updatedAt) else TimeUtil.formatRelativeTime(context, snippet.updatedAt),
                        style = CaptionStyle,
                        color = tc.text2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.S2))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onCopySnippet != null) {
                    IconButton(
                        onClick = onCopySnippet,
                        modifier = Modifier.testTag("copy_button_${snippet.id}")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = "Copy Code",
                            tint = tc.text2,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.testTag("star_button_${snippet.id}")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_star),
                        contentDescription = stringResource(R.string.filter_fav),
                        tint = if (snippet.starred) StarOn else tc.text2,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("more_button_${snippet.id}")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more_vert),
                            contentDescription = "More",
                            tint = tc.text2,
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
