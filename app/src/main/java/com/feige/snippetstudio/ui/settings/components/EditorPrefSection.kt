package com.feige.snippetstudio.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.AppSettings
import com.feige.snippetstudio.ui.components.AppSettingGroup
import com.feige.snippetstudio.ui.components.AppSettingSwitchTile
import com.feige.snippetstudio.ui.components.AppSettingTile
import com.feige.snippetstudio.ui.settings.SettingsViewModel
import com.feige.snippetstudio.ui.theme.BadgeStyle
import com.feige.snippetstudio.ui.theme.LocalThemeColors
import com.feige.snippetstudio.ui.theme.Spacing

/**
 * [EditorPrefSection] 代码编辑器偏好配置分组视图。
 *
 * 架构职责：
 * 1. 提供实时 Live 效果预览框 [LivePreviewBox]。
 * 2. 提供编辑器文本字号 (sp)、WordWrap 软换行、行号辅助栏、Tab 空格缩进与括号自动配对设置 Tile 集合。
 *
 * @param settings 系统全局偏好设置 [AppSettings]
 * @param viewModel 设置页 ViewModel
 * @param onOpenChoiceDialog 弹出单选对话框闭包
 * @param modifier 外部 Modifier 修饰符
 */
@Composable
fun EditorPrefSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onOpenChoiceDialog: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    AppSettingGroup(
        title = "代码编辑器偏好",
        modifier = modifier
    ) {
        // 动态 Live 效果预览盒
        LivePreviewBox(
            fontSp = settings.editorFontSp,
            isWordWrap = settings.isWordWrap,
            showLineNumbers = settings.showLineNumbers,
            modifier = Modifier.padding(bottom = Spacing.S3)
        )

        // 1. 编辑器字号选择
        AppSettingTile(
            iconRes = R.drawable.ic_code,
            title = "文本字号大小",
            subTitle = "调整代码编辑与预览字号大小",
            iconColor = tc.primary,
            iconBgColor = tc.primarySoft,
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${settings.editorFontSp} sp",
                        style = BadgeStyle,
                        color = tc.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(Spacing.S2))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = tc.text2.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            onClick = { onOpenChoiceDialog("font_size") }
        )
        HorizontalDivider(color = tc.line)

        // 2. 自动软换行
        AppSettingSwitchTile(
            iconRes = R.drawable.ic_code,
            title = "自动软换行",
            subTitle = "超长代码行自动折行显示",
            checked = settings.isWordWrap,
            onCheckedChange = { viewModel.toggleWordWrap(it) },
            iconColor = tc.primary,
            iconBgColor = tc.primarySoft
        )
        HorizontalDivider(color = tc.line)

        // 3. 显示行号
        AppSettingSwitchTile(
            iconRes = R.drawable.ic_list,
            title = "显示代码行号",
            subTitle = "编辑器左侧渲染行号辅助栏",
            checked = settings.showLineNumbers,
            onCheckedChange = { viewModel.toggleShowLineNumbers(it) },
            iconColor = tc.primary,
            iconBgColor = tc.primarySoft
        )
        HorizontalDivider(color = tc.line)

        // 4. Tab 缩进空格
        AppSettingTile(
            iconRes = R.drawable.ic_code,
            title = "Tab 键缩进空格",
            subTitle = "按下 Tab 键输入的空格数量",
            iconColor = tc.primary,
            iconBgColor = tc.primarySoft,
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${settings.tabSize} 空格",
                        style = BadgeStyle,
                        color = tc.primary
                    )
                    Spacer(modifier = Modifier.width(Spacing.S2))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = tc.text2.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            onClick = { onOpenChoiceDialog("tab_size") }
        )
        HorizontalDivider(color = tc.line)

        // 5. 符号自动补全
        AppSettingSwitchTile(
            iconRes = R.drawable.ic_code,
            title = "括号与引号自动配对",
            subTitle = "输入 {, (, [, \" 时自动补全对应右符号",
            checked = settings.autoPairBrackets,
            onCheckedChange = { viewModel.toggleAutoPairBrackets(it) },
            iconColor = tc.primary,
            iconBgColor = tc.primarySoft
        )
    }
}
