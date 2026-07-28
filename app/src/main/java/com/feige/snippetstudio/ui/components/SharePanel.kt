package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.Snippet
import com.feige.snippetstudio.model.SnippetType
import com.feige.snippetstudio.ui.theme.*

/**
 * [SharePanel] 系统分享剪藏快速编辑与卡片预览面板组件 (ModalBottomSheet)。
 *
 * 视觉与交互特性：
 * 1. 当用户从其他应用通过系统 Share Sheet 分享文本或文件时弹出。
 * 2. 带有 Mac 风格经典红黄绿三色控窗按键 Header 结合高质感代码卡片。
 * 3. 允许用户在保存前自定义标题、选择片段语言分类，并开展内容即时预览。
 *
 * @param show 是否显隐控制
 * @param sharedText 接收到的文本数据
 * @param detectedType 智能检测导出的片段类型
 * @param sharedFileName 分享来源文件名（仅文件分享时存在）
 * @param onDismiss 关闭弹窗回调
 * @param onConfirm 保存确认闭包 (title, type)
 * @param onPreview 仅预览闭包
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePanel(
    show: Boolean,
    sharedText: String,
    detectedType: SnippetType,
    sharedFileName: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, type: SnippetType) -> Unit,
    onPreview: ((content: String, type: SnippetType) -> Unit)? = null
) {
    if (!show) return

    val tc = LocalThemeColors.current
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(detectedType) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tc.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.S5, vertical = Spacing.S3)
                .padding(bottom = Spacing.S6),
            verticalArrangement = Arrangement.spacedBy(Spacing.S4)
        ) {
            // 标题栏
            Text(
                text = stringResource(R.string.share_panel_title),
                style = SectionTitleStyle,
                color = tc.text
            )

            // 标题输入框
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.share_panel_name_hint), style = CaptionStyle) },
                placeholder = {
                    Text(
                        text = sharedFileName?.substringBeforeLast('.')
                            ?: stringResource(R.string.share_panel_name_placeholder),
                        style = CaptionStyle
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 类型选择 Chips
            Text(
                text = stringResource(R.string.share_panel_type_label),
                style = CaptionStyle,
                color = tc.text2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
            ) {
                SnippetType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = {
                            Text(
                                text = type.displayName,
                                style = CaptionStyle,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tc.primarySoft,
                            selectedLabelColor = tc.primary
                        )
                    )
                }
            }

            // 分享内容预览 label
            Text(
                text = stringResource(R.string.share_panel_preview_label),
                style = CaptionStyle,
                color = tc.text2
            )

            // 文件分享时显示文件名标签
            if (sharedFileName != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tc.primarySoft
                ) {
                    Text(
                        text = "${stringResource(R.string.share_panel_file_label)}: $sharedFileName",
                        style = CaptionStyle,
                        fontSize = 12.sp,
                        color = tc.primary,
                        modifier = Modifier.padding(horizontal = Spacing.S2, vertical = Spacing.S1)
                    )
                }
            }

            // Mac 风格三色红黄绿按键 Header 结合的高质感代码卡片容器
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .border(1.dp, tc.line, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                color = tc.surface2
            ) {
                Column {
                    // Mac 风格控窗三点
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(tc.line.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(9.dp).background(Color(0xFFFF5F56), CircleShape))
                        Box(modifier = Modifier.size(9.dp).background(Color(0xFFFFBD2E), CircleShape))
                        Box(modifier = Modifier.size(9.dp).background(Color(0xFF27C93F), CircleShape))
                    }

                    Text(
                        text = sharedText,
                        style = CaptionStyle,
                        fontSize = 12.sp,
                        color = tc.text2,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(Spacing.S3)
                    )
                }
            }

            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        style = ListTitleStyle,
                        color = tc.text2
                    )
                }
                if (onPreview != null) {
                    TextButton(
                        onClick = { onPreview(sharedText, selectedType) }
                    ) {
                        Text(
                            text = stringResource(R.string.share_panel_preview),
                            style = ListTitleStyle,
                            color = tc.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Spacing.S3))
                Button(
                    onClick = {
                        val finalTitle = title.ifBlank {
                            sharedFileName?.substringBeforeLast('.')
                                ?: Snippet.generateDefaultTitle(selectedType)
                        }
                        onConfirm(finalTitle, selectedType)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tc.primary)
                ) {
                    Text(
                        text = stringResource(R.string.share_panel_save),
                        style = ListTitleStyle,
                        fontWeight = FontWeight.W600
                    )
                }
            }
        }
    }
}
