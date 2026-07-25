package com.feige.snippetstudio.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.theme.*

/**
 * [SettingsScreen] 系统设置与系统偏好主界面。
 *
 * 分组构成：
 * 1. **工作区仓库 [Group 1]**：绑定外部 SAF 磁盘目录树。
 * 2. **同步与版本控制 [Group 2]**：Git 账号授权配置与全量同步通道。
 * 3. **内容组织与标签 [Group 3]**：自定义分类与标签管理器。
 * 4. **数据维护与备份 [Group 4]**：导出 JSON 备份文件（调用 SAF CreateDocument Launcher）与回收站清空。
 * 5. **外观与系统偏好 [Group 5]**：深色/浅色模式切换、卡片点击触发动作与多语言选择。
 *
 * @param viewModel 设置页 ViewModel
 * @param onNavigateToSubPage 打开二级设置子页面路由 (如 "git", "repo", "lang")
 * @param onShowSnackbar 显示提示消息闭包
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

    // ===== Android SAF 系统文件创建 Launcher (保存 JSON 全量备份文件) =====
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupJson(context, uri) { success ->
                if (success) {
                    onShowSnackbar(context.getString(R.string.toast_exported))
                } else {
                    onShowSnackbar("Export failed")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = DisplayTitleStyle,
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
            verticalArrangement = Arrangement.spacedBy(Spacing.S5)
        ) {
            // ===== 分组 1: 工作区仓库设置 =====
            SettingsGroup(title = stringResource(R.string.set_repo)) {
                SettingsItem(
                    iconRes = R.drawable.ic_layers,
                    title = stringResource(R.string.set_repo_cur),
                    subTitle = settings.repoPath,
                    onClick = { onNavigateToSubPage("repo") }
                )
            }

            // ===== 分组 2: 同步与 Git 版本控制 =====
            SettingsGroup(title = stringResource(R.string.set_sync)) {
                SettingsItem(
                    iconRes = R.drawable.ic_git,
                    title = stringResource(R.string.set_git),
                    subTitle = if (settings.gitConnected) "已连接 (JGit 真实仓库)" else stringResource(R.string.sub_git_disconnected),
                    onClick = { onNavigateToSubPage("git") }
                )
            }

            // ===== 分组 3: 内容组织与标签 =====
            SettingsGroup(title = stringResource(R.string.set_org)) {
                SettingsItem(
                    iconRes = R.drawable.ic_layers,
                    title = stringResource(R.string.set_cat),
                    onClick = { onNavigateToSubPage("cat") }
                )
                HorizontalDivider(color = tc.line)
                SettingsItem(
                    iconRes = R.drawable.ic_tag,
                    title = stringResource(R.string.set_tags),
                    onClick = { onNavigateToSubPage("tags") }
                )
            }

            // ===== 分组 4: 数据维护与备份备份 =====
            SettingsGroup(title = stringResource(R.string.set_maintain)) {
                SettingsItem(
                    iconRes = R.drawable.ic_spark,
                    title = stringResource(R.string.set_backup),
                    onClick = {
                        createDocLauncher.launch("snippet-studio-backup.json")
                    }
                )
                HorizontalDivider(color = tc.line)
                SettingsItem(
                    iconRes = R.drawable.ic_spark,
                    title = stringResource(R.string.set_trash),
                    onClick = { onNavigateToSubPage("trash") }
                )
            }

            // ===== 分组 5: 外观与多语言偏好设置 =====
            SettingsGroup(title = stringResource(R.string.set_look)) {
                // 配色风格选择入口
                val colorThemeLabel = ColorThemeStyle.fromId(settings.colorTheme).displayName
                SettingsItem(
                    iconRes = R.drawable.ic_spark,
                    title = stringResource(R.string.set_color_theme),
                    subTitle = colorThemeLabel,
                    onClick = { onNavigateToSubPage("theme") }
                )
                HorizontalDivider(color = tc.line)

                // 明暗模式切换
                AppSwitch(
                    checked = (settings.theme == "dark" || (settings.theme == "system" && tc.isDark)),
                    onCheckedChange = { viewModel.toggleDarkMode(it) },
                    label = stringResource(R.string.set_dark)
                )
                HorizontalDivider(color = tc.line)

                AppSwitch(
                    checked = settings.useBoilerplate,
                    onCheckedChange = { viewModel.toggleUseBoilerplate(it) },
                    label = stringResource(R.string.set_use_boilerplate)
                )
                HorizontalDivider(color = tc.line)
                
                val cardClickLabel = if (settings.cardClickAction == "editor") "直接进入编辑器" else "查看片段详情"
                SettingsItem(
                    iconRes = R.drawable.ic_spark,
                    title = "卡片默认点击行为",
                    subTitle = cardClickLabel,
                    onClick = {
                        val next = if (settings.cardClickAction == "editor") "detail" else "editor"
                        viewModel.updateCardClickAction(next)
                    }
                )
                HorizontalDivider(color = tc.line)

                val langLabel = when (settings.lang) {
                    "ja" -> stringResource(R.string.lang_ja)
                    "en" -> stringResource(R.string.lang_en)
                    else -> stringResource(R.string.lang_zh)
                }
                SettingsItem(
                    iconRes = R.drawable.ic_globe,
                    title = stringResource(R.string.set_lang),
                    subTitle = langLabel,
                    onClick = { onNavigateToSubPage("lang") }
                )
            }
        }
    }
}

/**
 * [SettingsGroup] 设置分组外框卡片组件。
 */
@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val tc = LocalThemeColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = CaptionStyle,
            color = tc.text2,
            modifier = Modifier.padding(start = Spacing.S2, bottom = Spacing.S2)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
                .border(1.dp, tc.line, RoundedCornerShape(R_MD)),
            shape = RoundedCornerShape(R_MD),
            color = tc.surface
        ) {
            Column(content = content)
        }
    }
}

/**
 * [SettingsItem] 单个设置跳转条目组件。
 */
@Composable
fun SettingsItem(
    iconRes: Int,
    title: String,
    subTitle: String? = null,
    onClick: () -> Unit
) {
    val tc = LocalThemeColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = tc.primary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(Spacing.S3))

            Column {
                Text(
                    text = title,
                    style = ListTitleStyle,
                    color = tc.text
                )
                if (!subTitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subTitle,
                        style = CaptionStyle,
                        color = tc.text2
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Navigate",
            tint = tc.text2,
            modifier = Modifier.size(20.dp)
        )
    }
}

