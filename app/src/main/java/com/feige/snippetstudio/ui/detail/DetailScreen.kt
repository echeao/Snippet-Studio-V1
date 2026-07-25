package com.feige.snippetstudio.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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

/**
 * [DetailScreen] 代码片段详情查看主界面。
 *
 * 布局划分：
 * 1. **Hero 顶层头部卡片**：展示语言图标、大字标题、创建时间、文件名与 FlowRow 标签集合。
 * 2. **4 大核心动作网格**：【编辑】、【运行】、【复制】与【删除】。
 * 3. **实时运行预览面板 [RunPreview]**：交互式嵌入效果展现。
 * 4. **源代码预览面板 [DetailPanel]**：可展开/收起完整源代码。
 * 5. **元数据面板**：展示文件真实大小、路径、修改时间与 Git 仓同步状态。
 *
 * @param viewModel 详情页 ViewModel
 * @param onBack 页面返回闭包
 * @param onNavigateToEditor 打开编辑页面路由闭包
 * @param onShowSnackbar 底部提示闭包
 */
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
    val tc = LocalThemeColors.current

    // 交互弹窗挂起状态
    var showTrashDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }

    val snippet = uiState.snippet

    if (uiState.isLoading || snippet == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tc.bg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = tc.primary)
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
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = tc.text
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.detail_title),
                        style = SectionTitleStyle,
                        color = tc.text
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.bg
                )
            )
        },
        containerColor = tc.bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.S4),
            verticalArrangement = Arrangement.spacedBy(Spacing.S4)
        ) {
            // ===== 1. Hero 顶层头部卡片 =====
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(AppElevation.Sm, RoundedCornerShape(R_XL), ambientColor = AppElevation.SmColor)
                    .border(1.dp, tc.line, RoundedCornerShape(R_XL)),
                shape = RoundedCornerShape(R_XL),
                color = tc.surface
            ) {
                Column(modifier = Modifier.padding(Spacing.S5)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TypeIcon(type = snippet.type, size = 48.dp)

                        Surface(
                            color = tc.primarySoft,
                            shape = RoundedCornerShape(R_SM)
                        ) {
                            Text(
                                text = snippet.type.displayName,
                                style = BadgeStyle,
                                color = tc.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.S4))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRenameDialog = true }
                    ) {
                        Text(
                            text = snippet.displayTitle,
                            style = DisplayTitleStyle,
                            color = tc.text,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showRenameDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = "Rename",
                                tint = tc.text2,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.S2))

                    Text(
                        text = "创建于 ${TimeUtil.formatFullDateTime(snippet.createdAt)}",
                        style = CaptionStyle,
                        color = tc.text2
                    )

                    Spacer(modifier = Modifier.height(Spacing.S3))

                    Surface(
                        color = tc.surface2,
                        shape = RoundedCornerShape(R_SM),
                        modifier = Modifier.clickable { showRenameDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Spacing.S3, vertical = Spacing.S2)
                        ) {
                            Text(
                                text = snippet.fileName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                color = tc.text2
                            )
                            Spacer(modifier = Modifier.width(Spacing.S2))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = "Edit Filename",
                                tint = tc.text2,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.S3))

                    // 标签展示与编辑触发区
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (snippet.tags.isEmpty()) {
                            Surface(
                                color = tc.primarySoft,
                                shape = RoundedCornerShape(R_SM),
                                modifier = Modifier.clickable { showTagDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = Spacing.S3, vertical = Spacing.S1)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_plus),
                                        contentDescription = "Add Tag",
                                        tint = tc.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "添加标签",
                                        style = CaptionStyle,
                                        color = tc.primary
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
                                    painter = painterResource(id = R.drawable.ic_edit),
                                    contentDescription = "Edit Tags",
                                    tint = tc.text2,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ===== 2. 4 大快捷动作按钮网格 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
            ) {
                DetailActionButton(
                    iconRes = R.drawable.ic_edit,
                    label = stringResource(R.string.act_edit),
                    onClick = { onNavigateToEditor(snippet.id) },
                    modifier = Modifier.weight(1f)
                )
                DetailActionButton(
                    iconRes = R.drawable.ic_play,
                    label = stringResource(R.string.act_run),
                    onClick = { onNavigateToEditor(snippet.id) },
                    modifier = Modifier.weight(1f)
                )
                DetailActionButton(
                    iconRes = R.drawable.ic_copy,
                    label = stringResource(R.string.act_copy),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(snippet.content))
                        onShowSnackbar(context.getString(R.string.toast_copied))
                    },
                    modifier = Modifier.weight(1f)
                )
                DetailActionButton(
                    iconRes = R.drawable.ic_trash,
                    label = stringResource(R.string.act_delete),
                    onClick = { showTrashDialog = true },
                    isDanger = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // ===== 3. "运行预览" 面板 =====
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

            // ===== 4. "源码片段" 可折叠/展开面板 =====
            DetailPanel(
                title = stringResource(R.string.detail_source),
                headerAction = {
                    Text(
                        text = if (uiState.isSourceExpanded) "收起" else "展开",
                        style = CaptionStyle,
                        color = tc.primary,
                        modifier = Modifier.clickable { viewModel.toggleSourceExpanded() }
                    )
                }
            ) {
                val previewLines = if (uiState.isSourceExpanded) snippet.content else snippet.content.lines().take(8).joinToString("\n")
                Surface(
                    color = tc.surface2,
                    shape = RoundedCornerShape(R_SM),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = previewLines,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = tc.text,
                        modifier = Modifier.padding(Spacing.S3)
                    )
                }
            }

            // ===== 5. "详细信息与元数据" 面板 =====
            DetailPanel(
                title = stringResource(R.string.detail_info)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.S3)) {
                    InfoRow(label = stringResource(R.string.detail_filename), value = snippet.fileName)
                    InfoRow(
                        label = "所属文件夹",
                        value = if (snippet.folder.isBlank()) "/ (根目录)" else snippet.folder,
                        onClick = { showFolderDialog = true }
                    )
                    InfoRow(label = stringResource(R.string.detail_size), value = SizeUtil.formatBytes(snippet.sizeBytes))
                    InfoRow(label = stringResource(R.string.detail_path), value = if (snippet.folder.isBlank()) "snippets/${snippet.fileName}" else "snippets/${snippet.folder}/${snippet.fileName}")
                    InfoRow(label = stringResource(R.string.detail_updated), value = TimeUtil.formatFullDateTime(snippet.updatedAt))
                    InfoRow(label = stringResource(R.string.detail_git_status), value = stringResource(R.string.detail_git_status_val))
                }
            }
        }

        // ===== 交互弹窗集合 =====
        RenameDialog(
            show = showRenameDialog,
            initialTitle = snippet.title,
            initialFileName = snippet.fileName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newTitle, newFileName ->
                viewModel.renameSnippet(newTitle, newFileName)
                onShowSnackbar("片段已重命名")
            }
        )

        FolderMoveDialog(
            show = showFolderDialog,
            currentFolder = snippet.folder,
            existingFolders = uiState.existingFolders,
            onDismiss = { showFolderDialog = false },
            onConfirm = { targetFolder ->
                viewModel.updateFolder(targetFolder)
                onShowSnackbar("已移动至文件夹")
            }
        )

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

