package com.feige.snippetstudio.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.SizeUtil
import com.feige.snippetstudio.util.TimeUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light
    val cardBg = if (isDark) SurfaceDark else SurfaceLight
    val borderColor = if (isDark) LineDark else LineLight

    var showTrashDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    val snippet = uiState.snippet

    if (uiState.isLoading || snippet == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) BgDark else BgLight),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.detail_title),
                        style = SectionTitleStyle,
                        color = textPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) BgDark else BgLight
                )
            )
        },
        containerColor = if (isDark) BgDark else BgLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.S4),
            verticalArrangement = Arrangement.spacedBy(Spacing.S4)
        ) {
            // Hero Card (Surface R_XL)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(AppElevation.Sm, RoundedCornerShape(R_XL), ambientColor = AppElevation.SmColor)
                    .border(1.dp, borderColor, RoundedCornerShape(R_XL)),
                shape = RoundedCornerShape(R_XL),
                color = cardBg
            ) {
                Column(modifier = Modifier.padding(Spacing.S5)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TypeIcon(type = snippet.type, size = 48.dp)

                        Surface(
                            color = PrimarySoft,
                            shape = RoundedCornerShape(R_SM)
                        ) {
                            Text(
                                text = snippet.type.displayName,
                                style = BadgeStyle,
                                color = Primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.S4))

                    Text(
                        text = snippet.displayTitle,
                        style = DisplayTitleStyle,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(Spacing.S2))

                    Text(
                        text = "创建于 ${TimeUtil.formatFullDateTime(snippet.createdAt)}",
                        style = CaptionStyle,
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(Spacing.S3))

                    Surface(
                        color = if (isDark) Surface2Dark else Surface2Light,
                        shape = RoundedCornerShape(R_SM)
                    ) {
                        Text(
                            text = snippet.fileName,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = Spacing.S3, vertical = Spacing.S2)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.S3))

                    // Tags Display / Add Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (snippet.tags.isEmpty()) {
                            Surface(
                                color = PrimarySoft,
                                shape = RoundedCornerShape(R_SM),
                                modifier = Modifier.clickable { showTagDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = Spacing.S3, vertical = Spacing.S1)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add Tag",
                                        tint = Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "添加标签",
                                        style = CaptionStyle,
                                        color = Primary
                                    )
                                }
                            }
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showTagDialog = true }
                            ) {
                                snippet.tags.forEach { tag ->
                                    Surface(
                                        color = C_TagBg,
                                        shape = RoundedCornerShape(R_SM)
                                    ) {
                                        Text(
                                            text = "# $tag",
                                            style = BadgeStyle,
                                            color = C_Tag,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { showTagDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit Tags",
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4 Action Grid (Edit, Run, Copy, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
            ) {
                DetailActionButton(
                    iconVector = Icons.Filled.Edit,
                    label = stringResource(R.string.act_edit),
                    onClick = { onNavigateToEditor(snippet.id) },
                    modifier = Modifier.weight(1f)
                )
                DetailActionButton(
                    iconVector = Icons.Filled.PlayArrow,
                    label = stringResource(R.string.act_run),
                    onClick = { onNavigateToEditor(snippet.id) },
                    modifier = Modifier.weight(1f)
                )
                DetailActionButton(
                    iconVector = Icons.Filled.ContentCopy,
                    label = stringResource(R.string.act_copy),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(snippet.content))
                        onShowSnackbar(context.getString(R.string.toast_copied))
                    },
                    modifier = Modifier.weight(1f)
                )
                DetailActionButton(
                    iconVector = Icons.Filled.Delete,
                    label = stringResource(R.string.act_delete),
                    onClick = { showTrashDialog = true },
                    isDanger = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Panel "运行预览"
            DetailPanel(
                title = stringResource(R.string.detail_preview)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    RunPreview(
                        type = snippet.type,
                        content = snippet.content,
                        onToast = onShowSnackbar
                    )
                }
            }

            // Panel "源码片段"
            DetailPanel(
                title = stringResource(R.string.detail_source),
                headerAction = {
                    Text(
                        text = if (uiState.isSourceExpanded) "收起" else "展开",
                        style = CaptionStyle,
                        color = Primary,
                        modifier = Modifier.clickable { viewModel.toggleSourceExpanded() }
                    )
                }
            ) {
                val previewLines = if (uiState.isSourceExpanded) snippet.content else snippet.content.lines().take(8).joinToString("\n")
                Surface(
                    color = if (isDark) Surface2Dark else Surface2Light,
                    shape = RoundedCornerShape(R_SM),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = previewLines,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = textPrimary,
                        modifier = Modifier.padding(Spacing.S3)
                    )
                }
            }

            // Panel "详细信息"
            DetailPanel(
                title = stringResource(R.string.detail_info)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.S3)) {
                    InfoRow(label = stringResource(R.string.detail_filename), value = snippet.fileName)
                    InfoRow(label = stringResource(R.string.detail_size), value = SizeUtil.formatBytes(snippet.sizeBytes))
                    InfoRow(label = stringResource(R.string.detail_path), value = "snippets/${snippet.fileName}")
                    InfoRow(label = stringResource(R.string.detail_updated), value = TimeUtil.formatFullDateTime(snippet.updatedAt))
                    InfoRow(label = stringResource(R.string.detail_git_status), value = stringResource(R.string.detail_git_status_val))
                }
            }
        }

        // Tag Edit Dialog
        TagEditDialog(
            show = showTagDialog,
            initialTags = snippet.tags,
            allAvailableTags = uiState.allAvailableTags,
            onDismiss = { showTagDialog = false },
            onSave = { updatedTags ->
                viewModel.updateTags(updatedTags)
                onShowSnackbar("标签已更新")
            }
        )

        // Confirmation Dialog
        ConfirmDialog(
            show = showTrashDialog,
            title = stringResource(R.string.confirm_trash_title),
            desc = stringResource(R.string.confirm_trash_desc),
            onConfirm = {
                viewModel.trashSnippet {
                    onShowSnackbar(context.getString(R.string.toast_trashed))
                    onBack()
                }
            },
            onDismiss = { showTrashDialog = false },
            isDanger = true
        )
    }
}

