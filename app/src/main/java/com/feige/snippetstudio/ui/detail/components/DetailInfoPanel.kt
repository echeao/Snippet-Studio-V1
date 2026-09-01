package com.feige.snippetstudio.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * 2. 带有语义化矢量图标，规整对齐。
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
    val tc = LocalThemeColors.current

    DetailPanel(
        title = stringResource(R.string.detail_info),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.S3)) {
            // 文件名
            InfoRow(
                iconRes = R.drawable.ic_code,
                label = stringResource(R.string.detail_filename),
                value = snippet.fileName
            )

            // 所属文件夹（带点击高亮胶囊）
            InfoRow(
                iconRes = R.drawable.ic_folder,
                label = "所属文件夹",
                value = if (snippet.folder.isBlank()) "/ (根目录)" else snippet.folder,
                onClick = onFolderClick
            )

            // 文件大小
            InfoRow(
                iconRes = R.drawable.ic_layers,
                label = stringResource(R.string.detail_size),
                value = SizeUtil.formatBytes(snippet.sizeBytes)
            )

            // 存储路径
            InfoRow(
                iconRes = R.drawable.ic_tree,
                label = stringResource(R.string.detail_path),
                value = if (snippet.folder.isBlank()) "snippets/${snippet.fileName}" else "snippets/${snippet.folder}/${snippet.fileName}"
            )

            HorizontalDivider(color = tc.line.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))

            // 最后更新时间
            InfoRow(
                iconRes = R.drawable.ic_refresh,
                label = stringResource(R.string.detail_updated),
                value = TimeUtil.formatFullDateTime(snippet.updatedAt)
            )

            // Git 仓状态
            InfoRow(
                iconRes = R.drawable.ic_git,
                label = stringResource(R.string.detail_git_status),
                value = stringResource(R.string.detail_git_status_val)
            )
        }
    }
}

/**
 * [DetailPanel] 详情页通用带柔和环境光与微边框卡片基类组件。
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
            .shadow(AppElevation.Sm, RoundedCornerShape(R_LG), ambientColor = AppElevation.SmColor)
            .border(1.dp, tc.line.copy(alpha = 0.85f), RoundedCornerShape(R_LG)),
        shape = RoundedCornerShape(R_LG),
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
 * @param iconRes 属性左侧语义图标 ID（可选）
 * @param label 属性名描述文案
 * @param value 属性值字符串
 * @param onClick 如果非空，则点击整行会触发此闭包（常用于属性可被编辑的场景）
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    iconRes: Int? = null,
    onClick: (() -> Unit)? = null
) {
    val tc = LocalThemeColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = tc.text3,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = BodyStyle.copy(fontSize = 13.5.sp),
                color = tc.text2
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onClick != null) {
                Surface(
                    color = tc.primarySoft.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(R_SM)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = value,
                            style = CaptionStyle,
                            color = tc.primary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "编辑",
                            tint = tc.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = value,
                    style = BodyStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal),
                    color = tc.text,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
