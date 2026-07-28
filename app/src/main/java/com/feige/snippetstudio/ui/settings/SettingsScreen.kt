package com.feige.snippetstudio.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.settings.components.AppearanceSection
import com.feige.snippetstudio.ui.settings.components.DataBackupSection
import com.feige.snippetstudio.ui.settings.components.EditorPrefSection
import com.feige.snippetstudio.ui.theme.*

/**
 * 深色模式图标背景色自适应辅助函数。
 * 浅色模式下返回原始色，深色模式下降低 alpha 以避免过于刺眼。
 *
 * @param color 原始图标背景色（浅色模式下的柔和品牌色）
 * @param isDark 当前是否为深色模式
 * @return 适配后的背景色
 */
private fun adaptiveIconBg(color: Color, isDark: Boolean): Color =
    if (isDark) color.copy(alpha = 0.15f) else color

/**
 * [SettingsScreen] 系统设置与全局偏好管理主界面。
 *
 * 架构重构与设计说明：
 * 拆分为四大核心模块与 7 个分类设置组：
 * 1. **存储与工作区**：SAF 物理磁盘目录选取与路径展示。
 * 2. **同步与 Git 控制**：JGit 沙盒仓与远程 Git 鉴权状态卡片。
 * 3. **内容组织与标签**：分类代码统计与全局预设标签项。
 * 4. **代码编辑器偏好 [EditorPrefSection]**：含 [LivePreviewBox] 动态效果展示、字号大小、自动软换行、显示行号、Tab 缩进与符号自动配对。
 * 5. **数据维护与备份 [DataBackupSection]**：JSON 导出备份、JSON 数据导入恢复、ZIP 全量打包与回收站管理。
 * 6. **外观与系统偏好 [AppearanceSection]**：配色风格、深色模式切换、样板代码注入、点击行为与语言。
 * 7. **关于与系统支持**：应用版本号展示、版本更新日志弹窗、恢复全局默认设置。
 *
 * @param viewModel 设置页 ViewModel 控制器
 * @param onNavigateToSubPage 打开二级子设置路由闭包 (如 "git", "repo", "lang", "trash", "theme", "cat", "tags")
 * @param onShowSnackbar 底部消息提示闭包
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToSubPage: (String) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tc = LocalThemeColors.current

    // ===== 交互弹窗状态控制 =====
    var choiceDialogType by remember { mutableStateOf<String?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }

    // ===== Android SAF 系统文件创建 Launcher (保存 JSON 全量备份文件) =====
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupJson(context, uri) { success ->
                if (success) {
                    onShowSnackbar(context.getString(R.string.toast_exported))
                } else {
                    onShowSnackbar(context.getString(R.string.toast_export_failed))
                }
            }
        }
    }

    // ===== Android SAF 系统文件选择 Launcher (从 JSON 备份文件导入恢复) =====
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupJson(context, uri) { success, count ->
                if (success) {
                    onShowSnackbar(context.getString(R.string.toast_import_ok, count))
                } else {
                    onShowSnackbar(context.getString(R.string.toast_import_failed))
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = DisplayTitleStyle,
                        color = tc.text
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tc.bg)
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
            verticalArrangement = Arrangement.spacedBy(Spacing.S5)
        ) {
            // ===== 分组 1: 工作区与存储设置 =====
            AppSettingGroup(title = stringResource(R.string.set_repo)) {
                AppSettingTile(
                    iconRes = R.drawable.ic_folder,
                    title = stringResource(R.string.set_repo_cur),
                    subTitle = settings.repoPath,
                    iconColor = Color(0xFF2e7d32),
                    iconBgColor = adaptiveIconBg(Color(0xFFe8f5e9), tc.isDark),
                    onClick = { onNavigateToSubPage("repo") }
                )
            }

            // ===== 分组 2: 同步与 Git 版本控制 =====
            AppSettingGroup(title = stringResource(R.string.set_sync)) {
                val gitSubTitle = if (settings.gitConnected) "已连接 (JGit 真实仓库)" else stringResource(R.string.sub_git_disconnected)
                AppSettingTile(
                    iconRes = R.drawable.ic_git,
                    title = stringResource(R.string.set_git),
                    subTitle = gitSubTitle,
                    iconColor = Color(0xFF0277bd),
                    iconBgColor = adaptiveIconBg(Color(0xFFe1f5fe), tc.isDark),
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (settings.gitConnected) Color(0xFFe8f5e9) else tc.line.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(R_SM)
                            ) {
                                Text(
                                    text = if (settings.gitConnected) "已就绪" else "未连通",
                                    fontSize = 11.sp,
                                    color = if (settings.gitConnected) Color(0xFF2e7d32) else tc.text2,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.S2))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = tc.text2.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    onClick = { onNavigateToSubPage("git") }
                )
            }

            // ===== 分组 3: 内容组织与标签管理 =====
            AppSettingGroup(title = stringResource(R.string.set_org)) {
                AppSettingTile(
                    iconRes = R.drawable.ic_folder_details,
                    title = stringResource(R.string.set_cat),
                    subTitle = "查看各语言代码片段数量分布",
                    iconColor = Color(0xFF6a1b9a),
                    iconBgColor = adaptiveIconBg(Color(0xFFf3e5f5), tc.isDark),
                    onClick = { onNavigateToSubPage("cat") }
                )
                HorizontalDivider(color = tc.line)
                AppSettingTile(
                    iconRes = R.drawable.ic_tag,
                    title = stringResource(R.string.set_tags),
                    subTitle = "自定义常用标签列表 (${settings.customTags.size} 个)",
                    iconColor = Color(0xFFad1457),
                    iconBgColor = adaptiveIconBg(Color(0xFFfce4ec), tc.isDark),
                    onClick = { onNavigateToSubPage("tags") }
                )
            }

            // ===== 分组 4: 代码编辑器偏好设置 =====
            EditorPrefSection(
                settings = settings,
                viewModel = viewModel,
                onOpenChoiceDialog = { choiceDialogType = it }
            )

            // ===== 分组 5: 数据维护与全量备份 =====
            DataBackupSection(
                onExportJson = { createDocLauncher.launch("snippet-studio-backup.json") },
                onImportJson = { openDocLauncher.launch("application/json") },
                onExportZip = {
                    viewModel.exportZipFile(context) { zipFile ->
                        if (zipFile != null && zipFile.exists()) {
                            onShowSnackbar(context.getString(R.string.toast_zip_ok))
                        } else {
                            onShowSnackbar(context.getString(R.string.toast_zip_failed))
                        }
                    }
                },
                onNavigateToTrash = { onNavigateToSubPage("trash") }
            )

            // ===== 分组 6: 外观与系统交互偏好 =====
            AppearanceSection(
                settings = settings,
                viewModel = viewModel,
                onNavigateToSubPage = onNavigateToSubPage,
                onOpenChoiceDialog = { choiceDialogType = it }
            )

            // ===== 分组 7: 关于与系统支持 =====
            AppSettingGroup(title = "关于与支持") {
                AppSettingTile(
                    iconRes = R.drawable.ic_settings,
                    title = "应用版本号",
                    subTitle = "Snippet Studio v1.2.0 (Build 20260728)",
                    iconColor = Color(0xFF424242),
                    iconBgColor = adaptiveIconBg(Color(0xFFf5f5f5), tc.isDark),
                    trailingContent = {
                        Text(text = "v1.2.0", style = CaptionStyle, color = tc.text2)
                    },
                    onClick = { onShowSnackbar(context.getString(R.string.toast_latest_version)) }
                )
                HorizontalDivider(color = tc.line)

                AppSettingTile(
                    iconRes = R.drawable.ic_spark,
                    title = "版本更新日志",
                    subTitle = "查看本次更新特性与重大优化",
                    iconColor = Color(0xFFf57c00),
                    iconBgColor = Color(0xFFfff3e0),
                    onClick = { showChangelogDialog = true }
                )
                HorizontalDivider(color = tc.line)

                AppSettingTile(
                    iconRes = R.drawable.ic_warning,
                    title = "重置为默认偏好",
                    subTitle = "将所有外观、编辑器及交互选项恢复为初始默认值",
                    iconColor = Danger,
                    iconBgColor = Danger.copy(alpha = 0.12f),
                    onClick = { showResetConfirmDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }

    // ===== 单选交互弹窗 =====
    when (choiceDialogType) {
        "card_click" -> {
            SettingChoiceDialog(
                show = true,
                title = "选择卡片默认点击行为",
                options = listOf(
                    SettingChoiceOption("detail", "查看片段详情", "推荐：展示渲染预览、详细元数据与快捷复制功能"),
                    SettingChoiceOption("editor", "直接进入编辑器", "高效：点击代码卡片后直接全屏进入代码编辑模式")
                ),
                selectedKey = settings.cardClickAction,
                onSelect = { viewModel.updateCardClickAction(it) },
                onDismiss = { choiceDialogType = null }
            )
        }
        "share_action" -> {
            SettingChoiceDialog(
                show = true,
                title = "选择系统剪藏接收行为",
                options = listOf(
                    SettingChoiceOption("panel", stringResource(R.string.share_action_panel), "接收分享内容时弹出快速预览与编辑面板"),
                    SettingChoiceOption("silent", stringResource(R.string.share_action_silent), "无感知后台自动解析保存为草稿代码片段")
                ),
                selectedKey = settings.shareAction,
                onSelect = { viewModel.updateShareAction(it) },
                onDismiss = { choiceDialogType = null }
            )
        }
        "tab_size" -> {
            SettingChoiceDialog(
                show = true,
                title = "选择 Tab 键缩进空格数",
                options = listOf(
                    SettingChoiceOption("2", "2 个空格", "紧凑风格（适用于 HTML / JS / Web 开发）"),
                    SettingChoiceOption("4", "4 个空格", "标准风格（适用于 Java / Python / C++ 开发）")
                ),
                selectedKey = settings.tabSize.toString(),
                onSelect = { viewModel.updateTabSize(it.toIntOrNull() ?: 4) },
                onDismiss = { choiceDialogType = null }
            )
        }
        "font_size" -> {
            SettingChoiceDialog(
                show = true,
                title = "选择代码编辑器字号",
                options = listOf(
                    SettingChoiceOption("12.0", "小字号 (12.0 sp)", "适合显示更多行文本"),
                    SettingChoiceOption("13.5", "标准字号 (13.5 sp)", "阅读体验最舒适的默认字号"),
                    SettingChoiceOption("16.0", "大字号 (16.0 sp)", "清晰大字，防疲劳"),
                    SettingChoiceOption("18.0", "特大字号 (18.0 sp)", "高对比大屏展示")
                ),
                selectedKey = settings.editorFontSp.toString(),
                onSelect = { viewModel.updateEditorFontSp(it.toFloatOrNull() ?: 13.5f) },
                onDismiss = { choiceDialogType = null }
            )
        }
    }

    // ===== 恢复默认设置确认对话框 =====
    ConfirmDialog(
        show = showResetConfirmDialog,
        title = "确认重置默认设置？",
        desc = "此操作将把所有界面颜色、代码编辑器偏好及交互选择还原为初始状态，代码片段数据不会被影响。",
        confirmText = "确认重置",
        dismissText = "取消",
        isDanger = true,
        onConfirm = {
            viewModel.resetToDefaults()
            onShowSnackbar(context.getString(R.string.toast_reset_done))
        },
        onDismiss = { showResetConfirmDialog = false }
    )

    // ===== 版本更新日志对话框 =====
    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            title = {
                Text(text = "Snippet Studio 更新日志", style = SectionTitleStyle, color = tc.text)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.S2)
                ) {
                    Text(text = "🎉 v1.2.0 重磅更新：", style = ListTitleStyle, color = tc.primary, fontWeight = FontWeight.Bold)
                    Text(text = "• 🎨 全方位 UI 与动效升级：全新的模块化 Tile、五套质感配色及流畅手感", style = BodyStyle, color = tc.text)
                    Text(text = "• 💻 编辑器偏好全面开放：自由调配字号、软换行、Tab 缩进与符号自动补全", style = BodyStyle, color = tc.text)
                    Text(text = "• 📦 数据全闭包备份：新增从 JSON 备份导入恢复，支持源码 ZIP 包全量打包导出", style = BodyStyle, color = tc.text)
                    Text(text = "• 🔄 JGit 沙盒双向增量同步：更稳定的 Pull/Push 冲突解决与 Local Diff 预览", style = BodyStyle, color = tc.text)
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelogDialog = false }) {
                    Text(text = "知道了", style = ListTitleStyle, color = tc.primary)
                }
            },
            shape = AppShapes.large,
            containerColor = tc.surface
        )
    }
}
