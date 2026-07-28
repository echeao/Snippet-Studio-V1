package com.feige.snippetstudio.ui.detail.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.SizeUtil
import com.feige.snippetstudio.util.TimeUtil

/**
 * [DetailInfoPanel] 代码片段元数据与文件系统属性查看面板。
 *
 * 职责：
 * 1. 结构化展示文件名、所在文件夹路径、占据字节大小、本地逻辑存储相对路径。
 * 2. 展示最后更新时间与 Git 存储库同步状态。
 * 3. 响应文件夹修改快捷触发动作。
 *
 * @param snippet 代码片段领域模型
 * @param onFolderClick 触发文件夹移动/修改弹窗回调
 * @param modifier 外部布局修饰符
 */
@Composable
fun DetailInfoPanel(
    snippet: Snippet,
    onFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DetailPanel(
        title = stringResource(R.string.detail_info),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.S3)) {
            InfoRow(
                label = stringResource(R.string.detail_filename),
                value = snippet.fileName
            )
            InfoRow(
                label = "所属文件夹",
                value = if (snippet.folder.isBlank()) "/ (根目录)" else snippet.folder,
                onClick = onFolderClick
            )
            InfoRow(
                label = stringResource(R.string.detail_size),
                value = SizeUtil.formatBytes(snippet.sizeBytes)
            )
            InfoRow(
                label = stringResource(R.string.detail_path),
                value = if (snippet.folder.isBlank()) "snippets/${snippet.fileName}" else "snippets/${snippet.folder}/${snippet.fileName}"
            )
            InfoRow(
                label = stringResource(R.string.detail_updated),
                value = TimeUtil.formatFullDateTime(snippet.updatedAt)
            )
            InfoRow(
                label = stringResource(R.string.detail_git_status),
                value = stringResource(R.string.detail_git_status_val)
            )
        }
    }
}

/**
 * [DetailPanel] 详情页通用带阴影边框卡片基类组件。
 *
 * @param title 面板标题
 * @param modifier 外部布局修饰符
 * @param headerAction 标题栏右侧自定义动作组件
 * @param content 面板内部正文内容
 */
@Composable
fun DetailPanel(
    title: String,
    modifier: Modifier = Modifier,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val tc = LocalThemeColors.current

    Surface(
        modifier = modifier
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
 * [InfoRow] 详情信息单行属性键值对展示组件。
 *
 * @param label 属性名描述文案
 * @param value 属性值字符串
 * @param onClick 如果非空，则点击整行会触发此闭包（常用于属性可被编辑的场景）
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
                    contentDescription = "编辑",
                    tint = tc.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
