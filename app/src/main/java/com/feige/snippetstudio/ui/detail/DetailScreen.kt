package com.feige.snippetstudio.ui.detail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.detail.components.*
import com.feige.snippetstudio.ui.theme.*

/**
 * [DetailScreen] 代码片段详情查看与交互管理主界面。
 *
 * 现代化轻量重构亮点：
 * 1. **去除长篇源码堆叠**：代码阅读与修改聚焦在专业编辑器中，详情页轻量聚焦于运行预览、元数据与快捷管理。
 * 2. **Hero 大卡片升级**：右上角集成一键星标收藏与重命名，聚合呈现文件名、行数与大小。
 * 3. **4 大快捷动作网格**：配备物理弹性微缩手感，提供【编辑】、【分享】、【复制】与【删除】。
 * 4. **运行预览与元数据**：结构化呈现预览效果与属性详情，支持直接点击修改文件夹。
 *
 * @param viewModel 详情页状态与数据控制器
 * @param onBack 页面返回闭包
 * @param onNavigateToEditor 跳转至代码编辑器路由闭包
 * @param onShowSnackbar 底部全局 Snackbar 消息提示闭包
 * @param onNavigateToHistory 跳转至 Git 版本历史闭包 (可选)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
    onNavigateToHistory: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val tc = LocalThemeColors.current
    val scrollState = rememberScrollState()

    // 交互弹窗状态管理
    var showTrashDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var previewRefreshKey by remember { mutableIntStateOf(0) }

    val snippet = uiState.snippet

    // 加载中状态反馈
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
                            contentDescription = "返回",
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
                actions = {
                    // Git 历史记录入口
                    if (onNavigateToHistory != null) {
                        IconButton(onClick = { onNavigateToHistory(snippet.id) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_git),
                                contentDescription = stringResource(R.string.menu_git_history),
                                tint = tc.text
                            )
                        }
                    }
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
                .verticalScroll(scrollState)
                .padding(horizontal = Spacing.S4, vertical = Spacing.S3),
            verticalArrangement = Arrangement.spacedBy(Spacing.S4)
        ) {
            // ===== 1. Hero 顶层头部一体化大卡片（含星标与 4 大动作） =====
            DetailHeroCard(
                snippet = snippet,
                onRenameClick = { showRenameDialog = true },
                onTagClick = { showTagDialog = true },
                onToggleStar = { viewModel.toggleStar() },
                onEditClick = { onNavigateToEditor(snippet.id) },
                onShareClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, snippet.content)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, snippet.displayTitle)
                    context.startActivity(shareIntent)
                },
                onCopyClick = {
                    clipboardManager.setText(AnnotatedString(snippet.content))
                    onShowSnackbar(context.getString(R.string.toast_copied))
                },
                onDeleteClick = { showTrashDialog = true }
            )

            // ===== 2. 代码运行沙盒视口画板 (Sandbox Viewport) =====
            key(previewRefreshKey) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(AppElevation.Sm, RoundedCornerShape(R_LG), ambientColor = AppElevation.SmColor)
                            .border(1.dp, tc.line.copy(alpha = 0.85f), RoundedCornerShape(R_LG)),
                        shape = RoundedCornerShape(R_LG),
                        color = tc.surface
                    ) {
                        Column {
                            // 沙盒视口顶部控制栏 (Sandbox Window Chrome)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(tc.surface2)
                                    .padding(horizontal = Spacing.S3, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 窗口微光三色点 (macOS 风格)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).background(androidx.compose.ui.graphics.Color(0xFFFF5F56), RoundedCornerShape(50)))
                                    Box(modifier = Modifier.size(8.dp).background(androidx.compose.ui.graphics.Color(0xFFFFBD2E), RoundedCornerShape(50)))
                                    Box(modifier = Modifier.size(8.dp).background(androidx.compose.ui.graphics.Color(0xFF27C93F), RoundedCornerShape(50)))
                                }

                                // 视口标题标签
                                Text(
                                    text = "${snippet.type.displayName} 运行视口",
                                    fontSize = 11.5.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = tc.text3
                                )

                                // 刷新预览按钮
                                IconButton(
                                    onClick = { previewRefreshKey++ },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_refresh),
                                        contentDescription = "刷新预览",
                                        tint = tc.text3,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = tc.line.copy(alpha = 0.6f))

                            // 运行画板内容区
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 130.dp, max = 320.dp)
                                    .background(tc.codeBg)
                                    .padding(Spacing.S2)
                            ) {
                                RunPreview(
                                    type = snippet.type,
                                    content = snippet.content,
                                    onToast = onShowSnackbar
                                )
                            }
                        }
                    }
                }
            }

            // ===== 3. "详细信息与元数据" 面板 =====
            DetailInfoPanel(
                snippet = snippet,
                onFolderClick = { showFolderDialog = true }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        // ===== 交互弹窗集合 =====

        // 1. 重命名弹窗
        RenameDialog(
            show = showRenameDialog,
            initialTitle = snippet.title,
            initialFileName = snippet.fileName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newTitle, newFileName ->
                viewModel.renameSnippet(newTitle, newFileName)
                onShowSnackbar(context.getString(R.string.toast_renamed))
            }
        )

        // 2. 文件夹移动弹窗
        FolderMoveDialog(
            show = showFolderDialog,
            currentFolder = snippet.folder,
            existingFolders = uiState.existingFolders,
            onDismiss = { showFolderDialog = false },
            onConfirm = { targetFolder ->
                viewModel.updateFolder(targetFolder)
                onShowSnackbar(context.getString(R.string.toast_moved_folder))
            }
        )

        // 3. 标签编辑弹窗
        TagEditDialog(
            show = showTagDialog,
            initialTags = snippet.tags,
            allAvailableTags = uiState.allAvailableTags,
            onDismiss = { showTagDialog = false },
            onSave = { updatedTags ->
                viewModel.updateTags(updatedTags)
                onShowSnackbar(context.getString(R.string.toast_tags_updated))
            }
        )

        // 4. 放入回收站确认弹窗
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