@Composable
fun DetailActionButton(
    iconVector: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) SurfaceDark else SurfaceLight
    val borderColor = if (isDark) LineDark else LineLight
    val iconColor = if (isDanger) Danger else Primary
    val iconBg = if (isDanger) DangerSoft else PrimarySoft

    Surface(
        modifier = modifier
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
            .border(1.dp, borderColor, RoundedCornerShape(R_MD))
            .clickable(onClick = onClick)
            .testTag("detail_act_$label"),
        shape = RoundedCornerShape(R_MD),
        color = cardBg
    ) {
        Column(
            modifier = Modifier.padding(vertical = Spacing.S3),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, RoundedCornerShape(R_SM)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.S2))
            Text(
                text = label,
                style = CaptionStyle,
                color = if (isDark) TextDark else TextLight
            )
        }
    }
}

@Composable
fun DetailPanel(
    title: String,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) SurfaceDark else SurfaceLight
    val borderColor = if (isDark) LineDark else LineLight
    val textPrimary = if (isDark) TextDark else TextLight

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
            .border(1.dp, borderColor, RoundedCornerShape(R_MD)),
        shape = RoundedCornerShape(R_MD),
        color = cardBg
    ) {
        Column(modifier = Modifier.padding(Spacing.S4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = SectionTitleStyle,
                    color = textPrimary
                )
                headerAction?.invoke()
            }

            Spacer(modifier = Modifier.height(Spacing.S3))

            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = BodyStyle, color = textSecondary)
        Text(
            text = value,
            style = BodyStyle,
            color = textPrimary,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
