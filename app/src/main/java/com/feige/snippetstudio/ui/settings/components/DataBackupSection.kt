package com.feige.snippetstudio.ui.settings.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.AppSettingGroup
import com.feige.snippetstudio.ui.components.AppSettingTile
import com.feige.snippetstudio.ui.theme.LocalThemeColors

/**
 * [DataBackupSection] 数据维护与全量备份设置分组组件。
 *
 * 架构职责：
 * 1. 导出全量代码片段为 JSON 备份文件。
 * 2. 从已有的 JSON 备份中还原恢复片段数据。
 * 3. 导出项目全量代码结构的 ZIP 压缩归档包。
 * 4. 提供回收站软删除数据管理入口。
 *
 * @param onExportJson 触发 SAF 导出 JSON 闭包
 * @param onImportJson 触发 SAF 导入 JSON 闭包
 * @param onExportZip 触发 ZIP 打包导出闭包
 * @param onNavigateToTrash 跳转回收站路由闭包
 * @param modifier 外部 Modifier 修饰符
 */
@Composable
fun DataBackupSection(
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onExportZip: () -> Unit,
    onNavigateToTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    AppSettingGroup(
        title = stringResource(R.string.set_maintain),
        modifier = modifier
    ) {
        // 1. 导出 JSON 备份
        AppSettingTile(
            iconRes = R.drawable.ic_download,
            title = stringResource(R.string.set_backup),
            subTitle = "导出全量代码片段为 JSON 备份文件",
            iconColor = Color(0xFFef6c00),
            iconBgColor = Color(0xFFfff3e0),
            onClick = onExportJson
        )
        HorizontalDivider(color = tc.line)

        // 2. 恢复导入 JSON 备份
        AppSettingTile(
            iconRes = R.drawable.ic_restore,
            title = "恢复导入 JSON 备份",
            subTitle = "从已有的 JSON 备份文件中恢复合并片段",
            iconColor = Color(0xFFd84315),
            iconBgColor = Color(0xFFfbe9e7),
            onClick = onImportJson
        )
        HorizontalDivider(color = tc.line)

        // 3. 导出 ZIP 物理归档包
        AppSettingTile(
            iconRes = R.drawable.ic_download,
            title = "导出 ZIP 源码压缩包",
            subTitle = "按文件夹树结构打包导出源码归档",
            iconColor = Color(0xFFd84315),
            iconBgColor = Color(0xFFfbe9e7),
            onClick = onExportZip
        )
        HorizontalDivider(color = tc.line)

        // 4. 回收站管理
        AppSettingTile(
            iconRes = R.drawable.ic_trash,
            title = stringResource(R.string.set_trash),
            subTitle = "管理已软删除的代码片段与废弃数据",
            iconColor = Color(0xFFc62828),
            iconBgColor = Color(0xFFffebee),
            onClick = onNavigateToTrash
        )
    }
}
