package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.feige.snippetstudio.util.SizeUtil
import com.feige.snippetstudio.util.TimeUtil

@Composable
fun SnippetCard(
    snippet: Snippet,
    onOpen: () -> Unit,
    onToggleStar: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    showFullDateTime: Boolean = false
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
            TypeIcon(type = snippet.type, size = 44.dp)

            Spacer(modifier = Modifier.width(Spacing.S3))

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

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Badge
                    Surface(
                        color = PrimarySoft,
                        shape = RoundedCornerShape(R_SM)
                    ) {
                        Text(
                            text = snippet.type.displayName,
                            style = BadgeStyle,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "  ·  ",
                        style = CaptionStyle,
                        color = textSecondary
                    )

                    Text(
                        text = SizeUtil.formatBytes(snippet.sizeBytes),
                        style = CaptionStyle,
                        color = textSecondary
                    )

                    Text(
                        text = "  ·  ",
                        style = CaptionStyle,
                        color = textSecondary
                    )

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

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.testTag("star_button_${snippet.id}")
                ) {
                    Icon(
                        imageVector = if (snippet.starred) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = stringResource(R.string.filter_fav),
                        tint = if (snippet.starred) StarOn else textSecondary
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("more_button_${snippet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = textSecondary
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