/**
 * [DetailActionButton] 详情页网格按钮组件。
 */
@Composable
fun DetailActionButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false
) {
    val tc = LocalThemeColors.current
    val iconColor = if (isDanger) Danger else tc.primary
    val iconBg = if (isDanger) DangerSoft else tc.primarySoft

    Surface(
        modifier = modifier
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
            .border(1.dp, tc.line, RoundedCornerShape(R_MD))
            .clickable(onClick = onClick)
            .testTag("detail_act_$label"),
        shape = RoundedCornerShape(R_MD),
        color = tc.surface
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
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.S2))
            Text(
                text = label,
                style = CaptionStyle,
                color = tc.text
            )
        }
    }
}

/**
 * [DetailPanel] 详情页通用带有阴影与 Header 动作的卡片面板组件。
 */
@Composable
fun DetailPanel(
    title: String,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val tc = LocalThemeColors.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
            .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
        shape = RoundedCornerShape(R_MD),
        color = tc.surface
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
                    color = tc.text
                )
                headerAction?.invoke()
            }

            Spacer(modifier = Modifier.height(Spacing.S3))

            content()
        }
    }
}

/**
 * [InfoRow] 详情信息键值对单行展示组件。
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val tc = LocalThemeColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = BodyStyle, color = tc.text2)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = BodyStyle,
                color = if (onClick != null) tc.primary else tc.text,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = "Edit",
                    tint = tc.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

