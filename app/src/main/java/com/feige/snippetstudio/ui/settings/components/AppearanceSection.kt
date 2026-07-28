package com.feige.snippetstudio.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.AppSettings
import com.feige.snippetstudio.ui.components.AppSettingGroup
import com.feige.snippetstudio.ui.components.AppSettingSwitchTile
import com.feige.snippetstudio.ui.components.AppSettingTile
import com.feige.snippetstudio.ui.settings.SettingsViewModel
import com.feige.snippetstudio.ui.theme.*

/**
 * [AppearanceSection] 外观与系统交互偏好设置分组组件。
 *
 * 架构职责：
 * 1. 提供全局主题配色方案切换（经典极客蓝、海盐抹茶、高雅紫等）。
 * 2. 控制深色/浅色模式切换与默认样板代码开关。
 * 3. 管理首页卡片点击默认行为、剪贴板分享识别动作及多语言切换。
 *
 * @param settings 系统偏好 [AppSettings]
 * @param viewModel 设置页 ViewModel 控制器
 * @param onNavigateToSubPage 打开子页面闭包
 * @param onOpenChoiceDialog 弹出选择对话框闭包
 * @param modifier 外部 Modifier 修饰符
 */
@Composable
fun AppearanceSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onNavigateToSubPage: (String) -> Unit,
    onOpenChoiceDialog: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    AppSettingGroup(
        title = stringResource(R.string.set_look),
        modifier = modifier
    ) {
        // 1. 配色风格选择
        val colorThemeLabel = ColorThemeStyle.fromId(settings.colorTheme).displayName
        AppSettingTile(
            iconRes = R.drawable.ic_palette,
            title = stringResource(R.string.set_color_theme),
            subTitle = "五套定制主题色调切换",
            iconColor = Color(0xFF4a148c),
            iconBgColor = Color(0xFFf3e5f5),
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = tc.primarySoft,
                        shape = RoundedCornerShape(R_SM)
                    ) {
                        Text(
                            text = colorThemeLabel,
                            style = BadgeStyle,
                            color = tc.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
            onClick = { onNavigateToSubPage("theme") }
        )
        HorizontalDivider(color = tc.line)

        // 2. 深色 / 浅色模式
        val isDarkTheme = (settings.theme == "dark" || (settings.theme == "system" && tc.isDark))
        AppSettingSwitchTile(
            iconRes = if (isDarkTheme) R.drawable.ic_moon else R.drawable.ic_sun,
            title = stringResource(R.string.set_dark),
            subTitle = if (isDarkTheme) "当前已开启夜间深色调" else "当前为日间浅色调",
            checked = isDarkTheme,
            onCheckedChange = { viewModel.toggleDarkMode(it) },
            iconColor = Color(0xFFf57f17),
            iconBgColor = Color(0xFFfffde7)
        )
        HorizontalDivider(color = tc.line)

        // 3. 默认样板代码
        AppSettingSwitchTile(
            iconRes = R.drawable.ic_code,
            title = stringResource(R.string.set_use_boilerplate),
            subTitle = "新建片段时自动填充对应类型的示例代码",
            checked = settings.useBoilerplate,
            onCheckedChange = { viewModel.toggleUseBoilerplate(it) },
            iconColor = Color(0xFF00838f),
            iconBgColor = Color(0xFFe0f7fa)
        )
        HorizontalDivider(color = tc.line)

        // 4. 卡片点击行为
        val cardClickLabel = if (settings.cardClickAction == "editor") "直接进入编辑器" else "查看片段详情"
        AppSettingTile(
            iconRes = R.drawable.ic_touch,
            title = "卡片默认点击行为",
            subTitle = cardClickLabel,
            iconColor = Color(0xFF283593),
            iconBgColor = Color(0xFFe8eaf6),
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cardClickLabel,
                        style = CaptionStyle,
                        color = tc.text2
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
            onClick = { onOpenChoiceDialog("card_click") }
        )
        HorizontalDivider(color = tc.line)

        // 5. 分享动作单选
        val shareActionLabel = if (settings.shareAction == "silent") stringResource(R.string.share_action_silent) else stringResource(R.string.share_action_panel)
        AppSettingTile(
            iconRes = R.drawable.ic_clipboard,
            title = stringResource(R.string.set_share_action),
            subTitle = shareActionLabel,
            iconColor = Color(0xFF00695c),
            iconBgColor = Color(0xFFe0f2f1),
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shareActionLabel,
                        style = CaptionStyle,
                        color = tc.text2
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
            onClick = { onOpenChoiceDialog("share_action") }
        )
        HorizontalDivider(color = tc.line)

        // 6. 多语言选择
        val langLabel = when (settings.lang) {
            "ja" -> stringResource(R.string.lang_ja)
            "en" -> stringResource(R.string.lang_en)
            else -> stringResource(R.string.lang_zh)
        }
        AppSettingTile(
            iconRes = R.drawable.ic_globe,
            title = stringResource(R.string.set_lang),
            subTitle = langLabel,
            iconColor = Color(0xFF1565c0),
            iconBgColor = Color(0xFFe3f2fd),
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = langLabel,
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
            onClick = { onNavigateToSubPage("lang") }
        )
    }
}
